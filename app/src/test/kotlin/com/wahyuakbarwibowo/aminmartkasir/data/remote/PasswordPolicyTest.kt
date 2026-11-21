package com.wahyuakbarwibowo.aminmartkasir.data.remote

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PasswordPolicyTest {
    @Test
    fun `password minimal 8 karakter dengan huruf dan angka`() {
        assertTrue(AuthManager.isPasswordStrong("kasir123"))
        assertFalse(AuthManager.isPasswordStrong("kas123"))     // terlalu pendek
        assertFalse(AuthManager.isPasswordStrong("kasirkasir")) // tanpa angka
        assertFalse(AuthManager.isPasswordStrong("12345678"))   // tanpa huruf
    }
}
