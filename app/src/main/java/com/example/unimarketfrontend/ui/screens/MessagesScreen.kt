package com.example.unimarketfrontend.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.unimarketfrontend.model.message.ConversationPreview
import com.example.unimarketfrontend.ui.components.BottomNavigationBar
import com.example.unimarketfrontend.ui.navigation.navigateTracked
import com.example.unimarketfrontend.viewmodel.MessagesViewModel

/*
 * Pantalla principal del modulo de mensajeria para el Sprint 3.
 * Implementa el Patron Observer para reaccionar a cambios en la DB local (Room).
 * Incluye un banner informativo para casos de falta de conectividad (Evita NIM antipattern).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessagesScreen(
    navController: NavController,
    viewModel: MessagesViewModel = viewModel()
) {
    // Observamos el estado del ViewModel (Lista de conversaciones, estado de carga y red)
    val conversations by viewModel.conversations.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val isOffline by viewModel.isOffline.collectAsState()

    // Cada vez que el usuario entra, intentamos refrescar los datos del servidor
    LaunchedEffect(Unit) {
        viewModel.loadConversations()
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Messages", fontWeight = FontWeight.Bold) }
            )
        },
        bottomBar = {
            BottomNavigationBar(
                currentRoute = "messages",
                onRouteChange = { route ->
                    navController.navigateTracked(route) {
                        popUpTo(navController.graph.startDestinationId)
                        launchSingleTop = true
                    }
                }
            )
        }
    ) { innerPadding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // --- SPRINT 3: GESTION DE CONECTIVIDAD ---
            // Si el monitor detecta que no hay internet, avisamos al usuario (Evita NIM)
            if (isOffline) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.errorContainer
                ) {
                    Text(
                        text = "You are offline. Showing saved conversations.",
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }

            // Indicador de que estamos buscando datos nuevos en background (Cache-then-Network)
            if (isRefreshing) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            Box(modifier = Modifier.fillMaxSize()) {
                if (conversations.isEmpty() && !isRefreshing) {
                    // Estado vacio: el usuario no tiene chats guardados ni red para bajar nuevos
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.outline
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = "No messages yet",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.outline
                        )
                        Text(
                            text = "Contact a seller to start a conversation",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                } else {
                    // Lista de conversaciones reactiva (conecta con Room)
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(conversations) { entity ->
                            ConversationRow(
                                // Mapeamos la entidad de Room al modelo de vista
                                conversation = ConversationPreview(
                                    otherPersonId = entity.otherPersonId,
                                    otherPersonName = entity.otherPersonName,
                                    lastMessage = entity.lastMessage,
                                    lastMessageTime = entity.lastMessageTime,
                                    isRead = entity.isRead
                                ),
                                onClick = {
                                    navController.navigate(
                                        "chat/${entity.otherPersonId}/${
                                            java.net.URLEncoder.encode(entity.otherPersonName, "UTF-8")
                                        }"
                                    )
                                }
                            )
                            HorizontalDivider(
                                modifier = Modifier.padding(start = 76.dp),
                                color = MaterialTheme.colorScheme.outlineVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

/*
 * Fila individual para cada conversacion en la lista.
 * Resalta visualmente si hay mensajes pendientes de lectura.
 */
@Composable
fun ConversationRow(conversation: ConversationPreview, onClick: () -> Unit = {}) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Icono de perfil generico
        Box(
            modifier = Modifier
                .size(50.dp)
                .background(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(26.dp)
            )
        }

        Spacer(Modifier.width(12.dp))

        // Info de la conversacion (Nombre, Mensaje, Hora)
        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = conversation.otherPersonName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = if (!conversation.isRead) FontWeight.Bold else FontWeight.Normal
                )
                Text(
                    text = conversation.lastMessageTime,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }

            Spacer(Modifier.height(2.dp))

            Text(
                text = conversation.lastMessage,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                color = if (!conversation.isRead)
                    MaterialTheme.colorScheme.onSurface
                else
                    MaterialTheme.colorScheme.outline
            )
        }
        
        // Punto azul de notificacion para mensajes no leidos
        if (!conversation.isRead) {
            Spacer(Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .background(
                        color = MaterialTheme.colorScheme.primary,
                        shape = CircleShape
                    )
            )
        }
    }
}
