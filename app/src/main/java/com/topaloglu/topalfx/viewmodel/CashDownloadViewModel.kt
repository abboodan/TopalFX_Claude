package com.topaloglu.topalfx.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.topaloglu.topalfx.data.CashDownloadEngine
import com.topaloglu.topalfx.data.CashDownloadInput
import com.topaloglu.topalfx.data.CashDownloadResult
import com.topaloglu.topalfx.data.Currency
import com.topaloglu.topalfx.util.Prefs
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class DownloadField { AMOUNT, PCT_RATE }

data class CashDownloadUiState(
    val currency: Currency = Currency.EUR,
    /** true → the typed amount is the cash in hand and the cut comes out of it. */
    val feeFromAmount: Boolean = true,
    val fields: Map<DownloadField, String> = emptyMap(),
    val result: CashDownloadResult? = null,
    /** Bumped by explicit actions so the UI can confirm them; see CalculatorUiState. */
    val actionId: Long = 0L,
    val lastCalculatedAt: Long = 0L,
) {
    fun field(field: DownloadField): String = fields[field] ?: ""

    val hasAmount: Boolean get() = field(DownloadField.AMOUNT).isNotBlank()
}

/**
 * Host for إعادة تنزيل مبلغ. Shares the office's إعادة التنزيل default with the transfer
 * calculator, because it is the same rate for the same operation.
 */
class CashDownloadViewModel(app: Application) : AndroidViewModel(app) {

    private val _uiState = MutableStateFlow(recalculated(withDefaults(CashDownloadUiState())))
    val uiState: StateFlow<CashDownloadUiState> = _uiState.asStateFlow()

    fun onFieldChanged(field: DownloadField, raw: String) = update {
        it.copy(fields = it.fields + (field to raw))
    }

    fun onCurrencyChanged(currency: Currency) = update { it.copy(currency = currency) }

    fun onFeeFromAmountChanged(enabled: Boolean) = update { it.copy(feeFromAmount = enabled) }

    fun recalculateNow() = update {
        it.copy(
            actionId = it.actionId + 1,
            lastCalculatedAt = System.currentTimeMillis(),
        )
    }

    fun reset() {
        val previous = _uiState.value
        _uiState.value = recalculated(
            withDefaults(
                CashDownloadUiState(
                    currency = previous.currency,
                    feeFromAmount = previous.feeFromAmount,
                    actionId = previous.actionId + 1,
                )
            )
        )
    }

    /** Re-applies the saved إعادة التنزيل default. */
    fun applyDefaults() = update { withDefaults(it) }

    private fun withDefaults(state: CashDownloadUiState): CashDownloadUiState = state.copy(
        fields = state.fields +
            (DownloadField.PCT_RATE to Prefs.getDefaultPctAgentCost(getApplication()))
    )

    private fun update(transform: (CashDownloadUiState) -> CashDownloadUiState) {
        _uiState.value = recalculated(transform(_uiState.value))
    }

    private fun recalculated(state: CashDownloadUiState): CashDownloadUiState {
        if (!state.hasAmount) return state.copy(result = null)
        return state.copy(
            result = CashDownloadEngine.calculate(
                CashDownloadInput(
                    currency = state.currency,
                    amount = CalculatorViewModel.parse(state.field(DownloadField.AMOUNT)),
                    pctRate = CalculatorViewModel.parse(state.field(DownloadField.PCT_RATE)),
                    feeFromAmount = state.feeFromAmount,
                )
            )
        )
    }
}
