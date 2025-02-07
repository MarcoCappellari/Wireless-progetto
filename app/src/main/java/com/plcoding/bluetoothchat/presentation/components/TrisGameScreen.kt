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
import com.plcoding.bluetoothchat.domain.chat.BluetoothMessage
import kotlin.random.Random

@Composable
fun TrisGameScreen(
    state: BluetoothUiState,
    isServer: Boolean,
    onDisconnect: () -> Unit,
    onSendMove: (Int, Int) -> Unit
) {
    var board by remember { mutableStateOf(MutableList(3) { MutableList(3) { "" } }) }
    val playerSymbol = if (isServer) "X" else "O"
    val opponentSymbol = if (isServer) "O" else "X"
    var currentPlayer by remember { mutableStateOf(if (Random.nextBoolean()) "X" else "O") } // Inizia casualmente
    var winner by remember { mutableStateOf<String?>(null) }
    val isPlayerTurn = currentPlayer == playerSymbol

    LaunchedEffect(state.messages) {
        state.messages.lastOrNull()?.let { message ->
            val move = message.message.split(",").map { it.toInt() }
            if (move.size == 2 && board[move[0]][move[1]].isEmpty()) {
                board[move[0]][move[1]] = opponentSymbol
                currentPlayer = playerSymbol // Passa il turno al giocatore locale
                winner = checkWinner(board)
            }
        }
    }

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
                                    onSendMove(rowIndex, colIndex) // Invia la mossa all'avversario
                                    currentPlayer = opponentSymbol // Passa il turno all'avversario
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

// Funzione per controllare il vincitore
fun checkWinner(board: List<List<String>>): String? {
    // Controlla righe, colonne e diagonali
    for (i in 0..2) {
        if (board[i][0] == board[i][1] && board[i][1] == board[i][2] && board[i][0].isNotEmpty()) return board[i][0]
        if (board[0][i] == board[1][i] && board[1][i] == board[2][i] && board[0][i].isNotEmpty()) return board[0][i]
    }
    if (board[0][0] == board[1][1] && board[1][1] == board[2][2] && board[0][0].isNotEmpty()) return board[0][0]
    if (board[0][2] == board[1][1] && board[1][1] == board[2][0] && board[0][2].isNotEmpty()) return board[0][2]
    return null
}
