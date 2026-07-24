package me.kuku.common.logic

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class KuGouCryptoTest {

    @Test
    fun encryptMobileCodeMatchesKguserVectors() {
        // Vectors generated from kguser.v2.min.js AES.encrypt + RSA.encrypt with fixed key.
        val phone = "13800138000"
        val time = 1710000000000L
        val key = "0123456789ABCDEF"
        val expectedParams = "9701ba232146623f585416002dd3c36692c9b8f8033b3e489f23c370621eefe0"
        val expectedPk =
            "5a680d268be841a6e37d842733e5ccda36da7aad8595e8c2d8a6f4deac536537a83a3c4fc314555d10437ce07a29836bd64e13b5e1d470b33b0f99d67e80f0a9132eddf2527bf3aade78b7223d619de8288354865999ca0350d6af519fae96517fdab3b128ef3ea0ca84615d7a08681211007675e6db70189b0f3baa08dc2720"

        val (params, pk) = KuGouLogic.encryptMobileCode(phone, time, fixedKey = key)
        assertEquals(expectedParams, params)
        assertEquals(expectedPk, pk)
        assertEquals(256, pk.length)
    }

    @Test
    fun rsaHexIsAlways256CharsWithLeadingZeros() {
        // Known kguser output that needs a leading '0' (BigInteger.toString(16) would be 255 chars).
        val phone = "13800138000"
        val time = 1784878857357L
        val key = "8QKCF3141DML22RR"
        val expectedPk =
            "0a1bb16d6dd26396cd877031e6e26f967dc1543c24dee10a1c2e7b1c445ffdd999907605c0f0fd1ff1cf53ea1d7d7b6be4a6dc8d6c2ad84f3f3edc88a48bd3ebf5ee58db46da23e1b1687fb9c63bbe274742bf3e7511e48fdf2e9b127f0210d3925dde0f73e7c75e538e3a2db427bbbbfa282baef0efee027d2f383eff52bd3b"

        val (_, pk) = KuGouLogic.encryptMobileCode(phone, time, fixedKey = key)
        assertEquals(256, pk.length)
        assertEquals(expectedPk, pk)
        assertTrue(pk.startsWith("0"))
    }
}
