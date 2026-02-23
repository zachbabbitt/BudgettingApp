package com.example.budgettingtogether

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.budgettingtogether.categories.Category
import com.example.budgettingtogether.core.SupabaseClientProvider
import com.example.budgettingtogether.expenses.Expense
import com.example.budgettingtogether.income.Income
import com.example.budgettingtogether.limits.BudgetLimit
import com.example.budgettingtogether.storage.source.RemoteBudgetLimitSource
import com.example.budgettingtogether.storage.source.RemoteCategorySource
import com.example.budgettingtogether.storage.source.RemoteExpenseSource
import com.example.budgettingtogether.storage.source.RemoteIncomeSource
import com.example.budgettingtogether.util.RecurringType
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Integration tests that hit the real Supabase project.
 * All test rows use DUMMY_GUID so they can be cleaned up reliably.
 * Run with: ./gradlew connectedAndroidTest
 */
@RunWith(AndroidJUnit4::class)
class RemoteSourceTest {

    companion object {
        const val DUMMY_GUID = "test-dummy-00000000-0000"
        const val OTHER_GUID  = "test-other-00000000-0000"
    }

    private val expenseSource     = RemoteExpenseSource()
    private val incomeSource      = RemoteIncomeSource()
    private val budgetLimitSource = RemoteBudgetLimitSource()
    private val categorySource    = RemoteCategorySource()

    // ── Teardown ──────────────────────────────────────────────────────────────

    @After
    fun teardown() = runBlocking {
        val client = SupabaseClientProvider.client
        client.from("expenses")        .delete { filter { eq("user_guid", DUMMY_GUID) } }
        client.from("expenses")        .delete { filter { eq("user_guid", OTHER_GUID)  } }
        client.from("income")          .delete { filter { eq("user_guid", DUMMY_GUID) } }
        client.from("budget_limits")   .delete { filter { eq("user_guid", DUMMY_GUID) } }
        client.from("categories")      .delete { filter { eq("user_guid", DUMMY_GUID) } }
        client.from("categories")      .delete { filter { eq("user_guid", OTHER_GUID)  } }
        client.from("user_preferences").delete { filter { eq("user_guid", DUMMY_GUID) } }
    }

    // ── Expenses ──────────────────────────────────────────────────────────────

    @Test
    fun expense_insert_thenRead_returnsRow() = runTest {
        val expense = Expense(
            title = "Test Groceries",
            amount = 42.50,
            category = "Food",
            userGuid = DUMMY_GUID
        )

        expenseSource.insert(expense)

        val result = expenseSource.getAllExpenses(DUMMY_GUID).first()
        assertEquals(1, result.size)
        assertEquals("Test Groceries", result[0].title)
        assertEquals(42.50, result[0].amount, 0.01)
        assertEquals("Food", result[0].category)
        assertEquals(DUMMY_GUID, result[0].userGuid)
    }

    @Test
    fun expense_insert_thenDelete_removesRow() = runTest {
        val expense = Expense(title = "Delete Me", amount = 5.0, category = "Test", userGuid = DUMMY_GUID)

        expenseSource.insert(expense)
        assertEquals(1, expenseSource.getAllExpenses(DUMMY_GUID).first().size)

        expenseSource.delete(expense)
        assertEquals(0, expenseSource.getAllExpenses(DUMMY_GUID).first().size)
    }

    @Test
    fun expense_insertMultiple_allReturned() = runTest {
        expenseSource.insert(Expense(title = "A", amount = 10.0, category = "Food", userGuid = DUMMY_GUID))
        expenseSource.insert(Expense(title = "B", amount = 20.0, category = "Food", userGuid = DUMMY_GUID))
        expenseSource.insert(Expense(title = "C", amount = 30.0, category = "Food", userGuid = DUMMY_GUID))

        val result = expenseSource.getAllExpenses(DUMMY_GUID).first()
        assertEquals(3, result.size)
        assertEquals(60.0, result.sumOf { it.amount }, 0.01)
    }

    @Test
    fun expense_recurringFilter_returnsOnlyRecurring() = runTest {
        expenseSource.insert(Expense(title = "One-time",  amount = 10.0, category = "Food",
            recurringType = RecurringType.NONE,    userGuid = DUMMY_GUID))
        expenseSource.insert(Expense(title = "Weekly gym", amount = 15.0, category = "Health",
            recurringType = RecurringType.WEEKLY,  userGuid = DUMMY_GUID))
        expenseSource.insert(Expense(title = "Monthly sub", amount = 9.99, category = "Entertainment",
            recurringType = RecurringType.MONTHLY, userGuid = DUMMY_GUID))

        val recurring = expenseSource.getRecurringExpenses(DUMMY_GUID).first()
        assertEquals(2, recurring.size)
        assertTrue(recurring.none { it.recurringType == RecurringType.NONE })
    }

