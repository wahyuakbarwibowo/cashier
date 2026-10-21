package com.wahyuakbarwibowo.aminmartkasir.data.repository

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.wahyuakbarwibowo.aminmartkasir.data.local.entity.BackupData
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.lang.reflect.Modifier
import java.lang.reflect.ParameterizedType
import java.nio.charset.StandardCharsets
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import javax.xml.XMLConstants
import javax.xml.parsers.DocumentBuilderFactory
import org.w3c.dom.Element

/** Spreadsheet format for the same 19 entity groups used by JSON backup. */
object ExcelBackupCodec {
    private const val MAIN_NS = "http://schemas.openxmlformats.org/spreadsheetml/2006/main"
    private const val REL_NS = "http://schemas.openxmlformats.org/officeDocument/2006/relationships"
    private const val MAX_XML_BYTES = 64 * 1024 * 1024

    private data class Sheet(val name: String, val columns: List<String>, val single: Boolean)

    private val sheets = BackupData::class.java.declaredFields
        .filter { !Modifier.isStatic(it.modifiers) && !it.isSynthetic }
        .sortedBy { it.name }
        .map { field ->
            val single = field.type != List::class.java
            val entityType = if (single) field.type else
                (field.genericType as ParameterizedType).actualTypeArguments[0] as Class<*>
            Sheet(
                field.name,
                entityType.declaredFields.filter { !Modifier.isStatic(it.modifiers) && !it.isSynthetic }
                    .map { it.name },
                single
            )
        }

    fun sheetNames(): List<String> = sheets.map { it.name }

    fun write(output: OutputStream, backupJson: String? = null) {
        val data = backupJson?.let { JsonParser.parseString(it).asJsonObject }
        ZipOutputStream(output).use { zip ->
            zip.entry("[Content_Types].xml", contentTypes())
            zip.entry("_rels/.rels", """
                <?xml version="1.0" encoding="UTF-8"?>
                <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
                <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>
                </Relationships>
            """.trimIndent())
            zip.entry("xl/workbook.xml", workbook())
            zip.entry("xl/_rels/workbook.xml.rels", workbookRelationships())
            zip.entry("xl/styles.xml", styles())
            sheets.forEachIndexed { index, sheet ->
                val value = data?.get(sheet.name)
                val rows = when {
                    value == null || value.isJsonNull -> emptyList()
                    sheet.single -> listOf(value.asJsonObject)
                    else -> value.asJsonArray.map { it.asJsonObject }
                }
                zip.putNextEntry(ZipEntry("xl/worksheets/sheet${index + 1}.xml"))
                zip.writeUtf8("<?xml version=\"1.0\" encoding=\"UTF-8\"?><worksheet xmlns=\"$MAIN_NS\"><cols><col min=\"1\" max=\"${sheet.columns.size}\" width=\"18\" customWidth=\"1\" style=\"1\"/></cols><sheetData>")
                zip.row(1, sheet.columns)
                rows.forEachIndexed { rowIndex, row ->
                    zip.row(rowIndex + 2, sheet.columns.map { key ->
                        row.get(key)?.takeUnless { it.isJsonNull }?.asString
                    })
                }
                zip.writeUtf8("</sheetData></worksheet>")
                zip.closeEntry()
            }
        }
    }

