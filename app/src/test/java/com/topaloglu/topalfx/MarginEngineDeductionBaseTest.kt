package com.topaloglu.topalfx

import com.topaloglu.topalfx.data.CalcInput
import com.topaloglu.topalfx.data.CalcMode
import com.topaloglu.topalfx.data.DeductionBase
import com.topaloglu.topalfx.data.MarginEngine
import com.topaloglu.topalfx.data.TransferDirection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MarginEngineDeductionBaseTest {

    private val delta = 1e-9

    private fun input(base: DeductionBase) = CalcInput(
        direction = TransferDirection.EUR_TO_USD,
        mode = CalcMode.SEND_EXACT,
        deductionBase = base,
        feeInclusive = true,
        baseAmount = 100.0,
        marketRate = 1.1699,
        customerRate = 1.1599,
        pctFee = 5.0,
        pctAgentCost = 2.5,
    )

    @Test
    fun `on received charges the full cash taken in`() {
        val result = MarginEngine.calculateProfit(input(DeductionBase.ON_RECEIVED))
        assertNull(result.error)
        assertEquals(2.5, result.agentCostBase, delta)   // 100 × 2.5%
    }

    @Test
    fun `on delivered charges the transfer amount`() {
        val result = MarginEngine.calculateProfit(input(DeductionBase.ON_DELIVERED))
        assertNull(result.error)
        assertEquals(2.375, result.agentCostBase, delta) // 95 × 2.5%
    }

    /**
     * The old engine ran every percentage cost through Math.round, so 2.5 became 3 —
     * a 20% overstatement of the cost on a 100 EUR transfer, in a business that settles
     * to the cent.
     */
    @Test
    fun `the cost is never rounded to a whole unit`() {
        val result = MarginEngine.calculateProfit(input(DeductionBase.ON_RECEIVED))
        assertEquals(2.5, result.agentCostBase, 0.0)
        assertTrue(result.agentCostBase != 3.0)
    }

    @Test
    fun `a fractional cost survives intact`() {
        val result = MarginEngine.calculateProfit(
            input(DeductionBase.ON_RECEIVED).copy(pctAgentCost = 2.37)
        )
        assertEquals(2.37, result.agentCostBase, delta)
    }

    /**
     * Costs come out of the office's profit, so switching the deduction base must never
     * change the number quoted to the customer.
     */
    @Test
    fun `switching the base leaves the customer-facing figures untouched`() {
        val onReceived = MarginEngine.calculateProfit(input(DeductionBase.ON_RECEIVED))
        val onDelivered = MarginEngine.calculateProfit(input(DeductionBase.ON_DELIVERED))

        assertEquals(onReceived.cashReceived, onDelivered.cashReceived, delta)
        assertEquals(onReceived.transferAmount, onDelivered.transferAmount, delta)
        assertEquals(onReceived.targetDelivered, onDelivered.targetDelivered, delta)
        assertEquals(onReceived.customerFeeBase, onDelivered.customerFeeBase, delta)
        assertEquals(onReceived.hiddenSpread, onDelivered.hiddenSpread, delta)
        // Only the office's own cost — and therefore its profit — differs.
        assertTrue(onDelivered.agentCostBase < onReceived.agentCostBase)
    }

    @Test
    fun `custom deal charges the received amount`() {
        val result = MarginEngine.calculateProfit(
            CalcInput(
                direction = TransferDirection.EUR_TO_USD,
                mode = CalcMode.CUSTOM_DEAL,
                deductionBase = DeductionBase.ON_RECEIVED,
                customBaseReceived = 1000.0,
                customTargetDelivered = 1040.0,
                marketRate = 1.10,
                pctAgentCost = 1.0,
            )
        )
        assertNull(result.error)
        assertEquals(10.0, result.agentCostBase, delta)
    }
}
