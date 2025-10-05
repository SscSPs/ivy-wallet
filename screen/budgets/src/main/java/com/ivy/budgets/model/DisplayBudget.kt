package com.ivy.budgets.model

import androidx.compose.runtime.Immutable
import com.ivy.data.db.entity.BudgetEntity
import com.ivy.wallet.domain.data.Reorderable

@Immutable
data class DisplayBudget(
    val budget: BudgetEntity,
    val spentAmount: Double
) : Reorderable {
    override fun getItemOrderNum(): Double {
        return budget.orderId
    }

    override fun withNewOrderNum(newOrderNum: Double): Reorderable {
        return this.copy(
            budget = budget.copy(
                orderId = newOrderNum
            )
        )
    }
}
