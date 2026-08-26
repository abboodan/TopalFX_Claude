package com.topaloglu.topalfx

import com.topaloglu.topalfx.data.CalcError
import com.topaloglu.topalfx.data.CalcInput
import com.topaloglu.topalfx.data.CalcMode
import com.topaloglu.topalfx.data.CashDownloadEngine
import com.topaloglu.topalfx.data.CashDownloadInput
import com.topaloglu.topalfx.data.MarginEngine
import com.topaloglu.topalfx.data.TransferDirection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CashDownloadEngineTest {

    private val delta = 1e-9

    /**
     * The office's own report: 100 in hand at 2% leaves 98 in the wallet. Shipping this
     * the other way round first is what made 102.04 show up on screen.
     */
    @Test
    fun `fee from the amount leaves the cut behind in the wallet`() {
        val result = CashDownloadEngine.calculate(
            CashDownloadInput(amount = 100.0, pctRate = 2.0, feeFromAmount = true)
        )
        assertNull(result.error)
        assertEquals(100.0, result.cashRequired, delta)
        assertEquals(98.0, result.digitalArrived, delta)
        assertEquals(2.0, result.cost, delta)
    }

    @Test
    fun `external fee grosses the cash up so the full amount arrives`() {
        val result = CashDownloadEngine.calculate(
            CashDownloadInput(amount = 100.0, pctRate = 2.0, feeFromAmount = false)
        )
        assertNull(result.error)
        assertEquals(102.040816, result.cashRequired, 1e-6)
        assertEquals(100.0, result.digitalArrived, delta)
        assertEquals(2.040816, result.cost, 1e-6)
    }

    @Test
    fun `the cost is always the percentage of the cash in both modes`() {
        listOf(true, false).forEach { fromAmount ->
            val result = CashDownloadEngine.calculate(
                CashDownloadInput(amount = 5000.0, pctRate = 2.5, feeFromAmount = fromAmount)
            )
            assertEquals(result.cashRequired * 0.025, result.cost, 1e-9)
            assertEquals(result.cashRequired - result.digitalArrived, result.cost, 1e-9)
        }
    }

    @Test
    fun `the two modes are inverses of each other`() {
        val external = CashDownloadEngine.calculate(
            CashDownloadInput(amount = 5000.0, pctRate = 2.5, feeFromAmount = false)
        )
        // Feeding the cash it asked for back in the other mode returns the original.
        val roundTrip = CashDownloadEngine.calculate(
            CashDownloadInput(
                amount = external.cashRequired,
                pctRate = 2.5,
                feeFromAmount = true,
            )
        )
        assertEquals(5000.0, roundTrip.digitalArrived, 1e-9)
    }

    /** The transfer engine charges the same operation; the two must not drift apart. */
    @Test
    fun `it agrees with the transfer engine on the same cash amount`() {
        val download = CashDownloadEngine.calculate(
            CashDownloadInput(amount = 5000.0, pctRate = 2.5, feeFromAmount = true)
        )
        val transfer = MarginEngine.calculateProfit(
            CalcInput(
                direction = TransferDirection.EUR_TO_EUR,
                mode = CalcMode.SEND_EXACT,
                feeInclusive = true,
                baseAmount = 5000.0,
                marketRate = 1.0,
                customerRate = 1.0,
                pctFee = 0.0,
                pctAgentCost = 2.5,
            )
        )
        assertEquals(download.cost, transfer.agentCostBase, 1e-9)
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
        assertEquals(10000.0, result.cashRequired, 1e-6)
    }
}
