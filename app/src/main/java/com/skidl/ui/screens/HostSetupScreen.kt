package com.skidl.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.skidl.R
import com.skidl.ui.SkidlUiState

@Composable
fun HostSetupScreen(
    state: SkidlUiState,
    onBack: () -> Unit,
    onRoomNameChange: (String) -> Unit,
    onRoomCodeChange: (String) -> Unit,
    onDisplayNameChange: (String) -> Unit,
    onStartHost: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Top
    ) {
        TopAppBar(
            title = { Text(text = stringResource(id = R.string.start_host)) },
            navigationIcon = {
                TextButton(onClick = onBack) { Text("Back") }
            }
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = "Enable your hotspot before starting hosting.", style = MaterialTheme.typography.bodySmall)
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedTextField(
            value = state.displayName,
            onValueChange = onDisplayNameChange,
            label = { Text(text = stringResource(id = R.string.player_name_hint)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedTextField(
            value = state.roomName,
            onValueChange = onRoomNameChange,
            label = { Text(text = stringResource(id = R.string.room_name_hint)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedTextField(
            value = state.roomCode ?: "",
            onValueChange = onRoomCodeChange,
            label = { Text(text = stringResource(id = R.string.room_code_hint)) },
            modifier = Modifier.fillMaxWidth(),
            maxLines = 1,
            singleLine = true
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onStartHost,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = stringResource(id = R.string.start_host))
        }
    }
}
