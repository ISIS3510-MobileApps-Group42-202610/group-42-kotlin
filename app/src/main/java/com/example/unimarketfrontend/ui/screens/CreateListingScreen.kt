package com.example.unimarketfrontend.ui.screens


import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.unimarketfrontend.network.model.*
import com.example.unimarketfrontend.viewmodel.CreateListingState
import com.example.unimarketfrontend.viewmodel.CreateListingViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateListingScreen(
    navController: NavController,
    viewModel: CreateListingViewModel = viewModel()
) {

    val state by viewModel.state.collectAsState()

    var title by remember { mutableStateOf("") }
    var product by remember { mutableStateOf("") }
    var sellingPrice by remember { mutableStateOf("") }

    var selectedCategory by remember {
        mutableStateOf(ListingCategory.TEXTBOOK)
    }

    var selectedCondition by remember {
        mutableStateOf(ListingCondition.NEW)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {

        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text("Title") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = product,
            onValueChange = { product = it },
            label = { Text("Product Description") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = sellingPrice,
            onValueChange = { sellingPrice = it },
            label = { Text("Selling Price") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {

                val request = CreateListingRequest(
                    title = title,
                    product = product.ifBlank { null },
                    category = selectedCategory.value,
                    condition = selectedCondition.value,
                    original_price = null,
                    selling_price = sellingPrice.toDouble(),
                    course_id = null
                )

                viewModel.createListing(request)
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Create Listing")
        }

        Spacer(modifier = Modifier.height(24.dp))

        when (state) {

            is CreateListingState.Loading -> {
                CircularProgressIndicator()
            }

            is CreateListingState.Success -> {
                LaunchedEffect(Unit) {
                    navController.popBackStack()
                }
            }

            is CreateListingState.Error -> {
                Text("Error creating listing")
            }

            else -> {}
        }
    }
}