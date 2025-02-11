package com.plcoding.bluetoothchat.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.plcoding.bluetoothchat.presentation.BluetoothUiState
import kotlin.random.Random

@Composable
fun TrisGameScreen(
    state: BluetoothUiState,
    onSendHandshake: (Int) -> Unit,
    onSendMove: (Int, Int) -> Unit
) {
    // Stato handshake
    var handshakeCompleted by remember { mutableStateOf(false) }
    // Genera una volta il numero casuale locale
    val localRandomNumber = remember { Random.nextInt(0, 10000) }
    var remoteRandomNumber by remember { mutableStateOf<Int?>(null) }
    // Ruolo: "X" oppure "O"
    var playerSymbol by remember { mutableStateOf("") }
    var opponentSymbol by remember { mutableStateOf("") }
    // Il turno iniziale è sempre "X"
    var currentPlayer by remember { mutableStateOf("X") }
    // Stato della griglia e vincitore
    var board by remember { mutableStateOf(MutableList(3) { MutableList(3) { "" } }) }
    var winner by remember { mutableStateOf<String?>(null) }

    // Invia il messaggio di handshake (solo una volta)
    LaunchedEffect(Unit) {
        onSendHandshake(localRandomNumber)
    }

    // Gestione dei messaggi in arrivo
    LaunchedEffect(state.messages) {
        // Se il handshake non è completato, cerchiamo un messaggio HANDSHAKE che non sia il nostro
        if (!handshakeCompleted) {
            val handshakeMsg = state.messages
                .filter { it.message.startsWith("HANDSHAKE:") }
                .firstOrNull { it.message != "HANDSHAKE:$localRandomNumber" }
            handshakeMsg?.let { message ->
                val num = message.message.removePrefix("HANDSHAKE:").toIntOrNull()
                if (num != null) {
                    remoteRandomNumber = num
                    // Confronta il numero locale con quello remoto
                    when {
                        localRandomNumber == num -> {
                            // Caso di parità (estremamente improbabile)
                            playerSymbol = "X"
                            opponentSymbol = "O"
                        }
                        localRandomNumber > num -> {
                            playerSymbol = "X"
                            opponentSymbol = "O"
                        }
                        else -> {
                            playerSymbol = "O"
                            opponentSymbol = "X"
                        }
                    }
                    handshakeCompleted = true
                }
            }
        } else {
            // Una volta completato l'handshake, processa i messaggi delle mosse
            val lastMoveMsg = state.messages
                .filter { !it.message.startsWith("HANDSHAKE:") }
                .lastOrNull()
            lastMoveMsg?.let { message ->
                val parts = message.message.split(",")
                if (parts.size == 2) {
                    val row = parts[0].toIntOrNull()
                    val col = parts[1].toIntOrNull()
                    if (row != null && col != null && board[row][col].isEmpty()) {
                        board[row][col] = opponentSymbol
                        currentPlayer = playerSymbol // dopo la mossa dell'avversario, tocca a noi
                        winner = checkWinner(board)
                    }
                }
            }
        }
    }

    // Se il handshake non è completato, mostra "Waiting..."
    if (!handshakeCompleted) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text("Waiting for handshake...")
        }
    } else {
        val isPlayerTurn = currentPlayer == playerSymbol
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(text = "TRIS", fontSize = 24.sp)
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = when {
                    winner != null -> "Winner: $winner"
                    isPlayerTurn -> "Your turn ($playerSymbol)"
                    else -> "Opponent's turn ($opponentSymbol)"
                },
                fontSize = 18.sp
            )
            Spacer(modifier = Modifier.height(16.dp))
            board.forEachIndexed { rowIndex, row ->
                Row {
                    row.forEachIndexed { colIndex, cell ->
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .background(Color.Gray)
                                .clickable(
                                    enabled = isPlayerTurn && cell.isEmpty() && winner == null,
                                    onClick = {
                                        board[rowIndex][colIndex] = playerSymbol
                                        onSendMove(rowIndex, colIndex)
                                        currentPlayer = opponentSymbol
                                        winner = checkWinner(board)
                                    }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = cell, fontSize = 32.sp, color = Color.White)
                        }
                    }
                }
            }
        }
    }
}

/**
 * Verifica se esiste un vincitore controllando righe, colonne e diagonali.
 */
fun checkWinner(board: List<List<String>>): String? {
    for (i in 0..2) {
        if (board[i][0] == board[i][1] &&
            board[i][1] == board[i][2] &&
            board[i][0].isNotEmpty()
        ) return board[i][0]
        if (board[0][i] == board[1][i] &&
            board[1][i] == board[2][i] &&
            board[0][i].isNotEmpty()
        ) return board[0][i]
    }
    if (board[0][0] == board[1][1] &&
        board[1][1] == board[2][2] &&
        board[0][0].isNotEmpty()
    ) return board[0][0]
    if (board[0][2] == board[1][1] &&
        board[1][1] == board[2][0] &&
        board[0][2].isNotEmpty()
    ) return board[0][2]
    return null
}
