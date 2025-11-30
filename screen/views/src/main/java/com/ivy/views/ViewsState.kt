package com.ivy.views

import androidx.compose.runtime.Immutable
import com.ivy.legacy.data.model.AccountData
import kotlinx.collections.immutable.ImmutableList

@Immutable
data class ViewsState(
    val baseCurrency: String,
    val accountsData: ImmutableList<AccountData>,
    val groupedAccounts: ImmutableList<AccountGroup>,
    val hideTotalBalance: Boolean,
    val includeExcluded: Boolean,
    val includeArchived: Boolean,
)

@Immutable
data class AccountGroup(
    val category: String,
    val accounts: ImmutableList<AccountData>,
    val currencyTotals: ImmutableList<CurrencyTotal>,
    val baseCurrencyTotal: Double, // Total converted to base currency
)

@Immutable
data class CurrencyTotal(
    val currency: String,
    val originalBalance: Double,
    val baseCurrencyBalance: Double, // Converted amount
    val exchangeRate: Double?, // Exchange rate used (null if same as base)
)
