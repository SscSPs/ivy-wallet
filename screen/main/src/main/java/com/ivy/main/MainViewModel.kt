package com.ivy.main

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewModelScope
import com.ivy.base.legacy.SharedPrefs
import com.ivy.data.preferences.UserPreferencesRepository
import com.ivy.data.repository.CurrencyRepository
import com.ivy.domain.usecase.exchange.SyncExchangeRatesUseCase
import com.ivy.frp.test.TestIdlingResource
import com.ivy.legacy.IvyWalletCtx
import com.ivy.legacy.data.model.MainTab
import com.ivy.legacy.domain.deprecated.logic.AccountCreator
import com.ivy.legacy.utils.ioThread
import com.ivy.navigation.MainScreen
import com.ivy.navigation.Navigation
import com.ivy.ui.ComposeViewModel
import com.ivy.wallet.domain.deprecated.logic.model.CreateAccountData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@Immutable
data class MainScreenState(
    val currentTab: MainTab = MainTab.HOME,
    val baseCurrency: String = ""
)

sealed interface MainScreenEvent {
    data class OnTabSelected(val tab: MainTab) : MainScreenEvent
    data class OnCreateAccount(val data: CreateAccountData) : MainScreenEvent
}

@HiltViewModel
class MainViewModel @Inject constructor(
    private val nav: Navigation,
    private val syncExchangeRatesUseCase: SyncExchangeRatesUseCase,
    private val accountCreator: AccountCreator,
    private val sharedPrefs: SharedPrefs,
    private val currencyRepository: CurrencyRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    // Keeping IvyWalletCtx for now to maintain compatibility with other legacy components
    // but it should not be used for state managed by UserPreferencesRepository
    private val ivyContext: IvyWalletCtx
) : ComposeViewModel<MainScreenState, MainScreenEvent>() {

    private val _currency = MutableStateFlow("")
    private val _currentTab = MutableStateFlow(MainTab.HOME)

    // Combine flows to create the UI state
    private val _uiState = combine(
        _currentTab,
        _currency
    ) { tab, currency ->
        // Sync legacy context for compatibility
        ivyContext.selectMainTab(tab)

        MainScreenState(
            currentTab = tab,
            baseCurrency = currency
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = MainScreenState()
    )

    val state: StateFlow<MainScreenState> = _uiState

    @Composable
    override fun uiState(): MainScreenState {
        val state by _uiState.collectAsState()
        return state
    }

    override fun onEvent(event: MainScreenEvent) {
        when (event) {
            is MainScreenEvent.OnTabSelected -> selectTab(event.tab)
            is MainScreenEvent.OnCreateAccount -> createAccount(event.data)
        }
    }

    fun start(screen: MainScreen) {
        nav.onBackPressed[screen] = {
            val currentTab = _uiState.value.currentTab
            if (currentTab == MainTab.ACCOUNTS) {
                onEvent(MainScreenEvent.OnTabSelected(MainTab.HOME))
                true
            } else {
                // Exiting (the backstack will close the app)
                false
            }
        }

        viewModelScope.launch {
            TestIdlingResource.increment()

            val baseCurrency = currencyRepository.getBaseCurrency()
            _currency.value = baseCurrency.code

            // Sync legacy shared prefs to new repository if needed
            val backupCompleted = sharedPrefs.getBoolean(SharedPrefs.DATA_BACKUP_COMPLETED, false)
            userPreferencesRepository.setDataBackupCompleted(backupCompleted)

            ioThread {
                // Sync exchange rates
                syncExchangeRatesUseCase.sync(baseCurrency)
            }

            TestIdlingResource.decrement()
        }
    }

    private fun selectTab(tab: MainTab) {
        _currentTab.value = tab
    }

    private fun createAccount(data: CreateAccountData) {
        viewModelScope.launch {
            TestIdlingResource.increment()

            accountCreator.createAccount(data) {}

            TestIdlingResource.decrement()
        }
    }
}
