package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.i18n.LocalAppStrings
import com.example.model.GameLogic
import com.example.model.GameType
import com.example.model.MultiplayerGame
import com.example.ui.theme.MsnAzurePrimary
import com.example.ui.theme.MsnGreenOnline
import com.example.ui.theme.MsnRedBusy

@Composable
fun InChatGamePanel(
    currentUserId: String,
    partnerUid: String,
    games: List<MultiplayerGame>,
    onStartNewGame: (GameType) -> Unit,
    onPlayGomoku: (MultiplayerGame, Int, Boolean) -> Unit,
    onPlayMemory: (MultiplayerGame, Int) -> Unit,
    onClose: () -> Unit
) {
    val strings = LocalAppStrings.current
    var selectedType by remember { mutableStateOf(GameType.GOMOKU_XO) }
    val isSelfChat = currentUserId == partnerUid
    var soloBotMode by remember(partnerUid) { mutableStateOf(isSelfChat) }

    val activeGame = remember(games, selectedType) {
        games.firstOrNull { it.gameTypeEnum() == selectedType }
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 4.dp,
        modifier = Modifier
            .fillMaxWidth()
            .border(1.5.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(
                        selected = selectedType == GameType.GOMOKU_XO,
                        onClick = { selectedType = GameType.GOMOKU_XO },
                        label = { Text(strings.gameGomokuTitle, maxLines = 1) },
                        modifier = Modifier.testTag("game_tab_gomoku")
                    )
                    FilterChip(
                        selected = selectedType == GameType.MEMORY_MATCH,
                        onClick = { selectedType = GameType.MEMORY_MATCH },
                        label = { Text(strings.gameMemoryTitle, maxLines = 1) },
                        modifier = Modifier.testTag("game_tab_memory")
                    )
                }
                OutlinedButton(
                    onClick = onClose,
                    modifier = Modifier.testTag("close_games_button")
                ) {
                    Text("✕")
                }
            }

            if (isSelfChat && selectedType == GameType.GOMOKU_XO) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(strings.practiceVsBot, style = MaterialTheme.typography.labelMedium)
                    Switch(checked = soloBotMode, onCheckedChange = { soloBotMode = it })
                }
            }

            if (activeGame == null) {
                Text(
                    text = if (selectedType == GameType.GOMOKU_XO) strings.gomokuRuleHint else strings.gameMemoryTitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Button(
                    onClick = { onStartNewGame(selectedType) },
                    modifier = Modifier.testTag("start_new_game_button")
                ) {
                    Text(strings.newGameBtn)
                }
            } else {
                // Status & Score Banner
                val statusMsg = when {
                    activeGame.status == "FINISHED" && activeGame.winnerUid == "DRAW" -> strings.drawGame
                    activeGame.status == "FINISHED" && activeGame.winnerUid == currentUserId -> "🏆 ${strings.youWon}"
                    activeGame.status == "FINISHED" -> strings.opponentWon
                    activeGame.currentTurnUid == currentUserId -> "🟢 ${strings.yourTurn}"
                    else -> "⏳ ${strings.opponentTurn}"
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = statusMsg,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        color = if (activeGame.status == "FINISHED") MsnGreenOnline else MaterialTheme.colorScheme.primary
                    )
                    OutlinedButton(
                        onClick = { onStartNewGame(selectedType) },
                        modifier = Modifier.testTag("restart_game_button")
                    ) {
                        Text(strings.newGameBtn)
                    }
                }

                if (selectedType == GameType.GOMOKU_XO) {
                    Text(
                        text = strings.gomokuRuleHint,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    GomokuBoard(
                        game = activeGame,
                        canTap = activeGame.status == "ACTIVE" && (soloBotMode || isSelfChat || activeGame.currentTurnUid == currentUserId),
                        onCellTap = { idx -> onPlayGomoku(activeGame, idx, soloBotMode) }
                    )
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Text("X: ${activeGame.scoreX}", fontWeight = FontWeight.Bold, color = MsnAzurePrimary)
                        Text("O: ${activeGame.scoreO}", fontWeight = FontWeight.Bold, color = MsnRedBusy)
                    }
                    MemoryBoard(
                        game = activeGame,
                        canTap = activeGame.status == "ACTIVE" && (isSelfChat || activeGame.currentTurnUid == currentUserId),
                        onCardTap = { idx -> onPlayMemory(activeGame, idx) }
                    )
                }
            }
        }
    }
}

@Composable
private fun GomokuBoard(
    game: MultiplayerGame,
    canTap: Boolean,
    onCellTap: (Int) -> Unit
) {
    val size = GameLogic.GOMOKU_SIZE
    val winningSet = remember(game.winningLine) { game.winningLine.map { it.toInt() }.toSet() }

    BoxWithConstraints(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        val boardWidth = minOf(maxWidth, 330.dp)
        val cellSize = (boardWidth - 12.dp) / size
        Column(
            modifier = Modifier
                .width(boardWidth)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFFDCEBFC))
                .padding(4.dp),
            verticalArrangement = Arrangement.spacedBy(1.dp)
        ) {
            for (r in 0 until size) {
                Row(horizontalArrangement = Arrangement.spacedBy(1.dp)) {
                    for (c in 0 until size) {
                        val idx = r * size + c
                        val valStr = game.boardCells.getOrElse(idx) { "" }
                        val isWinCell = winningSet.contains(idx)
                        Box(
                            modifier = Modifier
                                .size(cellSize)
                                .background(
                                    when {
                                        isWinCell -> Color(0xFFFFF59D)
                                        else -> Color.White
                                    }
                                )
                                .border(0.5.dp, Color(0xFF9EC0EA))
                                .clickable(enabled = canTap && valStr.isEmpty()) { onCellTap(idx) }
                                .testTag("xo_cell_$idx"),
                            contentAlignment = Alignment.Center
                        ) {
                            if (valStr.isNotEmpty()) {
                                Text(
                                    text = valStr,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (valStr == "X") MsnAzurePrimary else MsnRedBusy
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MemoryBoard(
    game: MultiplayerGame,
    canTap: Boolean,
    onCardTap: (Int) -> Unit
) {
    val revealed = remember(game.revealedIndices) { game.revealedIndices.map { it.toInt() }.toSet() }
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        for (row in 0 until 4) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                for (col in 0 until 4) {
                    val idx = row * 4 + col
                    val matched = game.matchedMask.getOrElse(idx) { 0L } == 1L
                    val isOpen = matched || revealed.contains(idx)
                    val symbol = game.boardCells.getOrElse(idx) { "❓" }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1.15f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                when {
                                    matched -> Color(0xFFD8F5DC)
                                    isOpen -> Color(0xFFFFF9C4)
                                    else -> MsnAzurePrimary
                                }
                            )
                            .border(
                                1.dp,
                                if (matched) MsnGreenOnline else Color(0xFF7BA4D8),
                                RoundedCornerShape(8.dp)
                            )
                            .clickable(enabled = canTap && !isOpen) { onCardTap(idx) }
                            .testTag("memory_card_$idx"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isOpen) symbol else "🦋",
                            fontSize = if (isOpen) 22.sp else 18.sp,
                            textAlign = TextAlign.Center,
                            color = if (isOpen) Color.Unspecified else Color.White.copy(alpha = 0.85f)
                        )
                    }
                }
            }
        }
    }
}
