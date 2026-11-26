package com.example.model

object GameLogic {
    const val GOMOKU_SIZE = 11
    const val GOMOKU_CELLS = GOMOKU_SIZE * GOMOKU_SIZE // 121
    const val GOMOKU_WIN_LENGTH = 5

    val memoryDeckSymbols = listOf(
        "😊", "😎", "❤️", "🎸",
        "🦋", "⭐", "🌹", "🎂"
    )

    fun createEmptyGomokuBoard(): List<String> = List(GOMOKU_CELLS) { "" }

    /**
     * Checks if placing symbol ("X" or "O") at [lastIndex] produces 5 or more in a row
     * horizontally, vertically, or diagonally. Returns the 5 winning cell indices as Longs,
     * or null if no winner yet.
     */
    fun checkGomokuWin(board: List<String>, lastIndex: Int): List<Long>? {
        if (lastIndex !in board.indices) return null
        val symbol = board[lastIndex]
        if (symbol != "X" && symbol != "O") return null

        val row = lastIndex / GOMOKU_SIZE
        val col = lastIndex % GOMOKU_SIZE
        val directions = listOf(
            0 to 1,   // Horizontal
            1 to 0,   // Vertical
            1 to 1,   // Diagonal down-right
            1 to -1   // Diagonal down-left
        )

        for ((dr, dc) in directions) {
            val line = mutableListOf<Int>()
            // Walk backwards
            var r = row - dr
            var c = col - dc
            while (r in 0 until GOMOKU_SIZE && c in 0 until GOMOKU_SIZE && board[r * GOMOKU_SIZE + c] == symbol) {
                line.add(0, r * GOMOKU_SIZE + c)
                r -= dr
                c -= dc
            }
            line.add(lastIndex)
            // Walk forwards
            r = row + dr
            c = col + dc
            while (r in 0 until GOMOKU_SIZE && c in 0 until GOMOKU_SIZE && board[r * GOMOKU_SIZE + c] == symbol) {
                line.add(r * GOMOKU_SIZE + c)
                r += dr
                c += dc
            }
            if (line.size >= GOMOKU_WIN_LENGTH) {
                return line.take(GOMOKU_WIN_LENGTH).map { it.toLong() }
            }
        }
        return null
    }

    /**
     * Picks a tactical bot move for Gomoku 5-in-a-row:
     * 1) Win immediately if possible, 2) Block opponent 4-in-a-row or 3-in-a-row, 3) Expand near existing pieces.
     */
    fun chooseGomokuBotMove(board: List<String>, botSymbol: String = "O", humanSymbol: String = "X"): Int? {
        val emptyIndices = board.indices.filter { board[it].isEmpty() }
        if (emptyIndices.isEmpty()) return null

        // 1. Immediate win
        for (idx in emptyIndices) {
            val copy = board.toMutableList()
            copy[idx] = botSymbol
            if (checkGomokuWin(copy, idx) != null) return idx
        }
        // 2. Immediate block
        for (idx in emptyIndices) {
            val copy = board.toMutableList()
            copy[idx] = humanSymbol
            if (checkGomokuWin(copy, idx) != null) return idx
        }

        // 3. Heuristic score based on adjacent friendly/enemy chains
        var bestIdx = emptyIndices.first()
        var bestScore = -1
        val center = GOMOKU_CELLS / 2
        for (idx in emptyIndices) {
            val r = idx / GOMOKU_SIZE
            val c = idx % GOMOKU_SIZE
            var score = 0
            for (dr in -1..1) {
                for (dc in -1..1) {
                    if (dr == 0 && dc == 0) continue
                    val nr = r + dr
                    val nc = c + dc
                    if (nr in 0 until GOMOKU_SIZE && nc in 0 until GOMOKU_SIZE) {
                        when (board[nr * GOMOKU_SIZE + nc]) {
                            botSymbol -> score += 4
                            humanSymbol -> score += 3
                        }
                    }
                }
            }
            if (idx == center) score += 2
            if (score > bestScore) {
                bestScore = score
                bestIdx = idx
            }
        }
        return bestIdx
    }

    fun createShuffledMemoryBoard(): List<String> {
        return (memoryDeckSymbols + memoryDeckSymbols).shuffled()
    }
}
