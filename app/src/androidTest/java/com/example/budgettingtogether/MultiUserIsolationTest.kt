package com.example.budgettingtogether

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.budgettingtogether.categories.Category
import com.example.budgettingtogether.categories.CategoryDao
import com.example.budgettingtogether.core.AppDatabase
import com.example.budgettingtogether.expenses.Expense
import com.example.budgettingtogether.expenses.ExpenseDao
import com.example.budgettingtogether.income.Income
import com.example.budgettingtogether.income.IncomeDao
import com.example.budgettingtogether.limits.BudgetLimit
import com.example.budgettingtogether.limits.BudgetLimitDao
import com.example.budgettingtogether.util.RecurringType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MultiUserIsolationTest {

    private lateinit var database: AppDatabase
    private lateinit var expenseDao: ExpenseDao
    private lateinit var incomeDao: IncomeDao
    private lateinit var categoryDao: CategoryDao
    private lateinit var budgetLimitDao: BudgetLimitDao

    private val userA = "user-guid-aaa-111"
    private val userB = "user-guid-bbb-222"

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        expenseDao = database.expenseDao()
        incomeDao = database.incomeDao()
        categoryDao = database.categoryDao()
        budgetLimitDao = database.budgetLimitDao()
    }

    @After
    fun teardown() {
        database.close()
    }

    // ── Expense isolation ────────────────────────────────────────────────

    @Test
    fun expenses_userA_doesNotSeeUserBExpenses() = runTest {
        expenseDao.insert(Expense(title = "Rent", amount = 1200.0, category = "Housing", userGuid = userA))
        expenseDao.insert(Expense(title = "Coffee", amount = 5.0, category = "Food", userGuid = userB))

        val userAExpenses = expenseDao.getAllExpenses(userA).first()
        assertEquals(1, userAExpenses.size)
        assertEquals("Rent", userAExpenses[0].title)

        val userBExpenses = expenseDao.getAllExpenses(userB).first()
        assertEquals(1, userBExpenses.size)
        assertEquals("Coffee", userBExpenses[0].title)
    }

    @Test
    fun expenses_totalAmount_isolatedPerUser() = runTest {
        expenseDao.insert(Expense(title = "A", amount = 100.0, category = "Food", userGuid = userA))
        expenseDao.insert(Expense(title = "B", amount = 200.0, category = "Food", userGuid = userA))
        expenseDao.insert(Expense(title = "C", amount = 999.0, category = "Food", userGuid = userB))

        val totalA = expenseDao.getTotalAmount(userA).first()
        assertEquals(300.0, totalA ?: 0.0, 0.01)

        val totalB = expenseDao.getTotalAmount(userB).first()
        assertEquals(999.0, totalB ?: 0.0, 0.01)
    }

    @Test
    fun expenses_recurringFilter_isolatedPerUser() = runTest {
        expenseDao.insert(
            Expense(title = "Gym", amount = 30.0, category = "Health", recurringType = RecurringType.MONTHLY, userGuid = userA)
        )
        expenseDao.insert(
            Expense(title = "Netflix", amount = 15.0, category = "Entertainment", recurringType = RecurringType.MONTHLY, userGuid = userB)
        )
        expenseDao.insert(
            Expense(title = "Lunch", amount = 12.0, category = "Food", recurringType = RecurringType.NONE, userGuid = userA)
        )

        val recurringA = expenseDao.getRecurringExpenses(userA).first()
        assertEquals(1, recurringA.size)
        assertEquals("Gym", recurringA[0].title)

        val recurringB = expenseDao.getRecurringExpenses(userB).first()
        assertEquals(1, recurringB.size)
        assertEquals("Netflix", recurringB[0].title)
    }

    @Test
    fun expenses_deleteForOneUser_doesNotAffectOther() = runTest {
        val expenseA = Expense(title = "Rent", amount = 1200.0, category = "Housing", userGuid = userA)
        val expenseB = Expense(title = "Rent", amount = 900.0, category = "Housing", userGuid = userB)

        expenseDao.insert(expenseA)
        expenseDao.insert(expenseB)

        expenseDao.delete(expenseA)

        val userAExpenses = expenseDao.getAllExpenses(userA).first()
        assertEquals(0, userAExpenses.size)

        val userBExpenses = expenseDao.getAllExpenses(userB).first()
        assertEquals(1, userBExpenses.size)
        assertEquals(900.0, userBExpenses[0].amount, 0.01)
    }

    @Test
    fun expenses_byRecurringType_isolatedPerUser() = runTest {
        expenseDao.insert(
            Expense(title = "A-Weekly", amount = 10.0, category = "Food", recurringType = RecurringType.WEEKLY, userGuid = userA)
        )
        expenseDao.insert(
            Expense(title = "B-Weekly", amount = 20.0, category = "Food", recurringType = RecurringType.WEEKLY, userGuid = userB)
        )

        val weeklyA = expenseDao.getExpensesByRecurringType(userA, RecurringType.WEEKLY.name).first()
        assertEquals(1, weeklyA.size)
        assertEquals("A-Weekly", weeklyA[0].title)

        val weeklyB = expenseDao.getExpensesByRecurringType(userB, RecurringType.WEEKLY.name).first()
        assertEquals(1, weeklyB.size)
        assertEquals("B-Weekly", weeklyB[0].title)
    }

    // ── Income isolation ─────────────────────────────────────────────────

    @Test
    fun income_userA_doesNotSeeUserBIncome() = runTest {
        incomeDao.insert(Income(title = "Salary", amount = 5000.0, source = "Salary", userGuid = userA))
        incomeDao.insert(Income(title = "Freelance", amount = 1000.0, source = "Freelance", userGuid = userB))

        val userAIncome = incomeDao.getAllIncome(userA).first()
        assertEquals(1, userAIncome.size)
        assertEquals("Salary", userAIncome[0].title)

        val userBIncome = incomeDao.getAllIncome(userB).first()
        assertEquals(1, userBIncome.size)
        assertEquals("Freelance", userBIncome[0].title)
    }

    @Test
    fun income_totalAmount_isolatedPerUser() = runTest {
        incomeDao.insert(Income(title = "A", amount = 3000.0, source = "Salary", userGuid = userA))
        incomeDao.insert(Income(title = "B", amount = 500.0, source = "Freelance", userGuid = userA))
        incomeDao.insert(Income(title = "C", amount = 8000.0, source = "Salary", userGuid = userB))

        val totalA = incomeDao.getTotalAmount(userA).first()
        assertEquals(3500.0, totalA ?: 0.0, 0.01)

        val totalB = incomeDao.getTotalAmount(userB).first()
        assertEquals(8000.0, totalB ?: 0.0, 0.01)
    }

    @Test
    fun income_recurringFilter_isolatedPerUser() = runTest {
        incomeDao.insert(
            Income(title = "Weekly Gig", amount = 200.0, source = "Freelance", recurringType = RecurringType.WEEKLY, userGuid = userA)
        )
        incomeDao.insert(
            Income(title = "Monthly Pay", amount = 4000.0, source = "Salary", recurringType = RecurringType.MONTHLY, userGuid = userB)
        )
        incomeDao.insert(
            Income(title = "Bonus", amount = 1000.0, source = "Salary", recurringType = RecurringType.NONE, userGuid = userA)
        )

        val recurringA = incomeDao.getRecurringIncome(userA).first()
        assertEquals(1, recurringA.size)
        assertEquals("Weekly Gig", recurringA[0].title)

        val recurringB = incomeDao.getRecurringIncome(userB).first()
        assertEquals(1, recurringB.size)
        assertEquals("Monthly Pay", recurringB[0].title)
    }

    @Test
    fun income_bySource_isolatedPerUser() = runTest {
        incomeDao.insert(Income(title = "Main Job", amount = 5000.0, source = "Salary", userGuid = userA))
        incomeDao.insert(Income(title = "Other Job", amount = 3000.0, source = "Salary", userGuid = userB))

        val salaryA = incomeDao.getIncomeBySource(userA, "Salary").first()
        assertEquals(1, salaryA.size)
        assertEquals("Main Job", salaryA[0].title)

        val salaryB = incomeDao.getIncomeBySource(userB, "Salary").first()
        assertEquals(1, salaryB.size)
        assertEquals("Other Job", salaryB[0].title)
    }

    @Test
    fun income_deleteForOneUser_doesNotAffectOther() = runTest {
        val incomeA = Income(title = "Salary", amount = 5000.0, source = "Salary", userGuid = userA)
        val incomeB = Income(title = "Salary", amount = 6000.0, source = "Salary", userGuid = userB)

        incomeDao.insert(incomeA)
        incomeDao.insert(incomeB)

        incomeDao.delete(incomeA)

        assertEquals(0, incomeDao.getAllIncome(userA).first().size)
        assertEquals(1, incomeDao.getAllIncome(userB).first().size)
    }

    // ── Category isolation ───────────────────────────────────────────────

    @Test
    fun categories_userA_doesNotSeeUserBCustomCategories() = runTest {
        categoryDao.insert(Category(name = "Pets", isDefault = false, userGuid = userA))
        categoryDao.insert(Category(name = "Crypto", isDefault = false, userGuid = userB))

        val categoriesA = categoryDao.getAllCategories(userA).first()
        assertTrue(categoriesA.any { it.name == "Pets" })
        assertFalse(categoriesA.any { it.name == "Crypto" })

        val categoriesB = categoryDao.getAllCategories(userB).first()
        assertTrue(categoriesB.any { it.name == "Crypto" })
        assertFalse(categoriesB.any { it.name == "Pets" })
    }

    @Test
    fun categories_defaultCategories_visibleToAllUsers() = runTest {
        categoryDao.insert(Category(name = "Food & Dining", isDefault = true, userGuid = ""))
        categoryDao.insert(Category(name = "Pets", isDefault = false, userGuid = userA))

        val categoriesA = categoryDao.getAllCategories(userA).first()
        assertTrue(categoriesA.any { it.name == "Food & Dining" })
        assertTrue(categoriesA.any { it.name == "Pets" })

        val categoriesB = categoryDao.getAllCategories(userB).first()
        assertTrue(categoriesB.any { it.name == "Food & Dining" })
        assertFalse(categoriesB.any { it.name == "Pets" })
    }

    @Test
    fun categoryNames_isolatedPerUser() = runTest {
        categoryDao.insert(Category(name = "Pets", isDefault = false, userGuid = userA))
        categoryDao.insert(Category(name = "Crypto", isDefault = false, userGuid = userB))

        val namesA = categoryDao.getAllCategoryNames(userA).first()
        assertTrue(namesA.contains("Pets"))
        assertFalse(namesA.contains("Crypto"))

        val namesB = categoryDao.getAllCategoryNames(userB).first()
        assertTrue(namesB.contains("Crypto"))
        assertFalse(namesB.contains("Pets"))
    }

    // ── Budget limit isolation ───────────────────────────────────────────

    @Test
    fun budgetLimits_userA_doesNotSeeUserBLimits() = runTest {
        budgetLimitDao.insertOrUpdate(BudgetLimit(category = "Food", limitAmount = 500.0, userGuid = userA))
        budgetLimitDao.insertOrUpdate(BudgetLimit(category = "Food", limitAmount = 300.0, userGuid = userB))

        val limitsA = budgetLimitDao.getAllLimits(userA).first()
        assertEquals(1, limitsA.size)
        assertEquals(500.0, limitsA[0].limitAmount, 0.01)

        val limitsB = budgetLimitDao.getAllLimits(userB).first()
        assertEquals(1, limitsB.size)
        assertEquals(300.0, limitsB[0].limitAmount, 0.01)
    }

    @Test
    fun budgetLimits_getLimitForCategory_isolatedPerUser() = runTest {
        budgetLimitDao.insertOrUpdate(BudgetLimit(category = "Food", limitAmount = 500.0, userGuid = userA))
        budgetLimitDao.insertOrUpdate(BudgetLimit(category = "Food", limitAmount = 300.0, userGuid = userB))

        val limitA = budgetLimitDao.getLimitForCategory(userA, "Food")
        assertNotNull(limitA)
        assertEquals(500.0, limitA!!.limitAmount, 0.01)

        val limitB = budgetLimitDao.getLimitForCategory(userB, "Food")
        assertNotNull(limitB)
        assertEquals(300.0, limitB!!.limitAmount, 0.01)
    }

    @Test
    fun budgetLimits_deleteForOneUser_doesNotAffectOther() = runTest {
        budgetLimitDao.insertOrUpdate(BudgetLimit(category = "Food", limitAmount = 500.0, userGuid = userA))
        budgetLimitDao.insertOrUpdate(BudgetLimit(category = "Food", limitAmount = 300.0, userGuid = userB))

        budgetLimitDao.delete(userA, "Food")

        val limitsA = budgetLimitDao.getAllLimits(userA).first()
        assertEquals(0, limitsA.size)

        val limitsB = budgetLimitDao.getAllLimits(userB).first()
        assertEquals(1, limitsB.size)
    }

    @Test
    fun budgetLimits_getAllLimitsOnce_isolatedPerUser() = runTest {
        budgetLimitDao.insertOrUpdate(BudgetLimit(category = "Food", limitAmount = 500.0, userGuid = userA))
        budgetLimitDao.insertOrUpdate(BudgetLimit(category = "Transport", limitAmount = 200.0, userGuid = userA))
        budgetLimitDao.insertOrUpdate(BudgetLimit(category = "Food", limitAmount = 300.0, userGuid = userB))

        val limitsA = budgetLimitDao.getAllLimitsOnce(userA)
        assertEquals(2, limitsA.size)

        val limitsB = budgetLimitDao.getAllLimitsOnce(userB)
        assertEquals(1, limitsB.size)
    }

    // ── Cross-entity isolation ───────────────────────────────────────────

    @Test
    fun fullScenario_twoUsersWithCompletelyDifferentData() = runTest {
        // User A: low-budget student
        expenseDao.insert(Expense(title = "Ramen", amount = 3.0, category = "Food", userGuid = userA))
        incomeDao.insert(Income(title = "Part-time", amount = 800.0, source = "Salary", userGuid = userA))
        categoryDao.insert(Category(name = "Textbooks", isDefault = false, userGuid = userA))
        budgetLimitDao.insertOrUpdate(BudgetLimit(category = "Food", limitAmount = 200.0, userGuid = userA))

        // User B: higher earner
        expenseDao.insert(Expense(title = "Steak Dinner", amount = 75.0, category = "Food", userGuid = userB))
        expenseDao.insert(Expense(title = "Concert", amount = 120.0, category = "Entertainment", userGuid = userB))
        incomeDao.insert(Income(title = "Salary", amount = 6000.0, source = "Salary", userGuid = userB))
        categoryDao.insert(Category(name = "Travel", isDefault = false, userGuid = userB))
        budgetLimitDao.insertOrUpdate(BudgetLimit(category = "Food", limitAmount = 1000.0, userGuid = userB))

        // Verify User A sees only their data
        assertEquals(1, expenseDao.getAllExpenses(userA).first().size)
        assertEquals(3.0, expenseDao.getTotalAmount(userA).first() ?: 0.0, 0.01)
        assertEquals(1, incomeDao.getAllIncome(userA).first().size)
        assertEquals(800.0, incomeDao.getTotalAmount(userA).first() ?: 0.0, 0.01)
        assertTrue(categoryDao.getAllCategoryNames(userA).first().contains("Textbooks"))
        assertFalse(categoryDao.getAllCategoryNames(userA).first().contains("Travel"))
        assertEquals(200.0, budgetLimitDao.getLimitForCategory(userA, "Food")!!.limitAmount, 0.01)

        // Verify User B sees only their data
        assertEquals(2, expenseDao.getAllExpenses(userB).first().size)
        assertEquals(195.0, expenseDao.getTotalAmount(userB).first() ?: 0.0, 0.01)
        assertEquals(1, incomeDao.getAllIncome(userB).first().size)
        assertEquals(6000.0, incomeDao.getTotalAmount(userB).first() ?: 0.0, 0.01)
        assertTrue(categoryDao.getAllCategoryNames(userB).first().contains("Travel"))
        assertFalse(categoryDao.getAllCategoryNames(userB).first().contains("Textbooks"))
        assertEquals(1000.0, budgetLimitDao.getLimitForCategory(userB, "Food")!!.limitAmount, 0.01)
    }
}
