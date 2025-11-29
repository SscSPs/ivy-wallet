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
)

@Immutable
data class AccountGroup(
    val category: String,
    val accounts: ImmutableList<AccountData>,
    val currencyTotals: ImmutableList<CurrencyTotal>,
)

@Immutable
data class CurrencyTotal(
    val currency: String,
    val totalBalance: Double,
)
