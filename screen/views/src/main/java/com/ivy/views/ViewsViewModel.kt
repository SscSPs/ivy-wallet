package com.ivy.views

import android.annotation.SuppressLint
import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewModelScope
import com.ivy.base.legacy.SharedPrefs
import com.ivy.base.time.TimeConverter
import com.ivy.base.time.TimeProvider
import com.ivy.data.DataObserver
import com.ivy.data.DataWriteEvent
import com.ivy.data.repository.AccountRepository
import com.ivy.domain.features.Features
import com.ivy.legacy.IvyWalletCtx
import com.ivy.legacy.data.model.AccountData
import com.ivy.legacy.data.model.toCloseTimeRange
import com.ivy.legacy.utils.ioThread
import com.ivy.ui.ComposeViewModel
import com.ivy.wallet.domain.action.settings.BaseCurrencyAct
import com.ivy.wallet.domain.action.viewmodel.account.AccountDataAct
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@Stable
@SuppressLint("StaticFieldLeak")
@HiltViewModel
class ViewsViewModel @Inject constructor(
    @ApplicationContext
    private val context: Context,
    private val ivyContext: IvyWalletCtx,
    private val sharedPrefs: SharedPrefs,
    private val baseCurrencyAct: BaseCurrencyAct,
    private val accountDataAct: AccountDataAct,
    private val accountRepository: AccountRepository,
    private val dataObserver: DataObserver,
    private val features: Features,
    private val timeConverter: TimeConverter,
    private val timeProvider: TimeProvider,
) : ComposeViewModel<ViewsState, ViewsEvent>() {

    private var baseCurrency by mutableStateOf("USD") // Default fallback
    private var accountsData by mutableStateOf(listOf<AccountData>())
    private var groupedAccounts by mutableStateOf(listOf<AccountGroup>())
    private var hideTotalBalance by mutableStateOf(false)

    @Composable
    override fun uiState(): ViewsState {
        return ViewsState(
            baseCurrency = baseCurrency,
            accountsData = accountsData.toImmutableList(),
            groupedAccounts = groupedAccounts.toImmutableList(),
            hideTotalBalance = hideTotalBalance,
        )
    }

    override fun onEvent(event: ViewsEvent) {
        when (event) {
            is ViewsEvent.LoadData -> loadData()
        }
    }

    init {
        viewModelScope.launch {
            try {
                baseCurrency = baseCurrencyAct(Unit)
                hideTotalBalance = false // Simplified for now
            } catch (e: Exception) {
                baseCurrency = "USD" // Fallback
            }
            
            // Start listening for data changes
            dataObserver.writeEvents.collectLatest { event ->
                when (event) {
                    is DataWriteEvent.AccountChange -> {
                        loadData()
                    }
                    else -> {}
                }
            }
        }
    }

    private fun loadData() {
        viewModelScope.launch {
            loadAccounts()
        }
    }

    private fun loadAccounts() {
        viewModelScope.launch {
            try {
                val range = ivyContext.selectedPeriod.toRange(ivyContext.startDayOfMonth, timeConverter, timeProvider)
                val baseCurrencyCode = baseCurrencyAct(Unit)
                val accounts = accountRepository.findAll(includeArchived = true).toImmutableList()

                val accountsDataList = accountDataAct(
                    AccountDataAct.Input(
                        accounts = accounts,
                        range = range.toCloseTimeRange(),
                        baseCurrency = baseCurrencyCode
                    )
                )

                val groupedAccounts = groupAccountsByCategory(accountsDataList.toImmutableList())

                this@ViewsViewModel.accountsData = accountsDataList
                this@ViewsViewModel.groupedAccounts = groupedAccounts
            } catch (e: Exception) {
                // Handle any exceptions and show empty state
                this@ViewsViewModel.accountsData = emptyList()
                this@ViewsViewModel.groupedAccounts = emptyList()
            }
        }
    }

    private fun groupAccountsByCategory(accountsData: ImmutableList<AccountData>): ImmutableList<AccountGroup> {
        if (accountsData.isEmpty()) return emptyList<AccountGroup>().toImmutableList()
        
        val grouped = accountsData
            .groupBy { it.account.accountCategory.name.replace("_", " ") }
            .map { (category, accounts) ->
                val currencyTotals = accounts
                    .groupBy { it.account.asset.code }
                    .map { (currency, currencyAccounts) ->
                        CurrencyTotal(
                            currency = currency,
                            totalBalance = currencyAccounts.sumOf { it.balance }
                        )
                    }
                    .sortedBy { it.currency }
                    .toImmutableList()
                
                AccountGroup(
                    category = category,
                    accounts = accounts.toImmutableList(),
                    currencyTotals = currencyTotals
                )
            }
            .sortedBy { it.category }
            .toImmutableList()

        return grouped
    }
}
