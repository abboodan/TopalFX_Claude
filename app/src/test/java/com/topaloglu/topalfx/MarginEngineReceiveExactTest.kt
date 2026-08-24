package com.topaloglu.topalfx

import com.topaloglu.topalfx.data.CalcInput
import com.topaloglu.topalfx.data.CalcMode
import com.topaloglu.topalfx.data.MarginEngine
import com.topaloglu.topalfx.data.TransferDirection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MarginEngineReceiveExactTest {

    private val delta = 1e-9

    private fun input(
        feeInclusive: Boolean = true,
        targetReceived: Double = 500.0,
        marketRate: Double = 1.10,
        customerRate: Double = 1.05,
        pctFee: Double = 4.0,
        pctAgentCost: Double = 2.0,
        deliveryFee: Double = 0.0,
    ) = CalcInput(
        direction = TransferDirection.EUR_TO_USD,
        mode = CalcMode.RECEIVE_EXACT,
        feeInclusive = feeInclusive,
        targetReceived = targetReceived,
        marketRate = marketRate,
        customerRate = customerRate,
        pctFee = pctFee,
        pctAgentCost = pctAgentCost,
        deliveryFee = deliveryFee,
    )

    /**
     * The old engine grossed the requested amount up by أجرة التسليم, so the beneficiary
     * was quoted one number and received another.
     */
    @Test
    fun `the beneficiary receives exactly what was asked for even with a delivery fee`() {
        val result = MarginEngine.calculateProfit(input(deliveryFee = 12.0))
        assertNull(result.error)
        assertEquals(500.0, result.targetDelivered, delta)
    }

    @Test
    fun `fee inclusive grosses the cash up so the fee comes out of it`() {
        val result = MarginEngine.calculateProfit(input(feeInclusive = true))
        val transfer = 500.0 / 1.05
        assertEquals(transfer, result.transferAmount, delta)
        assertEquals(transfer / 0.96, result.cashReceived, delta)
        assertEquals(result.cashReceived - transfer, result.customerFeeBase, delta)
    }

    @Test
    fun `fee exclusive adds the fee on top of the transfer amount`() {
        val result = MarginEngine.calculateProfit(input(feeInclusive = false))
        val transfer = 500.0 / 1.05
        assertEquals(transfer, result.transferAmount, delta)
        assertEquals(transfer * 1.04, result.cashReceived, delta)
    }

    @Test
    fun `sending back the quoted cash reproduces the requested amount and profit`() {
        val reverse = MarginEngine.calculateProfit(input(feeInclusive = true, deliveryFee = 7.0))
        val forward = MarginEngine.calculateProfit(
            CalcInput(
                direction = TransferDirection.EUR_TO_USD,
                mode = CalcMode.SEND_EXACT,
                feeInclusive = true,
                baseAmount = reverse.cashReceived,
                marketRate = 1.10,
                customerRate = 1.05,
                pctFee = 4.0,
                pctAgentCost = 2.0,
                deliveryFee = 7.0,
            )
        )
        assertEquals(500.0, forward.targetDelivered, 1e-6)
        assertEquals(reverse.netProfitBase, forward.netProfitBase, 1e-6)
        assertEquals(reverse.transferAmount, forward.transferAmount, 1e-6)
    }

    @Test
    fun `same currency receive is a pass-through with no spread`() {
        val result = MarginEngine.calculateProfit(
            CalcInput(
                direction = TransferDirection.EUR_TO_EUR,
                mode = CalcMode.RECEIVE_EXACT,
                feeInclusive = true,
                targetReceived = 500.0,
                marketRate = 1.25,
                customerRate = 1.4,
                pctFee = 4.0,
                pctAgentCost = 2.0,
            )
        )
        assertNull(result.error)
        assertEquals(500.0, result.transferAmount, delta)
        assertEquals(500.0, result.targetDelivered, delta)
        assertEquals(0.0, result.hiddenSpread, 0.0)
    }
}
