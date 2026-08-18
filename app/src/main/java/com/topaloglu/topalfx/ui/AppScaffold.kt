package com.topaloglu.topalfx.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.topaloglu.topalfx.R
import com.topaloglu.topalfx.ui.calculator.CalculatorScreen
import com.topaloglu.topalfx.ui.ticker.TickerBoard
import com.topaloglu.topalfx.ui.updater.UpdateDialog
import com.topaloglu.topalfx.viewmodel.CalculatorViewModel
import com.topaloglu.topalfx.viewmodel.TickerViewModel
import com.topaloglu.topalfx.viewmodel.UpdateViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppScaffold(
    onToggleLanguage: () -> Unit,
    calculatorViewModel: CalculatorViewModel = viewModel(),
    tickerViewModel: TickerViewModel = viewModel(),
    updateViewModel: UpdateViewModel = viewModel(),
) {
    val calculatorState by calculatorViewModel.uiState.collectAsState()
    val pairs by tickerViewModel.pairs.collectAsState()
    val rates by tickerViewModel.rates.collectAsState()
    val isRefreshing by tickerViewModel.isRefreshing.collectAsState()
    val hasError by tickerViewModel.hasError.collectAsState()

    LaunchedEffect(Unit) {
        updateViewModel.checkForUpdate(silent = true)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                actions = {
                    TextButton(onClick = onToggleLanguage) {
                        Text(stringResource(R.string.action_language))
                    }
                    IconButton(onClick = { updateViewModel.checkForUpdate() }) {
                        Icon(
                            Icons.Filled.SystemUpdate,
                            contentDescription = stringResource(R.string.update_check),
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            TickerBoard(
                pairs = pairs,
                rates = rates,
                isRefreshing = isRefreshing,
                hasError = hasError,
                onRefresh = tickerViewModel::manualRefresh,
                onUpdatePairs = tickerViewModel::updatePairs,
            )
            CalculatorScreen(
                state = calculatorState,
                viewModel = calculatorViewModel,
            )
        }
    }

    UpdateDialog(viewModel = updateViewModel)
}
