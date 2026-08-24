package com.topaloglu.topalfx

import com.topaloglu.topalfx.data.CalcInput
import com.topaloglu.topalfx.data.CalcMode
import com.topaloglu.topalfx.data.MarginEngine
import com.topaloglu.topalfx.data.TransferDirection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MarginEngineSendExactTest {

    private val delta = 1e-9

    private fun input(
        direction: TransferDirection = TransferDirection.EUR_TO_USD,
        feeInclusive: Boolean = true,
        baseAmount: Double = 1000.0,
        marketRate: Double = 1.10,
        customerRate: Double = 1.05,
        pctFee: Double = 4.0,
        pctAgentCost: Double = 2.0,
        deliveryFee: Double = 0.0,
    ) = CalcInput(
        direction = direction,
        mode = CalcMode.SEND_EXACT,
        feeInclusive = feeInclusive,
        baseAmount = baseAmount,
        marketRate = marketRate,
        customerRate = customerRate,
        pctFee = pctFee,
        pctAgentCost = pctAgentCost,
        deliveryFee = deliveryFee,
    )

    @Test
    fun `fee inclusive treats the typed amount as the cash taken in`() {
        val result = MarginEngine.calculateProfit(input(feeInclusive = true))
        assertNull(result.error)
        assertEquals(1000.0, result.cashReceived, delta)
        assertEquals(960.0, result.transferAmount, delta)   // 1000 * (1 - 0.04)
        assertEquals(40.0, result.customerFeeBase, delta)
    }

    @Test
    fun `fee exclusive treats the typed amount as the transfer and adds the fee on top`() {
        val result = MarginEngine.calculateProfit(input(feeInclusive = false))
        assertNull(result.error)
        assertEquals(1040.0, result.cashReceived, delta)    // 1000 * (1 + 0.04)
        assertEquals(1000.0, result.transferAmount, delta)
        assertEquals(40.0, result.customerFeeBase, delta)
    }

    @Test
    fun `the fee is the gap between cash and transfer in both modes`() {
        listOf(true, false).forEach { inclusive ->
            val result = MarginEngine.calculateProfit(input(feeInclusive = inclusive))
            assertEquals(
                result.cashReceived - result.transferAmount,
                result.customerFeeBase,
                delta,
            )
        }
    }

    /**
     * The bug this whole rewrite exists to kill: the old engine subtracted أجرة التسليم
     * from the beneficiary AND from the profit. It is a cost to the office only.
     */
    @Test
    fun `delivery fee never reduces what the beneficiary receives`() {
        val free = MarginEngine.calculateProfit(input(deliveryFee = 0.0))
        val charged = MarginEngine.calculateProfit(input(deliveryFee = 22.0))

        assertEquals(free.targetDelivered, charged.targetDelivered, delta)
        assertEquals(
            charged.transferAmount * 1.05,
            charged.targetDelivered,
            delta,
        )
        // ...and it lands on the profit exactly once: 22 / 1.10 = 20.
        assertEquals(20.0, charged.deliveryFeeBase, delta)
        assertEquals(free.netProfitBase - 20.0, charged.netProfitBase, delta)
    }

    @Test
    fun `the beneficiary always gets the full transfer amount at the customer rate`() {
        val result = MarginEngine.calculateProfit(input(deliveryFee = 15.0, pctAgentCost = 3.0))
        assertEquals(result.transferAmount * 1.05, result.targetDelivered, delta)
    }

    /**
     * The old engine had a USD-base branch for both the fee and the agent cost. Neither
     * had any business meaning — the arithmetic is currency-agnostic.
     */
    @Test
    fun `a USD base behaves identically to an EUR base for matched numbers`() {
        val eur = MarginEngine.calculateProfit(input(direction = TransferDirection.EUR_TO_USD))
        val usd = MarginEngine.calculateProfit(input(direction = TransferDirection.USD_TO_EUR))

        assertEquals(eur.cashReceived, usd.cashReceived, delta)
        assertEquals(eur.transferAmount, usd.transferAmount, delta)
        assertEquals(eur.customerFeeBase, usd.customerFeeBase, delta)
        assertEquals(eur.agentCostBase, usd.agentCostBase, delta)
        assertEquals(eur.netProfitBase, usd.netProfitBase, delta)
    }

    @Test
    fun `same currency zeroes the spread exactly and ignores the typed rates`() {
        val result = MarginEngine.calculateProfit(
            input(direction = TransferDirection.USD_TO_USD, marketRate = 1.2, customerRate = 1.3)
        )
        assertNull(result.error)
        assertEquals(0.0, result.hiddenSpread, 0.0)
        assertEquals(result.transferAmount, result.targetDelivered, delta)
    }

    @Test
    fun `a customer rate below market produces the spread on the transfer amount`() {
        val result = MarginEngine.calculateProfit(input(customerRate = 1.09, marketRate = 1.10))
        val expected = result.transferAmount * (1.0 - 1.09 / 1.10)
        assertEquals(expected, result.hiddenSpread, delta)
        assertTrue(result.hiddenSpread > 0.0)
    }

    @Test
    fun `profit is fee plus spread minus the two costs`() {
        val result = MarginEngine.calculateProfit(input(deliveryFee = 11.0))
        val expected = result.customerFeeBase + result.hiddenSpread -
            result.agentCostBase - result.deliveryFeeBase
        assertEquals(expected, result.netProfitBase, delta)
    }
}
