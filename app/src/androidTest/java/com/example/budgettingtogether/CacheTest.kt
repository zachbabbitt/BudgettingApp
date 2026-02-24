package com.example.budgettingtogether

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.budgettingtogether.categories.Category
import com.example.budgettingtogether.core.AppDatabase
import com.example.budgettingtogether.core.SupabaseClientProvider
import com.example.budgettingtogether.expenses.Expense
import com.example.budgettingtogether.income.Income
import com.example.budgettingtogether.limits.BudgetLimit
import com.example.budgettingtogether.storage.source.CachedBudgetLimitSource
import com.example.budgettingtogether.storage.source.CachedCategorySource
import com.example.budgettingtogether.storage.source.CachedExpenseSource
import com.example.budgettingtogether.storage.source.CachedIncomeSource
import com.example.budgettingtogether.storage.source.LocalBudgetLimitSource
import com.example.budgettingtogether.storage.source.LocalCategorySource
import com.example.budgettingtogether.storage.source.LocalExpenseSource
import com.example.budgettingtogether.storage.source.LocalIncomeSource
import com.example.budgettingtogether.storage.source.RemoteBudgetLimitSource
import com.example.budgettingtogether.storage.source.RemoteCategorySource
import com.example.budgettingtogether.storage.source.RemoteExpenseSource
import com.example.budgettingtogether.storage.source.RemoteIncomeSource
import com.example.budgettingtogether.storage.sync.SyncService
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented tests verifying that the write-through cache (CachedXxxSource) and SyncService
 * correctly maintain data parity between Room and Supabase.
 *
 * Test data uses CACHE_GUID / PARTNER_GUID so rows can be reliably cleaned up after each test.
 */
@RunWith(AndroidJUnit4::class)
class CacheTest {

    companion object {
        const val CACHE_GUID   = "cache-test-00000000-0000"
        const val PARTNER_GUID = "cache-partner-0000-0000"
        private const val SYNC_WAIT_MS = 2_000L
    }

    private lateinit var db: AppDatabase

    // Local (Room) sources backed by an in-memory database
    private lateinit var localExpense:     LocalExpenseSource
    private lateinit var localIncome:      LocalIncomeSource
    private lateinit var localBudgetLimit: LocalBudgetLimitSource
    private lateinit var localCategory:    LocalCategorySource

    // Remote (Supabase) sources — real network calls
    private val remoteExpense     = RemoteExpenseSource()
    private val remoteIncome      = RemoteIncomeSource()
    private val remoteBudgetLimit = RemoteBudgetLimitSource()
    private val remoteCategory    = RemoteCategorySource()

    // Write-through cache sources under test
    private lateinit var cachedExpense:     CachedExpenseSource
    private lateinit var cachedIncome:      CachedIncomeSource
    private lateinit var cachedBudgetLimit: CachedBudgetLimitSource
    private lateinit var cachedCategory:    CachedCategorySource

    private lateinit var syncService: SyncService

    @Before
    fun setup() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        localExpense     = LocalExpenseSource(db.expenseDao())
        localIncome      = LocalIncomeSource(db.incomeDao())
        localBudgetLimit = LocalBudgetLimitSource(db.budgetLimitDao())
        localCategory    = LocalCategorySource(db.categoryDao())

        cachedExpense     = CachedExpenseSource(localExpense, remoteExpense)
        cachedIncome      = CachedIncomeSource(localIncome, remoteIncome)
        cachedBudgetLimit = CachedBudgetLimitSource(localBudgetLimit, remoteBudgetLimit)
        cachedCategory    = CachedCategorySource(localCategory, remoteCategory)

