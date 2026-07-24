package me.kuku.common.logic

import org.junit.jupiter.api.Assertions.assertEquals
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
    }
}
