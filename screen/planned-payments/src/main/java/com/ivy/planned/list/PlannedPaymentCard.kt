package com.ivy.planned.list

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ivy.base.model.TransactionType
import com.ivy.data.model.Category
import com.ivy.data.model.CategoryId
import com.ivy.data.model.IntervalType
import com.ivy.data.model.primitive.ColorInt
import com.ivy.data.model.primitive.NotBlankTrimmedString
import com.ivy.design.api.LocalTimeConverter
import com.ivy.design.l0_system.UI
import com.ivy.design.l0_system.style
import com.ivy.design.l1_buildingBlocks.IvyText
import com.ivy.design.l1_buildingBlocks.SpacerHor
import com.ivy.legacy.IvyWalletPreview
import com.ivy.legacy.datamodel.Account
import com.ivy.legacy.datamodel.PlannedPaymentRule
import com.ivy.legacy.forDisplay
import com.ivy.legacy.ui.component.transaction.TypeAmountCurrency
import com.ivy.legacy.utils.formatDateOnly
import com.ivy.legacy.utils.formatDateOnlyWithYear
import com.ivy.legacy.utils.isNotNullOrBlank
import com.ivy.legacy.utils.timeNowUTC
import com.ivy.legacy.utils.uppercaseLocal
import com.ivy.navigation.Navigation
import com.ivy.navigation.TransactionsScreen
import com.ivy.navigation.navigation
import com.ivy.ui.R
import com.ivy.wallet.ui.theme.Blue
import com.ivy.wallet.ui.theme.Gradient
import com.ivy.wallet.ui.theme.Green
import com.ivy.wallet.ui.theme.Orange
import com.ivy.wallet.ui.theme.components.IvyButton
import com.ivy.wallet.ui.theme.components.IvyIcon
import com.ivy.wallet.ui.theme.components.ItemIconSDefaultIcon
import com.ivy.wallet.ui.theme.components.getCustomIconIdS
import com.ivy.wallet.ui.theme.findContrastTextColor
import java.util.UUID
import com.ivy.wallet.ui.theme.toComposeColor
import androidx.compose.ui.graphics.Color
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import java.time.LocalDateTime
import java.time.ZoneOffset

