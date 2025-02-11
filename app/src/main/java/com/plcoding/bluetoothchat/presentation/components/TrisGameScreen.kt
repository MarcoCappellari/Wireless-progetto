package com.plcoding.bluetoothchat.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.Button
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
    onSendMove: (Int, Int) -> Unit,
    onSendReplay: (String) -> Unit
) {
    // --- Stato handshake ---
    var handshakeCompleted by remember { mutableStateOf(false) }
    // Genera una sola volta il numero casuale locale
    val localRandomNumber = remember { Random.nextInt(0, 10000) }
    var remoteRandomNumber by remember { mutableStateOf<Int?>(null) }
    // Ruolo: "X" oppure "O"
    var playerSymbol by remember { mutableStateOf("") }
    var opponentSymbol by remember { mutableStateOf("") }
    // Il turno iniziale è sempre "X"
    var currentPlayer by remember { mutableStateOf("X") }

    // --- Stato partita ---
    var board by remember { mutableStateOf(MutableList(3) { MutableList(3) { "" } }) }
    var winner by remember { mutableStateOf<String?>(null) }

    // --- Stato replay ---
    var replayRequestSent by remember { mutableStateOf(false) }
    var replayRequestReceived by remember { mutableStateOf(false) }

    // Funzione per resettare la partita (manteniamo i ruoli)
    fun resetGame() {
        board = MutableList(3) { MutableList(3) { "" } }
        winner = null
        currentPlayer = "X"
        replayRequestSent = false
        replayRequestReceived = false
    }

    // Invia il messaggio di handshake (solo una volta)
    LaunchedEffect(Unit) {
        onSendHandshake(localRandomNumber)
    }

    // Gestione dei messaggi in arrivo (handshake, mosse e replay)
    LaunchedEffect(state.messages) {
        state.messages.lastOrNull()?.let { message ->
            val msg = message.message
            if (!handshakeCompleted) {
                // Processa i messaggi di handshake, ignorando il proprio
                if (msg.startsWith("HANDSHAKE:") && msg != "HANDSHAKE:$localRandomNumber") {
                    val num = msg.removePrefix("HANDSHAKE:").toIntOrNull()
                    if (num != null) {
                        remoteRandomNumber = num
                        if (localRandomNumber == num) {
                            // Caso di parità (molto improbabile): assegniamo per default "X"
                            playerSymbol = "X"
                            opponentSymbol = "O"
                        } else if (localRandomNumber > num) {
                            playerSymbol = "X"
                            opponentSymbol = "O"
                        } else {
                            playerSymbol = "O"
                            opponentSymbol = "X"
                        }
                        handshakeCompleted = true
                    }
                }
            } else {
                // Gestione dei messaggi di replay e delle mosse, dopo l'handshake
                when {
                    // Gestisce la richiesta di replay solo se NON è già stata inviata localmente
                    msg.startsWith("REPLAY_REQUEST") -> {
                        if (!replayRequestSent) {
                            replayRequestReceived = true
                        }
                    }
                    msg.startsWith("REPLAY_ACCEPTED") -> {
                        // Se avevo richiesto il replay e l'altro ha accettato, resetta la partita
                        if (replayRequestSent) {
                            resetGame()
                        }
                    }
                    msg.startsWith("REPLAY_DECLINED") -> {
                        // Se la richiesta di replay è stata rifiutata, azzera lo stato della richiesta
                        replayRequestSent = false
                    }
                    // Se il messaggio non è un replay (e non è handshake) e contiene una mossa
                    !msg.startsWith("HANDSHAKE:") && msg.contains(",") -> {
                        val parts = msg.split(",")
                        if (parts.size == 2) {
                            val row = parts[0].toIntOrNull()
                            val col = parts[1].toIntOrNull()
                            if (row != null && col != null && board[row][col].isEmpty()) {
                                board[row][col] = opponentSymbol
                                // Dopo la mossa dell'avversario, passa il turno al giocatore locale
                                currentPlayer = playerSymbol
                                winner = checkWinner(board)
                            }
                        }
                    }
                }
            }
        }
    }

    // Se l'handshake non è ancora completato, mostra "Waiting..."
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
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(text = "TRIS", fontSize = 24.sp)
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = when {
                    winner == "Tie" -> "Pareggio!"
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
            Spacer(modifier = Modifier.height(16.dp))
            // Se la partita è finita (vincitore o pareggio) e non ho inviato una richiesta,
            // mostra il tasto "Rigioca"
            if (winner != null && !replayRequestSent && !replayRequestReceived) {
                Button(onClick = {
                    onSendReplay("REPLAY_REQUEST")
                    replayRequestSent = true
                }) {
                    Text("Rigioca")
                }
            }
            // Se ho ricevuto una richiesta di replay dall'avversario (e non ho inviato la mia richiesta),
            // mostra la UI di conferma
            if (replayRequestReceived && !replayRequestSent) {
                Spacer(modifier = Modifier.height(16.dp))
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Il tuo avversario vuole rigiocare. Accetti?")
                    Spacer(modifier = Modifier.height(8.dp))
                    Row {
                        Button(onClick = {
                            onSendReplay("REPLAY_ACCEPTED")
                            resetGame()
                        }) {
                            Text("Accetta")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(onClick = {
                            onSendReplay("REPLAY_DECLINED")
                            replayRequestReceived = false
                        }) {
                            Text("Rifiuta")
                        }
                    }
                }
            }
        }
    }
}

/**
 * Funzione per verificare se c'è un vincitore o se c'è un pareggio.
 * Controlla righe, colonne e diagonali; se la griglia è piena e non c'è vincitore,
 * restituisce "Tie".
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
    // Se la griglia è piena e non c'è vincitore, è un pareggio
    if (board.flatten().none { it.isEmpty() }) return "Tie"
    return null
}
