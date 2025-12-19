package com.ivy.wallet.domain.deprecated.logic

import com.ivy.base.legacy.Transaction
import com.ivy.base.model.TransactionType
import com.ivy.data.db.dao.read.AccountDao
import com.ivy.data.db.dao.read.SettingsDao
import com.ivy.data.db.entity.SettingsEntity
import com.ivy.data.db.entity.TransactionEntity
import com.ivy.data.db.entity.AccountEntity
import com.ivy.data.model.IntervalType
import com.ivy.data.repository.TransactionRepository
import com.ivy.data.repository.mapper.TransactionMapper
import com.ivy.legacy.datamodel.Account
import com.ivy.legacy.datamodel.PlannedPaymentRule
import com.ivy.wallet.domain.deprecated.logic.currency.ExchangeRatesLogic
import arrow.core.Either
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.time.Instant
import java.util.UUID

class PlannedPaymentsGeneratorTest {

    private val transactionMapper = mockk<TransactionMapper>()
    private val transactionRepository = mockk<TransactionRepository>(relaxed = true)
    private val accountDao = mockk<AccountDao>()
    private val settingsDao = mockk<SettingsDao>()
    private val exchangeRatesLogic = mockk<ExchangeRatesLogic>()

    private lateinit var generator: PlannedPaymentsGenerator

    @Before
    fun setup() {
        generator = PlannedPaymentsGenerator(
            transactionMapper,
            transactionRepository,
            accountDao,
            settingsDao,
            exchangeRatesLogic
        )
    }

    @Test
    fun `generateTransaction converts currency for transfers when toAmount is missing`() = runBlocking {
        // Given
        val fromAccountId = UUID.randomUUID()
        val toAccountId = UUID.randomUUID()
        val ruleId = UUID.randomUUID()
        val startDate = Instant.now()
        val amount = 10.0
        val convertedAmount = 20.0 // 10 INR -> 20 USD (hypothetical)

        val rule = PlannedPaymentRule(
            startDate = startDate,
            intervalN = 1,
            intervalType = IntervalType.MONTH,
            oneTime = true,
            type = TransactionType.TRANSFER,
            accountId = fromAccountId,
            amount = amount,
            toAccountId = toAccountId,
            toAmount = null, // Missing toAmount
            id = ruleId
        )


        val fromAccount = mockk<AccountEntity>(relaxed = true) {
            coEvery { currency } returns "INR"
            coEvery { id } returns fromAccountId
        }
        val toAccount = mockk<AccountEntity>(relaxed = true) {
            coEvery { currency } returns "USD"
            coEvery { id } returns toAccountId
        }
        val settings = mockk<SettingsEntity> {
            coEvery { currency } returns "EUR"
        }

        coEvery { transactionRepository.findAllByRecurringRuleId(ruleId) } returns emptyList()
        coEvery { accountDao.findById(fromAccountId) } returns fromAccount
        coEvery { accountDao.findById(toAccountId) } returns toAccount
        coEvery { settingsDao.findFirst() } returns settings
        coEvery {
            exchangeRatesLogic.convertAmount(
                baseCurrency = "EUR",
                amount = amount,
                fromCurrency = "INR",
                toCurrency = "USD"
            )
        } returns convertedAmount

        val transactionSlot = slot<Transaction>()
        coEvery {
            with(transactionMapper) {
                any<TransactionEntity>().toDomain(any())
            }
        } returns Either.Right(mockk())

        generator.generate(rule)

        coVerify {
            exchangeRatesLogic.convertAmount(
                baseCurrency = "EUR",
                amount = amount,
                fromCurrency = "INR",
                toCurrency = "USD"
            )
        }
    }
}
