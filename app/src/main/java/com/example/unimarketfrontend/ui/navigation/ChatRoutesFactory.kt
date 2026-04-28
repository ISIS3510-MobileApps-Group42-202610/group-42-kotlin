package com.example.unimarketfrontend.ui.navigation

/*
 * Fabrica de rutas para el Chat - SPRINT 3
 * Agregamos iAmBuyer como parametro de ruta (1=true, 0=false)
 * para que la pantalla de chat sepa que rol tomar instantaneamente.
 */
object ChatRoutesFactory {
    const val CHAT_ROUTE = "chat/{otherId}/{otherName}/{iAmBuyer}"

    fun chatPath(otherId: Int, otherName: String, iAmBuyer: Boolean): String {
        val encodedName = java.net.URLEncoder.encode(otherName, "UTF-8")
        val role = if (iAmBuyer) 1 else 0
        return "chat/$otherId/$encodedName/$role"
    }
}