package com.rafidalhaque.emailreader

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EmlParserTest {
    @Test
    fun parsesPlainTextEmail() {
        val eml = """
            From: sender@example.com
            To: recipient@example.com
            Subject: Hello
            Date: Tue, 01 Jan 2025 10:00:00 +0000
            Content-Type: text/plain; charset=UTF-8
            Content-Transfer-Encoding: 7bit

            This is a sample email body.
        """.trimIndent()

        val result = EmlParser.parse(eml)

        assertEquals("Hello", result.subject)
        assertEquals("sender@example.com", result.from)
        assertEquals("recipient@example.com", result.to)
        assertTrue(result.body.contains("sample email body"))
    }

    @Test
    fun parsesMultipartEmailBody() {
        val eml = """
            From: sender@example.com
            To: recipient@example.com
            Subject: Multipart
            Content-Type: multipart/alternative; boundary="abc123"

            --abc123
            Content-Type: text/plain; charset=UTF-8

            Plain text section
            --abc123
            Content-Type: text/html; charset=UTF-8

            <p>HTML section</p>
            --abc123--
        """.trimIndent()

        val result = EmlParser.parse(eml)

        assertEquals("Multipart", result.subject)
        assertEquals("Plain text section", result.body.trim())
    }
}
