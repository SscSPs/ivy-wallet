package com.ivy.loans.loan.data

import com.ivy.data.db.entity.LoanRecordEntity
import com.ivy.legacy.datamodel.Account

data class DisplayLoanRecord(
    val loanRecord: LoanRecordEntity,
    val account: Account? = null,
    val loanRecordCurrencyCode: String = "",
    val loanCurrencyCode: String = "",
    val loanRecordTransaction: Boolean = false,
)
