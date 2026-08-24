package com.topaloglu.topalfx

import com.topaloglu.topalfx.data.CalcInput
import com.topaloglu.topalfx.data.CalcMode
import com.topaloglu.topalfx.data.MarginEngine
import com.topaloglu.topalfx.data.TransferDirection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * The four transfers the office owner worked through by hand during the spec session.
 * These are the regression anchor: if any of them moves, the engine no longer matches
 * how the office actually prices a transfer.
 *
 * Shared setup: market 1.1699, أتعاب 5%, إعادة التنزيل 2.5% على المقبوض, تسليم 1 unit.
 */
class MarginEngineOwnerVectorsTest {

    private val delta = 1e-3

    @Test
    fun `fee inclusive at market rate delivers 111 140 and earns 1 645`() {
        val result = MarginEngine.calculateProfit(
            CalcInput(
                direction = TransferDirection.EUR_TO_USD,
                mode = CalcMode.SEND_EXACT,
                feeInclusive = true,
                baseAmount = 100.0,
                marketRate = 1.1699,
                customerRate = 1.1699,
                pctFee = 5.0,
                pctAgentCost = 2.5,
                deliveryFee = 1.0,
            )
        )
        assertNull(result.error)
        assertEquals(100.0, result.cashReceived, delta)
        assertEquals(95.0, result.transferAmount, delta)
        assertEquals(111.1405, result.targetDelivered, delta)
        assertEquals(5.0, result.customerFeeBase, delta)
        assertEquals(0.0, result.hiddenSpread, delta)
        assertEquals(2.5, result.agentCostBase, delta)
        assertEquals(0.854774, result.deliveryFeeBase, delta)
        assertEquals(1.6452, result.netProfitBase, delta)
    }

    @Test
    fun `fee exclusive at market rate delivers 116 990 and earns 1 520`() {
        val result = MarginEngine.calculateProfit(
            CalcInput(
                direction = TransferDirection.EUR_TO_USD,
                mode = CalcMode.SEND_EXACT,
                feeInclusive = false,
                baseAmount = 100.0,
                marketRate = 1.1699,
                customerRate = 1.1699,
                pctFee = 5.0,
                pctAgentCost = 2.5,
                deliveryFee = 1.0,
            )
        )
        assertNull(result.error)
        assertEquals(105.0, result.cashReceived, delta)
        assertEquals(100.0, result.transferAmount, delta)
        assertEquals(116.99, result.targetDelivered, delta)
        assertEquals(5.0, result.customerFeeBase, delta)
        // إعادة التنزيل is charged on the full cash taken in, so 105 not 100.
        assertEquals(2.625, result.agentCostBase, delta)
        assertEquals(1.5202, result.netProfitBase, delta)
    }

    @Test
    fun `same currency exclusive collects 105 delivers 100 and earns 1 375`() {
        val result = MarginEngine.calculateProfit(
            CalcInput(
                direction = TransferDirection.EUR_TO_EUR,
                mode = CalcMode.SEND_EXACT,
                feeInclusive = false,
                baseAmount = 100.0,
                // Garbage rates must be ignored by the same-currency lock.
                marketRate = 1.3,
                customerRate = 1.4,
                pctFee = 5.0,
                pctAgentCost = 2.5,
                deliveryFee = 1.0,
            )
        )
        assertNull(result.error)
        assertEquals(105.0, result.cashReceived, delta)
        assertEquals(100.0, result.targetDelivered, delta)
        assertEquals(0.0, result.hiddenSpread, 0.0)
        assertEquals(1.0, result.deliveryFeeBase, delta)
        assertEquals(1.375, result.netProfitBase, delta)
    }

    @Test
    fun `receive exact for 500 dollars costs the customer 453 76 and earns 14 174`() {
        val result = MarginEngine.calculateProfit(
            CalcInput(
                direction = TransferDirection.EUR_TO_USD,
                mode = CalcMode.RECEIVE_EXACT,
                feeInclusive = true,
                targetReceived = 500.0,
                marketRate = 1.1699,
                customerRate = 1.1599,
                pctFee = 5.0,
                pctAgentCost = 2.5,
                deliveryFee = 1.0,
            )
        )
        assertNull(result.error)
        assertEquals(431.0716, result.transferAmount, delta)
        assertEquals(453.7596, result.cashReceived, delta)
        assertEquals(500.0, result.targetDelivered, delta)
        assertEquals(22.688, result.customerFeeBase, delta)
        assertEquals(3.6846, result.hiddenSpread, delta)
        assertEquals(11.344, result.agentCostBase, delta)
        assertEquals(14.1738, result.netProfitBase, delta)
    }
}
