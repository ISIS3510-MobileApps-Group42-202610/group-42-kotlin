package com.example.unimarketfrontend.network.external

import android.content.ContentResolver
import android.net.Uri
import com.example.unimarketfrontend.model.uploads.CloudinarySignatureResponse
import org.json.JSONObject
import java.io.DataOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID

object CloudinaryUploadService {

    /**
     * Carga una imagen a Cloudinary usando autenticación firmada.
     *
     * @param contentResolver Resolvedor para acceder al contenido del dispositivo
     * @param imageUri URI de la imagen seleccionada por el usuario
     * @param signature Datos de autenticación (apiKey, timestamp, firma de seguridad)
     * @return URL segura de la imagen alojada en Cloudinary
     * @throws IllegalArgumentException Si no se puede leer la imagen
     * @throws IllegalStateException Si Cloudinary rechaza la petición o no devuelve la URL
     */
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
                // Enviar credenciales de seguridad

                writeTextPart(out, boundary, "api_key", signature.apiKey)
                writeTextPart(out, boundary, "timestamp", signature.timestamp.toString())
                writeTextPart(out, boundary, "signature", signature.signature)
                //Organizar imágenes en Cloudinary

                if (!signature.folder.isNullOrBlank()) {
                    writeTextPart(out, boundary, "folder", signature.folder)
                }
                //Enviamos la imagen binaria a Cloudinary
                writeFilePart(out, boundary, "file", fileName, mimeType, imageBytes)
                //Cierre del formulario - OutputStream
                out.writeBytes("--$boundary--\r\n")
                out.flush()
            }
                //Validación respuesta HTTP
            val responseCode = connection.responseCode
            val responseText = if (responseCode in 200..299) {
                connection.inputStream.bufferedReader().use { it.readText() }
            } else {
                connection.errorStream?.bufferedReader()?.use { it.readText() }.orEmpty()
            }
            //Manejo de errores de la respuesta de Cloudinary
            if (responseCode !in 200..299) {
                throw IllegalStateException("Cloudinary upload fallo (HTTP $responseCode): $responseText")
            }
            //Extraer y retornar la URL segura de respuesta
            val json = JSONObject(responseText)
            return json.optString("secure_url").takeIf { it.isNotBlank() }
                ?: throw IllegalStateException("Cloudinary no devolvio secure_url")
        } finally {
            //Cierrre de conexión
            connection.disconnect()
        }
    }
//Escritura de campo de texto en formato multipart/form-data
    private fun writeTextPart(out: DataOutputStream, boundary: String, name: String, value: String) {
        out.writeBytes("--$boundary\r\n")
        out.writeBytes("Content-Disposition: form-data; name=\"$name\"\r\n\r\n")
        out.writeBytes(value)
        out.writeBytes("\r\n")
    }
//Escritura de imagen en formato multipart/form-data

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