package com.ivy.wallet.domain.deprecated.logic.model

import com.ivy.data.db.entity.LoanRecordEntity

data class EditLoanRecordData(
    val newLoanRecord: LoanRecordEntity,
    val originalLoanRecord: LoanRecordEntity,
    val createLoanRecordTransaction: Boolean = false,
    val reCalculateLoanAmount: Boolean = false
)
