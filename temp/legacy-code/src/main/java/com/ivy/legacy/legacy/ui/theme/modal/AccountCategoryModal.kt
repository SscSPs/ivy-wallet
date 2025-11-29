package com.ivy.wallet.ui.theme.modal

import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ivy.data.model.AccountCategory
import com.ivy.design.l0_system.UI
import com.ivy.design.l0_system.style
import com.ivy.legacy.IvyWalletPreview
import com.ivy.ui.R
import com.ivy.wallet.ui.theme.components.AccountCategoryPicker
import java.util.UUID

@Deprecated("Old design system. Use `:ivy-design` and Material3")
@Composable
fun BoxWithConstraintsScope.AccountCategoryModal(
    title: String,
    initialCategory: AccountCategory,
    visible: Boolean,
    dismiss: () -> Unit,
    id: UUID = UUID.randomUUID(),

    onSetCategory: (AccountCategory) -> Unit
) {
    var category by remember(id) {
        mutableStateOf(initialCategory)
    }

    IvyModal(
        id = id,
        visible = visible,
        dismiss = dismiss,
        PrimaryAction = {
            ModalSave(
                modifier = Modifier.testTag("set_category_save")
            ) {
                onSetCategory(category)
                dismiss()
            }
        },
        includeActionsRowPadding = false,
        scrollState = null
    ) {
        Spacer(Modifier.height(32.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            ModalTitle(text = title)

            Spacer(Modifier.weight(1f))

            Spacer(Modifier.width(32.dp))
        }

        Spacer(Modifier.height(24.dp))

        AccountCategoryPicker(
            modifier = Modifier,
            initialSelectedCategory = category,
            lastItemSpacer = 120.dp
        ) {
            category = it
        }
    }
}

@Preview
@Composable
private fun Preview() {
    IvyWalletPreview {
        AccountCategoryModal(
            title = "Choose Account Category",
            initialCategory = AccountCategory.ASSET,
            visible = true,
            dismiss = {}
        ) {
        }
    }
}
