package com.example.alearning.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.alearning.domain.model.people.BiologicalSex
import java.util.Date
import java.util.Locale

private val BackgroundGray = Color(0xFFF8F9FB)
private val SurfaceWhite = Color(0xFFFFFFFF)
private val TextPrimary = Color(0xFF1A1A1A)
private val TextSecondary = Color(0xFF757575)
private val BrandAccent = Color(0xFFF97D28)
private val BorderLight = Color(0xFFE5E7EB)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterAthleteSheet(
    onDismiss: () -> Unit,
    onConfirm: (String, String, Long, BiologicalSex, String?, String?) -> Unit
) {
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var selectedSex by remember { mutableStateOf(BiologicalSex.UNSPECIFIED) }
    var email by remember { mutableStateOf("") }
    var medicalAlert by remember { mutableStateOf("") }
    
    val datePickerState = rememberDatePickerState()
    var showDatePicker by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = SurfaceWhite,
        dragHandle = { BottomSheetDefaults.DragHandle(color = BorderLight) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Register Athlete", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = firstName,
                    onValueChange = { firstName = it },
                    label = { Text("First Name *") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                )
                OutlinedTextField(
                    value = lastName,
                    onValueChange = { lastName = it },
                    label = { Text("Last Name *") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                )
            }

            // Date of Birth Trigger
            OutlinedCard(
                onClick = { showDatePicker = true },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, BorderLight)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Cake, contentDescription = null, tint = BrandAccent)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = if (datePickerState.selectedDateMillis != null) {
                            val date = Date(datePickerState.selectedDateMillis!!)
                            java.text.SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(date)
                        } else "Date of Birth *",
                        color = if (datePickerState.selectedDateMillis != null) TextPrimary else TextSecondary
                    )
                }
            }

            Text("Biological Sex", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                BiologicalSex.entries.forEach { sex ->
                    val isSelected = selectedSex == sex
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (isSelected) BrandAccent else BackgroundGray)
                            .clickable { selectedSex = sex }
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text(
                            sex.name.lowercase().replaceFirstChar { it.uppercase() },
                            color = if (isSelected) Color.White else TextSecondary,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email (Optional)") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            OutlinedTextField(
                value = medicalAlert,
                onValueChange = { medicalAlert = it },
                label = { Text("Medical Alert / Restriction") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                placeholder = { Text("e.g. Asthma, Knee injury") }
            )

            Button(
                onClick = {
                    onConfirm(
                        firstName,
                        lastName,
                        datePickerState.selectedDateMillis ?: 0L,
                        selectedSex,
                        email.ifBlank { null },
                        medicalAlert.ifBlank { null }
                    )
                },
                enabled = firstName.isNotBlank() && lastName.isNotBlank() && datePickerState.selectedDateMillis != null,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BrandAccent)
            ) {
                Text("Register Athlete", fontWeight = FontWeight.Bold)
            }
        }
    }

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("OK", color = BrandAccent) }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}
