package com.skidl.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.skidl.ui.screens.GameScreen
import com.skidl.ui.screens.HostSetupScreen
import com.skidl.ui.screens.JoinScreen
import com.skidl.ui.screens.LobbyScreen
import com.skidl.ui.screens.ScoresScreen
import com.skidl.ui.screens.WelcomeScreen

@Composable
fun AppNavHost(
    uiState: SkidlUiState,
    viewModel: SkidlViewModel,
    modifier: Modifier = Modifier
) {
    val navController = rememberNavController()

    LaunchedEffect(uiState.screen) {
        val route = when (uiState.screen) {
            SkidlScreen.Welcome -> "welcome"
            SkidlScreen.HostSetup -> "host"
            SkidlScreen.Join -> "join"
            SkidlScreen.Lobby -> "lobby"
            SkidlScreen.Game -> "game"
            SkidlScreen.Scores -> "scores"
        }
        if (navController.currentDestination?.route != route) {
            navController.navigate(route) {
                launchSingleTop = true
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = "welcome",
        modifier = modifier
    ) {
        composable("welcome") {
            WelcomeScreen(
                state = uiState,
                onHost = { viewModel.navigate(SkidlScreen.HostSetup) },
                onJoin = { viewModel.startDiscovery() }
            )
        }
        composable("host") {
            HostSetupScreen(
                state = uiState,
                onBack = { viewModel.navigate(SkidlScreen.Welcome) },
                onRoomNameChange = viewModel::setRoomName,
                onRoomCodeChange = viewModel::setRoomCode,
                onDisplayNameChange = viewModel::setPlayerName,
                onStartHost = { viewModel.startHosting() }
            )
        }
        composable("join") {
            JoinScreen(
                state = uiState,
                onBack = {
                    viewModel.stopDiscovery()
                    viewModel.navigate(SkidlScreen.Welcome)
                },
                onManualIpChange = viewModel::setManualIp,
                onDisplayNameChange = viewModel::setPlayerName,
                onJoinSelected = { ad ->
                    val url = "ws://${ad.ip}:${ad.port}"
                    viewModel.joinHost(url)
                },
                onJoinManual = {
                    val ip = uiState.manualIp
                    if (ip.isNotBlank()) {
                        val url = if (ip.startsWith("ws")) ip else "ws://${ip}:${com.skidl.util.NetworkDefaults.WEBSOCKET_PORT}"
                        viewModel.joinHost(url)
                    }
                }
            )
        }
        composable("lobby") {
            LobbyScreen(
                state = uiState,
                onToggleReady = viewModel::readyUp,
                onStartRound = viewModel::hostStartRound,
                onStopHost = viewModel::stopHosting
            )
        }
        composable("game") {
            GameScreen(
                state = uiState,
                onSendGuess = viewModel::sendGuess,
                onStrokeStart = viewModel::onStrokeStart,
                onStrokePoint = viewModel::onStrokePoint,
                onStrokeEnd = viewModel::onStrokeEnd,
                onUndo = viewModel::undoLastStroke,
                onClearCanvas = viewModel::clearCanvas
            )
        }
        composable("scores") {
            ScoresScreen(
                state = uiState,
                onNextRound = viewModel::hostNextRound,
                onBackToLobby = viewModel::backToLobby
            )
        }
    }
}

