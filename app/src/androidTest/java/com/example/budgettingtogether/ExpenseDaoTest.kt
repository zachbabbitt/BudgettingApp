package com.example.budgettingtogether

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.budgettingtogether.core.AppDatabase
import com.example.budgettingtogether.expenses.Expense
import com.example.budgettingtogether.expenses.ExpenseDao
import com.example.budgettingtogether.util.RecurringType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Date

@RunWith(AndroidJUnit4::class)
class ExpenseDaoTest {

    private lateinit var database: AppDatabase
    private lateinit var expenseDao: ExpenseDao

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        expenseDao = database.expenseDao()
    }

    @After
    fun teardown() {
        database.close()
    }

    @Test
    fun insertExpense_addsToDatabase() = runTest {
        val expense = Expense(
            title = "Groceries",
            amount = 50.0,
            category = "Food & Dining",
            recurringType = RecurringType.NONE
        )

        expenseDao.insert(expense)

        val expenses = expenseDao.getAllExpenses().first()
        assertEquals(1, expenses.size)
        assertEquals("Groceries", expenses[0].title)
        assertEquals(50.0, expenses[0].amount, 0.01)
    }

    @Test
    fun insertMultipleExpenses_allAddedToDatabase() = runTest {
        val expense1 = Expense(title = "Groceries", amount = 50.0, category = "Food")
        val expense2 = Expense(title = "Gas", amount = 40.0, category = "Transportation")
        val expense3 = Expense(title = "Movie", amount = 15.0, category = "Entertainment")

        expenseDao.insert(expense1)
        expenseDao.insert(expense2)
        expenseDao.insert(expense3)

        val expenses = expenseDao.getAllExpenses().first()
        assertEquals(3, expenses.size)
    }

    @Test
    fun deleteExpense_removesFromDatabase() = runTest {
        val expense = Expense(title = "Groceries", amount = 50.0, category = "Food")

        expenseDao.insert(expense)
        var expenses = expenseDao.getAllExpenses().first()
        assertEquals(1, expenses.size)

        expenseDao.delete(expense)
        expenses = expenseDao.getAllExpenses().first()
        assertEquals(0, expenses.size)
    }

    @Test
    fun getRecurringExpenses_returnsOnlyRecurring() = runTest {
        val oneTime = Expense(
            title = "Groceries",
            amount = 50.0,
            category = "Food",
            recurringType = RecurringType.NONE
        )
        val weekly = Expense(
            title = "Gym",
            amount = 10.0,
            category = "Health",
            recurringType = RecurringType.WEEKLY
        )
        val monthly = Expense(
            title = "Netflix",
            amount = 15.0,
            category = "Entertainment",
            recurringType = RecurringType.MONTHLY
        )

        expenseDao.insert(oneTime)
        expenseDao.insert(weekly)
        expenseDao.insert(monthly)

        val recurring = expenseDao.getRecurringExpenses().first()
        assertEquals(2, recurring.size)
        assertTrue(recurring.none { it.recurringType == RecurringType.NONE })
    }

    @Test
    fun getTotalAmount_returnsSumOfAllExpenses() = runTest {
        expenseDao.insert(Expense(title = "A", amount = 10.0, category = "Food"))
        expenseDao.insert(Expense(title = "B", amount = 20.0, category = "Food"))
        expenseDao.insert(Expense(title = "C", amount = 30.0, category = "Food"))

        val total = expenseDao.getTotalAmount().first()
        assertEquals(60.0, total ?: 0.0, 0.01)
    }

    @Test
    fun getTotalAmount_emptyDatabase_returnsNull() = runTest {
        val total = expenseDao.getTotalAmount().first()
        assertNull(total)
    }

    @Test
    fun expenseFields_persistCorrectly() = runTest {
        val date = Date()
        val expense = Expense(
            id = "test-id-123",
            title = "Test Expense",
            amount = 99.99,
            category = "Shopping",
            date = date,
            recurringType = RecurringType.WEEKLY
        )

        expenseDao.insert(expense)
        val retrieved = expenseDao.getAllExpenses().first()[0]

        assertEquals("test-id-123", retrieved.id)
        assertEquals("Test Expense", retrieved.title)
        assertEquals(99.99, retrieved.amount, 0.01)
        assertEquals("Shopping", retrieved.category)
        assertEquals(RecurringType.WEEKLY, retrieved.recurringType)
    }

    @Test
    fun getExpensesByRecurringType_filtersCorrectly() = runTest {
        expenseDao.insert(
            Expense(
                title = "A",
                amount = 10.0,
                category = "Food",
                recurringType = RecurringType.WEEKLY
            )
        )
        expenseDao.insert(
            Expense(
                title = "B",
                amount = 20.0,
                category = "Food",
                recurringType = RecurringType.WEEKLY
            )
        )
        expenseDao.insert(
            Expense(
                title = "C",
                amount = 30.0,
                category = "Food",
                recurringType = RecurringType.MONTHLY
            )
        )

        val weeklyExpenses = expenseDao.getExpensesByRecurringType(RecurringType.WEEKLY.name).first()
        assertEquals(2, weeklyExpenses.size)
        assertTrue(weeklyExpenses.all { it.recurringType == RecurringType.WEEKLY })
    }
}