    @Test
    fun expense_userIsolation_otherGuidNotReturned() = runTest {
        expenseSource.insert(Expense(title = "Mine",  amount = 10.0, category = "Food", userGuid = DUMMY_GUID))
        expenseSource.insert(Expense(title = "Theirs", amount = 20.0, category = "Food", userGuid = OTHER_GUID))

        val mine = expenseSource.getAllExpenses(DUMMY_GUID).first()
        assertEquals(1, mine.size)
        assertEquals("Mine", mine[0].title)
    }

    @Test
    fun expense_multiUserQuery_combinesResults() = runTest {
        expenseSource.insert(Expense(title = "Mine",  amount = 10.0, category = "Food", userGuid = DUMMY_GUID))
        expenseSource.insert(Expense(title = "Theirs", amount = 20.0, category = "Food", userGuid = OTHER_GUID))

        val combined = expenseSource.getAllExpenses(listOf(DUMMY_GUID, OTHER_GUID)).first()
        assertEquals(2, combined.size)
        assertEquals(30.0, combined.sumOf { it.amount }, 0.01)
    }

    @Test
    fun expense_fieldRoundTrip_preservesAllFields() = runTest {
        val original = Expense(
            id               = "rt-expense-test-id",
            title            = "Round Trip",
            amount           = 99.99,
            category         = "Shopping",
            recurringType    = RecurringType.MONTHLY,
            originalAmount   = 110.00,
            originalCurrency = "EUR",
            userGuid         = DUMMY_GUID
        )

        expenseSource.insert(original)
        val fetched = expenseSource.getAllExpenses(DUMMY_GUID).first()[0]

        assertEquals(original.id,               fetched.id)
        assertEquals(original.title,            fetched.title)
        assertEquals(original.amount,           fetched.amount, 0.01)
        assertEquals(original.category,         fetched.category)
        assertEquals(original.recurringType,    fetched.recurringType)
        assertEquals(original.originalAmount,   fetched.originalAmount)
        assertEquals(original.originalCurrency, fetched.originalCurrency)
        assertEquals(original.userGuid,         fetched.userGuid)
    }

    // ── Income ────────────────────────────────────────────────────────────────

    @Test
    fun income_insert_thenRead_returnsRow() = runTest {
        val income = Income(
            title    = "Test Salary",
            amount   = 3000.0,
            source   = "Salary",
            userGuid = DUMMY_GUID
        )

        incomeSource.insert(income)

        val result = incomeSource.getAllIncome(DUMMY_GUID).first()
        assertEquals(1, result.size)
        assertEquals("Test Salary", result[0].title)
        assertEquals(3000.0, result[0].amount, 0.01)
        assertEquals("Salary", result[0].source)
        assertEquals(DUMMY_GUID, result[0].userGuid)
    }

    @Test
    fun income_insert_thenDelete_removesRow() = runTest {
        val income = Income(title = "Delete Me", amount = 1.0, source = "Other", userGuid = DUMMY_GUID)

        incomeSource.insert(income)
        assertEquals(1, incomeSource.getAllIncome(DUMMY_GUID).first().size)

        incomeSource.delete(income)
        assertEquals(0, incomeSource.getAllIncome(DUMMY_GUID).first().size)
    }

    @Test
    fun income_sourceFilter_returnsMatchingOnly() = runTest {
        incomeSource.insert(Income(title = "Job",  amount = 2000.0, source = "Salary",    userGuid = DUMMY_GUID))
        incomeSource.insert(Income(title = "Gig",  amount =  500.0, source = "Freelance", userGuid = DUMMY_GUID))
        incomeSource.insert(Income(title = "Gig2", amount =  300.0, source = "Freelance", userGuid = DUMMY_GUID))

        val freelance = incomeSource.getIncomeBySource(DUMMY_GUID, "Freelance").first()
        assertEquals(2, freelance.size)
        assertTrue(freelance.all { it.source == "Freelance" })
    }

    @Test
    fun income_recurringFilter_returnsOnlyRecurring() = runTest {
        incomeSource.insert(Income(title = "One-time bonus", amount = 500.0, source = "Other",
            recurringType = RecurringType.NONE,    userGuid = DUMMY_GUID))
        incomeSource.insert(Income(title = "Monthly salary", amount = 2000.0, source = "Salary",
            recurringType = RecurringType.MONTHLY, userGuid = DUMMY_GUID))

        val recurring = incomeSource.getRecurringIncome(DUMMY_GUID).first()
        assertEquals(1, recurring.size)
        assertEquals(RecurringType.MONTHLY, recurring[0].recurringType)
    }

