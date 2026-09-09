package com.rafidalhaque.emailreader

import java.util.Base64

data class EmlMessage(
    val subject: String,
    val from: String,
    val to: String,
    val date: String,
    val body: String
)

object EmlParser {
    fun parse(rawEml: String): EmlMessage {
        val normalized = rawEml.replace("\r\n", "\n")
        val headerBodySplit = normalized.split("\n\n", limit = 2)
        require(headerBodySplit.isNotEmpty()) { "Invalid EML format." }

        val headers = parseHeaders(headerBodySplit[0])
        val bodySection = headerBodySplit.getOrElse(1) { "" }

        val parsedBody = extractBody(headers, bodySection).trim()

        return EmlMessage(
            subject = headers["Subject"].orEmpty(),
            from = headers["From"].orEmpty(),
            to = headers["To"].orEmpty(),
            date = headers["Date"].orEmpty(),
            body = parsedBody
        )
    }

    private fun parseHeaders(rawHeaders: String): Map<String, String> {
        val lines = rawHeaders.lines()
        val merged = mutableListOf<String>()

        for (line in lines) {
            if ((line.startsWith(" ") || line.startsWith("\t")) && merged.isNotEmpty()) {
                merged[merged.lastIndex] += " ${line.trim()}"
            } else {
                merged.add(line)
            }
        }

        return buildMap {
            merged.forEach { headerLine ->
                val index = headerLine.indexOf(':')
                if (index > 0) {
                    val name = headerLine.substring(0, index).trim()
                    val value = headerLine.substring(index + 1).trim()
                    put(name, decodeEncodedWords(value))
                }
            }
        }
    }

    private fun extractBody(headers: Map<String, String>, rawBody: String): String {
        val contentType = headers["Content-Type"].orEmpty()
        val encoding = headers["Content-Transfer-Encoding"].orEmpty().lowercase()

        return if (contentType.contains("multipart", ignoreCase = true)) {
            parseMultipartBody(contentType, rawBody)
        } else {
            decodeTransferEncoding(rawBody, encoding)
        }
    }

    private fun parseMultipartBody(contentTypeHeader: String, rawBody: String): String {
        val boundary = Regex("boundary=\\\"?([^\\\";]+)\\\"?", RegexOption.IGNORE_CASE)
            .find(contentTypeHeader)
            ?.groupValues
            ?.getOrNull(1)
            ?.trim()
            ?: return rawBody

        val parts = rawBody.split("--$boundary")
            .map { it.trim() }
            .filter { it.isNotBlank() && it != "--" }

        for (part in parts) {
            val sections = part.split("\n\n", limit = 2)
            if (sections.size < 2) continue

            val partHeaders = parseHeaders(sections[0])
            val partContentType = partHeaders["Content-Type"].orEmpty()
            if (partContentType.contains("text/plain", ignoreCase = true)) {
                val encoding = partHeaders["Content-Transfer-Encoding"].orEmpty().lowercase()
                return decodeTransferEncoding(sections[1], encoding)
            }
        }

        return rawBody
    }

    private fun decodeTransferEncoding(content: String, encoding: String): String {
        return when {
            encoding.contains("base64") -> runCatching {
                String(Base64.getMimeDecoder().decode(content.trim()))
            }.getOrElse { content }

            encoding.contains("quoted-printable") -> decodeQuotedPrintable(content)
            else -> content
        }
    }

    private fun decodeQuotedPrintable(input: String): String {
        val out = StringBuilder()
        var i = 0
        while (i < input.length) {
            val c = input[i]
            when {
                c == '=' && i + 2 < input.length -> {
                    val hex = input.substring(i + 1, i + 3)
                    if (hex == "\n" || hex == "\r\n") {
                        i += if (hex == "\n") 2 else 3
                    } else {
                        val decoded = hex.toIntOrNull(16)
                        if (decoded != null) {
                            out.append(decoded.toChar())
                            i += 3
                        } else {
                            out.append(c)
                            i++
                        }
                    }
                }

                else -> {
                    out.append(c)
                    i++
                }
            }
        }
        return out.toString()
    }

    private fun decodeEncodedWords(value: String): String {
        val encodedWordRegex = Regex("=\\?([^?]+)\\?([BbQq])\\?([^?]+)\\?=")
        return encodedWordRegex.replace(value) { match ->
            val charset = match.groupValues[1]
            val encoding = match.groupValues[2]
            val encoded = match.groupValues[3]

            val bytes = when {
                encoding.equals("B", ignoreCase = true) -> Base64.getDecoder().decode(encoded)
                else -> decodeQuotedPrintable(encoded.replace('_', ' ')).toByteArray()
            }

            runCatching { String(bytes, charset(charset)) }.getOrDefault(match.value)
        }
    }
}
