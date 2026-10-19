import { createClient } from "https://esm.sh/@supabase/supabase-js@2";

const corsHeaders = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers": "authorization, apikey, content-type",
  "Access-Control-Allow-Methods": "POST, OPTIONS",
};

Deno.serve(async (request) => {
  if (request.method === "OPTIONS") return new Response("ok", { headers: corsHeaders });
  if (request.method !== "POST") return json({ error: "Method not allowed" }, 405);

  const token = request.headers.get("Authorization")?.replace(/^Bearer\s+/i, "");
  if (!token) return json({ error: "Tidak terautentikasi" }, 401);

  const url = Deno.env.get("SUPABASE_URL")!;
  const serviceKey = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!;
  const anonKey = Deno.env.get("SUPABASE_ANON_KEY")!;
  const service = createClient(url, serviceKey, { auth: { persistSession: false } });
  const callerClient = createClient(url, anonKey, {
    auth: { persistSession: false },
    global: { headers: { Authorization: `Bearer ${token}` } },
  });

  const { data: { user }, error: authError } = await callerClient.auth.getUser(token);
  if (authError || !user) return json({ error: "Tidak terautentikasi" }, 401);
  const { data: caller } = await service.from("profiles")
    .select("id, role, store_id, is_active").eq("id", user.id).maybeSingle();
  if (!caller?.is_active) return json({ error: "Akun nonaktif" }, 403);
  if (!["admin", "super_admin"].includes(caller.role)) return json({ error: "Akses ditolak" }, 403);

  let userId: string;
  try {
    userId = (await request.json()).user_id;
  } catch {
    return json({ error: "Body tidak valid" }, 400);
  }
  if (!userId) return json({ error: "user_id wajib diisi" }, 400);
  if (userId === caller.id) return json({ error: "Tidak dapat menghapus akun sendiri" }, 400);

  const { data: target } = await service.from("profiles")
    .select("id, role, store_id").eq("id", userId).maybeSingle();
  if (!target) return json({ error: "User tidak ditemukan" }, 404);
  if (caller.role === "admin" &&
      (target.store_id !== caller.store_id || !["admin", "kasir"].includes(target.role))) {
    return json({ error: "Akses ditolak untuk user ini" }, 403);
  }

  const { error } = await service.auth.admin.deleteUser(userId);
  if (error) return json({ error: error.message }, 400);
  return json({ ok: true }, 200);
});

function json(body: unknown, status: number) {
  return new Response(JSON.stringify(body), {
    status,
    headers: { ...corsHeaders, "Content-Type": "application/json" },
  });
}