    // ── Budget Limits ─────────────────────────────────────────────────────────

    @Test
    fun budgetLimit_insertOrUpdate_thenRead_returnsRow() = runTest {
        val limit = BudgetLimit(
            category     = "TestFood",
            limitAmount  = 500.0,
            currencyCode = "USD",
            userGuid     = DUMMY_GUID
        )

        budgetLimitSource.insertOrUpdate(limit)

        val result = budgetLimitSource.getAllLimits(DUMMY_GUID).first()
        assertEquals(1, result.size)
        assertEquals("TestFood", result[0].category)
        assertEquals(500.0, result[0].limitAmount, 0.01)
    }

    @Test
    fun budgetLimit_update_overwritesExistingAmount() = runTest {
        budgetLimitSource.insertOrUpdate(BudgetLimit("TestFood", 300.0, "USD", userGuid = DUMMY_GUID))
        budgetLimitSource.insertOrUpdate(BudgetLimit("TestFood", 600.0, "USD", userGuid = DUMMY_GUID))

        val result = budgetLimitSource.getAllLimits(DUMMY_GUID).first()
        assertEquals(1, result.size)   // upsert should not duplicate
        assertEquals(600.0, result[0].limitAmount, 0.01)
    }

    @Test
    fun budgetLimit_delete_removesRow() = runTest {
        budgetLimitSource.insertOrUpdate(BudgetLimit("TestFood", 200.0, "USD", userGuid = DUMMY_GUID))
        assertEquals(1, budgetLimitSource.getAllLimits(DUMMY_GUID).first().size)

        budgetLimitSource.delete(DUMMY_GUID, "TestFood")
        assertEquals(0, budgetLimitSource.getAllLimits(DUMMY_GUID).first().size)
    }

    @Test
    fun budgetLimit_getLimitForCategory_returnsCorrectRow() = runTest {
        budgetLimitSource.insertOrUpdate(BudgetLimit("TestFood",      200.0, "USD", userGuid = DUMMY_GUID))
        budgetLimitSource.insertOrUpdate(BudgetLimit("TestTransport", 150.0, "USD", userGuid = DUMMY_GUID))

        val result = budgetLimitSource.getLimitForCategory(DUMMY_GUID, "TestFood")
        assertNotNull(result)
        assertEquals(200.0, result!!.limitAmount, 0.01)
    }

    // ── Categories ────────────────────────────────────────────────────────────

    @Test
    fun category_insert_thenRead_returnsRow() = runTest {
        val category = Category(name = "TestTravel", isDefault = false, userGuid = DUMMY_GUID)

        categorySource.insert(category)

        val result = categorySource.getAllCategories(DUMMY_GUID).first()
        val inserted = result.find { it.name == "TestTravel" }
        assertNotNull(inserted)
        assertFalse(inserted!!.isDefault)
    }

    @Test
    fun category_insert_thenDelete_removesRow() = runTest {
        val category = Category(name = "TestDelete", isDefault = false, userGuid = DUMMY_GUID)

        categorySource.insert(category)
        val before = categorySource.getAllCategories(DUMMY_GUID).first()
            .count { it.name == "TestDelete" }
        assertEquals(1, before)

        categorySource.delete(category)
        val after = categorySource.getAllCategories(DUMMY_GUID).first()
            .count { it.name == "TestDelete" }
        assertEquals(0, after)
    }

    @Test
    fun category_insertAll_allRowsPresent() = runTest {
        val categories = listOf(
            Category(name = "TestHobbies", isDefault = false, userGuid = DUMMY_GUID),
            Category(name = "TestPets",    isDefault = false, userGuid = DUMMY_GUID),
            Category(name = "TestSports",  isDefault = false, userGuid = DUMMY_GUID)
        )

        categorySource.insertAll(categories)

        val names = categorySource.getAllCategoryNames(DUMMY_GUID).first()
        assertTrue(names.contains("TestHobbies"))
        assertTrue(names.contains("TestPets"))
        assertTrue(names.contains("TestSports"))
    }

    @Test
    fun category_multiUserQuery_combinesResults() = runTest {
        categorySource.insert(Category(name = "TestShared1", isDefault = false, userGuid = DUMMY_GUID))
        categorySource.insert(Category(name = "TestShared2", isDefault = false, userGuid = OTHER_GUID))

        val names = categorySource.getAllCategoryNames(listOf(DUMMY_GUID, OTHER_GUID)).first()
        assertTrue(names.contains("TestShared1"))
        assertTrue(names.contains("TestShared2"))
    }
}
