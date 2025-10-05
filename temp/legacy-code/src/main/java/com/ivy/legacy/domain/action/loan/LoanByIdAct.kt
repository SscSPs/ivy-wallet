package com.ivy.wallet.domain.action.loan

import com.ivy.data.db.dao.read.LoanDao
import com.ivy.data.db.entity.LoanEntity
import com.ivy.frp.action.FPAction
import java.util.UUID
import javax.inject.Inject

class LoanByIdAct @Inject constructor(
    private val loanDao: LoanDao
) : FPAction<UUID, LoanEntity?>() {
    override suspend fun UUID.compose(): suspend () -> LoanEntity? = suspend {
        loanDao.findById(this)
    }
}
