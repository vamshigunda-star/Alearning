package com.example.alearning.reports.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.alearning.domain.model.testing.TestingEvent
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val df = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())

@Composable
fun SessionPill(
    session: TestingEvent,
    onTap: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .background(Color(0xFFE3F2FD), RoundedCornerShape(999.dp))
            .clickable(onClick = onTap)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(
            Icons.Default.CalendarMonth,
            contentDescription = null,
            tint = Color(0xFF0D47A1),
            modifier = Modifier.size(14.dp)
        )
        Text(
            "${df.format(Date(session.date))} · ${session.name}",
            style = MaterialTheme.typography.labelMedium,
            color = Color(0xFF0D47A1),
            fontWeight = FontWeight.Medium
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionSwitcherSheet(
    sessions: List<TestingEvent>,
    currentId: String,
    onPick: (TestingEvent) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Text("Switch session", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            sessions.forEach { ev ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onPick(ev) }
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(ev.name, style = MaterialTheme.typography.bodyLarge)
                        Text(df.format(Date(ev.date)), style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    }
                    if (ev.id == currentId) {
                        Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    }
}
