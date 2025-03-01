package com.plcoding.bluetoothtris.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import com.plcoding.bluetoothtris.presentation.BluetoothUiState
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
    val localRandomNumber = remember { Random.nextInt(0, 10000) }
    var remoteRandomNumber by remember { mutableStateOf<Int?>(null) }
    var playerSymbol by remember { mutableStateOf("") }
    var opponentSymbol by remember { mutableStateOf("") }

    // Variabile per gestire il turno iniziale: il giocatore "X" inizia per default
    var startingPlayer by remember { mutableStateOf("X") }
    var currentPlayer by remember { mutableStateOf(startingPlayer) }

    // --- Stato partita ---
    var board by remember { mutableStateOf(List(3) { MutableList(3) { "" } }) }
    var winner by remember { mutableStateOf<String?>(null) }
    var replayRequestSent by remember { mutableStateOf(false) }
    var replayRequestReceived by remember { mutableStateOf(false) }

    // --- Stato contatore punti ---
    var playerScore by remember { mutableStateOf(0) }
    var opponentScore by remember { mutableStateOf(0) }
    // Flag per assicurarsi che i punti vengano aggiornati una sola volta per partita
    var scoreUpdated by remember { mutableStateOf(false) }

    // Funzione per resettare la partita mantenendo i ruoli,
    // ma alternando il turno iniziale ad ogni nuova partita.
    fun resetGame() {
        board = List(3) { MutableList(3) { "" } }
        winner = null
        // Invertiamo il turno iniziale: se era "X" diventa "O" e viceversa.
        startingPlayer = if (startingPlayer == "X") "O" else "X"
        currentPlayer = startingPlayer
        replayRequestSent = false
        replayRequestReceived = false
        scoreUpdated = false // Resettiamo il flag per la nuova partita
    }

    // Invia l'handshake solo una volta
    LaunchedEffect(Unit) {
        onSendHandshake(localRandomNumber)
    }

    // Aggiorna il contatore dei punti quando viene determinato un vincitore
    LaunchedEffect(winner) {
        if (winner != null && !scoreUpdated) {
            if (winner != "Tie") {
                if (winner == playerSymbol) {
                    playerScore++
                } else {
                    opponentScore++
                }
            }
            scoreUpdated = true
        }
    }

    // Gestione dei messaggi in arrivo
    LaunchedEffect(state.messages) {
        state.messages.lastOrNull()?.let { message ->
            val msg = message.message
            if (!handshakeCompleted) {
                if (msg.startsWith("HANDSHAKE:") && msg != "HANDSHAKE:$localRandomNumber") {
                    val num = msg.removePrefix("HANDSHAKE:").toIntOrNull()
                    if (num != null) {
                        remoteRandomNumber = num
                        playerSymbol = if (localRandomNumber >= num) "X" else "O"
                        opponentSymbol = if (playerSymbol == "X") "O" else "X"
                        handshakeCompleted = true
                        // Il turno iniziale viene determinato dalla variabile startingPlayer.
                        // Quindi, se il giocatore locale è "O", l'avversario inizierà.
                        currentPlayer = startingPlayer
                    }
                }
            } else {
                when {
                    msg.startsWith("REPLAY_REQUEST") -> if (!replayRequestSent) replayRequestReceived = true
                    msg.startsWith("REPLAY_ACCEPTED") -> if (replayRequestSent) resetGame()
                    msg.startsWith("REPLAY_DECLINED") -> replayRequestSent = false
                    msg.contains(",") -> {
                        val (row, col) = msg.split(",").mapNotNull { it.toIntOrNull() }
                        if (board[row][col].isEmpty()) {
                            board[row][col] = opponentSymbol
                            currentPlayer = playerSymbol
                            winner = checkWinner(board)
                        }
                    }
                }
            }
        }
    }

    if (!handshakeCompleted) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Attesa connessione...", fontSize = 20.sp, color = Color.White)
        }
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(text = "TRIS", fontSize = 32.sp, color = Color.White)
            Spacer(modifier = Modifier.height(8.dp))
            // Visualizzazione del contatore dei punti
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Text("Tu: $playerScore", fontSize = 20.sp, color = Color.White)
                Text("Avversario: $opponentScore", fontSize = 20.sp, color = Color.White)
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = when {
                    winner == "Tie" -> "Pareggio!"
                    winner != null -> "Vincitore: $winner"
                    currentPlayer == playerSymbol -> "Tocca a te ($playerSymbol)"
                    else -> "Turno avversario ($opponentSymbol)"
                },
                fontSize = 20.sp,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(16.dp))

            // Griglia del gioco
            TrisGrid(board = board, onCellClick = { row, col ->
                if (currentPlayer == playerSymbol && board[row][col].isEmpty() && winner == null) {
                    board[row][col] = playerSymbol
                    onSendMove(row, col)
                    currentPlayer = opponentSymbol
                    winner = checkWinner(board)
                }
            })

            Spacer(modifier = Modifier.height(20.dp))

            if (winner != null && !replayRequestSent && !replayRequestReceived) {
                Button(onClick = {
                    onSendReplay("REPLAY_REQUEST")
                    replayRequestSent = true
                }) {
                    Text("Rigioca")
                }
            }

            if (replayRequestReceived && !replayRequestSent) {
                Spacer(modifier = Modifier.height(16.dp))
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("L'avversario vuole rigiocare. Accetti?", color = Color.White)
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

@Composable
fun TrisGrid(
    board: List<List<String>>,
    onCellClick: (Int, Int) -> Unit
) {
    Column(
        modifier = Modifier
            .size(300.dp)
            .background(Color.Black) // Sfondo per simulare i bordi della griglia
    ) {
        board.forEachIndexed { rowIndex, row ->
            Row(modifier = Modifier.weight(1f)) {
                row.forEachIndexed { colIndex, cell ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxSize()
                            .background(Color.White) // Sfondo della cella
                            .clickable { onCellClick(rowIndex, colIndex) }
                            .border(2.dp, Color.Black), // Bordo della cella
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = cell, fontSize = 36.sp, color = Color.Black)
                    }
                }
            }
        }
    }
}

fun checkWinner(board: List<List<String>>): String? {
    for (i in 0..2) {
        if (board[i][0] == board[i][1] && board[i][1] == board[i][2] && board[i][0].isNotEmpty())
            return board[i][0]  // Controllo righe
        if (board[0][i] == board[1][i] && board[1][i] == board[2][i] && board[0][i].isNotEmpty())
            return board[0][i]  // Controllo colonne
    }
    // Controllo diagonale principale
    if (board[0][0] == board[1][1] && board[1][1] == board[2][2] && board[0][0].isNotEmpty())
        return board[0][0]

    // Controllo diagonale secondaria
    if (board[0][2] == board[1][1] && board[1][1] == board[2][0] && board[0][2].isNotEmpty())
        return board[0][2]

    // Controllo pareggio
    return if (board.flatten().all { it.isNotEmpty() }) "Tie" else null
}
