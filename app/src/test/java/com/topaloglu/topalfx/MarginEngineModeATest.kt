package com.topaloglu.topalfx

import com.topaloglu.topalfx.data.CalcError
import com.topaloglu.topalfx.data.CalcInput
import com.topaloglu.topalfx.data.CalcMode
import com.topaloglu.topalfx.data.MarginEngine
import com.topaloglu.topalfx.data.TransferDirection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MarginEngineModeATest {

    private val delta = 1e-6

    @Test
    fun `basic cross-currency EUR to USD`() {
        val result = MarginEngine.calculateProfit(
            CalcInput(
                direction = TransferDirection.EUR_TO_USD,
                mode = CalcMode.SEND_EXACT,
                baseAmount = 1000.0,
                marketRate = 1.10,
                customerRate = 1.05,
            )
        )
        assertNull(result.error)
        assertEquals(1000.0, result.principal, delta)
        assertEquals(1050.0, result.targetDelivered, delta)
        assertEquals(1050.0, result.totalReceivedByCustomer, delta)
        assertEquals(1000.0, result.totalPaidByCustomer, delta)
        val expectedSpread = 1000.0 - 1050.0 / 1.10
        assertEquals(expectedSpread, result.hiddenSpread, delta)
        assertEquals(expectedSpread, result.netProfitBase, delta)
    }

    @Test
    fun `fee inclusive strips fees from principal and totalPaid equals baseAmount`() {
        val result = MarginEngine.calculateProfit(
            CalcInput(
                direction = TransferDirection.EUR_TO_USD,
                mode = CalcMode.SEND_EXACT,
                feeInclusive = true,
                baseAmount = 1000.0,
                flatFee = 10.0,
                pctFee = 2.0,
                marketRate = 1.10,
                customerRate = 1.05,
            )
        )
        assertNull(result.error)
        val expectedPrincipal = (1000.0 - 10.0) / 1.02
        assertEquals(expectedPrincipal, result.principal, delta)
        assertEquals(expectedPrincipal * 1.05, result.targetDelivered, delta)
        assertEquals(expectedPrincipal * 0.02, result.pctFeeBase, delta)
        assertEquals(1000.0, result.totalPaidByCustomer, 0.0)
    }

    @Test
    fun `fee exclusive adds fees on top of principal`() {
        val result = MarginEngine.calculateProfit(
            CalcInput(
                direction = TransferDirection.EUR_TO_USD,
                mode = CalcMode.SEND_EXACT,
                feeInclusive = false,
                baseAmount = 1000.0,
                flatFee = 10.0,
                pctFee = 2.0,
                marketRate = 1.10,
                customerRate = 1.05,
            )
        )
        assertNull(result.error)
        assertEquals(1000.0, result.principal, delta)
        assertEquals(1000.0 + 10.0 + 20.0, result.totalPaidByCustomer, delta)
    }

    @Test
    fun `USD base pct fee branch matches spec formula`() {
        val result = MarginEngine.calculateProfit(
            CalcInput(
                direction = TransferDirection.USD_TO_EUR,
                mode = CalcMode.SEND_EXACT,
                baseAmount = 1000.0,
                pctFee = 2.0,
                marketRate = 0.92,
                customerRate = 0.90,
            )
        )
        assertNull(result.error)
        // (targetDelivered * pct) / (100 * customerRate) = (900 * 2) / (100 * 0.9) = 20
        assertEquals(20.0, result.pctFeeBase, delta)
    }

    @Test
    fun `delivery fee reduces receiver total and converts to base for net profit`() {
        val result = MarginEngine.calculateProfit(
            CalcInput(
                direction = TransferDirection.EUR_TO_USD,
                mode = CalcMode.SEND_EXACT,
                baseAmount = 1000.0,
                deliveryFee = 22.0,
                marketRate = 1.10,
                customerRate = 1.05,
            )
        )
        assertNull(result.error)
        assertEquals(1050.0 - 22.0, result.totalReceivedByCustomer, delta)
        assertEquals(20.0, result.deliveryFeeBase, delta)
        val expectedSpread = 1000.0 - 1050.0 / 1.10
        assertEquals(expectedSpread - 20.0, result.netProfitBase, delta)
    }

    @Test
    fun `same-currency locks rates to 1 and zeroes hidden spread`() {
        val result = MarginEngine.calculateProfit(
            CalcInput(
                direction = TransferDirection.EUR_TO_EUR,
                mode = CalcMode.SEND_EXACT,
                baseAmount = 1000.0,
                deliveryFee = 5.0,
                // Garbage rates must be ignored by the static lock.
                marketRate = 1.2,
                customerRate = 1.3,
            )
        )
        assertNull(result.error)
        assertEquals(1000.0, result.targetDelivered, delta)
        assertEquals(0.0, result.hiddenSpread, 0.0)
        assertEquals(995.0, result.totalReceivedByCustomer, delta)
        assertEquals(5.0, result.deliveryFeeBase, delta)
    }

    @Test
    fun `validation errors are distinct`() {
        val base = CalcInput(
            direction = TransferDirection.EUR_TO_USD,
            mode = CalcMode.SEND_EXACT,
            baseAmount = 1000.0,
            marketRate = 1.10,
            customerRate = 1.05,
        )
        assertEquals(
            CalcError.NEGATIVE_VALUE,
            MarginEngine.calculateProfit(base.copy(baseAmount = -1.0)).error
        )
        assertEquals(
            CalcError.INVALID_NUMBER,
            MarginEngine.calculateProfit(base.copy(marketRate = Double.NaN)).error
        )
        assertEquals(
            CalcError.MISSING_RATE,
            MarginEngine.calculateProfit(base.copy(marketRate = 0.0)).error
        )
        assertEquals(
            CalcError.FEE_OVERFLOW,
            MarginEngine.calculateProfit(base.copy(feeInclusive = true, flatFee = 1000.0)).error
        )
    }
}