@SuppressLint("ComposeModifierMissing")
@Composable
fun LazyItemScope.PlannedPaymentCard(
    baseCurrency: String,
    categories: ImmutableList<Category>,
    accounts: ImmutableList<Account>,
    plannedPayment: PlannedPaymentRule,
    shouldShowAccountSpecificColorInTransactions: Boolean,
    onClick: (PlannedPaymentRule) -> Unit,
) {
    Spacer(Modifier.height(12.dp))

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(top = 12.dp)
            .clip(UI.shapes.r4)
            .clickable {
                if (accounts.find { it.id == plannedPayment.accountId } != null) {
                    onClick(plannedPayment)
                }
            }
            .background(UI.colors.medium, UI.shapes.r4)
            .testTag("planned_payment_card")
    ) {
        val currency = accounts.find { it.id == plannedPayment.accountId }?.currency ?: baseCurrency

        Spacer(Modifier.height(20.dp))

        PlannedPaymentHeaderRow(
            plannedPayment = plannedPayment,
            categories = categories,
            accounts = accounts,
            shouldShowAccountSpecificColorInTransactions = shouldShowAccountSpecificColorInTransactions
        )

        Spacer(Modifier.height(16.dp))

        RuleTextRow(
            oneTime = plannedPayment.oneTime,
            startDate = with(LocalTimeConverter.current) {
                plannedPayment.startDate?.toLocalDateTime()
            },
            intervalN = plannedPayment.intervalN,
            intervalType = plannedPayment.intervalType
        )

        if (plannedPayment.title.isNotNullOrBlank()) {
            Spacer(Modifier.height(8.dp))

            Text(
                modifier = Modifier.padding(horizontal = 24.dp),
                text = plannedPayment.title!!,
                style = UI.typo.b1.style(
                    fontWeight = FontWeight.ExtraBold,
                    color = UI.colors.pureInverse
                )
            )
        }

        Spacer(Modifier.height(20.dp))

        TypeAmountCurrency(
            transactionType = plannedPayment.type,
            dueDate = null,
            currency = currency,
            amount = plannedPayment.amount
        )

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun PlannedPaymentHeaderRow(
    plannedPayment: PlannedPaymentRule,
    categories: ImmutableList<Category>,
    accounts: ImmutableList<Account>,
    shouldShowAccountSpecificColorInTransactions: Boolean,
) {
    val nav = navigation()

    val category = plannedPayment.categoryId?.let { targetId -> categories.find { it.id.value == targetId } }

    if (plannedPayment.type == TransactionType.TRANSFER) {
        Column(
            modifier = Modifier.padding(horizontal = 20.dp),
        ) {
            if (category != null) {
                CategoryBadgeDisplay(category, nav)
                Spacer(modifier = Modifier.height(8.dp))
            }
            TransferHeader(
                accounts = accounts,
                plannedPayment = plannedPayment,
                shouldShowAccountSpecificColorInTransactions = shouldShowAccountSpecificColorInTransactions
            )
        }
    } else {
        @OptIn(ExperimentalLayoutApi::class)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (category != null) {
                CategoryBadgeDisplay(category, nav)
            }

            val account = accounts.find { it.id == plannedPayment.accountId }

            val accountBackgroundColor = if (shouldShowAccountSpecificColorInTransactions) {
                account?.color?.toComposeColor() ?: UI.colors.pure
            } else {
                UI.colors.pure
            }

            TransactionBadge(
                text = account?.name ?: stringResource(R.string.deleted),
                backgroundColor = accountBackgroundColor,
                icon = account?.icon,
                defaultIcon = R.drawable.ic_custom_account_s
            ) {
                account?.let {
                    nav.navigateTo(
                        TransactionsScreen(
                            accountId = account.id,
                            categoryId = null
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun RuleTextRow(
    oneTime: Boolean,
    startDate: LocalDateTime?,
    intervalN: Int?,
    intervalType: IntervalType?
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Spacer(Modifier.width(24.dp))

        if (oneTime) {
            Text(
                text = stringResource(R.string.planned_for_uppercase),
                style = UI.typo.nC.style(
                    color = Orange,
                    fontWeight = FontWeight.SemiBold
                )
            )
            Text(
                modifier = Modifier.padding(bottom = 1.dp),
                text = startDate?.toLocalDate()?.formatDateOnlyWithYear()?.uppercaseLocal()
                    ?: stringResource(R.string.null_text),
                style = UI.typo.nC.style(
                    color = Orange,
                    fontWeight = FontWeight.ExtraBold
                )
            )
        } else {
            val startDateFormatted = startDate?.toLocalDate()?.formatDateOnly()?.uppercaseLocal()
            Text(
                text = stringResource(R.string.starts_date, startDateFormatted ?: ""),
                style = UI.typo.nC.style(
                    color = Orange,
                    fontWeight = FontWeight.SemiBold
                )
            )
            val intervalTypeFormatted = intervalType?.forDisplay(intervalN ?: 0)?.uppercaseLocal()
            Text(
                modifier = Modifier.padding(bottom = 1.dp),
                text = stringResource(
                    R.string.repeats_every,
                    intervalN ?: 0,
                    intervalTypeFormatted ?: ""
                ),
                style = UI.typo.nC.style(
                    color = Orange,
                    fontWeight = FontWeight.ExtraBold
                )
            )
        }

        Spacer(Modifier.width(24.dp))
    }
}

@Preview
@Composable
private fun Preview_oneTime() {
    IvyWalletPreview {
        LazyColumn(Modifier.fillMaxSize()) {
            val cash = Account(name = "Cash", Green.toArgb())
            val food = Category(
                name = NotBlankTrimmedString.unsafe("Food"),
                color = ColorInt(Blue.toArgb()),
                icon = null,
                id = CategoryId(UUID.randomUUID()),
                orderNum = 0.0,
            )

            item {
                Spacer(Modifier.height(68.dp))

                PlannedPaymentCard(
                    baseCurrency = "BGN",
                    categories = persistentListOf(food),
                    accounts = persistentListOf(cash),
                    plannedPayment = PlannedPaymentRule(
                        accountId = cash.id,
                        title = "Lidl pazar",
                        categoryId = food.id.value,
                        amount = 250.75,
                        startDate = timeNowUTC().plusDays(5).toInstant(ZoneOffset.UTC),
                        oneTime = true,
                        intervalType = null,
                        intervalN = null,
                        type = TransactionType.EXPENSE
                    ),

                    shouldShowAccountSpecificColorInTransactions = false,
                ) {}
            }
        }
    }
}

@Preview
@Composable
private fun Preview_recurring() {
    IvyWalletPreview {
        LazyColumn(Modifier.fillMaxSize()) {
            val account = Account(name = "Revolut", Blue.toArgb())
            val shisha = Category(
                name = NotBlankTrimmedString.unsafe("Shisha"),
                color = ColorInt(Orange.toArgb()),
                icon = null,
                id = CategoryId(UUID.randomUUID()),
                orderNum = 0.0,
            )

            item {
                Spacer(Modifier.height(68.dp))

                PlannedPaymentCard(
                    baseCurrency = "BGN",
                    categories = persistentListOf(shisha),
                    accounts = persistentListOf(account),
                    shouldShowAccountSpecificColorInTransactions = true,
                    plannedPayment = PlannedPaymentRule(
                        accountId = account.id,
                        title = "Tabu",
                        categoryId = shisha.id.value,
                        amount = 250.75,
                        startDate = timeNowUTC().plusDays(5).toInstant(ZoneOffset.UTC),
                        oneTime = false,
                        intervalType = IntervalType.MONTH,
                        intervalN = 1,
                        type = TransactionType.EXPENSE
                    )
                ) {}
            }
        }
    }
}

@Preview
@Composable
private fun Preview_recurringError() {
    IvyWalletPreview {
        LazyColumn(Modifier.fillMaxSize()) {
            val account = Account(name = "Revolut", Blue.toArgb())
            val shisha = Category(
                name = NotBlankTrimmedString.unsafe("Shisha"),
                color = ColorInt(Orange.toArgb()),
                icon = null,
                id = CategoryId(UUID.randomUUID()),
                orderNum = 0.0,
            )

            item {
                Spacer(Modifier.height(68.dp))

                PlannedPaymentCard(
                    baseCurrency = "BGN",
                    categories = persistentListOf(shisha),
                    accounts = persistentListOf(account),
                    plannedPayment = PlannedPaymentRule(
                        accountId = account.id,
                        title = "Tabu",
                        categoryId = shisha.id.value,
                        amount = 250.75,
                        startDate = timeNowUTC().plusDays(5).toInstant(ZoneOffset.UTC),
                        oneTime = false,
                        intervalType = null,
                        intervalN = null,
                        type = TransactionType.EXPENSE
                    ),
                    shouldShowAccountSpecificColorInTransactions = true,
                ) {}
            }
        }
    }
}

@Composable
fun CategoryBadgeDisplay(
    category: Category,
    nav: Navigation,
) {
    TransactionBadge(
        text = category.name.value,
        backgroundColor = category.color.value.toComposeColor(),
        icon = category.icon?.id,
        defaultIcon = R.drawable.ic_custom_category_s
    ) {
        // Navigation logic
        nav.navigateTo(
            TransactionsScreen(
                accountId = null,
                categoryId = category.id.value
            )
        )
    }
}

private const val TransferHeaderGradientThreshold = 0.35f

@Composable
private fun TransferHeader(
    accounts: List<Account>,
    plannedPayment: PlannedPaymentRule,
    shouldShowAccountSpecificColorInTransactions: Boolean
) {
    val account = remember(accounts, plannedPayment) {
        accounts.find { plannedPayment.accountId == it.id }
    }
    val toAccount = remember(accounts, plannedPayment) {
        accounts.find { plannedPayment.toAccountId == it.id }
    }

    Row(
        modifier = Modifier
            .then(
                if (account != null && toAccount != null) {
                    Modifier
                        .background(
                            brush = Brush.horizontalGradient(
                                0f to account.color.toComposeColor(),
                                (TransferHeaderGradientThreshold) to account.color.toComposeColor(),
                                (1f - TransferHeaderGradientThreshold) to toAccount.color.toComposeColor(),
                                1f to toAccount.color.toComposeColor()
                            ),
                            shape = UI.shapes.rFull
                        )
                } else {
                    Modifier.background(UI.colors.pure, UI.shapes.rFull)
                }
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Spacer(Modifier.width(8.dp))

        val accountContrastColor =
            if (shouldShowAccountSpecificColorInTransactions && account != null) {
                findContrastTextColor(account.color.toComposeColor())
            } else {
                UI.colors.pureInverse
            }

        ItemIconSDefaultIcon(
            iconName = account?.icon,
            defaultIcon = R.drawable.ic_custom_account_s,
            tint = accountContrastColor
        )

        Spacer(Modifier.width(4.dp))

        Text(
            modifier = Modifier
                .padding(vertical = 8.dp),
            // used toString() in case of null
            text = account?.name.toString(),
            style = UI.typo.c.style(
                fontWeight = FontWeight.ExtraBold,
                color = accountContrastColor
            )
        )

        Spacer(Modifier.width(12.dp))

        IvyIcon(icon = R.drawable.ic_arrow_right, tint = accountContrastColor)

        Spacer(Modifier.width(12.dp))

        val toAccountContrastColor =
            if (shouldShowAccountSpecificColorInTransactions && toAccount != null) {
                findContrastTextColor(toAccount.color.toComposeColor())
            } else {
                UI.colors.pureInverse
            }

        ItemIconSDefaultIcon(
            iconName = toAccount?.icon,
            defaultIcon = R.drawable.ic_custom_account_s,
            tint = toAccountContrastColor
        )

        Spacer(Modifier.width(4.dp))

        Text(
            modifier = Modifier
                .padding(vertical = 8.dp),
            // used toString() in case of null
            text = toAccount?.name.toString(),
            style = UI.typo.c.style(
                fontWeight = FontWeight.ExtraBold,
                color = toAccountContrastColor
            )
        )

        Spacer(Modifier.width(20.dp))
    }
}

@Composable
fun AccountBadge(
    text: String,
    backgroundColor: Color,
    icon: String?,
    defaultIcon: Int,
    nav: Navigation,
    accountId: UUID?
) {
    IvyButton(
        iconTint = findContrastTextColor(backgroundColor),
        iconStart = getCustomIconIdS(icon, defaultIcon),
        text = text,
        backgroundGradient = Gradient.solid(backgroundColor),
        textStyle = UI.typo.nB2.style(
            color = findContrastTextColor(backgroundColor),
            fontWeight = FontWeight.SemiBold
        ),
        padding = 6.dp,
        iconEdgePadding = 8.dp
    ) {
        nav.navigateTo(
            TransactionsScreen(
                accountId = accountId,
                categoryId = null
            )
        )
    }
}

@Composable
private fun TransactionBadge(
    text: String,
    backgroundColor: Color,
    icon: String?,
    @DrawableRes
    defaultIcon: Int,

    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .clip(UI.shapes.rFull)
            .background(backgroundColor, UI.shapes.rFull)
            .clickable {
                onClick()
            }
            .padding(end = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SpacerHor(width = 8.dp)

        val contrastColor = findContrastTextColor(backgroundColor)

        ItemIconSDefaultIcon(
            iconName = icon,
            defaultIcon = defaultIcon,
            tint = contrastColor
        )

        SpacerHor(width = 4.dp)

        IvyText(
            text = text,
            typo = UI.typo.c.style(
                color = contrastColor,
                fontWeight = FontWeight.ExtraBold
            )
        )

        SpacerHor(width = 20.dp)
    }
}
