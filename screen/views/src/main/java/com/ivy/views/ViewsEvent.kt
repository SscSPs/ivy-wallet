package com.ivy.views

sealed interface ViewsEvent {
    data object LoadData : ViewsEvent
    data object ToggleExcluded : ViewsEvent
    data object ToggleArchived : ViewsEvent
    data object ToggleZeroBalance : ViewsEvent
    data class ToggleCategoryExpand(val category: String) : ViewsEvent
    data class OnFilterOverlayVisible(val filterOverlayVisible: Boolean) : ViewsEvent
}
