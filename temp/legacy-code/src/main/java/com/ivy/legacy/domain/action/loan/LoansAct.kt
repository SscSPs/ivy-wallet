package com.ivy.wallet.domain.action.loan

import com.ivy.data.db.dao.read.LoanDao
import com.ivy.data.db.entity.LoanEntity
import com.ivy.frp.action.FPAction
import com.ivy.frp.action.thenMap
import com.ivy.frp.then
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import javax.inject.Inject

class LoansAct @Inject constructor(
    private val loanDao: LoanDao
) : FPAction<Unit, ImmutableList<LoanEntity>>() {
    override suspend fun Unit.compose(): suspend () -> ImmutableList<LoanEntity> = suspend {
        loanDao.findAll()
    } thenMap { it } then { it.toImmutableList() }
}
