package com.ivy.views

sealed interface ViewsEvent {
    data object LoadData : ViewsEvent
}
