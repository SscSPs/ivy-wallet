package com.ivy.loans.loandetails.events

import com.ivy.data.db.entity.LoanEntity

sealed interface LoanModalEvent : LoanDetailsScreenEvent {
    data object OnDismissLoanModal : LoanModalEvent
    data class OnEditLoanModal(val loan: LoanEntity, val createLoanTransaction: Boolean) :
        LoanModalEvent

    data object PerformCalculation : LoanModalEvent
    data object OnChangeDate : LoanModalEvent
    data object OnChangeTime : LoanModalEvent
}
