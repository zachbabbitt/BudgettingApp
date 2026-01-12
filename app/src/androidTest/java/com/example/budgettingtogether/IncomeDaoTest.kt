package com.example.budgettingtogether

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Date

@RunWith(AndroidJUnit4::class)
class IncomeDaoTest {

    private lateinit var database: AppDatabase
    private lateinit var incomeDao: IncomeDao

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        incomeDao = database.incomeDao()
    }

    @After
    fun teardown() {
        database.close()
    }

    @Test
    fun insertIncome_addsToDatabase() = runTest {
        val income = Income(
            title = "Monthly Salary",
            amount = 5000.0,
            source = "Salary",
            recurringType = RecurringType.MONTHLY
        )

        incomeDao.insert(income)

        val incomeList = incomeDao.getAllIncome().first()
        assertEquals(1, incomeList.size)
        assertEquals("Monthly Salary", incomeList[0].title)
        assertEquals(5000.0, incomeList[0].amount, 0.01)
    }

    @Test
    fun insertMultipleIncome_allAddedToDatabase() = runTest {
        val income1 = Income(title = "Salary", amount = 5000.0, source = "Salary")
        val income2 = Income(title = "Freelance Project", amount = 1000.0, source = "Freelance")
        val income3 = Income(title = "Dividend", amount = 200.0, source = "Investments")

        incomeDao.insert(income1)
        incomeDao.insert(income2)
        incomeDao.insert(income3)

        val incomeList = incomeDao.getAllIncome().first()
        assertEquals(3, incomeList.size)
    }

    @Test
    fun deleteIncome_removesFromDatabase() = runTest {
        val income = Income(title = "Salary", amount = 5000.0, source = "Salary")

        incomeDao.insert(income)
        var incomeList = incomeDao.getAllIncome().first()
        assertEquals(1, incomeList.size)

        incomeDao.delete(income)
        incomeList = incomeDao.getAllIncome().first()
        assertEquals(0, incomeList.size)
    }

    @Test
    fun getRecurringIncome_returnsOnlyRecurring() = runTest {
        val oneTime = Income(title = "Bonus", amount = 1000.0, source = "Salary", recurringType = RecurringType.NONE)
        val weekly = Income(title = "Part-time", amount = 500.0, source = "Freelance", recurringType = RecurringType.WEEKLY)
        val monthly = Income(title = "Salary", amount = 5000.0, source = "Salary", recurringType = RecurringType.MONTHLY)

        incomeDao.insert(oneTime)
        incomeDao.insert(weekly)
        incomeDao.insert(monthly)

        val recurring = incomeDao.getRecurringIncome().first()
        assertEquals(2, recurring.size)
        assertTrue(recurring.none { it.recurringType == RecurringType.NONE })
    }

    @Test
    fun getIncomeBySource_filtersCorrectly() = runTest {
        incomeDao.insert(Income(title = "Main Job", amount = 5000.0, source = "Salary"))
        incomeDao.insert(Income(title = "Side Project", amount = 1000.0, source = "Freelance"))
        incomeDao.insert(Income(title = "Bonus", amount = 500.0, source = "Salary"))

        val salaryIncome = incomeDao.getIncomeBySource("Salary").first()
        assertEquals(2, salaryIncome.size)
        assertTrue(salaryIncome.all { it.source == "Salary" })
    }

    @Test
    fun getTotalAmount_returnsSumOfAllIncome() = runTest {
        incomeDao.insert(Income(title = "A", amount = 1000.0, source = "Salary"))
        incomeDao.insert(Income(title = "B", amount = 500.0, source = "Freelance"))
        incomeDao.insert(Income(title = "C", amount = 200.0, source = "Investments"))

        val total = incomeDao.getTotalAmount().first()
        assertEquals(1700.0, total ?: 0.0, 0.01)
    }

    @Test
    fun getTotalAmount_emptyDatabase_returnsNull() = runTest {
        val total = incomeDao.getTotalAmount().first()
        assertNull(total)
    }

    @Test
    fun incomeFields_persistCorrectly() = runTest {
        val date = Date()
        val income = Income(
            id = "test-income-123",
            title = "Test Income",
            amount = 1234.56,
            source = "Business",
            date = date,
            recurringType = RecurringType.WEEKLY,
            notes = "Test notes"
        )

        incomeDao.insert(income)
        val retrieved = incomeDao.getAllIncome().first()[0]

        assertEquals("test-income-123", retrieved.id)
        assertEquals("Test Income", retrieved.title)
        assertEquals(1234.56, retrieved.amount, 0.01)
        assertEquals("Business", retrieved.source)
        assertEquals(RecurringType.WEEKLY, retrieved.recurringType)
        assertEquals("Test notes", retrieved.notes)
    }

    @Test
    fun getIncomeByRecurringType_filtersCorrectly() = runTest {
        incomeDao.insert(Income(title = "A", amount = 100.0, source = "Salary", recurringType = RecurringType.MONTHLY))
        incomeDao.insert(Income(title = "B", amount = 200.0, source = "Salary", recurringType = RecurringType.MONTHLY))
        incomeDao.insert(Income(title = "C", amount = 300.0, source = "Freelance", recurringType = RecurringType.WEEKLY))

        val monthlyIncome = incomeDao.getIncomeByRecurringType(RecurringType.MONTHLY.name).first()
        assertEquals(2, monthlyIncome.size)
        assertTrue(monthlyIncome.all { it.recurringType == RecurringType.MONTHLY })
    }
}
