package com.ivy.reports

import com.ivy.base.model.TransactionType
import com.ivy.data.model.Category
import com.ivy.data.model.TagId
import com.ivy.legacy.data.model.TimePeriod
import com.ivy.legacy.datamodel.Account
import java.util.UUID

data class ReportFilter(
    val id: UUID = UUID.randomUUID(),
    val trnTypes: List<TransactionType>,
    val period: TimePeriod?,
    val accounts: List<Account>,
    val categories: List<Category>,
    val currency: String,
    val minAmount: Double?,
    val maxAmount: Double?,
    val includeKeywords: List<String>,
    val excludeKeywords: List<String>,
    val includedTags: List<TagId>,
    val excludedTags: List<TagId>,

    ) {
    companion object {
        fun emptyFilter(
            baseCurrency: String
        ) = ReportFilter(
            trnTypes = emptyList(),
            period = null,
            accounts = emptyList(),
            categories = emptyList(),
            currency = baseCurrency,
            includeKeywords = emptyList(),
            excludeKeywords = emptyList(),
            minAmount = null,
            maxAmount = null,
            includedTags = emptyList(),
            excludedTags = emptyList()
        )
    }

    fun validate(): Boolean {
        // Validate amount range if both min and max are set
        if (minAmount != null && maxAmount != null) {
            if (minAmount > maxAmount) return false
        }

        // At least one filter criterion must be specified
        val hasAtLeastOneFilter = trnTypes.isNotEmpty() ||
                period != null ||
                accounts.isNotEmpty() ||
                categories.isNotEmpty() ||
                minAmount != null ||
                maxAmount != null ||
                includeKeywords.isNotEmpty() ||
                excludeKeywords.isNotEmpty() ||
                includedTags.isNotEmpty() ||
                excludedTags.isNotEmpty()

        return hasAtLeastOneFilter
    }
}
