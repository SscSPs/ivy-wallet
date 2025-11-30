package com.ivy.views

sealed interface ViewsEvent {
    object LoadData : ViewsEvent
    object ToggleExcluded : ViewsEvent
    object ToggleArchived : ViewsEvent
    object ToggleZeroBalance : ViewsEvent
    data class ToggleCategoryExpand(val category: String) : ViewsEvent
}
