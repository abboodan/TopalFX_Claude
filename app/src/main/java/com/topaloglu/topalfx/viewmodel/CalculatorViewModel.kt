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
    MARKET_RATE, CUSTOMER_RATE, PCT_FEE, DELIVERY_FEE, PCT_AGENT_COST,
}

/** What the user last did on purpose, so the UI can confirm it. */
enum class CalculatorAction { CALCULATED, RESET }

data class CalculatorUiState(
    val direction: TransferDirection = Prefs.DEFAULT_DIRECTION,
    val mode: CalcMode = Prefs.DEFAULT_MODE,
    val deductionBase: DeductionBase = Prefs.DEFAULT_DEDUCTION_BASE,
    val feeInclusive: Boolean = Prefs.DEFAULT_FEE_INCLUSIVE,
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
    /**
     * Bumped only by deliberate user actions. A StateFlow drops structurally equal
     * emissions, so without this counter pressing Calculate on unchanged inputs would
     * produce no emission and therefore no confirmation.
     */
    val actionId: Long = 0L,
    val lastAction: CalculatorAction? = null,
    /** Wall-clock stamp of the last explicit calculation; 0 until the first one. */
    val lastCalculatedAt: Long = 0L,
) {
    val ratesLocked: Boolean get() = direction.isSameCurrency

    fun field(field: InputField): String = fields[field] ?: ""

    /** Whether the mode's primary amount is filled in. */
    val hasPrimaryAmount: Boolean
        get() = field(
            when (mode) {
                CalcMode.SEND_EXACT -> InputField.BASE_AMOUNT
                CalcMode.RECEIVE_EXACT -> InputField.TARGET_RECEIVED
                CalcMode.CUSTOM_DEAL -> InputField.CUSTOM_BASE_RECEIVED
            }
        ).isNotBlank()
}

/**
 * Reactive calculation engine host: every input change immediately re-runs
 * [MarginEngine.calculateProfit] — pure math, no coroutines needed. The Calculate
 * button re-runs it explicitly on top of that, so the owner can confirm the figure
 * he is about to quote came from the numbers currently on screen.
 */
class CalculatorViewModel(app: Application) : AndroidViewModel(app) {

    private val _uiState = MutableStateFlow(recalculated(stateWithDefaults(CalculatorUiState())))
    val uiState: StateFlow<CalculatorUiState> = _uiState.asStateFlow()

    /** The state captured just before the last reset, for the Undo action. */
    private var stateBeforeReset: CalculatorUiState? = null

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
            state.copy(
                direction = direction,
                fields = directionRateFields(state.fields, state.direction, direction),
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

    /**
     * Re-runs the engine on the numbers currently on screen and flags the run so the UI
     * can confirm it. Inputs are untouched and results are never hidden — this sits on
     * top of the live recalculation, it does not replace it.
     */
    fun recalculateNow() = update {
        it.copy(
            actionId = it.actionId + 1,
            lastAction = CalculatorAction.CALCULATED,
            lastCalculatedAt = System.currentTimeMillis(),
        )
    }

    /**
     * Clears the sheet for the next customer and restores every saved default.
     *
     * Starts from a fresh [CalculatorUiState] rather than copying the current one, so no
     * field can survive a reset — including fields added to the enum later.
     *
     * [liveMarketRate] re-seeds the market rate immediately. Leaving it blank would be a
     * trap: the screen only pushes a live rate when the rate or the direction changes, so
     * a refresh that returns the same rate would leave the field empty and the results
     * hidden behind a missing-rate error.
     */
    fun reset(liveMarketRate: Double? = null) {
        val previous = _uiState.value
        stateBeforeReset = previous

        val direction = Prefs.getDefaultDirection(getApplication())
        var state = CalculatorUiState(
            direction = direction,
            mode = Prefs.getDefaultMode(getApplication()),
            usdToEurRate = previous.usdToEurRate,
            actionId = previous.actionId + 1,
            lastAction = CalculatorAction.RESET,
        )
        state = stateWithDefaults(state, includeNavigation = true)
        state = state.copy(
            fields = directionRateFields(state.fields, previous.direction, direction)
        )
        if (!direction.isSameCurrency && liveMarketRate != null && liveMarketRate > 0.0) {
            state = state.copy(
                fields = state.fields + (InputField.MARKET_RATE to formatRate(liveMarketRate))
            )
        }
        _uiState.value = recalculated(withDerivedCustomerRate(state))
    }

    /** Restores the sheet captured before the last reset. */
    fun undoReset() {
        val restored = stateBeforeReset ?: return
        stateBeforeReset = null
        _uiState.value = restored.copy(
            actionId = _uiState.value.actionId + 1,
            lastAction = null,
        )
    }

    /** Re-applies the defaults saved in settings, leaving direction and mode alone. */
    fun applyDefaults() = update { stateWithDefaults(it) }

    private fun stateWithDefaults(
        state: CalculatorUiState,
        includeNavigation: Boolean = false,
    ): CalculatorUiState {
        val context = getApplication<Application>()
        val fields = state.fields +
            (InputField.PCT_FEE to Prefs.getDefaultPctFee(context)) +
            (InputField.PCT_AGENT_COST to Prefs.getDefaultPctAgentCost(context))
        var result = state.copy(
            fields = fields,
            deductionBase = Prefs.getDefaultDeductionBase(context),
            feeInclusive = Prefs.getDefaultFeeInclusive(context),
            customerRateDiscount = Prefs.getDefaultCustomerDiscount(context),
            customerRateAutoFilled = true,
        )
        if (includeNavigation) {
            result = result.copy(
                direction = Prefs.getDefaultDirection(context),
                mode = Prefs.getDefaultMode(context),
            )
        }
        return result
    }

    /**
     * Same-currency directions show a locked 1.0000; leaving one drops the placeholders
     * so the live rate can take over.
     */
    private fun directionRateFields(
        fields: Map<InputField, String>,
        from: TransferDirection,
        to: TransferDirection,
    ): Map<InputField, String> = when {
        to.isSameCurrency ->
            fields + (InputField.MARKET_RATE to LOCKED_RATE) +
                (InputField.CUSTOMER_RATE to LOCKED_RATE)
        from.isSameCurrency -> fields - InputField.MARKET_RATE - InputField.CUSTOMER_RATE
        else -> fields
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
        if (!state.hasPrimaryAmount) return state.copy(result = null)

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
            pctFee = parse(state.field(InputField.PCT_FEE)),
            deliveryFee = parse(state.field(InputField.DELIVERY_FEE)),
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
