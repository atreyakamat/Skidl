package com.skidl.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.skidl.R
import com.skidl.model.HostAdvertisement
import com.skidl.ui.SkidlUiState

@Composable
fun JoinScreen(
    state: SkidlUiState,
    onBack: () -> Unit,
    onManualIpChange: (String) -> Unit,
    onDisplayNameChange: (String) -> Unit,
    onJoinSelected: (HostAdvertisement) -> Unit,
    onJoinManual: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Top
    ) {
        TopAppBar(
            title = { Text(text = stringResource(id = R.string.join_button)) },
            navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }
        )
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedTextField(
            value = state.displayName,
            onValueChange = onDisplayNameChange,
            label = { Text(text = stringResource(id = R.string.player_name_hint)) },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(text = "Discovered Hosts", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))
        LazyColumn(
            modifier = Modifier.weight(1f, fill = false)
        ) {
            items(state.availableHosts) { host ->
                HostCard(host = host, onClick = { onJoinSelected(host) })
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = "Manual Join", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = state.manualIp,
            onValueChange = onManualIpChange,
            label = { Text(text = stringResource(id = R.string.manual_join_label)) },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = onJoinManual, modifier = Modifier.fillMaxWidth()) {
            Text(text = stringResource(id = R.string.join_host))
        }
    }
}

@Composable
private fun HostCard(host: HostAdvertisement, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = host.roomName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(text = "${host.players} players", style = MaterialTheme.typography.bodySmall)
            Text(text = "${host.ip}:${host.port}", style = MaterialTheme.typography.bodySmall)
        }
    }
}