    fun read(input: InputStream): String {
        val entries = mutableMapOf<String, ByteArray>()
        var total = 0
        var entryCount = 0
        ZipInputStream(input).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                require(++entryCount <= 128) { "File Excel memiliki terlalu banyak bagian." }
                if (!entry.isDirectory && (entry.name.endsWith(".xml") || entry.name.endsWith(".rels"))) {
                    val bytes = ByteArrayOutputStream()
                    val buffer = ByteArray(8192)
                    while (true) {
                        val count = zip.read(buffer)
                        if (count < 0) break
                        total += count
                        require(total <= MAX_XML_BYTES) { "File Excel terlalu besar." }
                        bytes.write(buffer, 0, count)
                    }
                    entries[entry.name] = bytes.toByteArray()
                }
                zip.closeEntry()
            }
        }
        val workbook = parse(entries["xl/workbook.xml"] ?: error("Workbook Excel tidak ditemukan."))
        val relationships = parse(entries["xl/_rels/workbook.xml.rels"] ?: error("Relasi workbook tidak ditemukan."))
        val paths = relationships.getElementsByTagNameNS("*", "Relationship")
            .asElements().associate { it.getAttribute("Id") to it.getAttribute("Target") }
        val shared = entries["xl/sharedStrings.xml"]?.let { bytes ->
            parse(bytes).getElementsByTagNameNS(MAIN_NS, "si").asElements().map { it.textParts() }
        } ?: emptyList()
        val workbookSheets = workbook.getElementsByTagNameNS(MAIN_NS, "sheet")
            .asElements().associate { it.getAttribute("name") to it.getAttributeNS(REL_NS, "id") }
        require(sheets.all { it.name in workbookSheets }) { "Sheet cadangan tidak lengkap. Gunakan template dari aplikasi." }

        val data = JsonObject()
        var recordCount = 0
        sheets.forEach { sheet ->
            val target = paths[workbookSheets.getValue(sheet.name)] ?: error("Sheet ${sheet.name} rusak.")
            val path = when {
                target.startsWith("/") -> target.removePrefix("/")
                target.startsWith("xl/") -> target
                else -> "xl/$target"
            }
            require(!path.contains("..")) { "Path sheet tidak valid." }
            val document = parse(entries[path] ?: error("Sheet ${sheet.name} tidak ditemukan."))
            val rows = document.getElementsByTagNameNS(MAIN_NS, "row").asElements()
            require(rows.isNotEmpty()) { "Header sheet ${sheet.name} tidak ditemukan." }
            val headers = rows.first().cells(shared)
            require(headers.values.toSet() == sheet.columns.toSet() && headers.size == sheet.columns.size) {
                "Kolom sheet ${sheet.name} tidak sesuai template."
            }
            val values = JsonArray()
            rows.drop(1).forEach { row ->
                val cells = row.cells(shared)
                if (cells.values.all { it.isBlank() }) return@forEach
                val item = JsonObject()
                cells.forEach { (index, value) ->
                    val column = headers[index] ?: error("Kolom tambahan pada sheet ${sheet.name}.")
                    if (value.isNotEmpty()) item.addProperty(column, value)
                    else item.addProperty(column, "")
                }
                values.add(item)
                recordCount++
            }
            if (sheet.single) {
                require(values.size() <= 1) { "Sheet ${sheet.name} hanya boleh berisi satu baris data." }
                values.firstOrNull()?.let { data.add(sheet.name, it) }
            } else {
                data.add(sheet.name, values)
            }
        }
        require(recordCount > 0) { "Template masih kosong; isi data sebelum impor." }
        return data.toString()
    }

    private fun contentTypes() = buildString {
        append("<?xml version=\"1.0\" encoding=\"UTF-8\"?><Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\">")
        append("<Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/>")
        append("<Default Extension=\"xml\" ContentType=\"application/xml\"/>")
        append("<Override PartName=\"/xl/workbook.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml\"/>")
        append("<Override PartName=\"/xl/styles.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml\"/>")
        sheets.indices.forEach { index ->
            append("<Override PartName=\"/xl/worksheets/sheet${index + 1}.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/>")
        }
        append("</Types>")
    }

    private fun workbook() = buildString {
        append("<?xml version=\"1.0\" encoding=\"UTF-8\"?><workbook xmlns=\"$MAIN_NS\" xmlns:r=\"$REL_NS\"><sheets>")
        sheets.forEachIndexed { index, sheet ->
            append("<sheet name=\"${escape(sheet.name)}\" sheetId=\"${index + 1}\" r:id=\"rId${index + 1}\"/>")
        }
        append("</sheets></workbook>")
    }

    private fun workbookRelationships() = buildString {
        append("<?xml version=\"1.0\" encoding=\"UTF-8\"?><Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">")
        sheets.indices.forEach { index ->
            append("<Relationship Id=\"rId${index + 1}\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet\" Target=\"worksheets/sheet${index + 1}.xml\"/>")
        }
        append("<Relationship Id=\"rId${sheets.size + 1}\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles\" Target=\"styles.xml\"/>")
        append("</Relationships>")
    }

    private fun styles() = """
        <?xml version="1.0" encoding="UTF-8"?>
        <styleSheet xmlns="$MAIN_NS">
        <fonts count="1"><font><sz val="11"/><name val="Calibri"/></font></fonts>
        <fills count="2"><fill><patternFill patternType="none"/></fill><fill><patternFill patternType="gray125"/></fill></fills>
        <borders count="1"><border/></borders>
        <cellStyleXfs count="1"><xf numFmtId="0" fontId="0" fillId="0" borderId="0"/></cellStyleXfs>
        <cellXfs count="2"><xf numFmtId="0" fontId="0" fillId="0" borderId="0"/><xf numFmtId="49" fontId="0" fillId="0" borderId="0" applyNumberFormat="1"/></cellXfs>
        </styleSheet>
    """.trimIndent()

    private fun ZipOutputStream.entry(name: String, value: String) {
        putNextEntry(ZipEntry(name))
        writeUtf8(value)
        closeEntry()
    }

    private fun ZipOutputStream.row(number: Int, values: List<String?>) {
        writeUtf8("<row r=\"$number\">")
        values.forEachIndexed { index, value ->
            if (value != null) {
                writeUtf8("<c r=\"${columnName(index)}$number\" s=\"1\" t=\"inlineStr\"><is><t xml:space=\"preserve\">${escape(value)}</t></is></c>")
            }
        }
        writeUtf8("</row>")
    }

    private fun ZipOutputStream.writeUtf8(value: String) = write(value.toByteArray(StandardCharsets.UTF_8))

    private fun columnName(index: Int): String {
        var value = index + 1
        val result = StringBuilder()
        while (value > 0) {
            value--
            result.insert(0, ('A'.code + value % 26).toChar())
            value /= 26
        }
        return result.toString()
    }

    private fun escape(value: String): String {
        require(value.none { it.code < 32 && it != '\n' && it != '\r' && it != '\t' }) {
            "Data mengandung karakter yang tidak didukung Excel. Gunakan backup JSON."
        }
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
            .replace("\"", "&quot;").replace("'", "&apos;")
    }

    private fun parse(bytes: ByteArray) = DocumentBuilderFactory.newInstance().apply {
        isNamespaceAware = true
        setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true)
        setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
        isXIncludeAware = false
        isExpandEntityReferences = false
    }.newDocumentBuilder().parse(ByteArrayInputStream(bytes))

    private fun org.w3c.dom.NodeList.asElements(): List<Element> =
        (0 until length).mapNotNull { item(it) as? Element }

    private fun Element.textParts(): String = getElementsByTagNameNS(MAIN_NS, "t")
        .asElements().joinToString("") { it.textContent }

    private fun Element.cells(shared: List<String>): Map<Int, String> =
        getElementsByTagNameNS(MAIN_NS, "c").asElements().associate { cell ->
            require(cell.getElementsByTagNameNS(MAIN_NS, "f").length == 0) {
                "Formula Excel tidak didukung saat impor."
            }
            val letters = cell.getAttribute("r").takeWhile { it.isLetter() }
            require(letters.isNotEmpty()) { "Posisi kolom Excel tidak valid." }
            val index = letters.fold(0) { total, char -> total * 26 + (char.uppercaseChar() - 'A' + 1) } - 1
            val type = cell.getAttribute("t")
            val raw = if (type == "inlineStr") cell.textParts() else
                cell.getElementsByTagNameNS(MAIN_NS, "v").item(0)?.textContent.orEmpty()
            val value = when (type) {
                "s" -> shared.getOrNull(raw.toIntOrNull() ?: -1) ?: error("Teks bersama Excel tidak valid.")
                "b" -> if (raw == "1") "true" else "false"
                "e" -> error("Sel Excel berisi error.")
                else -> raw
            }
            index to value
        }
}
