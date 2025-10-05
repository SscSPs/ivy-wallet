package com.ivy.wallet.domain.action.budget

import com.ivy.data.db.dao.read.BudgetDao
import com.ivy.data.db.entity.BudgetEntity
import com.ivy.frp.action.FPAction
import com.ivy.frp.action.thenMap
import com.ivy.frp.then
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import javax.inject.Inject

class BudgetsAct @Inject constructor(
    private val budgetDao: BudgetDao
) : FPAction<Unit, ImmutableList<BudgetEntity>>() {
    override suspend fun Unit.compose(): suspend () -> ImmutableList<BudgetEntity> = suspend {
        budgetDao.findAll()
    } thenMap { it } then { it.toImmutableList() }
}
