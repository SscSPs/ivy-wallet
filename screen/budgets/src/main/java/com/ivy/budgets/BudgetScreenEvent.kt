package com.ivy.budgets

import com.ivy.budgets.model.DisplayBudget
import com.ivy.data.db.entity.BudgetEntity
import com.ivy.wallet.domain.deprecated.logic.model.CreateBudgetData

sealed interface BudgetScreenEvent {
    data class OnReorder(val newOrder: List<DisplayBudget>) : BudgetScreenEvent
    data class OnCreateBudget(val budgetData: CreateBudgetData) : BudgetScreenEvent
    data class OnEditBudget(val budget: BudgetEntity) : BudgetScreenEvent
    data class OnDeleteBudget(val budget: BudgetEntity) : BudgetScreenEvent
    data class OnReorderModalVisible(val visible: Boolean) : BudgetScreenEvent
    data class OnBudgetModalData(val budgetModalData: BudgetModalData?) : BudgetScreenEvent
}
