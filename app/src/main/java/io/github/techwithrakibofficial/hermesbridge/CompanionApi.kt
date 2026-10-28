package io.github.techwithrakibofficial.hermesbridge

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.Proxy
import java.net.URL

class CompanionApi {
    class Rejected(val code: Int, message: String) : Exception(message)
    fun call(token: String, path: String, body: JSONObject? = null): JSONObject {
        require(token.matches(Regex("[A-Za-z0-9_-]{43}"))) { "Start and pair the Android bridge first" }
        require(path.matches(Regex("/v1/(health|jobs(?:/[a-f0-9-]{32,36}(?:/(?:cancel|approval))?)?)")))
        val connection = URL("http://127.0.0.1:18766$path").openConnection(Proxy.NO_PROXY) as HttpURLConnection
        try {
            connection.connectTimeout = 3000
            connection.readTimeout = 6000
            connection.instanceFollowRedirects = false
            connection.useCaches = false
            connection.setRequestProperty("Authorization", "Bearer $token")
            if (body != null) {
                val bytes = body.toString().toByteArray(Charsets.UTF_8)
                require(bytes.size <= 32768) { "Message too large" }
                connection.requestMethod = "POST"
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", "application/json")
                connection.setFixedLengthStreamingMode(bytes.size)
                connection.outputStream.use { it.write(bytes) }
            }
            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val bytes = stream?.use { input ->
                val output = java.io.ByteArrayOutputStream()
                val buffer = ByteArray(4096)
                while (output.size() <= 131072) {
                    val count = input.read(buffer, 0, minOf(buffer.size, 131073 - output.size()))
                    if (count < 0) break
                    output.write(buffer, 0, count)
                }
                output.toByteArray()
            } ?: byteArrayOf()
            if (bytes.size > 131072) throw IllegalStateException("Companion response too large; do not resend a task")
            val value = try { JSONObject(String(bytes, Charsets.UTF_8)) } catch (_: Exception) {
                throw IllegalStateException("Companion response interrupted; reconnect to check the same task")
            }
            if (code !in 200..299) throw Rejected(code, value.optString("error", "Companion HTTP $code"))
            return value
        } finally { connection.disconnect() }
    }
}
