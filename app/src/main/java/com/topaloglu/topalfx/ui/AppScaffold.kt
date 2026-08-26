package com.topaloglu.topalfx.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.topaloglu.topalfx.R
import com.topaloglu.topalfx.data.TransferDirection
import com.topaloglu.topalfx.ui.calculator.CalculatorActionBar
import com.topaloglu.topalfx.ui.calculator.CalculatorScreen
import com.topaloglu.topalfx.ui.download.CashDownloadScreen
import com.topaloglu.topalfx.ui.settings.SettingsScreen
import com.topaloglu.topalfx.ui.ticker.TickerBoard
import com.topaloglu.topalfx.ui.updater.UpdateDialog
import com.topaloglu.topalfx.viewmodel.CalculatorViewModel
import com.topaloglu.topalfx.viewmodel.CashDownloadViewModel
import com.topaloglu.topalfx.viewmodel.TickerViewModel
import com.topaloglu.topalfx.viewmodel.UpdateViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppScaffold(
    onToggleLanguage: () -> Unit,
    calculatorViewModel: CalculatorViewModel = viewModel(),
    tickerViewModel: TickerViewModel = viewModel(),
    updateViewModel: UpdateViewModel = viewModel(),
    downloadViewModel: CashDownloadViewModel = viewModel(),
) {
    val calculatorState by calculatorViewModel.uiState.collectAsState()
    val downloadState by downloadViewModel.uiState.collectAsState()
    val pairs by tickerViewModel.pairs.collectAsState()
    val rates by tickerViewModel.rates.collectAsState()
    val isRefreshing by tickerViewModel.isRefreshing.collectAsState()
    val hasError by tickerViewModel.hasError.collectAsState()

    // rememberSaveable, or a rotation silently kicks the user out of Settings.
    var showSettings by rememberSaveable { mutableStateOf(false) }
    var section by rememberSaveable { mutableStateOf(AppSection.TRANSFER) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val settingsSavedMessage = stringResource(R.string.settings_saved)
    val resetDoneMessage = stringResource(R.string.reset_done)
    val undoLabel = stringResource(R.string.action_undo)

    // Market rate follows the live ticker for the selected direction. actionId is a key
    // so a reset re-pushes the rate even when the ticker value has not moved.
    val liveMarketRate = tickerViewModel.liveRate(calculatorState.direction)
    LaunchedEffect(liveMarketRate, calculatorState.direction, calculatorState.actionId) {
        calculatorViewModel.onLiveMarketRate(liveMarketRate)
    }
    // EUR conversion for the profit line, needed when the base currency is USD.
    val liveUsdToEur = tickerViewModel.liveRate(TransferDirection.USD_TO_EUR)
    LaunchedEffect(liveUsdToEur) {
        calculatorViewModel.onLiveUsdToEurRate(liveUsdToEur)
    }

    LaunchedEffect(Unit) {
        updateViewModel.checkForUpdate(silent = true)
    }

    BackHandler(enabled = showSettings) { showSettings = false }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(
                            if (showSettings) R.string.settings_title else R.string.app_name
                        )
                    )
                },
                navigationIcon = {
                    if (showSettings) {
                        IconButton(onClick = { showSettings = false }) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.action_back),
                            )
                        }
                    }
                },
                actions = {
                    if (!showSettings) {
                        TextButton(onClick = onToggleLanguage) {
                            Text(stringResource(R.string.action_language))
                        }
                        IconButton(onClick = { showSettings = true }) {
                            Icon(
                                Icons.Filled.Settings,
                                contentDescription = stringResource(R.string.action_settings),
                            )
                        }
                        IconButton(onClick = { updateViewModel.checkForUpdate() }) {
                            Icon(
                                Icons.Filled.SystemUpdate,
                                contentDescription = stringResource(R.string.update_check),
                            )
                        }
                    }
                },
            )
        },
        bottomBar = {
            // Settings has its own full-width Save button; two action bars would confuse.
            if (showSettings) {
                // no action bar
            } else if (section == AppSection.DOWNLOAD) {
                CalculatorActionBar(
                    calculateEnabled = downloadState.hasAmount,
                    onCalculate = {
                        downloadViewModel.recalculateNow()
                        val state = downloadViewModel.uiState.value
                        state.hasAmount && state.result?.error == null
                    },
                    // No live rate and no saved sheet here — just two fields and the
                    // office default, so there is nothing to wait for and nothing to undo.
                    onReset = {
                        downloadViewModel.reset()
                        true
                    },
                )
            } else {
                CalculatorActionBar(
                    calculateEnabled = calculatorState.hasPrimaryAmount,
                    onCalculate = {
                        calculatorViewModel.recalculateNow()
                        val state = calculatorViewModel.uiState.value
                        state.hasPrimaryAmount && state.result?.error == null
                    },
                    onReset = {
                        calculatorViewModel.reset(liveMarketRate)
                        // The undo offer runs alongside the refresh, not after it.
                        scope.launch {
                            snackbarHostState.currentSnackbarData?.dismiss()
                            val outcome = snackbarHostState.showSnackbar(
                                message = resetDoneMessage,
                                actionLabel = undoLabel,
                                duration = SnackbarDuration.Short,
                            )
                            if (outcome == SnackbarResult.ActionPerformed) {
                                calculatorViewModel.undoReset()
                            }
                        }
                        // This is the real wait the spinner is showing.
                        tickerViewModel.manualRefresh().join()
                        val direction = calculatorViewModel.uiState.value.direction
                        val rate = tickerViewModel.liveRate(direction)
                        calculatorViewModel.onLiveMarketRate(rate)
                        direction.isSameCurrency || rate != null
                    },
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (showSettings) {
                SettingsScreen(
                    onSaved = {
                        calculatorViewModel.applyDefaults()
                        // Both sections read the same إعادة التنزيل default.
                        downloadViewModel.applyDefaults()
                        showSettings = false
                        scope.launch { snackbarHostState.showSnackbar(settingsSavedMessage) }
                    },
                )
            } else {
                TickerBoard(
                    pairs = pairs,
                    rates = rates,
                    isRefreshing = isRefreshing,
                    hasError = hasError,
                    onRefresh = { tickerViewModel.manualRefresh() },
                    onUpdatePairs = tickerViewModel::updatePairs,
                )

                // Two separate jobs, so they get separate screens rather than a fourth
                // mode tab: إعادة التنزيل has no direction, no rate and no beneficiary,
                // and the direction chips above the tabs would be meaningless for it.
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    AppSection.entries.forEachIndexed { index, entry ->
                        SegmentedButton(
                            selected = section == entry,
                            onClick = { section = entry },
                            shape = SegmentedButtonDefaults.itemShape(
                                index = index,
                                count = AppSection.entries.size,
                            ),
                        ) {
                            Text(
                                stringResource(
                                    if (entry == AppSection.TRANSFER) R.string.section_transfer
                                    else R.string.section_download
                                )
                            )
                        }
                    }
                }

                when (section) {
                    AppSection.TRANSFER -> CalculatorScreen(
                        state = calculatorState,
                        viewModel = calculatorViewModel,
                        liveMarketRate = liveMarketRate,
                    )
                    AppSection.DOWNLOAD -> CashDownloadScreen(
                        state = downloadState,
                        viewModel = downloadViewModel,
                    )
                }
            }
        }
    }

    UpdateDialog(viewModel = updateViewModel)
}

/** The two jobs the app does. Kept out of the transfer mode tabs on purpose. */
enum class AppSection { TRANSFER, DOWNLOAD }
