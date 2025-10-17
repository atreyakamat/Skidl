package com.skidl.ui.screens

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
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.Image
import androidx.compose.ui.unit.dp
import com.skidl.R
import com.skidl.model.Player
import com.skidl.ui.SkidlUiState
import com.skidl.ui.components.rememberQrCode

@Composable
fun LobbyScreen(
    state: SkidlUiState,
    onToggleReady: (Boolean) -> Unit,
    onStartRound: () -> Unit,
    onStopHost: () -> Unit
) {
    val lobby = state.lobbyState
    Surface(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = lobby?.settings?.roomName ?: stringResource(id = R.string.app_name), style = MaterialTheme.typography.headlineSmall)
            Text(text = "Players", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp))
            Divider(modifier = Modifier.padding(vertical = 8.dp))
            LazyColumn(modifier = Modifier.weight(1f, fill = false)) {
                items(lobby?.players ?: emptyList()) { player ->
                    PlayerRow(player = player, isCurrent = player.playerId == state.playerId)
                    Divider(modifier = Modifier.padding(vertical = 4.dp))
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            ReadyToggleRow(
                ready = lobby?.players?.firstOrNull { it.playerId == state.playerId }?.isReady == true,
                onToggleReady = onToggleReady
            )
            Spacer(modifier = Modifier.height(12.dp))
            if (state.isHosting) {
                HostShareCard(address = state.hostAddress)
                Spacer(modifier = Modifier.height(12.dp))
                Button(onClick = onStartRound, modifier = Modifier.fillMaxWidth()) {
                    Text(text = stringResource(id = R.string.start_round))
                }
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = onStopHost, modifier = Modifier.fillMaxWidth()) {
                    Text(text = stringResource(id = R.string.stop_host))
                }
            }
        }
    }
}

@Composable
private fun HostShareCard(address: String) {
    if (address.isBlank()) return
    val qr = rememberQrCode(address, size = 256)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "Share Link", style = MaterialTheme.typography.titleMedium)
        Text(text = address, style = MaterialTheme.typography.bodySmall)
        qr?.let {
            Image(bitmap = it, contentDescription = "Join QR code", modifier = Modifier.padding(top = 12.dp))
        }
    }
}

@Composable
private fun PlayerRow(player: Player, isCurrent: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(text = player.name, style = MaterialTheme.typography.bodyLarge)
            val subtitle = buildString {
                if (player.isHost) append("Host")
                if (player.isReady) {
                    if (isNotEmpty()) append(" · ")
                    append("Ready")
                }
            }
            if (subtitle.isNotBlank()) {
                Text(text = subtitle, style = MaterialTheme.typography.bodySmall)
            }
        }
        Text(text = "${player.score}", style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun ReadyToggleRow(ready: Boolean, onToggleReady: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = stringResource(id = R.string.ready_up))
        Checkbox(checked = ready, onCheckedChange = onToggleReady)
    }
}
