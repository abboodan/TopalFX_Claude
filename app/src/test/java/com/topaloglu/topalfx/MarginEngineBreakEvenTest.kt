package com.topaloglu.topalfx

import com.topaloglu.topalfx.data.CalcInput
import com.topaloglu.topalfx.data.CalcMode
import com.topaloglu.topalfx.data.DeductionBase
import com.topaloglu.topalfx.data.MarginEngine
import com.topaloglu.topalfx.data.TransferDirection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The break-even fee is what the owner otherwise finds by raising the percentage by hand
 * until the profit turns positive, so it has to be right to the digit he types.
 */
class MarginEngineBreakEvenTest {

    private fun sendExact(
        direction: TransferDirection = TransferDirection.EUR_TO_USD,
        feeInclusive: Boolean = true,
        marketRate: Double = 1.1699,
        customerRate: Double = 1.1699,
        deductionBase: DeductionBase = DeductionBase.ON_RECEIVED,
    ) = CalcInput(
        direction = direction,
        mode = CalcMode.SEND_EXACT,
        deductionBase = deductionBase,
        feeInclusive = feeInclusive,
        baseAmount = 100.0,
        marketRate = marketRate,
        customerRate = customerRate,
        pctFee = 5.0,
        pctAgentCost = 2.5,
        deliveryFee = 1.0,
    )

    @Test
    fun `fee inclusive at market rate breaks even at 3 355 percent`() {
        val result = MarginEngine.calculateProfit(sendExact())
        assertEquals(3.355, result.breakEvenPctFee!!, 1e-3)
    }

    @Test
    fun `fee exclusive at market rate breaks even at 3 441 percent`() {
        val result = MarginEngine.calculateProfit(sendExact(feeInclusive = false))
        assertEquals(3.441, result.breakEvenPctFee!!, 1e-3)
    }

    @Test
    fun `a one cent spread lowers the break even to 2 522 percent`() {
        val result = MarginEngine.calculateProfit(sendExact(customerRate = 1.1599))
        assertEquals(2.522, result.breakEvenPctFee!!, 1e-3)
    }

    /** No spread to absorb the costs, so same-currency always needs a higher fee. */
    @Test
    fun `same currency breaks even at 3 500 percent`() {
        val result = MarginEngine.calculateProfit(
            sendExact(direction = TransferDirection.EUR_TO_EUR)
        )
        assertEquals(3.5, result.breakEvenPctFee!!, 1e-3)
        val crossCurrency = MarginEngine.calculateProfit(sendExact()).breakEvenPctFee!!
        assertTrue(result.breakEvenPctFee!! > crossCurrency)
    }

    /**
     * The property that guards the closed form against any future edit: feeding the
     * answer back in as the fee must land the profit on zero, in every mode combination.
     */
    @Test
    fun `feeding the break even fee back in lands the profit on zero`() {
        var covered = 0
        for (mode in listOf(CalcMode.SEND_EXACT, CalcMode.RECEIVE_EXACT)) {
            for (deductionBase in DeductionBase.entries) {
                for (inclusive in listOf(true, false)) {
                    for (direction in listOf(
                        TransferDirection.EUR_TO_USD,
                        TransferDirection.EUR_TO_EUR,
                        TransferDirection.USD_TO_EUR,
                    )) {
                        for (amount in listOf(50.0, 100.0, 2500.0)) {
                            val input = CalcInput(
                                direction = direction,
                                mode = mode,
                                deductionBase = deductionBase,
                                feeInclusive = inclusive,
                                baseAmount = amount,
                                targetReceived = amount,
                                marketRate = 1.1699,
                                customerRate = 1.1599,
                                pctFee = 5.0,
                                pctAgentCost = 2.5,
                                deliveryFee = 1.0,
                            )
                            val breakEven = MarginEngine.calculateProfit(input).breakEvenPctFee
                            if (breakEven == null || breakEven <= 0.0) continue
                            val atBreakEven =
                                MarginEngine.calculateProfit(input.copy(pctFee = breakEven))
                            assertEquals(
                                "mode=$mode base=$deductionBase inclusive=$inclusive " +
                                    "direction=$direction amount=$amount",
                                0.0,
                                atBreakEven.netProfitBase,
                                1e-9,
                            )
                            covered++
                        }
                    }
                }
            }
        }
        assertTrue("the matrix produced no solvable cases", covered > 20)
    }

    @Test
    fun `a fee just under the break even loses money and just over earns`() {
        val breakEven = MarginEngine.calculateProfit(sendExact()).breakEvenPctFee!!
        val below = MarginEngine.calculateProfit(sendExact().copy(pctFee = breakEven - 0.1))
        val above = MarginEngine.calculateProfit(sendExact().copy(pctFee = breakEven + 0.1))
        assertTrue(below.netProfitBase < 0.0)
        assertTrue(above.netProfitBase > 0.0)
    }

    @Test
    fun `custom deal has no customer fee so there is no break even`() {
        val result = MarginEngine.calculateProfit(
            CalcInput(
                direction = TransferDirection.EUR_TO_USD,
                mode = CalcMode.CUSTOM_DEAL,
                customBaseReceived = 1000.0,
                customTargetDelivered = 1040.0,
                marketRate = 1.10,
                pctAgentCost = 1.0,
            )
        )
        assertNull(result.breakEvenPctFee)
    }

    @Test
    fun `a blank amount has no break even`() {
        val result = MarginEngine.calculateProfit(sendExact().copy(baseAmount = 0.0))
        assertNull(result.breakEvenPctFee)
    }

    @Test
    fun `it clamps to zero when the spread alone already covers the costs`() {
        val result = MarginEngine.calculateProfit(
            sendExact(customerRate = 1.0).copy(pctAgentCost = 0.1, deliveryFee = 0.0)
        )
        assertNotNull(result.breakEvenPctFee)
        assertEquals(0.0, result.breakEvenPctFee!!, 0.0)
    }

    @Test
    fun `an unreachable break even reports nothing rather than a bogus figure`() {
        // The delivery fee alone dwarfs the transfer, so no fee percentage can rescue it.
        val result = MarginEngine.calculateProfit(
            sendExact().copy(baseAmount = 10.0, deliveryFee = 5000.0)
        )
        assertNull(result.breakEvenPctFee)
    }
}
