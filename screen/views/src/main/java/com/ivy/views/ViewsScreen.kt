package com.ivy.views

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ivy.design.l0_system.UI
import com.ivy.design.l0_system.style
import com.ivy.legacy.ivyWalletCtx
import com.ivy.navigation.navigation
import com.ivy.navigation.screenScopedViewModel
import com.ivy.ui.R
import com.ivy.wallet.ui.theme.components.BalanceRow
import com.ivy.wallet.ui.theme.components.IvyIcon
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.size
import androidx.compose.ui.tooling.preview.Preview
import com.ivy.legacy.IvyWalletPreview

// UI Constants
private object UiConstants {
    val PADDING_MEDIUM = 16.dp
    val PADDING_SMALL = 8.dp
    val PADDING_LARGE = 12.dp
    val CORNER_RADIUS_SMALL = 8.dp
    val CORNER_RADIUS_MEDIUM = 12.dp
    val CORNER_RADIUS_TINY = 6.dp
    val ALPHA_LOW = 0.05f
    val ALPHA_MEDIUM = 0.7f
    val ALPHA_DISABLED = 0.5f
    val ALPHA_CATEGORY_BG = 0.1f
    val ALPHA_CATEGORY_BORDER = 0.3f
    val ALPHA_CURRENCY_TEXT = 0.7f
    val ALPHA_EXCHANGE_RATE = 0.5f
    val ALPHA_ACCOUNT_TEXT = 0.8f
    val SPACING_SMALL = 8.dp
    val SPACING_MEDIUM = 16.dp
    val SPACING_TINY = 4.dp
    val BORDER_WIDTH_THIN = 1.dp
    val ICON_WIDTH = 20.dp
    val INDICATOR_SIZE = 12.dp
}

// Color constants for categories
private object CategoryColors {
    val ASSET = Color(0xFF4CAF50)
    val EXPENSE = Color(0xFFFF9800)
    val LIABILITY = Color(0xFFF44336)
    val INCOME = Color(0xFF2196F3)
    val SAVINGS = Color(0xFF9C27B0)
    val DEFAULT = Color(0xFF607D8B)
}

