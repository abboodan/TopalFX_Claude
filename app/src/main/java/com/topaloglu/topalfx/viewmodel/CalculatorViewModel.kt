package com.topaloglu.topalfx.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.topaloglu.topalfx.data.CalcInput
import com.topaloglu.topalfx.data.CalcMode
import com.topaloglu.topalfx.data.CalcResult
import com.topaloglu.topalfx.data.DeductionBase
import com.topaloglu.topalfx.data.MarginEngine
import com.topaloglu.topalfx.data.TransferDirection
import com.topaloglu.topalfx.util.Prefs
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

enum class InputField {
    BASE_AMOUNT, TARGET_RECEIVED, CUSTOM_BASE_RECEIVED, CUSTOM_TARGET_DELIVERED,
    MARKET_RATE, CUSTOMER_RATE, FLAT_FEE, PCT_FEE, DELIVERY_FEE,
    FLAT_AGENT_COST, PCT_AGENT_COST,
}

data class CalculatorUiState(
    val direction: TransferDirection = TransferDirection.EUR_TO_USD,
    val mode: CalcMode = CalcMode.SEND_EXACT,
    val deductionBase: DeductionBase = DeductionBase.ON_RECEIVED,
    val feeInclusive: Boolean = false,
    val fields: Map<InputField, String> = emptyMap(),
    /** While true the market rate keeps tracking the live ticker rate. */
    val marketRateAutoFilled: Boolean = true,
    /** While true the customer rate stays derived from the market rate. */
    val customerRateAutoFilled: Boolean = true,
    /** How far below the market rate the customer rate sits, in currency units. */
    val customerRateDiscount: Double = Prefs.DEFAULT_CUSTOMER_DISCOUNT,
    /** Live EUR per 1 USD, used to unify the profit to EUR. */
    val usdToEurRate: Double = 0.0,
    val result: CalcResult? = null,
) {
    val ratesLocked: Boolean get() = direction.isSameCurrency
    fun field(field: InputField): String = fields[field] ?: ""
}

/**
 * Reactive calculation engine host: every input change immediately re-runs
 * [MarginEngine.calculateProfit] — pure math, no coroutines needed.
 */
class CalculatorViewModel(app: Application) : AndroidViewModel(app) {

    private val _uiState = MutableStateFlow(recalculated(stateWithDefaults(CalculatorUiState())))
    val uiState: StateFlow<CalculatorUiState> = _uiState.asStateFlow()

    fun onFieldChanged(field: InputField, raw: String) {
        update { state ->
            state.copy(
                fields = state.fields + (field to raw),
                // A manual edit detaches the field from its automatic source.
                marketRateAutoFilled =
                    if (field == InputField.MARKET_RATE) false else state.marketRateAutoFilled,
                customerRateAutoFilled =
                    if (field == InputField.CUSTOMER_RATE) false else state.customerRateAutoFilled,
            )
        }
    }

    /** Applies a market-rate discount (e.g. 0.10) and re-derives the customer rate. */
    fun onCustomerRateDiscountChanged(discount: Double) = update {
        it.copy(customerRateDiscount = discount, customerRateAutoFilled = true)
    }

    fun onDirectionChanged(direction: TransferDirection) {
        update { state ->
            if (state.direction == direction) return@update state
            var fields = state.fields
            fields = if (direction.isSameCurrency) {
                fields + (InputField.MARKET_RATE to LOCKED_RATE) +
                    (InputField.CUSTOMER_RATE to LOCKED_RATE)
            } else if (state.direction.isSameCurrency) {
                // Leaving a locked direction: drop the 1.0000 placeholders so the
                // live rate can take over.
                fields - InputField.MARKET_RATE - InputField.CUSTOMER_RATE
            } else {
                fields
            }
            state.copy(
                direction = direction,
                fields = fields,
                marketRateAutoFilled = true,
                customerRateAutoFilled = true,
            )
        }
    }

    fun onModeChanged(mode: CalcMode) = update { it.copy(mode = mode) }

    fun onDeductionBaseChanged(base: DeductionBase) = update { it.copy(deductionBase = base) }

    fun onFeeInclusiveChanged(enabled: Boolean) = update { it.copy(feeInclusive = enabled) }

    /** Pushes the live ticker rate into the market rate field while it is still tracking. */
    fun onLiveMarketRate(rate: Double?) {
        if (rate == null || rate <= 0.0) return
        val state = _uiState.value
        if (state.ratesLocked || !state.marketRateAutoFilled) return
        val formatted = formatRate(rate)
        if (state.field(InputField.MARKET_RATE) == formatted) return
        update { it.copy(fields = it.fields + (InputField.MARKET_RATE to formatted)) }
    }

