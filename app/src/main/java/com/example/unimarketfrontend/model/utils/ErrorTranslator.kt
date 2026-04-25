package com.example.unimarketfrontend.model.utils

import java.io.IOException
import java.net.UnknownHostException
import java.net.SocketTimeoutException

/**
 * Traduce excepciones técnicas a mensajes amigables para el usuario.
 * Nunca muestra detalles técnicos directamente.
 */
object ErrorTranslator {

    fun getUserFriendlyMessage(exception: Exception?): String {
        return when (exception) {
            is UnknownHostException -> {
                "No internet connection. Showing saved data when available."
            }
            is SocketTimeoutException -> {
                "Connection timeout. Please check your network and try again."
            }
            is IOException -> {
                "Connection error. Please try again later."
            }
            is IllegalStateException -> {
                when {
                    exception.message?.contains("IMAGE_TOO_LARGE_LOCAL", ignoreCase = true) == true ->
                        "Image is too large. Please select a smaller image or compress it."
                    exception.message?.contains("IMAGE_TOO_LARGE_REMOTE", ignoreCase = true) == true ->
                        "Image upload failed. Please try with a smaller image."
                    else -> "Something went wrong. Please try again."
                }
            }
            null -> "Could not connect. Please try again later."
            else -> "Something went wrong. Please try again."
        }
    }

    fun isNetworkError(exception: Exception?): Boolean {
        return exception is UnknownHostException ||
                exception is SocketTimeoutException ||
                exception is IOException ||
                (exception is IllegalStateException && exception.message?.contains("IMAGE_TOO_LARGE_REMOTE", ignoreCase = true) == true)
    }
}

