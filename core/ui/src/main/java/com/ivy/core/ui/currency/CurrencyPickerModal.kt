package com.ivy.core.ui.currency

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ivy.core.ui.currency.data.CurrencyListItem
import com.ivy.core.ui.currency.data.CurrencyUi
import com.ivy.data.CurrencyCode
import com.ivy.design.l0_system.UI
import com.ivy.design.l0_system.color.rememberContrastColor
import com.ivy.design.l1_buildingBlocks.*
import com.ivy.design.l2_components.modal.IvyModal
import com.ivy.design.l2_components.modal.Modal
import com.ivy.design.l2_components.modal.components.Choose
import com.ivy.design.l2_components.modal.components.Search
import com.ivy.design.l2_components.modal.components.SearchButton
import com.ivy.design.l2_components.modal.components.Title
import com.ivy.design.l2_components.modal.rememberIvyModal
import com.ivy.design.util.IvyPreview
import com.ivy.design.util.hiltViewmodelPreviewSafe
import com.ivy.resources.R

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun BoxScope.CurrencyPickerModal(
    modal: IvyModal,
    level: Int = 2,
    initialCurrency: CurrencyCode?,
    onCurrencyPick: (CurrencyCode) -> Unit,
) {
    val viewModel: CurrencyPickerModalViewModel? = hiltViewmodelPreviewSafe()
    val state = viewModel?.uiState?.collectAsState()?.value ?: previewState()

    LaunchedEffect(initialCurrency) {
        if (initialCurrency != null) {
            viewModel?.onEvent(CurrencyModalEvent.Initial(initialCurrency = initialCurrency))
        }
    }

    var searchBarVisible by remember { mutableStateOf(false) }

    val keyboardController = LocalSoftwareKeyboardController.current
    val resetSearch = {
        keyboardController?.hide()
        viewModel?.onEvent(CurrencyModalEvent.Search(query = ""))
        searchBarVisible = false
    }

    Modal(
        modal = modal,
        level = level,
        actions = {
            SearchButton(searchBarVisible = searchBarVisible) {
                if (searchBarVisible) resetSearch() else searchBarVisible = true
            }
            SpacerHor(width = 8.dp)
            Choose {
                keyboardController?.hide()
                resetSearch()
                modal.hide()
                state.selectedCurrency?.code?.let(onCurrencyPick)
            }
        }
    ) {
        Search(
            searchBarVisible = searchBarVisible,
            initialSearchQuery = state.searchQuery,
            searchHint = "Search currencies (e.g. EUR, USD, BTC, Bitcoin)",
            resetSearch = resetSearch,
            onSearch = { viewModel?.onEvent(CurrencyModalEvent.Search(it)) },
        ) {
            item(key = "cp_header") {
                Title(text = stringResource(R.string.choose_currency))
                SpacerVer(height = 12.dp)
            }
            item(key = "cp_selected_currency_${state.selectedCurrency?.code}") {
                SelectedCurrency(selectedCurrency = state.selectedCurrency)
            }
            currencies(
                items = state.items,
                selectedCurrency = state.selectedCurrency,
                onCurrencySelect = {
                    resetSearch()
                    modal.hide()
                    state.selectedCurrency?.code?.let(onCurrencyPick)
                    viewModel?.onEvent(CurrencyModalEvent.SelectCurrency(it))
                }
            )
            item(key = "cp_last_item_spacer") {
                // last item spacer
                SpacerVer(height = 24.dp)
            }
        }
    }
}

// region Currencies list
private fun LazyListScope.currencies(
    items: List<CurrencyListItem>,
    selectedCurrency: CurrencyUi?,
    onCurrencySelect: (CurrencyUi) -> Unit
) {
    items(
        items = items,
        key = {
            when (it) {
                is CurrencyListItem.Currency -> "${it.currency.code}${it.currency.name}"
                is CurrencyListItem.SectionDivider -> "divider_${it.name}"
            }
        }
    ) { item ->
        when (item) {
            is CurrencyListItem.Currency -> {
                SpacerVer(height = 12.dp)
                CurrencyItem(
                    currency = item.currency,
                    selected = item.currency == selectedCurrency,
                    onClick = onCurrencySelect
                )
            }
            is CurrencyListItem.SectionDivider -> {
                SpacerVer(height = 24.dp)
                SectionDivider(divider = item)
            }
        }
    }
}

@Composable
private fun SectionDivider(
    divider: CurrencyListItem.SectionDivider
) {
    Caption(
        modifier = Modifier.padding(start = 32.dp),
        text = divider.name,
        fontWeight = FontWeight.SemiBold
    )
}

@Composable
private fun CurrencyItem(
    currency: CurrencyUi,
    selected: Boolean,
    onClick: (CurrencyUi) -> Unit,
) {
    val bgColor = if (selected) UI.colors.primary else UI.colors.medium
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(UI.shapes.rounded)
            .background(bgColor, UI.shapes.rounded)
            .clickable { onClick(currency) }
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val textColor = rememberContrastColor(bgColor)
        B1Second(text = currency.code, fontWeight = FontWeight.ExtraBold, color = textColor)
        B2(
            modifier = Modifier
                .weight(1f)
                .padding(start = 24.dp),
            text = currency.name,
            fontWeight = FontWeight.SemiBold,
            color = textColor,
            textAlign = TextAlign.End,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
// endregion


@Composable
private fun SelectedCurrency(
    selectedCurrency: CurrencyUi?
) {
    if (selectedCurrency != null) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .background(UI.colors.primary, UI.shapes.squared)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val contrast = rememberContrastColor(UI.colors.primary)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 16.dp)
            ) {
                B2(
                    text = selectedCurrency.code,
                    color = contrast,
                    fontWeight = FontWeight.Normal
                )
                B1Second(
                    modifier = Modifier.fillMaxWidth(),
                    text = selectedCurrency.name,
                    color = contrast,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Start,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            IconRes(icon = R.drawable.ic_round_check_24, tint = contrast)
            SpacerHor(width = 4.dp)
            B2(
                text = stringResource(R.string.selected),
                color = contrast
            )
        }
    }
}

// region Previews
@Preview
@Composable
private fun Preview() {
    IvyPreview {
        val modal = rememberIvyModal()
        modal.show()
        CurrencyPickerModal(
            modal = modal,
            initialCurrency = null,
            onCurrencyPick = {}
        )
    }
}

private fun previewState() = CurrencyModalState(
    items = listOf(
        CurrencyListItem.SectionDivider(name = "A"),
        CurrencyListItem.Currency(CurrencyUi("BGN", "Bulgarian Lev")),
        CurrencyListItem.Currency(CurrencyUi("USD", "US Dollar")),
        CurrencyListItem.Currency(CurrencyUi("EUR", "Euro")),
        CurrencyListItem.SectionDivider(name = "Crypto"),
        CurrencyListItem.Currency(CurrencyUi("BTC", "Bitcoin")),
        CurrencyListItem.Currency(CurrencyUi("ADA", "Cardano")),
    ),
    selectedCurrency = CurrencyUi("BGN", "Bulgarian Lev"),
    searchQuery = ""
)
// endregion