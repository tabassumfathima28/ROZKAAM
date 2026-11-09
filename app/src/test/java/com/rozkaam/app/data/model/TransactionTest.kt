package com.rozkaam.app.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TransactionTest {
    @Test
    fun releaseLedgerDirectionControlsBalanceDisplay() {
        val workerCredit = Transaction.fromMap(
            "release_worker",
            mapOf("type" to "release", "direction" to "credit", "amount" to 25000L)
        )
        val posterDebit = Transaction.fromMap(
            "release_poster",
            mapOf("type" to "release", "direction" to "debit", "amount" to 25000L)
        )

        assertTrue(workerCredit.isCredit)
        assertEquals("+₹250", workerCredit.formattedAmount)
        assertFalse(posterDebit.isCredit)
        assertEquals("-₹250", posterDebit.formattedAmount)
    }

    @Test
    fun legacyWorkerReleaseDefaultsToCredit() {
        val transaction = Transaction.fromMap(
            "legacy_release",
            mapOf("type" to "release", "amount" to 10000L)
        )

        assertTrue(transaction.isCredit)
    }
}
