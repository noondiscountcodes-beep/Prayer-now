package com.example.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.localization.AppLanguage
import com.example.localization.AppStrings

@Composable
fun CustomAngleDialog(
    initialFajrAngle: Double,
    initialIshaAngle: Double,
    language: AppLanguage,
    onSave: (Double, Double) -> Unit,
    onDismiss: () -> Unit
) {
    var fajrText by remember { mutableStateOf(initialFajrAngle.toString()) }
    var ishaText by remember { mutableStateOf(initialIshaAngle.toString()) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .testTag("custom_angle_dialog"),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (language.code == "ar") "زوايا حساب مخصصة" else "Custom Calculation Angles",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = fajrText,
                    onValueChange = { fajrText = it },
                    label = {
                        Text(if (language.code == "ar") "زاوية الفجر (بالدرجات)" else "Fajr Angle (Degrees)")
                    },
                    modifier = Modifier.fillMaxWidth().testTag("fajr_angle_input"),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = ishaText,
                    onValueChange = { ishaText = it },
                    label = {
                        Text(if (language.code == "ar") "زاوية العشاء (بالدرجات)" else "Isha Angle (Degrees)")
                    },
                    modifier = Modifier.fillMaxWidth().testTag("isha_angle_input"),
                    singleLine = true
                )

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = errorMessage ?: "",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(AppStrings.cancel(language))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val f = fajrText.toDoubleOrNull()
                            val i = ishaText.toDoubleOrNull()
                            if (f != null && i != null && f in 5.0..25.0 && i in 5.0..25.0) {
                                onSave(f, i)
                                onDismiss()
                            } else {
                                errorMessage = if (language.code == "ar") "يرجى إدخال زاوية صحيحة بين 5 و 25 درجة" else "Please enter a valid angle between 5 and 25"
                            }
                        },
                        modifier = Modifier.testTag("save_angle_button")
                    ) {
                        Text(AppStrings.save(language))
                    }
                }
            }
        }
    }
}
