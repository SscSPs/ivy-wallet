package com.ivy.views

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ivy.design.l0_system.UI
import com.ivy.design.l0_system.style
import com.ivy.legacy.IvyWalletPreview
import com.ivy.legacy.ivyWalletCtx
import com.ivy.navigation.navigation
import com.ivy.navigation.screenScopedViewModel
import com.ivy.ui.R
import com.ivy.wallet.ui.theme.components.BalanceRow
import com.ivy.wallet.ui.theme.components.IvyIcon
import com.ivy.wallet.ui.theme.modal.AddModalBackHandling
import java.util.UUID
import androidx.compose.foundation.layout.BoxWithConstraintsScope

@Composable
fun BoxWithConstraintsScope.ViewsScreen() {
    val viewModel: ViewsViewModel = screenScopedViewModel()
    val state = viewModel.uiState()
    
    LaunchedEffect(Unit) {
        viewModel.onEvent(ViewsEvent.LoadData)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
    ) {
        Toolbar()

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
        ) {
            items(state.groupedAccounts) { accountGroup ->
                AccountGroupSection(
                    state = state,
                    category = accountGroup.category,
                    accounts = accountGroup.accounts,
                    currencyTotals = accountGroup.currencyTotals
                )
            }
        }
    }
}

@Composable
private fun Toolbar() {
    val nav = navigation()
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
    ) {
        IvyIcon(
            icon = R.drawable.ic_back,
            modifier = androidx.compose.ui.Modifier.clickable {
                nav.back()
            }
        )

        Spacer(modifier = Modifier.width(16.dp))

        Text(
            text = "Views",
            style = UI.typo.h2.style(
                fontWeight = FontWeight.ExtraBold
            )
        )
    }
}

@Composable
private fun AccountGroupSection(
    state: ViewsState,
    category: String,
    accounts: List<com.ivy.legacy.data.model.AccountData>,
    currencyTotals: List<com.ivy.views.CurrencyTotal>
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // Category header with currency totals
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {
            Text(
                text = category,
                style = UI.typo.b1.style(
                    fontWeight = FontWeight.Bold
                )
            )

            // Display multiple currency totals
            Column(
                horizontalAlignment = androidx.compose.ui.Alignment.End
            ) {
                currencyTotals.forEach { currencyTotal ->
                    BalanceRow(
                        currency = currencyTotal.currency,
                        balance = currencyTotal.totalBalance,
                        balanceFontSize = 16.sp,
                        currencyFontSize = 12.sp,
                        textColor = UI.colors.pureInverse,
                        currencyUpfront = false
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Accounts list
        accounts.forEach { account ->
            AccountItemRow(
                name = account.account.name.value,
                balance = account.balance,
                currency = account.account.asset.code
            )
            Spacer(modifier = Modifier.height(4.dp))
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun AccountItemRow(
    name: String,
    balance: Double,
    currency: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
    ) {
        Text(
            text = name,
            style = UI.typo.b2.style()
        )

        BalanceRow(
            currency = currency,
            balance = balance,
            balanceFontSize = 16.sp,
            currencyFontSize = 12.sp,
            textColor = UI.colors.pureInverse,
            currencyUpfront = false
        )
    }
}

@Preview
@Composable
private fun BoxWithConstraintsScope.PreviewViewsScreen() {
    IvyWalletPreview {
        ViewsScreen()
    }
}
