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
import com.ivy.data.repository.ExchangeRatesRepository
import com.ivy.data.model.ExchangeRate
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
import kotlinx.coroutines.flow.first
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
    private val exchangeRatesRepository: ExchangeRatesRepository,
    private val dataObserver: DataObserver,
    private val features: Features,
    private val timeConverter: TimeConverter,
    private val timeProvider: TimeProvider,
) : ComposeViewModel<ViewsState, ViewsEvent>() {

    private var baseCurrency by mutableStateOf("USD") // Default fallback
    private var accountsData by mutableStateOf(listOf<AccountData>())
    private var groupedAccounts by mutableStateOf(listOf<AccountGroup>())
    private var hideTotalBalance by mutableStateOf(false)
    private var includeExcluded by mutableStateOf(false)  // Default: exclude excluded accounts
    private var includeArchived by mutableStateOf(true)  // Default: include archived accounts
    private var expandedCategories by mutableStateOf(setOf<String>())  // Track expanded categories
    private var includeZeroBalance by mutableStateOf(true)  // Default: include zero balance accounts

    @Composable
    override fun uiState(): ViewsState {
        // Calculate net worth correctly: Assets - Liabilities
        val assetsTotal = groupedAccounts
            .filter { it.category.uppercase().contains("ASSET") }
            .sumOf { it.baseCurrencyTotal }
        
        val liabilitiesTotal = groupedAccounts
            .filter { it.category.uppercase().contains("LIABILITY") }
            .sumOf { it.baseCurrencyTotal }
        
        val netWorth = assetsTotal - liabilitiesTotal
        
        return ViewsState(
            baseCurrency = baseCurrency,
            accountsData = accountsData.toImmutableList(),
            groupedAccounts = groupedAccounts.toImmutableList(),
            hideTotalBalance = hideTotalBalance,
            includeExcluded = includeExcluded,
            includeArchived = includeArchived,
            expandedCategories = expandedCategories.toImmutableList(),
            includeZeroBalance = includeZeroBalance,
            netWorth = netWorth,
        )
    }

    override fun onEvent(event: ViewsEvent) {
        when (event) {
            is ViewsEvent.LoadData -> loadData()
            is ViewsEvent.ToggleExcluded -> {
                includeExcluded = !includeExcluded
                loadData()
            }
            is ViewsEvent.ToggleArchived -> {
                includeArchived = !includeArchived
                loadData()
            }
            is ViewsEvent.ToggleZeroBalance -> {
                includeZeroBalance = !includeZeroBalance
                loadData()
            }
            is ViewsEvent.ToggleCategoryExpand -> {
                expandedCategories = if (event.category in expandedCategories) {
                    expandedCategories - event.category
                } else {
                    expandedCategories + event.category
                }
            }
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
                
                // Apply filters based on toggle states
                val allAccounts = accountRepository.findAll(includeArchived = true)
                
                val filteredAccounts = allAccounts.filter { account ->
                    val shouldIncludeByExcluded = includeExcluded || account.includeInBalance
                    val shouldIncludeByArchived = includeArchived || !account.archived
                    shouldIncludeByExcluded && shouldIncludeByArchived
                }

                val accountsDataList = accountDataAct(
                    AccountDataAct.Input(
                        accounts = filteredAccounts.toImmutableList(),
                        range = range.toCloseTimeRange(),
                        baseCurrency = baseCurrencyCode
                    )
                )

                // Apply zero balance filter
                val finalAccountsData = if (includeZeroBalance) {
                    accountsDataList
                } else {
                    accountsDataList.filter { it.balance != 0.0 }
                }

                // Get exchange rates
                val exchangeRates = try {
                    exchangeRatesRepository.findAll().first()
                } catch (e: Exception) {
                    println("ViewsScreen: Error getting exchange rates: ${e.message}")
                    e.printStackTrace()
                    emptyList()
                }

                val groupedAccounts = groupAccountsByCategoryWithConversion(
                    finalAccountsData.toImmutableList(),
                    baseCurrencyCode,
                    exchangeRates
                ).toImmutableList()

                this@ViewsViewModel.accountsData = finalAccountsData
                this@ViewsViewModel.groupedAccounts = groupedAccounts
                
            } catch (e: Exception) {
                println("ViewsScreen: Error loading data: ${e.message}")
                this@ViewsViewModel.accountsData = emptyList()
                this@ViewsViewModel.groupedAccounts = emptyList()
            }
        }
    }

    private fun convertToBaseCurrency(
        amount: Double,
        fromCurrency: String,
        baseCurrency: String,
        exchangeRates: List<ExchangeRate>
    ): Double {
        if (fromCurrency == baseCurrency) return amount
        
        val exchangeRate = exchangeRates.find { 
            it.baseCurrency.code == baseCurrency && 
            it.currency.code == fromCurrency 
        }
        
        // Rate is inverted (currency per base unit), so invert it for conversion
        val rate = exchangeRate?.rate?.value ?: 1.0
        return amount * (1.0 / rate)
    }

    private fun groupAccountsByCategoryWithConversion(
        accountsData: ImmutableList<AccountData>,
        baseCurrency: String,
        exchangeRates: List<ExchangeRate>
    ): List<AccountGroup> {
        if (accountsData.isEmpty()) return emptyList<AccountGroup>()

        val result = accountsData
            .groupBy { it.account.accountCategory.name.replace("_", " ") }
            .map { (category, accounts) ->
                // Convert accounts to base currency
                val convertedAccounts = accounts.map { accountData ->
                    val baseCurrencyBalance = convertToBaseCurrency(
                        amount = accountData.balance,
                        fromCurrency = accountData.account.asset.code,
                        baseCurrency = baseCurrency,
                        exchangeRates = exchangeRates
                    )
                    Pair(accountData, baseCurrencyBalance)
                }

                val baseCurrencyTotal = convertedAccounts.sumOf { it.second }

                // Calculate currency breakdowns
                val currencyTotals = accounts
                    .groupBy { it.account.asset.code }
                    .map { (currency, currencyAccounts) ->
                        val originalBalance = currencyAccounts.sumOf { it.balance }
                        val baseCurrencyBalance = convertToBaseCurrency(
                            amount = originalBalance,
                            fromCurrency = currency,
                            baseCurrency = baseCurrency,
                            exchangeRates = exchangeRates
                        )
                        
                        val exchangeRate = if (currency == baseCurrency) {
                            null
                        } else {
                            exchangeRates.find {
                                it.baseCurrency.code == baseCurrency &&
                                it.currency.code == currency
                            }?.rate?.value
                        }

                        CurrencyTotal(
                            currency = currency,
                            originalBalance = originalBalance,
                            baseCurrencyBalance = baseCurrencyBalance,
                            exchangeRate = exchangeRate
                        )
                    }
                    .sortedBy { it.currency }
                    .toImmutableList()

                AccountGroup(
                    category = category,
                    accounts = accounts.toImmutableList(),
                    currencyTotals = currencyTotals,
                    baseCurrencyTotal = baseCurrencyTotal
                )
            }
            .sortedBy { it.category }
        
        return result
    }
}
