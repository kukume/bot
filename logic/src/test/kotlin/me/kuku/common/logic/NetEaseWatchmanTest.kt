package me.kuku.common.logic

import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.Test

class NetEaseWatchmanTest {

    @Test
    fun checkTokenFromOfficialSdkWithoutBrowser() {
        assumeTrue(NetEaseWatchman.nodeAvailable(), "node is required")
        val token = NetEaseWatchman.checkToken()
        assertTrue(token.length >= 64, "token too short: ${token.length}")
        assertTrue(token.all { it.isLetterOrDigit() }, "token should be encoded text")
        val again = NetEaseWatchman.checkToken()
        assertNotEquals(token, again)
    }
}
