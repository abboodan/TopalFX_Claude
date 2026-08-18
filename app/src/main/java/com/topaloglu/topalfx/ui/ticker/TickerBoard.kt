package com.topaloglu.topalfx.ui.ticker

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.topaloglu.topalfx.R
import com.topaloglu.topalfx.data.RatePair
import com.topaloglu.topalfx.data.TickerRate
import com.topaloglu.topalfx.ui.calculator.formatRate

@Composable
fun TickerBoard(
    pairs: List<RatePair>,
    rates: Map<RatePair, TickerRate>,
    isRefreshing: Boolean,
    hasError: Boolean,
    onRefresh: () -> Unit,
    onUpdatePairs: (List<RatePair>) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showEditor by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.ticker_title),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f),
            )
            if (isRefreshing) {
                CircularProgressIndicator(modifier = Modifier.padding(end = 8.dp).size(18.dp))
            }
            IconButton(onClick = { showEditor = true }) {
                Icon(Icons.Filled.Edit, contentDescription = stringResource(R.string.ticker_edit_pairs))
            }
            IconButton(onClick = onRefresh) {
                Icon(Icons.Filled.Refresh, contentDescription = stringResource(R.string.ticker_refresh))
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            pairs.forEach { pair ->
                TickerCell(pair, rates[pair])
            }
        }

        if (hasError) {
            Text(
                text = stringResource(R.string.ticker_offline),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }

    if (showEditor) {
        PairEditorDialog(
            pairs = pairs,
            onSave = { newPairs ->
                onUpdatePairs(newPairs)
                showEditor = false
            },
            onDismiss = { showEditor = false },
        )
    }
}

@Composable
private fun TickerCell(pair: RatePair, rate: TickerRate?) {
    val unavailable = rate == null || rate.rate == 0.0
    Card {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = pair.label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = if (unavailable) stringResource(R.string.ticker_unavailable)
                else formatRate(rate!!.rate),
                style = MaterialTheme.typography.bodyLarge,
                fontFamily = FontFamily.Monospace,
                color = if (unavailable) MaterialTheme.colorScheme.error
                else MaterialTheme.colorScheme.primary,
            )
        }
    }
}
