package com.topaloglu.topalfx

import com.topaloglu.topalfx.data.CalcInput
import com.topaloglu.topalfx.data.CalcMode
import com.topaloglu.topalfx.data.DeductionBase
import com.topaloglu.topalfx.data.MarginEngine
import com.topaloglu.topalfx.data.TransferDirection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MarginEngineDeductionBaseTest {

    @Test
    fun `on received with EUR base rounds base amount percentage`() {
        val result = MarginEngine.calculateProfit(
            CalcInput(
                direction = TransferDirection.EUR_TO_USD,
                mode = CalcMode.SEND_EXACT,
                deductionBase = DeductionBase.ON_RECEIVED,
                baseAmount = 1000.0,
                pctAgentCost = 1.5,
                marketRate = 1.10,
                customerRate = 1.05,
            )
        )
        assertNull(result.error)
        assertEquals(15L, result.agentCostPctBase)
    }

    @Test
    fun `office cost always rounds half up to whole integer`() {
        val result = MarginEngine.calculateProfit(
            CalcInput(
                direction = TransferDirection.EUR_TO_USD,
                mode = CalcMode.SEND_EXACT,
                deductionBase = DeductionBase.ON_RECEIVED,
                baseAmount = 1000.0,
                pctAgentCost = 0.25, // 2.5 → Math.round → 3
                marketRate = 1.10,
                customerRate = 1.05,
            )
        )
        assertNull(result.error)
        assertEquals(3L, result.agentCostPctBase)
    }

    @Test
    fun `on received with USD base derives cost from delivered target`() {
        // Fee-inclusive makes the USD branch differ from the plain base amount:
        // principal = (1000-100)/1.0 = 900, targetDelivered = 810,
        // cost = round((810 * 2 / 100) / 0.9) = round(18.0) = 18 (not round(1000*2%) = 20).
        val result = MarginEngine.calculateProfit(
            CalcInput(
                direction = TransferDirection.USD_TO_EUR,
                mode = CalcMode.SEND_EXACT,
                deductionBase = DeductionBase.ON_RECEIVED,
                feeInclusive = true,
                baseAmount = 1000.0,
                flatFee = 100.0,
                pctAgentCost = 2.0,
                marketRate = 0.92,
                customerRate = 0.90,
            )
        )
        assertNull(result.error)
        assertEquals(18L, result.agentCostPctBase)
    }

    @Test
    fun `on delivered target uses true cost at market rate`() {
        val result = MarginEngine.calculateProfit(
            CalcInput(
                direction = TransferDirection.EUR_TO_USD,
                mode = CalcMode.SEND_EXACT,
                deductionBase = DeductionBase.ON_DELIVERED,
                baseAmount = 1000.0,
                pctAgentCost = 2.0,
                marketRate = 1.10,
                customerRate = 1.05,
            )
        )
        assertNull(result.error)
        // trueCost = 1050 / 1.10 = 954.5454..., cost = round(19.0909...) = 19
        assertEquals(19L, result.agentCostPctBase)
    }

    @Test
    fun `custom deal deduction uses received base amount`() {
        val result = MarginEngine.calculateProfit(
            CalcInput(
                direction = TransferDirection.EUR_TO_USD,
                mode = CalcMode.CUSTOM_DEAL,
                deductionBase = DeductionBase.ON_RECEIVED,
                customBaseReceived = 1000.0,
                customTargetDelivered = 1040.0,
                pctAgentCost = 1.0,
                marketRate = 1.10,
            )
        )
        assertNull(result.error)
        assertEquals(10L, result.agentCostPctBase)
    }
}
