package com.ivy.data.model

import androidx.annotation.Keep
import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable

@Immutable
@Keep
@Serializable
enum class AccountCategory {
    ASSET,
    LIABILITY,
    EXPENSE,
    INCOME,
    EQUITY
}