        syncService = SyncService(db)
    }

    @After
    fun teardown() = runBlocking {
        db.close()
        val client = SupabaseClientProvider.client
        client.from("expenses")     .delete { filter { eq("user_guid", CACHE_GUID)   } }
        client.from("expenses")     .delete { filter { eq("user_guid", PARTNER_GUID) } }
        client.from("income")       .delete { filter { eq("user_guid", CACHE_GUID)   } }
        client.from("budget_limits").delete { filter { eq("user_guid", CACHE_GUID)   } }
        client.from("categories")   .delete { filter { eq("user_guid", CACHE_GUID)   } }
        client.from("categories")   .delete { filter { eq("user_guid", PARTNER_GUID) } }
    }

    // ── CachedExpenseSource ───────────────────────────────────────────────────

    @Test
    fun cachedExpense_insert_appearsInRoomImmediately() = runTest {
        val expense = Expense(title = "Immediate", amount = 10.0, category = "Food", userGuid = CACHE_GUID)

        cachedExpense.insert(expense)

        // Room should reflect the insert synchronously — no delay needed
        val roomResult = localExpense.getAllExpenses(CACHE_GUID).first()
        assertEquals(1, roomResult.size)
        assertEquals("Immediate", roomResult[0].title)
    }

    @Test
    fun cachedExpense_insert_eventuallyAppearsInSupabase() = runTest {
        val expense = Expense(title = "Supabase Sync", amount = 25.0, category = "Food", userGuid = CACHE_GUID)

        cachedExpense.insert(expense)
        Thread.sleep(SYNC_WAIT_MS) // background GlobalScope launch needs real time

        val remoteResult = remoteExpense.getAllExpenses(CACHE_GUID).first()
        assertEquals(1, remoteResult.size)
        assertEquals("Supabase Sync", remoteResult[0].title)
        assertEquals(25.0, remoteResult[0].amount, 0.01)
    }

    @Test
    fun cachedExpense_delete_removesFromRoomImmediately() = runTest {
        val expense = Expense(title = "Delete Me Room", amount = 5.0, category = "Food", userGuid = CACHE_GUID)
        localExpense.insert(expense)
        remoteExpense.insert(expense)

        cachedExpense.delete(expense)

        assertEquals(0, localExpense.getAllExpenses(CACHE_GUID).first().size)
    }

    @Test
    fun cachedExpense_delete_eventuallyRemovesFromSupabase() = runTest {
        val expense = Expense(title = "Delete Me Remote", amount = 5.0, category = "Food", userGuid = CACHE_GUID)
        localExpense.insert(expense)
        remoteExpense.insert(expense)

        cachedExpense.delete(expense)
        Thread.sleep(SYNC_WAIT_MS)

        assertEquals(0, remoteExpense.getAllExpenses(CACHE_GUID).first().size)
    }

    @Test
    fun cachedExpense_read_servesFromRoomNotSupabase() = runTest {
        // Insert directly to Room only — if the cached source read from Supabase it would miss this
        val expense = Expense(title = "Room Only", amount = 10.0, category = "Food", userGuid = CACHE_GUID)
        localExpense.insert(expense)

        val result = cachedExpense.getAllExpenses(CACHE_GUID).first()

        assertEquals(1, result.size)
        assertEquals("Room Only", result[0].title)
    }

    // ── CachedIncomeSource ────────────────────────────────────────────────────

    @Test
    fun cachedIncome_insert_appearsInRoomImmediately() = runTest {
        val income = Income(title = "Salary", amount = 3000.0, source = "Work", userGuid = CACHE_GUID)

        cachedIncome.insert(income)

        assertEquals(1, localIncome.getAllIncome(CACHE_GUID).first().size)
    }

    @Test
    fun cachedIncome_insert_eventuallyAppearsInSupabase() = runTest {
        val income = Income(title = "Freelance Pay", amount = 500.0, source = "Client", userGuid = CACHE_GUID)

        cachedIncome.insert(income)
        Thread.sleep(SYNC_WAIT_MS)

        val remoteResult = remoteIncome.getAllIncome(CACHE_GUID).first()
        assertEquals(1, remoteResult.size)
        assertEquals("Freelance Pay", remoteResult[0].title)
    }

    @Test
    fun cachedIncome_delete_removesFromBothStores() = runTest {
        val income = Income(title = "Delete Income", amount = 100.0, source = "Bonus", userGuid = CACHE_GUID)
        localIncome.insert(income)
        remoteIncome.insert(income)

        cachedIncome.delete(income)
        Thread.sleep(SYNC_WAIT_MS)

        assertEquals(0, localIncome.getAllIncome(CACHE_GUID).first().size)
        assertEquals(0, remoteIncome.getAllIncome(CACHE_GUID).first().size)
    }

    // ── CachedBudgetLimitSource ───────────────────────────────────────────────

    @Test
    fun cachedBudgetLimit_insertOrUpdate_appearsInRoomImmediately() = runTest {
        val limit = BudgetLimit(category = "CacheFood", limitAmount = 400.0, currencyCode = "USD", userGuid = CACHE_GUID)

        cachedBudgetLimit.insertOrUpdate(limit)

        val roomResult = localBudgetLimit.getAllLimitsOnce(CACHE_GUID)
        assertEquals(1, roomResult.size)
        assertEquals(400.0, roomResult[0].limitAmount, 0.01)
    }

    @Test
    fun cachedBudgetLimit_insertOrUpdate_eventuallyAppearsInSupabase() = runTest {
        val limit = BudgetLimit(category = "CacheShopping", limitAmount = 200.0, currencyCode = "USD", userGuid = CACHE_GUID)

        cachedBudgetLimit.insertOrUpdate(limit)
        Thread.sleep(SYNC_WAIT_MS)

        val remoteResult = remoteBudgetLimit.getAllLimitsOnce(CACHE_GUID)
        assertEquals(1, remoteResult.size)
        assertEquals(200.0, remoteResult[0].limitAmount, 0.01)
    }

    @Test
    fun cachedBudgetLimit_delete_removesFromBothStores() = runTest {
        val limit = BudgetLimit(category = "CacheDelete", limitAmount = 100.0, currencyCode = "USD", userGuid = CACHE_GUID)
        localBudgetLimit.insertOrUpdate(limit)
        remoteBudgetLimit.insertOrUpdate(limit)

        cachedBudgetLimit.delete(CACHE_GUID, "CacheDelete")
        Thread.sleep(SYNC_WAIT_MS)

        assertEquals(0, localBudgetLimit.getAllLimitsOnce(CACHE_GUID).size)
        assertEquals(0, remoteBudgetLimit.getAllLimitsOnce(CACHE_GUID).size)
    }

    // ── CachedCategorySource ──────────────────────────────────────────────────

    @Test
    fun cachedCategory_insert_appearsInRoomImmediately() = runTest {
        val category = Category(name = "CacheTravel", isDefault = false, userGuid = CACHE_GUID)

        cachedCategory.insert(category)

        val roomResult = localCategory.getAllCategories(CACHE_GUID).first()
        assertTrue(roomResult.any { it.name == "CacheTravel" })
    }

    @Test
    fun cachedCategory_insert_eventuallyAppearsInSupabase() = runTest {
        val category = Category(name = "CachePets", isDefault = false, userGuid = CACHE_GUID)

        cachedCategory.insert(category)
        Thread.sleep(SYNC_WAIT_MS)

        val remoteResult = remoteCategory.getAllCategories(CACHE_GUID).first()
        assertTrue(remoteResult.any { it.name == "CachePets" })
    }

    @Test
    fun cachedCategory_delete_removesFromBothStores() = runTest {
        val category = Category(name = "CacheHobbies", isDefault = false, userGuid = CACHE_GUID)
        localCategory.insert(category)
        remoteCategory.insert(category)

        cachedCategory.delete(category)
        Thread.sleep(SYNC_WAIT_MS)

        assertFalse(localCategory.getAllCategories(CACHE_GUID).first().any { it.name == "CacheHobbies" })
        assertFalse(remoteCategory.getAllCategories(CACHE_GUID).first().any { it.name == "CacheHobbies" })
    }

    // ── SyncService ───────────────────────────────────────────────────────────

    @Test
    fun syncService_pushToRemote_uploadsRoomExpensesToSupabase() = runTest {
        localExpense.insert(Expense(title = "Push Test", amount = 50.0, category = "Food", userGuid = CACHE_GUID))

        syncService.pushToRemote(CACHE_GUID)

        val remoteResult = remoteExpense.getAllExpenses(CACHE_GUID).first()
        assertEquals(1, remoteResult.size)
        assertEquals("Push Test", remoteResult[0].title)
    }

    @Test
    fun syncService_pullFromRemote_downloadsSupabaseExpensesToRoom() = runTest {
        remoteExpense.insert(Expense(title = "Pull Test", amount = 75.0, category = "Food", userGuid = CACHE_GUID))

        syncService.pullFromRemote(CACHE_GUID, listOf(CACHE_GUID))

        val roomResult = localExpense.getAllExpenses(CACHE_GUID).first()
        assertEquals(1, roomResult.size)
        assertEquals("Pull Test", roomResult[0].title)
    }

    @Test
    fun syncService_pullFromRemote_removesRoomRowsAbsentFromSupabase() = runTest {
        // Exists in Room but not Supabase — simulates a delete made on another device
        localExpense.insert(Expense(title = "Stale Local", amount = 5.0, category = "Food", userGuid = CACHE_GUID))
        assertEquals(1, localExpense.getAllExpenses(CACHE_GUID).first().size)

        // Supabase has no rows for CACHE_GUID — pull should reconcile
        syncService.pullFromRemote(CACHE_GUID, listOf(CACHE_GUID))

        assertEquals(0, localExpense.getAllExpenses(CACHE_GUID).first().size)
    }

    @Test
    fun syncService_pullFromRemote_includesPartnerDataWithoutDeletingOwn() = runTest {
        remoteExpense.insert(Expense(title = "My Expense",      amount = 10.0, category = "Food", userGuid = CACHE_GUID))
        remoteExpense.insert(Expense(title = "Partner Expense", amount = 20.0, category = "Food", userGuid = PARTNER_GUID))

        syncService.pullFromRemote(CACHE_GUID, listOf(CACHE_GUID, PARTNER_GUID))

        val allLocal = localExpense.getAllExpenses(listOf(CACHE_GUID, PARTNER_GUID)).first()
        assertEquals(2, allLocal.size)
        assertTrue(allLocal.any { it.title == "My Expense" })
        assertTrue(allLocal.any { it.title == "Partner Expense" })
    }

    @Test
    fun syncService_pushThenPull_roundTripPreservesExpense() = runTest {
        val expense = Expense(
            title    = "Round Trip",
            amount   = 99.0,
            category = "Shopping",
            userGuid = CACHE_GUID
        )
        localExpense.insert(expense)

        // Push device A → Supabase
        syncService.pushToRemote(CACHE_GUID)

        // Simulate device B: fresh in-memory DB, pull Supabase → Room
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val db2 = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val syncService2 = SyncService(db2)
        val localExpense2 = LocalExpenseSource(db2.expenseDao())

        syncService2.pullFromRemote(CACHE_GUID, listOf(CACHE_GUID))
        val pulled = localExpense2.getAllExpenses(CACHE_GUID).first()
        db2.close()

        assertEquals(1, pulled.size)
        assertEquals("Round Trip", pulled[0].title)
        assertEquals(99.0, pulled[0].amount, 0.01)
    }
}
