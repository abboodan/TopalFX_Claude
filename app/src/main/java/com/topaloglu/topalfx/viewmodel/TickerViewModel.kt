package com.topaloglu.topalfx.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.topaloglu.topalfx.data.RatePair
import com.topaloglu.topalfx.data.TickerRate
import com.topaloglu.topalfx.data.TransferDirection
import com.topaloglu.topalfx.network.RetrofitClient
import com.topaloglu.topalfx.util.Prefs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Live-rates board. Auto-refreshes every 30 seconds; on any network failure the
 * affected rates are set to 0.0 and the board stays visible — never crashes.
 */
class TickerViewModel(app: Application) : AndroidViewModel(app) {

    private val _pairs = MutableStateFlow(Prefs.getTickerPairs(app))
    val pairs: StateFlow<List<RatePair>> = _pairs.asStateFlow()

    private val _rates = MutableStateFlow<Map<RatePair, TickerRate>>(emptyMap())
    val rates: StateFlow<Map<RatePair, TickerRate>> = _rates.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _hasError = MutableStateFlow(false)
    val hasError: StateFlow<Boolean> = _hasError.asStateFlow()

    init {
        viewModelScope.launch {
            while (isActive) {
                refreshOnce()
                delay(30_000)
            }
        }
    }

    fun manualRefresh() {
        viewModelScope.launch { refreshOnce() }
    }

    /** Live market rate for a transfer direction, or null when unavailable. */
    fun liveRate(direction: TransferDirection): Double? {
        if (direction.isSameCurrency) return 1.0
        val pair = RatePair(direction.base.name, direction.target.name)
        return _rates.value[pair]?.rate?.takeIf { it > 0.0 }
    }

    fun updatePairs(newPairs: List<RatePair>) {
        val cleaned = newPairs.distinct()
        Prefs.setTickerPairs(getApplication(), cleaned)
        _pairs.value = cleaned
        manualRefresh()
    }

    private suspend fun refreshOnce() {
        _isRefreshing.value = true
        val now = System.currentTimeMillis()
        // Calculator pairs are always fetched so the market rate can auto-fill even
        // when the user removed them from the visible board.
        val currentPairs = (_pairs.value + CALCULATOR_PAIRS).distinct()
        val result = mutableMapOf<RatePair, TickerRate>()
        var anyFailure = false

        // Same-currency pairs never hit the API (Frankfurter rejects them).
        val (identity, remote) = currentPairs.partition { it.base == it.symbol }
        identity.forEach { result[it] = TickerRate(it, 1.0, now) }

        withContext(Dispatchers.IO) {
            remote.groupBy { it.base }.forEach { (base, group) ->
                try {
                    val response = RetrofitClient.frankfurter.latest(
                        base = base,
                        symbols = group.joinToString(",") { it.symbol },
                    )
                    group.forEach { pair ->
                        val rate = response.rates[pair.symbol] ?: 0.0
                        if (rate == 0.0) anyFailure = true
                        result[pair] = TickerRate(pair, rate, now)
                    }
                } catch (e: Exception) {
                    anyFailure = true
                    group.forEach { pair -> result[pair] = TickerRate(pair, 0.0, now) }
                }
            }
        }

        _rates.value = result
        // Only failures on pairs the user actually sees are surfaced on the board.
        _hasError.value = anyFailure && _pairs.value.any { result[it]?.rate == 0.0 }
        _isRefreshing.value = false
    }

    private companion object {
        val CALCULATOR_PAIRS = listOf(
            RatePair("EUR", "USD"),
            RatePair("USD", "EUR"),
        )
    }
}
