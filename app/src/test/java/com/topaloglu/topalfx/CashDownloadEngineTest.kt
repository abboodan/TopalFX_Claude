package com.topaloglu.topalfx

import com.topaloglu.topalfx.data.CalcError
import com.topaloglu.topalfx.data.CashDownloadEngine
import com.topaloglu.topalfx.data.CashDownloadInput
import com.topaloglu.topalfx.data.MarginEngine
import com.topaloglu.topalfx.data.CalcInput
import com.topaloglu.topalfx.data.CalcMode
import com.topaloglu.topalfx.data.TransferDirection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CashDownloadEngineTest {

    private val delta = 1e-9

    @Test
    fun `it grosses the cash up so the wanted balance actually arrives`() {
        val result = CashDownloadEngine.calculate(
            CashDownloadInput(desiredDigital = 5000.0, pctRate = 2.5)
        )
        assertNull(result.error)
        assertEquals(5128.205128, result.cashRequired, 1e-6)
        assertEquals(128.205128, result.cost, 1e-6)
    }

    @Test
    fun `handing over the computed cash lands exactly on the wanted balance`() {
        val result = CashDownloadEngine.calculate(
            CashDownloadInput(desiredDigital = 5000.0, pctRate = 2.5)
        )
        val credited = result.cashRequired * (1.0 - 2.5 / 100.0)
        assertEquals(5000.0, credited, 1e-9)
    }

    /** The cost is a slice of the cash, never of the balance — same rule as the transfer engine. */
    @Test
    fun `the cost is the percentage of the cash handed over`() {
        val result = CashDownloadEngine.calculate(
            CashDownloadInput(desiredDigital = 5000.0, pctRate = 2.5)
        )
        assertEquals(result.cashRequired * 0.025, result.cost, delta)
        assertTrue("charging on the balance would read 125.00", result.cost > 125.0)
    }

    @Test
    fun `it agrees with the transfer engine on the same cash amount`() {
        val download = CashDownloadEngine.calculate(
            CashDownloadInput(desiredDigital = 5000.0, pctRate = 2.5)
        )
        // Feeding the same cash through the transfer engine must charge the same cost.
        val transfer = MarginEngine.calculateProfit(
            CalcInput(
                direction = TransferDirection.EUR_TO_EUR,
                mode = CalcMode.SEND_EXACT,
                feeInclusive = true,
                baseAmount = download.cashRequired,
                marketRate = 1.0,
                customerRate = 1.0,
                pctFee = 0.0,
                pctAgentCost = 2.5,
            )
        )
        assertEquals(download.cost, transfer.agentCostBase, 1e-9)
    }

    @Test
    fun `a zero rate is a pass-through`() {
        val result = CashDownloadEngine.calculate(
            CashDownloadInput(desiredDigital = 800.0, pctRate = 0.0)
        )
        assertNull(result.error)
        assertEquals(800.0, result.cashRequired, delta)
        assertEquals(0.0, result.cost, delta)
    }

    @Test
    fun `invalid input reports its own error`() {
        assertEquals(
            CalcError.NEGATIVE_VALUE,
            CashDownloadEngine.calculate(
                CashDownloadInput(desiredDigital = -1.0, pctRate = 2.5)
            ).error
        )
        assertEquals(
            CalcError.INVALID_NUMBER,
            CashDownloadEngine.calculate(
                CashDownloadInput(desiredDigital = Double.NaN, pctRate = 2.5)
            ).error
        )
        assertEquals(
            CalcError.FEE_OVERFLOW,
            CashDownloadEngine.calculate(
                CashDownloadInput(desiredDigital = 100.0, pctRate = 100.0)
            ).error
        )
    }

    @Test
    fun `a rate just under 100 percent is allowed however absurd`() {
        val result = CashDownloadEngine.calculate(
            CashDownloadInput(desiredDigital = 100.0, pctRate = 99.0)
        )
        assertNull(result.error)
        assertEquals(10000.0, result.cashRequired, 1e-6)
    }
}
