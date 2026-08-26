package com.topaloglu.topalfx

import com.topaloglu.topalfx.data.CalcError
import com.topaloglu.topalfx.data.CashDownloadEngine
import com.topaloglu.topalfx.data.CashDownloadInput
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * The office quotes إعادة التنزيل against the amount being downloaded, so 2.5% on 100 is
 * 2.50 whichever side the fee sits on. No grossing up: 102.56 was wrong, 102.50 is right.
 *
 * This engine is intentionally not checked against MarginEngine — the download section is
 * a standalone calculator and the two must be free to differ.
 */
class CashDownloadEngineTest {

    private val delta = 1e-9

    @Test
    fun `fee out of the amount leaves the rest in the wallet`() {
        val result = CashDownloadEngine.calculate(
            CashDownloadInput(amount = 100.0, pctRate = 2.5, feeFromAmount = true)
        )
        assertNull(result.error)
        assertEquals(100.0, result.cashRequired, delta)
        assertEquals(97.5, result.digitalArrived, delta)
        assertEquals(2.5, result.cost, delta)
    }

    @Test
    fun `external fee is added on top of the amount`() {
        val result = CashDownloadEngine.calculate(
            CashDownloadInput(amount = 100.0, pctRate = 2.5, feeFromAmount = false)
        )
        assertNull(result.error)
        assertEquals(102.5, result.cashRequired, delta)
        assertEquals(100.0, result.digitalArrived, delta)
        assertEquals(2.5, result.cost, delta)
    }

    /** The whole point of the correction: the fee never compounds on itself. */
    @Test
    fun `the fee is identical in both directions`() {
        val internal = CashDownloadEngine.calculate(
            CashDownloadInput(amount = 100.0, pctRate = 2.5, feeFromAmount = true)
        )
        val external = CashDownloadEngine.calculate(
            CashDownloadInput(amount = 100.0, pctRate = 2.5, feeFromAmount = false)
        )
        assertEquals(internal.cost, external.cost, delta)
        assertEquals(2.5, external.cost, delta)
    }

    @Test
    fun `the fee is always the percentage of the typed amount`() {
        listOf(true, false).forEach { fromAmount ->
            val result = CashDownloadEngine.calculate(
                CashDownloadInput(amount = 5000.0, pctRate = 2.5, feeFromAmount = fromAmount)
            )
            assertEquals(125.0, result.cost, delta)
            assertEquals(result.cashRequired - result.digitalArrived, result.cost, delta)
        }
    }

    @Test
    fun `the two sides straddle the typed amount by the same fee`() {
        val amount = 800.0
        val internal = CashDownloadEngine.calculate(
            CashDownloadInput(amount = amount, pctRate = 4.0, feeFromAmount = true)
        )
        val external = CashDownloadEngine.calculate(
            CashDownloadInput(amount = amount, pctRate = 4.0, feeFromAmount = false)
        )
        assertEquals(amount - 32.0, internal.digitalArrived, delta)
        assertEquals(amount + 32.0, external.cashRequired, delta)
    }

    @Test
    fun `a zero rate is a pass-through in both modes`() {
        listOf(true, false).forEach { fromAmount ->
            val result = CashDownloadEngine.calculate(
                CashDownloadInput(amount = 800.0, pctRate = 0.0, feeFromAmount = fromAmount)
            )
            assertNull(result.error)
            assertEquals(800.0, result.cashRequired, delta)
            assertEquals(800.0, result.digitalArrived, delta)
            assertEquals(0.0, result.cost, delta)
        }
    }

    @Test
    fun `invalid input reports its own error`() {
        assertEquals(
            CalcError.NEGATIVE_VALUE,
            CashDownloadEngine.calculate(
                CashDownloadInput(amount = -1.0, pctRate = 2.5)
            ).error
        )
        assertEquals(
            CalcError.INVALID_NUMBER,
            CashDownloadEngine.calculate(
                CashDownloadInput(amount = Double.NaN, pctRate = 2.5)
            ).error
        )
        assertEquals(
            CalcError.FEE_OVERFLOW,
            CashDownloadEngine.calculate(
                CashDownloadInput(amount = 100.0, pctRate = 100.0)
            ).error
        )
    }

    @Test
    fun `a rate just under 100 percent is allowed however absurd`() {
        val result = CashDownloadEngine.calculate(
            CashDownloadInput(amount = 100.0, pctRate = 99.0, feeFromAmount = false)
        )
        assertNull(result.error)
        assertEquals(199.0, result.cashRequired, delta)
    }
}
