package com.blesense.app.features.bluetooth.presentation.widget

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.blesense.app.coreui.constants.AppStrings
import com.blesense.app.features.bluetooth.domain.model.SensorData

@Composable
fun ThresholdInputSection(
    thresholdValue: String,
    onThresholdChange: (String) -> Unit,
    parameterType: String,
    onParameterChange: (String) -> Unit,
    sensorData: SensorData?,
    onConfirmThreshold: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 1. Parameter type selector using FilterChips (Material 3 standard)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
        ) {
            val parameters = when (sensorData) {
                is SensorData.SHT40Data -> listOf(AppStrings.TEMPERATURE, AppStrings.HUMIDITY)
                is SensorData.AmmoniaSensorData -> listOf(AppStrings.AMMONIA)
                is SensorData.SoilSensorData -> listOf(AppStrings.MOISTURE, AppStrings.TEMPERATURE, AppStrings.PH)
                else -> emptyList()
            }

            parameters.forEach { type ->
                FilterChip(
                    selected = parameterType == type,
                    onClick = { onParameterChange(type) },
                    label = { Text(type) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 2. Modern TextField with clear error state handling
        val isError = thresholdValue.isNotEmpty() && thresholdValue.toFloatOrNull() == null

        OutlinedTextField(
            value = thresholdValue,
            onValueChange = { onThresholdChange(it) },
            label = { Text("Enter $parameterType Threshold") },
            placeholder = { Text("e.g. 25.5") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            isError = isError,
            supportingText = {
                if (isError) {
                    Text(
                        text = "Please enter a valid number",
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            shape = MaterialTheme.shapes.medium,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                errorBorderColor = MaterialTheme.colorScheme.error
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 3. Confirm button - using FilledTonalButton for a distinct action
        Button(
            onClick = onConfirmThreshold,
            enabled = thresholdValue.isNotEmpty() && !isError,
            modifier = Modifier.fillMaxWidth(0.7f),
            shape = MaterialTheme.shapes.large
        ) {
            Text(
                text = "Confirm Threshold",
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}