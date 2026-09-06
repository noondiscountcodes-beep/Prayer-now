package com.example.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.data.CitiesRepository
import com.example.data.City
import com.example.localization.AppLanguage
import com.example.localization.AppStrings

@Composable
fun CitySearchDialog(
    language: AppLanguage,
    onCitySelected: (City) -> Unit,
    onDismiss: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val filteredCities = remember(searchQuery, language) {
        CitiesRepository.search(searchQuery, language)
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .padding(8.dp)
                .testTag("city_search_dialog"),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (language.code == "ar") "اختر المدينة" else if (language.code == "fr") "Choisir la ville" else "Select City",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.testTag("close_city_dialog")) {
                        Icon(Icons.Default.Close, contentDescription = AppStrings.close(language))
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Search field
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("city_search_input"),
                    placeholder = {
                        Text(
                            if (language.code == "ar") "ابحث عن دولة أو مدينة..."
                            else if (language.code == "fr") "Rechercher pays ou ville..."
                            else "Search country or city..."
                        )
                    },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = null)
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // List of cities
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    items(filteredCities) { city ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clickable {
                                    onCitySelected(city)
                                    onDismiss()
                                },
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Place,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = city.getDisplayName(language),
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "${city.latitude}, ${city.longitude} • ${city.timezoneId}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
