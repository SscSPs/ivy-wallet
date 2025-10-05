package com.ivy.loans.loan.data

import com.ivy.data.db.entity.LoanEntity
import com.ivy.legacy.utils.getDefaultFIATCurrency
import com.ivy.wallet.domain.data.Reorderable

data class DisplayLoan(
    val loan: LoanEntity,
    val loanTotalAmount: Double,
    val amountPaid: Double,
    val currencyCode: String? = getDefaultFIATCurrency().currencyCode,
    val formattedDisplayText: String = "",
    val percentPaid: Double = 0.0
) : Reorderable {
    override fun getItemOrderNum(): Double {
        return loan.orderNum
    }

    override fun withNewOrderNum(newOrderNum: Double): Reorderable {
        return this.copy(
            loan = loan.copy(
                orderNum = newOrderNum
            )
        )
    }
}