    /** Live EUR/USD conversion used to express the office profit in EUR. */
    fun onLiveUsdToEurRate(rate: Double?) {
        val value = rate?.takeIf { it > 0.0 } ?: return
        if (_uiState.value.usdToEurRate == value) return
        update { it.copy(usdToEurRate = value) }
    }

    /** Manual re-sync: reattaches the market rate to the live ticker. */
    fun resyncMarketRate(rate: Double?) {
        if (rate == null || rate <= 0.0) return
        update {
            it.copy(
                fields = it.fields + (InputField.MARKET_RATE to formatRate(rate)),
                marketRateAutoFilled = true,
            )
        }
    }

    /** Re-applies the defaults saved in settings. */
    fun applyDefaults() = update { stateWithDefaults(it) }

    private fun stateWithDefaults(state: CalculatorUiState): CalculatorUiState {
        val context = getApplication<Application>()
        val fields = state.fields +
            (InputField.PCT_AGENT_COST to Prefs.getDefaultPctAgentCost(context)) +
            (InputField.FLAT_AGENT_COST to Prefs.getDefaultFlatAgentCost(context))
        return state.copy(
            fields = fields,
            deductionBase = Prefs.getDefaultDeductionBase(context),
            customerRateDiscount = Prefs.getDefaultCustomerDiscount(context),
            customerRateAutoFilled = true,
        )
    }

    private fun update(transform: (CalculatorUiState) -> CalculatorUiState) {
        _uiState.value = recalculated(withDerivedCustomerRate(transform(_uiState.value)))
    }

    /** Keeps the customer rate at market minus the selected discount while it is tracking. */
    private fun withDerivedCustomerRate(state: CalculatorUiState): CalculatorUiState {
        if (state.ratesLocked || !state.customerRateAutoFilled) return state
        val market = parse(state.field(InputField.MARKET_RATE))
        if (!market.isFinite() || market <= 0.0) return state
        val customer = market - state.customerRateDiscount
        if (customer <= 0.0) return state
        val formatted = formatRate(customer)
        if (state.field(InputField.CUSTOMER_RATE) == formatted) return state
        return state.copy(fields = state.fields + (InputField.CUSTOMER_RATE to formatted))
    }

    private fun recalculated(state: CalculatorUiState): CalculatorUiState {
        val primary = when (state.mode) {
            CalcMode.SEND_EXACT -> state.field(InputField.BASE_AMOUNT)
            CalcMode.RECEIVE_EXACT -> state.field(InputField.TARGET_RECEIVED)
            CalcMode.CUSTOM_DEAL -> state.field(InputField.CUSTOM_BASE_RECEIVED)
        }
        if (primary.isBlank()) return state.copy(result = null)

        val input = CalcInput(
            direction = state.direction,
            mode = state.mode,
            deductionBase = state.deductionBase,
            feeInclusive = state.feeInclusive,
            baseAmount = parse(state.field(InputField.BASE_AMOUNT)),
            targetReceived = parse(state.field(InputField.TARGET_RECEIVED)),
            customBaseReceived = parse(state.field(InputField.CUSTOM_BASE_RECEIVED)),
            customTargetDelivered = parse(state.field(InputField.CUSTOM_TARGET_DELIVERED)),
            marketRate = parse(state.field(InputField.MARKET_RATE)),
            customerRate = parse(state.field(InputField.CUSTOMER_RATE)),
            flatFee = parse(state.field(InputField.FLAT_FEE)),
            pctFee = parse(state.field(InputField.PCT_FEE)),
            deliveryFee = parse(state.field(InputField.DELIVERY_FEE)),
            flatAgentCost = parse(state.field(InputField.FLAT_AGENT_COST)),
            pctAgentCost = parse(state.field(InputField.PCT_AGENT_COST)),
            usdToEurRate = state.usdToEurRate,
        )
        return state.copy(result = MarginEngine.calculateProfit(input))
    }

    companion object {
        private const val LOCKED_RATE = "1.0000"

        private fun formatRate(value: Double): String = String.format(Locale.US, "%.4f", value)

        /**
         * Locale-tolerant numeric parsing: Arabic-Indic digits and Arabic/European
         * decimal separators are normalized. Blank → 0.0, garbage → NaN so the
         * engine reports INVALID_NUMBER.
         */
        fun parse(raw: String): Double {
            if (raw.isBlank()) return 0.0
            val normalized = buildString(raw.length) {
                for (ch in raw.trim()) {
                    when (ch) {
                        in '٠'..'٩' -> append('0' + (ch - '٠'))
                        in '۰'..'۹' -> append('0' + (ch - '۰'))
                        '٫', ',' -> append('.')
                        else -> append(ch)
                    }
                }
            }
            return normalized.toDoubleOrNull() ?: Double.NaN
        }
    }
}
