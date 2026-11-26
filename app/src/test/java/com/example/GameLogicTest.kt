package com.example

import com.example.i18n.MsnLanguages
import com.example.model.GameLogic
import com.example.model.MsnCatalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class GameLogicTest {

    @Test
    fun gomokuDetectsHorizontalVerticalAndDiagonalFiveInARow() {
        val boardH = GameLogic.createEmptyGomokuBoard().toMutableList()
        for (c in 2..6) boardH[c] = "X"
        val winH = GameLogic.checkGomokuWin(boardH, 6)
        assertNotNull(winH)
        assertEquals(5, winH?.size)

        val boardV = GameLogic.createEmptyGomokuBoard().toMutableList()
        for (r in 1..5) boardV[r * GameLogic.GOMOKU_SIZE + 3] = "O"
        val winV = GameLogic.checkGomokuWin(boardV, 5 * GameLogic.GOMOKU_SIZE + 3)
        assertNotNull(winV)
        assertEquals(5, winV?.size)

        val boardD = GameLogic.createEmptyGomokuBoard().toMutableList()
        for (k in 0..4) boardD[(2 + k) * GameLogic.GOMOKU_SIZE + (2 + k)] = "X"
        val winD = GameLogic.checkGomokuWin(boardD, 6 * GameLogic.GOMOKU_SIZE + 6)
        assertNotNull(winD)
        assertEquals(5, winD?.size)

        val boardFour = GameLogic.createEmptyGomokuBoard().toMutableList()
        for (c in 0..3) boardFour[c] = "X"
        assertNull(GameLogic.checkGomokuWin(boardFour, 3))
    }

    @Test
    fun memoryDeckHas16CardsIn8PairsAndLanguagesHave26Entries() {
        val deck = GameLogic.createShuffledMemoryBoard()
        assertEquals(16, deck.size)
        assertEquals(8, deck.distinct().size)
        assertEquals(26, MsnLanguages.all.size)
    }

    @Test
    fun emoticonsExpandShortcutsCleanly() {
        val expanded = MsnCatalog.expandEmoticons("Szia :) (L) :D")
        assertEquals("Szia 😊 ❤️ 😃", expanded)
    }
}
