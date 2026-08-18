package com.topaloglu.topalfx

import com.topaloglu.topalfx.data.CalcInput
import com.topaloglu.topalfx.data.CalcMode
import com.topaloglu.topalfx.data.MarginEngine
import com.topaloglu.topalfx.data.TransferDirection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MarginEngineModeBTest {

    private val delta = 1e-6

    @Test
    fun `receiver gets exact amount and sender pays grossed-up total`() {
        val result = MarginEngine.calculateProfit(
            CalcInput(
                direction = TransferDirection.EUR_TO_USD,
                mode = CalcMode.RECEIVE_EXACT,
                targetReceived = 500.0,
                deliveryFee = 20.0,
                flatFee = 5.0,
                pctFee = 2.0,
                marketRate = 1.10,
                customerRate = 1.05,
            )
        )
        assertNull(result.error)
        val targetNeeded = 520.0
        val principal = targetNeeded / 1.05
        assertEquals(targetNeeded, result.targetDelivered, delta)
        assertEquals(principal, result.principal, delta)
        assertEquals(500.0, result.totalReceivedByCustomer, delta)
        assertEquals(principal * 0.02, result.pctFeeBase, delta)
        assertEquals(principal + 5.0 + principal * 0.02, result.totalPaidByCustomer, delta)
        assertEquals(principal - targetNeeded / 1.10, result.hiddenSpread, delta)
    }

    @Test
    fun `net profit combines fees spread and costs`() {
        val result = MarginEngine.calculateProfit(
            CalcInput(
                direction = TransferDirection.EUR_TO_USD,
                mode = CalcMode.RECEIVE_EXACT,
                targetReceived = 500.0,
                deliveryFee = 20.0,
                flatFee = 5.0,
                pctFee = 2.0,
                flatAgentCost = 3.0,
                pctAgentCost = 1.0,
                marketRate = 1.10,
                customerRate = 1.05,
            )
        )
        assertNull(result.error)
        val principal = 520.0 / 1.05
        val pctFeeBase = principal * 0.02
        val hiddenSpread = principal - 520.0 / 1.10
        val agentPct = Math.round(principal * 1.0 / 100.0)
        val deliveryFeeBase = 20.0 / 1.10
        assertEquals(agentPct, result.agentCostPctBase)
        assertEquals(
            (5.0 + pctFeeBase + hiddenSpread) - (3.0 + agentPct) - deliveryFeeBase,
            result.netProfitBase,
            delta,
        )
    }

    @Test
    fun `same-currency receive is a pass-through with zero spread`() {
        val result = MarginEngine.calculateProfit(
            CalcInput(
                direction = TransferDirection.USD_TO_USD,
                mode = CalcMode.RECEIVE_EXACT,
                targetReceived = 750.0,
                deliveryFee = 10.0,
                flatFee = 4.0,
                marketRate = 9.9,
                customerRate = 9.9,
            )
        )
        assertNull(result.error)
        assertEquals(760.0, result.principal, delta)
        assertEquals(0.0, result.hiddenSpread, 0.0)
        assertEquals(764.0, result.totalPaidByCustomer, delta)
        assertEquals(10.0, result.deliveryFeeBase, delta)
    }
}
