package com.ivy.views

sealed interface ViewsEvent {
    object LoadData : ViewsEvent
    object ToggleExcluded : ViewsEvent
    object ToggleArchived : ViewsEvent
}
