package com.ivy.wallet.ui.theme.modal.edit

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ivy.design.l0_system.UI
import com.ivy.design.l0_system.style
import com.ivy.design.utils.thenIf
import com.ivy.legacy.IvyWalletPreview
import com.ivy.legacy.datamodel.Account
import com.ivy.legacy.utils.drawColoredShadow
import com.ivy.legacy.utils.hideKeyboard
import com.ivy.legacy.utils.onScreenStart
import com.ivy.ui.R
import com.ivy.wallet.ui.theme.Gradient
import com.ivy.wallet.ui.theme.Ivy
import com.ivy.wallet.ui.theme.Orange
import com.ivy.wallet.ui.theme.Red
import com.ivy.wallet.ui.theme.components.ItemIconSDefaultIcon
import com.ivy.wallet.ui.theme.components.IvyCircleButton
import com.ivy.wallet.ui.theme.components.WrapContentRow
import com.ivy.wallet.ui.theme.findContrastTextColor
import com.ivy.wallet.ui.theme.modal.IvyModal
import com.ivy.wallet.ui.theme.modal.ModalSkip
import com.ivy.wallet.ui.theme.modal.ModalTitle
import com.ivy.wallet.ui.theme.toComposeColor
import java.util.UUID

@Deprecated("Old design system. Use `:ivy-design` and Material3")
@Suppress("ParameterNaming")
@ExperimentalFoundationApi
@Composable
fun BoxWithConstraintsScope.ChooseAccountModal(
    id: UUID = UUID.randomUUID(),
    visible: Boolean,
    initialAccount: Account?,
    accounts: List<Account>,

    showAccountModal: (Account?) -> Unit,
    onAccountChanged: (Account?) -> Unit,
    dismiss: () -> Unit
) {
    var selectedAccount by remember(initialAccount) {
        mutableStateOf(initialAccount)
    }

    IvyModal(
        id = id,
        visible = visible,
        dismiss = dismiss,
        PrimaryAction = {
            ModalSkip {
                save(
                    account = selectedAccount,
                    onAccountChanged = onAccountChanged,
                    dismiss = dismiss
                )
            }
        }
    ) {
        val view = LocalView.current
        onScreenStart {
            hideKeyboard(view)
        }

        Spacer(Modifier.height(32.dp))

        ModalTitle(
            text = stringResource(R.string.choose_account)
        )

        Spacer(Modifier.height(24.dp))

        AccountPicker(
            accounts = accounts,
            selectedAccount = selectedAccount,
            showAccountModal = showAccountModal,
            onEditAccount = {
                showAccountModal(it)
            }
        ) {
            selectedAccount = it
            save(
                shouldDismissModal = it != null,
                account = it,
                onAccountChanged = onAccountChanged,
                dismiss = dismiss
            )
        }

        Spacer(Modifier.height(56.dp))
    }
}

private fun save(
    shouldDismissModal: Boolean = true,

    account: Account?,
    onAccountChanged: (Account?) -> Unit,
    dismiss: () -> Unit
) {
    onAccountChanged(account)
    if (shouldDismissModal) {
        dismiss()
    }
}

@ExperimentalFoundationApi
@Suppress("ParameterNaming")
@Composable
private fun AccountPicker(
    accounts: List<Account>,
    selectedAccount: Account?,
    showAccountModal: (Account?) -> Unit,
    onEditAccount: (Account) -> Unit,
    onSelected: (Account?) -> Unit,
) {
    val data = mutableListOf<Any>()
    data.addAll(accounts)
    data.add(AddNewAccount())

    WrapContentRow(
        modifier = Modifier
            .padding(horizontal = 16.dp),
        horizontalMarginBetweenItems = 12.dp,
        verticalMarginBetweenRows = 12.dp,
        items = data
    ) {
        when (it) {
            is Account -> {
                AccountButton(
                    account = it,
                    selected = it == selectedAccount,
                    onClick = {
                        onSelected(it)
                    },
                    onLongClick = {
                        onEditAccount(it)
                    },
                    onDeselect = {
                        onSelected(null)
                    }
                )
            }

            is AddNewAccount -> {
                AddNewButton {
                    showAccountModal(null)
                }
            }
        }
    }
}

@ExperimentalFoundationApi
@Composable
private fun AccountButton(
    account: Account,
    selected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onDeselect: () -> Unit,
) {
    val accountColor = Color(account.color)

    val rFull = UI.shapes.rFull

    Row(
        modifier = Modifier
            .thenIf(selected) {
                drawColoredShadow(accountColor)
            }
            .clip(UI.shapes.rFull)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .border(
                width = 2.dp,
                color = if (selected) UI.colors.pureInverse else UI.colors.medium,
                shape = UI.shapes.rFull
            )
            .thenIf(selected) {
                background(accountColor, rFull)
            }
            .testTag("choose_account_button"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Spacer(Modifier.width(if (selected) 12.dp else 8.dp))

        ItemIconSDefaultIcon(
            modifier = Modifier
                .background(accountColor, CircleShape),
            iconName = account.icon,
            defaultIcon = R.drawable.ic_custom_account_s,
            tint = findContrastTextColor(accountColor)
        )

        Text(
            modifier = Modifier
                .padding(vertical = 12.dp)
                .padding(
                    start = if (selected) 12.dp else 12.dp,
                    end = if (selected) 20.dp else 24.dp
                ),
            text = account.name,
            style = UI.typo.b2.style(
                color = if (selected) {
                    findContrastTextColor(accountColor)
                } else {
                    UI.colors.pureInverse
                },
                fontWeight = FontWeight.SemiBold
            )
        )

        if (selected) {
            val deselectBtnBackground = findContrastTextColor(accountColor)
            IvyCircleButton(
                modifier = Modifier
                    .size(32.dp),
                icon = R.drawable.ic_remove,
                backgroundGradient = Gradient.solid(deselectBtnBackground),
                tint = findContrastTextColor(deselectBtnBackground)
            ) {
                onDeselect()
            }

            Spacer(Modifier.width(8.dp))
        }
    }
}

@Deprecated("Old design system. Use `:ivy-design` and Material3")
private class AddNewAccount

@ExperimentalFoundationApi
@Preview
@Composable
private fun PreviewChooseAccountModal() {
    IvyWalletPreview {
        val accounts = mutableListOf(
            Account(
                name = "Test",
                color = Ivy.toArgb(),
                icon = null
            ),
            Account(
                name = "Second",
                color = Orange.toArgb(),
                icon = null
            ),
            Account(
                name = "Third",
                color = Red.toArgb(),
                icon = null
            ),
        )

        ChooseAccountModal(
            visible = true,
            initialAccount = accounts.first(),
            accounts = accounts,
            showAccountModal = { },
            onAccountChanged = { }
        ) {
        }
    }
}
