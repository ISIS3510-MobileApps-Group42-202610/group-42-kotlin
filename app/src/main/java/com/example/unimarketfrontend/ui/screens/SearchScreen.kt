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


/*
 * Esta es la pantalla de busqueda de la app.
 * La idea aca es que el usuario pueda encontrar productos rapido sin
 * tener que navegar por todas las categorias.
 */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    navController: NavController,
    viewModel: SearchViewModel = viewModel()
) {
    // Aca aplicamos el patron Observer: nos suscribimos a los flujos del ViewModel
    // Cada vez que cambie la consulta o los resultados, Compose redibuja la pantalla
    val query by viewModel.query.collectAsState()
    val results by viewModel.results.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Search") })
        },
        bottomBar = {
            // El menu de navegacion de abajo para movernos entre secciones
            BottomNavigationBar(
                currentRoute = "search",
                onRouteChange = { route ->
                    navController.navigate(route) {
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
                .padding(horizontal = 16.dp)
        ) {
            Spacer(Modifier.height(8.dp))

            // Este es el campo de texto donde el usuario escribe
            // Cada vez que cambia el texto, le avisamos al ViewModel para que filtre
            OutlinedTextField(
                value = query,
                onValueChange = { viewModel.onQueryChange(it) },
                label = { Text("Search listings...") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(12.dp))

            // Aca manejamos la logica visual de los diferentes estados de la busqueda
            Box(modifier = Modifier.fillMaxSize()) {
                when {
                    // Estado 1: El sistema esta procesando la busqueda
                    isLoading -> {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                    }
                    // Estado 2: El usuario no ha escrito nada todavia
                    query.isBlank() -> {
                        Text(
                            text = "Type to search for listings",
                            color = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                    // Estado 3: Escribio algo pero no hay coincidencias en la base de datos
                    results.isEmpty() -> {
                        Text(
                            text = "No results for \"$query\"",
                            color = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                    // Estado 4: Mostramos la lista de resultados usando LazyColumn para ahorrar memoria
                    else -> {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(bottom = 16.dp)
                        ) {
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

/*
 * Este es un componente pequeno para mostrar cada producto en la lista de busqueda.
 * Lo hice separado para que el codigo sea mas limpio y facil de mantener.
 */
@Composable
private fun SearchResultCard(
    title: String,
    price: String,
    condition: String,
    category: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = title, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            Text(
                text = price,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(2.dp))
            // Mostramos la condicion y categoria para que el usuario decida mejor
            Text(
                text = "$condition · $category",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline
            )
        }
    }
}