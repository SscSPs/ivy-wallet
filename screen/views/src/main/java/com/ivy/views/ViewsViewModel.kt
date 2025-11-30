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

    @Composable
    override fun uiState(): ViewsState {
        return ViewsState(
            baseCurrency = baseCurrency,
            accountsData = accountsData.toImmutableList(),
            groupedAccounts = groupedAccounts.toImmutableList(),
            hideTotalBalance = hideTotalBalance,
            includeExcluded = includeExcluded,
            includeArchived = includeArchived,
        )
    }

    override fun onEvent(event: ViewsEvent) {
        when (event) {
            is ViewsEvent.LoadData -> loadData()
            is ViewsEvent.ToggleExcluded -> {
                includeExcluded = !includeExcluded
                loadData() // Reload data with new filter
            }
            is ViewsEvent.ToggleArchived -> {
                includeArchived = !includeArchived
                loadData() // Reload data with new filter
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
                val allAccounts = accountRepository.findAll(
                    includeArchived = includeArchived
                ).toImmutableList()
                
                println("ViewsScreen: Total accounts found: ${allAccounts.size}, includeExcluded: $includeExcluded, includeArchived: $includeArchived")
                
                val accounts = allAccounts.filter { account ->
                    // Include excluded accounts only if toggle is on
                    val shouldInclude = if (!includeExcluded && !account.includeInBalance) {
                        false
                    } else {
                        true
                    }
                    shouldInclude
                }.toImmutableList()
                
                println("ViewsScreen: Accounts after filtering: ${accounts.size}")

                val accountsDataList = accountDataAct(
                    AccountDataAct.Input(
                        accounts = accounts,
                        range = range.toCloseTimeRange(),
                        baseCurrency = baseCurrencyCode
                    )
                )
                
                println("ViewsScreen: AccountDataAct returned: ${accountsDataList.size} items")

                // Get exchange rates - use findAll() to get all rates including synced ones
                val exchangeRates = try {
                    // Use first() to get the first emission without blocking
                    exchangeRatesRepository.findAll().first()
                } catch (e: Exception) {
                    println("ViewsScreen: Error getting exchange rates: ${e.message}")
                    e.printStackTrace()
                    emptyList()
                }
                
                println("ViewsScreen: Found ${exchangeRates.size} exchange rates")
                exchangeRates.forEach { rate ->
                    println("ViewsScreen: Rate: baseCurrency=${rate.baseCurrency.code}, currency=${rate.currency.code}, rate=${rate.rate.value}")
                }

                val groupedAccounts = groupAccountsByCategoryWithConversion(
                    accountsDataList.toImmutableList(),
                    baseCurrencyCode,
                    exchangeRates
                )

                this@ViewsViewModel.accountsData = accountsDataList
                this@ViewsViewModel.groupedAccounts = groupedAccounts
                
                // Debug logging
                println("ViewsScreen: Loaded ${accountsDataList.size} accounts, ${groupedAccounts.size} groups")
                
            } catch (e: Exception) {
                println("ViewsScreen: Error loading data: ${e.message}")
                // Handle any exceptions and show empty state
                this@ViewsViewModel.accountsData = emptyList()
                this@ViewsViewModel.groupedAccounts = emptyList()
            }
        }
    }

    private fun groupAccountsByCategoryWithConversion(
    accountsData: ImmutableList<AccountData>,
    baseCurrency: String,
    exchangeRates: List<ExchangeRate>
): ImmutableList<AccountGroup> {
    if (accountsData.isEmpty()) return emptyList<AccountGroup>().toImmutableList()
    
    val grouped = accountsData
        .groupBy { it.account.accountCategory.name.replace("_", " ") }
        .map { (category, accounts) ->
            // Convert each account to base currency
            val convertedAccounts = accounts.map { accountData ->
                val currency = accountData.account.asset.code
                val originalBalance = accountData.balance
                
                val baseCurrencyBalance = if (currency == baseCurrency) {
                    originalBalance
                } else {
                    val foundRate = exchangeRates.find { 
                        it.baseCurrency.code == baseCurrency && 
                        it.currency.code == currency 
                    }
                    
                    // The rate is inverted: it represents how much currency per 1 base unit
                    // So we need to use 1/rate to convert currency to base
                    val rate = foundRate?.rate?.value ?: 1.0
                    val invertedRate = 1.0 / rate
                    
                    println("ViewsScreen: Converting $originalBalance $currency to $baseCurrency")
                    println("ViewsScreen:   Looking for: baseCurrency=$baseCurrency, currency=$currency")
                    println("ViewsScreen:   Found rate: $foundRate")
                    println("ViewsScreen:   Original rate: $rate, Inverted rate: $invertedRate")
                    println("ViewsScreen:   Result: $originalBalance * $invertedRate = ${originalBalance * invertedRate}")
                    
                    originalBalance * invertedRate
                }
                
                Pair(accountData, baseCurrencyBalance)
            }
            
            // Calculate total in base currency by summing all converted amounts
            val baseCurrencyTotal = convertedAccounts.sumOf { it.second }
            
            // Group by currency to show breakdown
            val currencyTotals = accounts
                .groupBy { it.account.asset.code }
                .map { (currency, currencyAccounts) ->
                    val originalBalance = currencyAccounts.sumOf { it.balance }
                    
                    // Get the exchange rate for this currency
                    val exchangeRate = if (currency == baseCurrency) {
                        null
                    } else {
                        exchangeRates.find { 
                            it.baseCurrency.code == baseCurrency && 
                            it.currency.code == currency 
                        }?.rate?.value
                    }
                    
                    // Calculate what this currency's total converts to
                    val baseCurrencyBalance = if (currency == baseCurrency) {
                        originalBalance
                    } else {
                        val rate = exchangeRate ?: 1.0
                        val invertedRate = 1.0 / rate
                        originalBalance * invertedRate
                    }
                    
                    println("ViewsScreen: Category $category, Currency $currency: original=$originalBalance, rate=$exchangeRate, inverted=${if (exchangeRate != null) 1.0/exchangeRate else null}, converted=$baseCurrencyBalance")
                    
                    CurrencyTotal(
                        currency = currency,
                        originalBalance = originalBalance,
                        baseCurrencyBalance = baseCurrencyBalance,
                        exchangeRate = exchangeRate
                    )
                }
                .sortedBy { it.currency }
                .toImmutableList()
            
            println("ViewsScreen: Category $category: baseCurrencyTotal=$baseCurrencyTotal")
            
            AccountGroup(
                category = category,
                accounts = accounts.toImmutableList(),
                currencyTotals = currencyTotals,
                baseCurrencyTotal = baseCurrencyTotal
            )
        }
        .sortedBy { it.category }
        .toImmutableList()

    return grouped
}

private fun groupAccountsByCategory(accountsData: ImmutableList<AccountData>): ImmutableList<AccountGroup> {
        if (accountsData.isEmpty()) return emptyList<AccountGroup>().toImmutableList()
        
        val grouped = accountsData
            .groupBy { it.account.accountCategory.name.replace("_", " ") }
            .map { (category, accounts) ->
                val currencyTotals = accounts
                    .groupBy { it.account.asset.code }
                    .map { (currency, currencyAccounts) ->
                        val originalBalance = currencyAccounts.sumOf { it.balance }
                        CurrencyTotal(
                            currency = currency,
                            originalBalance = originalBalance,
                            baseCurrencyBalance = originalBalance, // No conversion in fallback
                            exchangeRate = null
                        )
                    }
                    .sortedBy { it.currency }
                    .toImmutableList()
                
                val baseCurrencyTotal = currencyTotals.sumOf { it.baseCurrencyBalance }
                
                AccountGroup(
                    category = category,
                    accounts = accounts.toImmutableList(),
                    currencyTotals = currencyTotals,
                    baseCurrencyTotal = baseCurrencyTotal
                )
            }
            .sortedBy { it.category }
            .toImmutableList()

        return grouped
    }
}
