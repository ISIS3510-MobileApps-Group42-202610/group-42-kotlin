package com.example.unimarketfrontend.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.unimarketfrontend.ui.components.BottomNavigationBar
import com.example.unimarketfrontend.viewmodel.SearchViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    navController: NavController,
    // viewModel() crea o reutiliza el SearchViewModel para esta pantalla
    viewModel: SearchViewModel = viewModel()
) {
    // collectAsState() convierte el StateFlow en un estado que Compose entiende
    // Cada vez que query/results/isLoading cambia, la pantalla se redibuja
    val query by viewModel.query.collectAsState()
    val results by viewModel.results.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    Scaffold(
        // Barra superior con título
        topBar = {
            TopAppBar(title = { Text("Search") })
        },
        // Barra de navegación inferior
        bottomBar = {
            BottomNavigationBar(
                currentRoute = "search",
                onRouteChange = { route ->
                    navController.navigate(route) {
                        // popUpTo evita que se apilen pantallas al navegar
                        popUpTo(navController.graph.startDestinationId)
                        // launchSingleTop evita crear dos instancias de la misma pantalla
                        launchSingleTop = true
                    }
                }
            )
        }
    ) { innerPadding ->
        // innerPadding = el espacio que dejan el topBar y bottomBar
        // Se lo pasamos al Column para que el contenido no quede tapado

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(Modifier.height(8.dp))

            // Campo de texto de búsqueda
            // Cada vez que el usuario escribe, llama a viewModel.onQueryChange
            OutlinedTextField(
                value = query,              // el texto actual del StateFlow
                onValueChange = { viewModel.onQueryChange(it) }, // "it" es el nuevo texto
                label = { Text("Search listings...") },
                singleLine = true,          // no permite múltiples líneas
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(12.dp))

            // Box permite posicionar elementos de forma absoluta dentro de él
            // Lo usamos para centrar el spinner y los mensajes vacíos
            Box(modifier = Modifier.fillMaxSize()) {

                // "when" en Kotlin es como un switch mejorado
                // Acá no usamos sealed class sino condiciones booleanas
                when {
                    // Si está cargando, muestra el spinner centrado
                    isLoading -> {
                        CircularProgressIndicator(
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }

                    // Si el campo está vacío, invita a escribir
                    query.isBlank() -> {
                        Text(
                            text = "Type to search for listings",
                            color = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }

                    // Si hay búsqueda pero no hay resultados
                    results.isEmpty() -> {
                        Text(
                            // "$query" inserta el valor de la variable dentro del String
                            text = "No results for \"$query\"",
                            color = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }

                    // Si hay resultados, los muestra en lista
                    else -> {
                        // LazyColumn = lista que solo renderiza los items visibles
                        // Equivalente a RecyclerView en el sistema antiguo
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            // contentPadding agrega espacio al final de la lista
                            contentPadding = PaddingValues(bottom = 16.dp)
                        ) {
                            // items() itera sobre la lista
                            // Por cada elemento llama al composable SearchResultCard
                            items(results) { listing ->
                                SearchResultCard(
                                    title = listing.title,
                                    price = "$${listing.selling_price}",
                                    condition = listing.condition ?: "Unknown",
                                    category = listing.category ?: ""
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// Composable privado — solo se usa dentro de este archivo
// Muestra una tarjeta con los datos de un listing
@Composable
private fun SearchResultCard(
    title: String,
    price: String,
    condition: String,
    category: String
) {
    // Card es un contenedor con sombra y forma redondeada de Material Design
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium
                // titleMedium = tamaño y peso predefinidos por Material Design
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = price,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary  // color principal del tema
            )
            Spacer(Modifier.height(2.dp))
            // "$condition · $category" construye el texto combinando las dos variables
            Text(
                text = "$condition · $category",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline  // gris secundario
            )
        }
    }
}