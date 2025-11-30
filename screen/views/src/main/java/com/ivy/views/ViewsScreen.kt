package com.ivy.views

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
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

// Category color mapping
private fun getCategoryColor(category: String): Color {
    return when (category.uppercase()) {
        "ASSET" -> Color(0xFF4CAF50) // Green
        "EXPENSE" -> Color(0xFFFF9800) // Orange  
        "LIABILITY" -> Color(0xFFF44336) // Red
        "INCOME" -> Color(0xFF2196F3) // Blue
        "SAVINGS" -> Color(0xFF9C27B0) // Purple
        else -> Color(0xFF607D8B) // Blue Grey default
    }
}

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

        // Filter Toggles
        FilterTogglesSection(
            includeExcluded = state.includeExcluded,
            includeArchived = state.includeArchived,
            onToggleExcluded = { viewModel.onEvent(ViewsEvent.ToggleExcluded) },
            onToggleArchived = { viewModel.onEvent(ViewsEvent.ToggleArchived) }
        )

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
                    accountGroup = accountGroup
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
private fun FilterTogglesSection(
    includeExcluded: Boolean,
    includeArchived: Boolean,
    onToggleExcluded: () -> Unit,
    onToggleArchived: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .background(
                color = UI.colors.pureInverse.copy(alpha = 0.05f),
                shape = RoundedCornerShape(8.dp)
            )
            .padding(12.dp)
    ) {
        Text(
            text = "Account Filters",
            style = UI.typo.b2.style(
                fontWeight = FontWeight.Medium,
                color = UI.colors.pureInverse.copy(alpha = 0.7f)
            )
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Excluded Toggle
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable { onToggleExcluded() }
            ) {
                Text(
                    text = "Excluded",
                    style = UI.typo.b2.style(
                        color = if (includeExcluded) 
                            UI.colors.pureInverse 
                        else 
                            UI.colors.pureInverse.copy(alpha = 0.5f)
                    )
                )
                Spacer(modifier = Modifier.width(8.dp))
                Switch(
                    checked = includeExcluded,
                    onCheckedChange = { onToggleExcluded() }
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // Archived Toggle
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable { onToggleArchived() }
            ) {
                Text(
                    text = "Archived",
                    style = UI.typo.b2.style(
                        color = if (includeArchived) 
                            UI.colors.pureInverse 
                        else 
                            UI.colors.pureInverse.copy(alpha = 0.5f)
                    )
                )
                Spacer(modifier = Modifier.width(8.dp))
                Switch(
                    checked = includeArchived,
                    onCheckedChange = { onToggleArchived() }
                )
            }
        }
    }
}

@Composable
private fun AccountGroupSection(
    state: ViewsState,
    category: String,
    accounts: List<com.ivy.legacy.data.model.AccountData>,
    accountGroup: com.ivy.views.AccountGroup
) {
    val categoryColor = getCategoryColor(category)
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .background(
                color = categoryColor.copy(alpha = 0.1f),
                shape = RoundedCornerShape(12.dp)
            )
            .border(
                width = 1.dp,
                color = categoryColor.copy(alpha = 0.3f),
                shape = RoundedCornerShape(12.dp)
            )
            .padding(12.dp)
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
                    fontWeight = FontWeight.Bold,
                    color = categoryColor
                )
            )

            // Display base currency total as main amount
            Column(
                horizontalAlignment = androidx.compose.ui.Alignment.End
            ) {
                BalanceRow(
                    currency = state.baseCurrency,
                    balance = accountGroup.baseCurrencyTotal,
                    balanceFontSize = 18.sp,
                    currencyFontSize = 14.sp,
                    textColor = categoryColor,
                    currencyUpfront = false
                )
                
                // Show all currency breakdowns below main total
                Spacer(modifier = Modifier.height(4.dp))
                accountGroup.currencyTotals.forEach { currencyTotal ->
                    Row(
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${currencyTotal.currency}: ",
                            style = UI.typo.b2.style(
                                color = categoryColor.copy(alpha = 0.7f)
                            )
                        )
                        Text(
                            text = String.format("%.2f", currencyTotal.originalBalance),
                            style = UI.typo.b2.style(
                                color = categoryColor.copy(alpha = 0.7f)
                            )
                        )
                        if (currencyTotal.exchangeRate != null) {
                            Text(
                                text = " (${String.format("%.4f", currencyTotal.exchangeRate)})",
                                style = UI.typo.b2.style(
                                    color = categoryColor.copy(alpha = 0.5f)
                                )
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Accounts list
        accounts.forEach { account ->
            AccountItemRow(
                account = account
            )
            Spacer(modifier = Modifier.height(4.dp))
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun AccountItemRow(
    account: com.ivy.legacy.data.model.AccountData
) {
    val accountColor = Color(account.account.color.value)
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 6.dp)
            .background(
                color = accountColor.copy(alpha = 0.08f),
                shape = RoundedCornerShape(8.dp)
            )
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            // Account color indicator
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .background(
                        color = accountColor,
                        shape = RoundedCornerShape(6.dp)
                    )
            )
            Spacer(modifier = Modifier.width(8.dp))
            
            Text(
                text = account.account.name.value,
                style = UI.typo.b2.style(
                    color = accountColor.copy(alpha = 0.8f)
                )
            )
        }

        BalanceRow(
            currency = account.account.asset.code,
            balance = account.balance,
            balanceFontSize = 14.sp,
            currencyFontSize = 11.sp,
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
