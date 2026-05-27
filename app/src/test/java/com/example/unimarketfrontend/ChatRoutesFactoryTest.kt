package com.example.unimarketfrontend

import com.example.unimarketfrontend.ui.navigation.ChatRoutesFactory
import org.junit.Assert.assertEquals
import org.junit.Test
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

class ChatRoutesFactoryTest {
    @Test
    fun chatPath_encodesNamesSafely() {
        val originalName = "Juan Perez / Campus"
        val path = ChatRoutesFactory.chatPath(15, originalName, iAmBuyer = true)

        assertEquals("chat/15/", path.substring(0, 8))

        val segments = path.split('/')
        val encodedName = segments[2]
        val decodedName = URLDecoder.decode(encodedName, StandardCharsets.UTF_8.name())
        val role = segments[3]

        assertEquals(originalName, decodedName)
        assertEquals("1", role)
    }
}