// Category color mapping
private fun getCategoryColor(category: String): Color {
    return when (category.uppercase()) {
        "ASSET" -> CategoryColors.ASSET
        "EXPENSE" -> CategoryColors.EXPENSE
        "LIABILITY" -> CategoryColors.LIABILITY
        "INCOME" -> CategoryColors.INCOME
        "SAVINGS" -> CategoryColors.SAVINGS
        else -> CategoryColors.DEFAULT
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

        // Net Worth Section
        NetWorthSection(
            netWorth = state.netWorth,
            baseCurrency = state.baseCurrency
        )

        // Filter Toggles
        FilterTogglesSection(
            includeExcluded = state.includeExcluded,
            includeArchived = state.includeArchived,
            includeZeroBalance = state.includeZeroBalance,
            onToggleExcluded = { viewModel.onEvent(ViewsEvent.ToggleExcluded) },
            onToggleArchived = { viewModel.onEvent(ViewsEvent.ToggleArchived) },
            onToggleZeroBalance = { viewModel.onEvent(ViewsEvent.ToggleZeroBalance) }
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
        ) {
            items(state.groupedAccounts) { accountGroup ->
                val isExpanded = accountGroup.category in state.expandedCategories
                AccountGroupSection(
                    state = state,
                    category = accountGroup.category,
                    accounts = accountGroup.accounts,
                    accountGroup = accountGroup,
                    isExpanded = isExpanded,
                    onToggleExpand = { viewModel.onEvent(ViewsEvent.ToggleCategoryExpand(accountGroup.category)) }
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
private fun NetWorthSection(
    netWorth: Double,
    baseCurrency: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = UiConstants.PADDING_MEDIUM, vertical = UiConstants.PADDING_SMALL)
            .background(
                color = UI.colors.pureInverse.copy(alpha = UiConstants.ALPHA_LOW),
                shape = RoundedCornerShape(UiConstants.CORNER_RADIUS_MEDIUM)
            )
            .padding(UiConstants.PADDING_LARGE)
    ) {
        Text(
            text = "Net Worth",
            style = UI.typo.b2.style(
                fontWeight = FontWeight.Medium,
                color = UI.colors.pureInverse.copy(alpha = UiConstants.ALPHA_MEDIUM)
            )
        )
        
        Spacer(modifier = Modifier.height(UiConstants.SPACING_SMALL))
        
        BalanceRow(
            currency = baseCurrency,
            balance = netWorth,
            balanceFontSize = 24.sp,
            currencyFontSize = 16.sp,
            textColor = UI.colors.pureInverse,
            currencyUpfront = false
        )
    }
}

@Composable
private fun FilterTogglesSection(
    includeExcluded: Boolean,
    includeArchived: Boolean,
    includeZeroBalance: Boolean,
    onToggleExcluded: () -> Unit,
    onToggleArchived: () -> Unit,
    onToggleZeroBalance: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = UiConstants.PADDING_MEDIUM, vertical = UiConstants.PADDING_SMALL)
            .background(
                color = UI.colors.pureInverse.copy(alpha = UiConstants.ALPHA_LOW),
                shape = RoundedCornerShape(UiConstants.CORNER_RADIUS_SMALL)
            )
            .padding(UiConstants.PADDING_LARGE)
    ) {
        Text(
            text = "Account Filters",
            style = UI.typo.b2.style(
                fontWeight = FontWeight.Medium,
                color = UI.colors.pureInverse.copy(alpha = UiConstants.ALPHA_MEDIUM)
            )
        )
        
        Spacer(modifier = Modifier.height(UiConstants.SPACING_SMALL))
        
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(UiConstants.SPACING_SMALL)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(UiConstants.SPACING_MEDIUM),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Excluded Toggle
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onToggleExcluded() }
                ) {
                    Text(
                        text = "Excluded",
                        style = UI.typo.b2.style(
                            color = if (includeExcluded) 
                                UI.colors.pureInverse 
                            else 
                                UI.colors.pureInverse.copy(alpha = UiConstants.ALPHA_DISABLED)
                        )
                    )
                    Spacer(modifier = Modifier.width(UiConstants.SPACING_SMALL))
                    Switch(
                        checked = includeExcluded,
                        onCheckedChange = { onToggleExcluded() }
                    )
                }
                
                // Archived Toggle
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onToggleArchived() }
                ) {
                    Text(
                        text = "Archived",
                        style = UI.typo.b2.style(
                            color = if (includeArchived) 
                                UI.colors.pureInverse 
                            else 
                                UI.colors.pureInverse.copy(alpha = UiConstants.ALPHA_DISABLED)
                        )
                    )
                    Spacer(modifier = Modifier.width(UiConstants.SPACING_SMALL))
                    Switch(
                        checked = includeArchived,
                        onCheckedChange = { onToggleArchived() }
                    )
                }
            }
            
            // Zero Balance Toggle
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable { onToggleZeroBalance() }
            ) {
                Text(
                    text = "Zero Balance",
                    style = UI.typo.b2.style(
                        color = if (includeZeroBalance) 
                            UI.colors.pureInverse 
                        else 
                            UI.colors.pureInverse.copy(alpha = UiConstants.ALPHA_DISABLED)
                    )
                )
                Spacer(modifier = Modifier.width(UiConstants.SPACING_SMALL))
                Switch(
                    checked = includeZeroBalance,
                    onCheckedChange = { onToggleZeroBalance() }
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
    accountGroup: com.ivy.views.AccountGroup,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit
) {
    val categoryColor = getCategoryColor(category)
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = UiConstants.PADDING_MEDIUM, vertical = UiConstants.PADDING_SMALL)
            .background(
                color = categoryColor.copy(alpha = UiConstants.ALPHA_CATEGORY_BG),
                shape = RoundedCornerShape(UiConstants.CORNER_RADIUS_MEDIUM)
            )
            .border(
                width = UiConstants.BORDER_WIDTH_THIN,
                color = categoryColor.copy(alpha = UiConstants.ALPHA_CATEGORY_BORDER),
                shape = RoundedCornerShape(UiConstants.CORNER_RADIUS_MEDIUM)
            )
            .padding(UiConstants.PADDING_LARGE)
    ) {
        // Category header with currency totals and expand/collapse
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onToggleExpand() },
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
            ) {
                // Expand/Collapse icon
                Text(
                    text = if (isExpanded) "▼" else "▶",
                    style = UI.typo.b1.style(
                        fontWeight = FontWeight.Bold,
                        color = categoryColor
                    ),
                    modifier = Modifier.width(UiConstants.ICON_WIDTH)
                )
                
                Spacer(modifier = Modifier.width(UiConstants.SPACING_SMALL))
                
                Text(
                    text = category,
                    style = UI.typo.b1.style(
                        fontWeight = FontWeight.Bold,
                        color = categoryColor
                    )
                )
            }

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
                Spacer(modifier = Modifier.height(UiConstants.SPACING_TINY))
                accountGroup.currencyTotals.forEach { currencyTotal ->
                    Row(
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${currencyTotal.currency}: ",
                            style = UI.typo.b2.style(
                                color = categoryColor.copy(alpha = UiConstants.ALPHA_CURRENCY_TEXT)
                            )
                        )
                        Text(
                            text = String.format("%.2f", currencyTotal.originalBalance),
                            style = UI.typo.b2.style(
                                color = categoryColor.copy(alpha = UiConstants.ALPHA_CURRENCY_TEXT)
                            )
                        )
                        if (currencyTotal.exchangeRate != null) {
                            Text(
                                text = " (${String.format("%.4f", currencyTotal.exchangeRate)})",
                                style = UI.typo.b2.style(
                                    color = categoryColor.copy(alpha = UiConstants.ALPHA_EXCHANGE_RATE)
                                )
                            )
                        }
                    }
                }
            }
        }

        // Accounts list - only show if expanded
        if (isExpanded) {
            Spacer(modifier = Modifier.height(UiConstants.SPACING_SMALL))
            accounts.forEach { account ->
                AccountItemRow(
                    account = account
                )
                Spacer(modifier = Modifier.height(UiConstants.SPACING_TINY))
            }
        }

        Spacer(modifier = Modifier.height(UiConstants.PADDING_MEDIUM))
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
                    .size(UiConstants.INDICATOR_SIZE)
                    .background(
                        color = accountColor,
                        shape = RoundedCornerShape(UiConstants.CORNER_RADIUS_TINY)
                    )
            )
            Spacer(modifier = Modifier.width(UiConstants.SPACING_SMALL))
            
            Text(
                text = account.account.name.value,
                style = UI.typo.b2.style(
                    color = accountColor.copy(alpha = UiConstants.ALPHA_ACCOUNT_TEXT)
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
