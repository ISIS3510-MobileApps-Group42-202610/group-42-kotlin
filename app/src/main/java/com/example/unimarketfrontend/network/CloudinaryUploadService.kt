package com.example.unimarketfrontend.network

import android.content.ContentResolver
import android.net.Uri
import com.example.unimarketfrontend.network.model.CloudinarySignatureResponse
import org.json.JSONObject
import java.io.DataOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID

object CloudinaryUploadService {

    fun uploadImage(
        contentResolver: ContentResolver,
        imageUri: Uri,
        signature: CloudinarySignatureResponse
    ): String {
        val imageBytes = contentResolver.openInputStream(imageUri)?.use { it.readBytes() }
            ?: throw IllegalArgumentException("No se pudo leer la imagen seleccionada")

        val mimeType = contentResolver.getType(imageUri) ?: "image/jpeg"
        val fileExtension = mimeType.substringAfter('/', "jpg")
        val fileName = "listing_${System.currentTimeMillis()}.$fileExtension"

        val endpoint = "https://api.cloudinary.com/v1_1/${signature.cloudName}/image/upload"
        val boundary = "Boundary-${UUID.randomUUID()}"

        val connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            doInput = true
            doOutput = true
            useCaches = false
            setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
        }

        try {
            DataOutputStream(connection.outputStream).use { out ->
                writeTextPart(out, boundary, "api_key", signature.apiKey)
                writeTextPart(out, boundary, "timestamp", signature.timestamp.toString())
                writeTextPart(out, boundary, "signature", signature.signature)
                if (!signature.folder.isNullOrBlank()) {
                    writeTextPart(out, boundary, "folder", signature.folder)
                }
                writeFilePart(out, boundary, "file", fileName, mimeType, imageBytes)
                out.writeBytes("--$boundary--\r\n")
                out.flush()
            }

            val responseCode = connection.responseCode
            val responseText = if (responseCode in 200..299) {
                connection.inputStream.bufferedReader().use { it.readText() }
            } else {
                connection.errorStream?.bufferedReader()?.use { it.readText() }.orEmpty()
            }

            if (responseCode !in 200..299) {
                throw IllegalStateException("Cloudinary upload fallo (HTTP $responseCode): $responseText")
            }

            val json = JSONObject(responseText)
            return json.optString("secure_url").takeIf { it.isNotBlank() }
                ?: throw IllegalStateException("Cloudinary no devolvio secure_url")
        } finally {
            connection.disconnect()
        }
    }

    private fun writeTextPart(out: DataOutputStream, boundary: String, name: String, value: String) {
        out.writeBytes("--$boundary\r\n")
        out.writeBytes("Content-Disposition: form-data; name=\"$name\"\r\n\r\n")
        out.writeBytes(value)
        out.writeBytes("\r\n")
    }

    private fun writeFilePart(
        out: DataOutputStream,
        boundary: String,
        name: String,
        fileName: String,
        mimeType: String,
        data: ByteArray
    ) {
        out.writeBytes("--$boundary\r\n")
        out.writeBytes("Content-Disposition: form-data; name=\"$name\"; filename=\"$fileName\"\r\n")
        out.writeBytes("Content-Type: $mimeType\r\n\r\n")
        out.write(data)
        out.writeBytes("\r\n")
    }
}
