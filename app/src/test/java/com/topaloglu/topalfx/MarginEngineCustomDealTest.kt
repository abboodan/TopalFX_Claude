package com.topaloglu.topalfx

import com.topaloglu.topalfx.data.CalcInput
import com.topaloglu.topalfx.data.CalcMode
import com.topaloglu.topalfx.data.MarginEngine
import com.topaloglu.topalfx.data.TransferDirection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MarginEngineCustomDealTest {

    private val delta = 1e-9

    private fun input(
        direction: TransferDirection = TransferDirection.EUR_TO_USD,
        received: Double = 1000.0,
        delivered: Double = 1040.0,
        marketRate: Double = 1.10,
        pctAgentCost: Double = 0.0,
    ) = CalcInput(
        direction = direction,
        mode = CalcMode.CUSTOM_DEAL,
        customBaseReceived = received,
        customTargetDelivered = delivered,
        marketRate = marketRate,
        pctAgentCost = pctAgentCost,
    )

    @Test
    fun `profit is the raw margin minus the office cost`() {
        val result = MarginEngine.calculateProfit(input(pctAgentCost = 1.0))
        assertNull(result.error)
        val expectedSpread = 1000.0 - 1040.0 / 1.10
        assertEquals(expectedSpread, result.hiddenSpread, delta)
        assertEquals(10.0, result.agentCostBase, delta)
        assertEquals(expectedSpread - 10.0, result.netProfitBase, delta)
    }

    @Test
    fun `customer fees are forced to zero`() {
        val result = MarginEngine.calculateProfit(
            input().copy(pctFee = 9.0, deliveryFee = 30.0, feeInclusive = true)
        )
        assertNull(result.error)
        assertEquals(0.0, result.customerFeeBase, 0.0)
        assertEquals(0.0, result.deliveryFeeBase, 0.0)
        assertEquals(1000.0, result.cashReceived, delta)
        assertEquals(1040.0, result.targetDelivered, delta)
    }

    @Test
    fun `same currency margin is the plain difference`() {
        val result = MarginEngine.calculateProfit(
            input(direction = TransferDirection.EUR_TO_EUR, received = 1000.0, delivered = 960.0)
        )
        assertNull(result.error)
        assertEquals(40.0, result.hiddenSpread, delta)
        assertEquals(40.0, result.netProfitBase, delta)
    }
}
