package com.ivy.wallet.domain.deprecated.logic

import com.ivy.data.db.dao.write.WriteLoanRecordDao
import com.ivy.data.db.entity.LoanRecordEntity
import com.ivy.legacy.utils.ioThread
import com.ivy.wallet.domain.deprecated.logic.model.CreateLoanRecordData
import java.util.UUID
import javax.inject.Inject

class LoanRecordCreator @Inject constructor(
    private val loanRecordWriter: WriteLoanRecordDao,
) {
    suspend fun create(
        loanId: UUID,
        data: CreateLoanRecordData,
        onRefreshUI: suspend (LoanRecordEntity) -> Unit
    ): UUID? {
        val note = data.note
        if (data.amount <= 0) return null

        try {
            var newItem: LoanRecordEntity? = null
            newItem = ioThread {
                val item = LoanRecordEntity(
                    loanId = loanId,
                    note = note?.trim(),
                    amount = data.amount,
                    dateTime = data.dateTime,
                    isSynced = false,
                    interest = data.interest,
                    accountId = data.account?.id,
                    convertedAmount = data.convertedAmount,
                    loanRecordType = data.loanRecordType
                )

                loanRecordWriter.save(item)
                item
            }

            onRefreshUI(newItem!!)
            return newItem?.id
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    suspend fun edit(
        updatedItem: LoanRecordEntity,
        onRefreshUI: suspend (LoanRecordEntity) -> Unit
    ) {
        if (updatedItem.amount <= 0.0) return

        try {
            ioThread {
                loanRecordWriter.save(
                    updatedItem.copy(
                        isSynced = false
                    )
                )
            }

            onRefreshUI(updatedItem)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun delete(
        item: LoanRecordEntity,
        onRefreshUI: suspend () -> Unit
    ) {
        try {
            ioThread {
                loanRecordWriter.deleteById(item.id)
            }

            onRefreshUI()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
