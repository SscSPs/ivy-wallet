package com.ivy.legacy.datamodel.temp

import com.ivy.data.db.entity.LoanEntity
import com.ivy.data.model.LoanType

fun LoanEntity.humanReadableType(): String {
    return if (type == LoanType.BORROW) "BORROWED" else "LENT"
}
