package com.ivy.accounts

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.toRect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ivy.design.l0_system.UI
import com.ivy.design.l0_system.style
import com.ivy.legacy.data.model.AccountData
import com.ivy.legacy.utils.clickableNoIndication
import com.ivy.legacy.utils.rememberInteractionSource
import com.ivy.ui.R
import com.ivy.wallet.ui.theme.components.BalanceRow
import com.ivy.wallet.ui.theme.components.BalanceRowMini
import com.ivy.wallet.ui.theme.components.ItemIconSDefaultIcon
import com.ivy.wallet.ui.theme.dynamicContrast
import com.ivy.wallet.ui.theme.findContrastTextColor
import com.ivy.wallet.ui.theme.toComposeColor
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import androidx.compose.ui.tooling.preview.Preview
import com.ivy.base.legacy.Theme
import com.ivy.legacy.IvyWalletPreview
import com.ivy.data.model.Account
import com.ivy.data.model.AccountId
import com.ivy.data.model.primitive.AssetCode
import com.ivy.data.model.primitive.ColorInt
import com.ivy.data.model.primitive.NotBlankTrimmedString
import com.ivy.wallet.ui.theme.Green
import androidx.compose.ui.graphics.toArgb
import java.util.UUID
import java.time.Instant

@Composable
fun AccountCard(
    baseCurrency: String,
    accountData: AccountData,
    compactModeEnabled: Boolean,
    onBalanceClick: () -> Unit,
    onClick: () -> Unit
) {
    val account = accountData.account
    val isArchived = account.archived
    val midCol = UI.colors.medium

    Column(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .clip(UI.shapes.r4)
            .border(2.dp, midCol, UI.shapes.r4)
            .then(
                if (isArchived) {
                    Modifier
                        .drawBehind {
                            val strokeWidth = 10f
                            val dashWidth = 30f
                            val dashGap = 20f
                            val cornerRadius = 16f

                            drawRoundRect(
                                color = midCol,
                                topLeft = Offset(strokeWidth / 2, strokeWidth / 2),
                                size = Size(size.width - strokeWidth, size.height - strokeWidth),
                                cornerRadius = CornerRadius(cornerRadius),
                                style = Stroke(
                                    width = strokeWidth,
                                    pathEffect = PathEffect.dashPathEffect(
                                        floatArrayOf(dashWidth, dashGap), 0f
                                    )
                                )
                            )
                        }
                        .drawWithContent {
                            val paint = Paint().apply {
                                colorFilter = androidx.compose.ui.graphics.ColorFilter.colorMatrix(
                                    ColorMatrix().apply {
                                        setToSaturation(0.3f) // Desaturate to 30%
                                    }
                                )
                                alpha = 0.7f
                            }
                            drawIntoCanvas { canvas ->
                                canvas.saveLayer(size.toRect(), paint)
                                drawContent()
                                canvas.nativeCanvas.restore()
                            }
                        }
                } else Modifier
            )
            .clickable(
                onClick = onClick
            )
    ) {
        val account = accountData.account
        val contrastColor = findContrastTextColor(account.color.value.toComposeColor())
        val currency = account.asset.code

        AccountHeader(
            accountData = accountData,
            currency = currency,
            baseCurrency = baseCurrency,
            contrastColor = contrastColor,
            onBalanceClick = onBalanceClick
        )

        if (!compactModeEnabled) {
            Spacer(Modifier.height(12.dp))

            IncomeExpensesRow(
                currency = currency,
                incomeLabel = stringResource(R.string.month_income),
                income = accountData.monthlyIncome,
                expensesLabel = stringResource(R.string.month_expenses),
                expenses = accountData.monthlyExpenses
            )

            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun AccountHeader(
    accountData: AccountData,
    currency: String,
    baseCurrency: String,
    contrastColor: Color,
    onBalanceClick: () -> Unit
) {
    val account = accountData.account

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(account.color.value.toComposeColor(), UI.shapes.r4Top)
    ) {
        Spacer(Modifier.height(16.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Spacer(Modifier.width(20.dp))

            ItemIconSDefaultIcon(
                iconName = account.icon?.id,
                defaultIcon = R.drawable.ic_custom_account_s,
                tint = contrastColor
            )

            Spacer(Modifier.width(8.dp))

            Text(
                text = account.name.value,
                style = UI.typo.b1.style(
                    color = contrastColor,
                    fontWeight = FontWeight.ExtraBold
                )
            )

            if (!account.includeInBalance) {
                Spacer(Modifier.width(8.dp))

                Text(
                    text = stringResource(R.string.excluded),
                    style = UI.typo.c.style(
                        color = account.color.value.toComposeColor().dynamicContrast()
                    )
                )
            }

            if (account.archived) {
                Spacer(Modifier.width(8.dp))

                Text(
                    text = stringResource(R.string.archived),
                    style = UI.typo.c.style(
                        color = account.color.value.toComposeColor().dynamicContrast()
                    )
                )
            }
        }

        Spacer(Modifier.height(4.dp))

        BalanceRow(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .clickableNoIndication(rememberInteractionSource()) {
                    onBalanceClick()
                },
            textColor = contrastColor,
            currency = currency,
            balance = accountData.balance,

            balanceFontSize = 30.sp,
            currencyFontSize = 30.sp,

            currencyUpfront = false
        )

        if (currency != baseCurrency && accountData.balanceBaseCurrency != null) {
            BalanceRowMini(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .clickableNoIndication(rememberInteractionSource()) {
                        onBalanceClick()
                    }
                    .testTag("baseCurrencyEquivalent"),
                textColor = account.color.value.toComposeColor().dynamicContrast(),
                currency = baseCurrency,
                balance = accountData.balanceBaseCurrency!!,
                currencyUpfront = false
            )
        }

        account.reconciliationDate?.let { instant ->
            val date = instant.atZone(ZoneId.systemDefault()).toLocalDate()
            val now = LocalDate.now()

            val yearsAgo = ChronoUnit.YEARS.between(date, now)
            val monthsAgo = ChronoUnit.MONTHS.between(date, now)
            val daysAgo = ChronoUnit.DAYS.between(date, now)

            val formatter = DateTimeFormatter.ofPattern("MMM d") // e.g. "Nov 2"
            val formattedDate = date.format(formatter)

            val relativeText = when {
                yearsAgo > 0 -> "Reconciled $yearsAgo year${if (yearsAgo > 1) "s" else ""} ago ($formattedDate)"
                monthsAgo > 0 -> "Reconciled $monthsAgo month${if (monthsAgo > 1) "s" else ""} ago ($formattedDate)"
                daysAgo > 7 -> "Reconciled on $formattedDate"
                daysAgo > 0 -> "Reconciled $daysAgo day${if (daysAgo > 1) "s" else ""} ago"
                else -> "Reconciled earlier today"
            }

            Text(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(top = 4.dp),
                text = relativeText,
                style = UI.typo.c.style(color = account.color.value.toComposeColor().dynamicContrast())
            )
        }

        Spacer(Modifier.height(16.dp))
    }
}

@Preview
@Composable
private fun PreviewAccountCard(theme: Theme = Theme.LIGHT) {
    IvyWalletPreview(theme = theme) {
        val account = Account(
            id = AccountId(UUID.randomUUID()),
            name = NotBlankTrimmedString.unsafe("Test Account"),
            color = ColorInt(Green.toArgb()),
            asset = AssetCode.unsafe("USD"),
            icon = null,
            includeInBalance = true,
            orderNum = 0.0,
            archived = false,
            reconciliationDate = Instant.now()
        )

        val accountData = AccountData(
            account = account,
            balance = 1234.56,
            balanceBaseCurrency = null,
            monthlyExpenses = 500.0,
            monthlyIncome = 2000.0
        )

        AccountCard(
            baseCurrency = "USD",
            accountData = accountData,
            compactModeEnabled = false,
            onBalanceClick = {},
            onClick = {}
        )
    }
}

@Preview
@Composable
private fun PreviewAccountCard2(theme: Theme = Theme.LIGHT) {
    IvyWalletPreview(theme = theme) {
        val account = Account(
            id = AccountId(UUID.randomUUID()),
            name = NotBlankTrimmedString.unsafe("Test Account"),
            color = ColorInt(Green.toArgb()),
            asset = AssetCode.unsafe("USD"),
            icon = null,
            includeInBalance = true,
            orderNum = 0.0,
            archived = false
        )

        val accountData = AccountData(
            account = account,
            balance = 1234.56,
            balanceBaseCurrency = null,
            monthlyExpenses = 500.0,
            monthlyIncome = 2000.0
        )

        AccountCard(
            baseCurrency = "USD",
            accountData = accountData,
            compactModeEnabled = false,
            onBalanceClick = {},
            onClick = {}
        )
    }
}

@Preview
@Composable
private fun PreviewAccountCardArchived(theme: Theme = Theme.LIGHT) {
    IvyWalletPreview(theme = theme) {
        val account = Account(
            id = AccountId(UUID.randomUUID()),
            name = NotBlankTrimmedString.unsafe("Test Account"),
            color = ColorInt(Green.toArgb()),
            asset = AssetCode.unsafe("USD"),
            icon = null,
            includeInBalance = true,
            orderNum = 0.0,
            archived = true
        )

        val accountData = AccountData(
            account = account,
            balance = 1234.56,
            balanceBaseCurrency = null,
            monthlyExpenses = 500.0,
            monthlyIncome = 2000.0
        )

        AccountCard(
            baseCurrency = "USD",
            accountData = accountData,
            compactModeEnabled = false,
            onBalanceClick = {},
            onClick = {}
        )
    }
}
