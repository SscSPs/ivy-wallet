package com.ivy.wallet.ui.theme.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ivy.data.model.AccountCategory
import com.ivy.design.l0_system.UI
import com.ivy.design.l0_system.style
import com.ivy.legacy.IvyWalletComponentPreview
import com.ivy.wallet.ui.theme.Ivy
import com.ivy.wallet.ui.theme.White

@Deprecated("Old design system. Use `:ivy-design` and Material3")
@Composable
fun AccountCategoryPicker(
    modifier: Modifier = Modifier,
    initialSelectedCategory: AccountCategory,
    lastItemSpacer: Dp = 0.dp,
    onCategorySelected: (AccountCategory) -> Unit
) {
    var selectedCategory by remember {
        mutableStateOf(initialSelectedCategory)
    }

    val categories = AccountCategory.values().toList()

    LazyColumn(
        modifier = modifier
            .testTag("category_picker")
    ) {
        itemsIndexed(categories) { index, category ->
            CategoryItemCard(
                category = category,
                selected = category == selectedCategory
            ) {
                selectedCategory = category
                onCategorySelected(category)
            }

            if (index == categories.lastIndex && lastItemSpacer > 0.dp) {
                Spacer(Modifier.height(lastItemSpacer))
            }
        }
    }
}

@Composable
private fun CategoryItemCard(
    category: AccountCategory,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Spacer(Modifier.height(12.dp))

    Row(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .clip(UI.shapes.r4)
            .background(
                color = if (selected) Ivy else UI.colors.medium,
                shape = UI.shapes.r4
            )
            .clickable {
                onClick()
            }
            .padding(vertical = 20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Spacer(Modifier.width(24.dp))

        val displayName = category.name.replace("_", " ")
        
        Text(
            text = displayName,
            style = UI.typo.b1.style(
                color = if (selected) White else UI.colors.pureInverse,
                fontWeight = FontWeight.ExtraBold
            )
        )

        Spacer(Modifier.weight(1f))

        Text(
            text = category.name.replace("_", " ").take(20),
            style = UI.typo.b2.style(
                color = if (selected) White else UI.colors.pureInverse,
                fontWeight = FontWeight.SemiBold
            )
        )

        Spacer(Modifier.width(32.dp))
    }
}

@Preview
@Composable
private fun PreviewAccountCategoryPicker() {
    IvyWalletComponentPreview {
        AccountCategoryPicker(
            initialSelectedCategory = AccountCategory.ASSET,
            onCategorySelected = {}
        )
    }
}
