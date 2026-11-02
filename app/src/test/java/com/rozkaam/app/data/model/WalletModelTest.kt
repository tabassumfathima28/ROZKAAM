package com.rozkaam.app.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WalletModelTest {

    @Test
    fun testWalletFromMap_parsesPaiseAndFormatsRupeesCorrectly() {
        val map = mapOf(
            "available" to 150000L, // ₹1,500
            "held" to 50000L        // ₹500
        )

        val wallet = Wallet.fromMap(map)

        assertEquals(150000L, wallet.available)
        assertEquals(50000L, wallet.held)
        assertEquals(1500.0, wallet.availableRupees, 0.001)
        assertEquals(500.0, wallet.heldRupees, 0.001)
        assertEquals("₹1500", wallet.formattedAvailable)
        assertEquals("₹500", wallet.formattedHeld)
        assertEquals(200000L, wallet.totalBalance)
        assertEquals("₹2000", wallet.formattedTotal)
    }

    @Test
    fun testWalletFromMap_handlesNullOrEmptyGracefully() {
        val emptyWallet = Wallet.fromMap(null)
        assertEquals(0L, emptyWallet.available)
        assertEquals(0L, emptyWallet.held)
        assertEquals("₹0", emptyWallet.formattedAvailable)
    }

    @Test
    fun testTransactionFromMap_creditAndDebitSemantics() {
        val topUpMap = mapOf(
            "uid" to "user_123",
            "type" to "topup",
            "amount" to 50000L,
            "razorpayPaymentId" to "pay_abc123",
            "createdAt" to 1700000000000L
        )

        val topUp = Transaction.fromMap("tx_01", topUpMap)
        assertEquals("tx_01", topUp.id)
        assertEquals("topup", topUp.type)
        assertEquals(50000L, topUp.amount)
        assertTrue(topUp.isCredit)
        assertEquals("+₹500", topUp.formattedAmount)
        assertEquals("Wallet Top-up", topUp.displayTitle)

        val holdMap = mapOf(
            "uid" to "user_123",
            "type" to "hold",
            "amount" to 30000L,
            "taskId" to "task_xyz",
            "createdAt" to 1700000000000L
        )

        val hold = Transaction.fromMap("tx_02", holdMap)
        assertEquals("hold", hold.type)
        assertEquals(30000L, hold.amount)
        assertFalse(hold.isCredit)
        assertEquals("-₹300", hold.formattedAmount)
        assertEquals("Escrow Hold", hold.displayTitle)
    }

    @Test
    fun testAppConfigFromMap_readsQuickAmountsAndDefaults() {
        val customConfigMap = mapOf(
            "quickAmounts" to listOf(100000L, 200000L, 500000L),
            "minPrice" to 20000L,
            "maxPrice" to 2000000L
        )

        val config = AppConfig.fromMap(customConfigMap)
        assertEquals(listOf(100000L, 200000L, 500000L), config.quickAmounts)
        assertEquals(20000L, config.minPrice)
        assertEquals(2000000L, config.maxPrice)

        val defaultConfig = AppConfig.fromMap(null)
        assertEquals(listOf(50000L, 100000L, 200000L, 500000L), defaultConfig.quickAmounts)
        assertEquals(10000L, defaultConfig.minPrice)
        assertEquals(1000000L, defaultConfig.maxPrice)
    }
}

