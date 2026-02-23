package com.example.budgettingtogether

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.budgettingtogether.auth.PairingRepository
import com.example.budgettingtogether.auth.User
import com.example.budgettingtogether.auth.UserDao
import com.example.budgettingtogether.auth.UserPairing
import com.example.budgettingtogether.auth.UserPairingDao
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
class MultiUserPairingTest {

    private lateinit var database: AppDatabase
    private lateinit var expenseDao: ExpenseDao
    private lateinit var incomeDao: IncomeDao
    private lateinit var categoryDao: CategoryDao
    private lateinit var budgetLimitDao: BudgetLimitDao
    private lateinit var userDao: UserDao
    private lateinit var userPairingDao: UserPairingDao
    private lateinit var pairingRepository: PairingRepository

    private lateinit var userA: User
    private lateinit var userB: User
    private lateinit var userC: User

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
        userDao = database.userDao()
        userPairingDao = database.userPairingDao()
        pairingRepository = PairingRepository(userDao, userPairingDao)

        userA = User(
            id = "user-id-aaa",
            firstName = "Alice",
            lastName = "Smith",
            username = "alice",
            email = "alice@test.com",
            passwordHash = "hash",
            userGuid = "guid-aaa"
        )
        userB = User(
            id = "user-id-bbb",
            firstName = "Bob",
            lastName = "Jones",
            username = "bob",
            email = "bob@test.com",
            passwordHash = "hash",
            userGuid = "guid-bbb"
        )
        userC = User(
            id = "user-id-ccc",
            firstName = "Carol",
            lastName = "Lee",
            username = "carol",
            email = "carol@test.com",
            passwordHash = "hash",
            userGuid = "guid-ccc"
        )
    }

    @After
    fun teardown() {
        database.close()
    }

    // ── UserPairingDao direct tests ───────────────────────────────────────

    @Test
    fun insertPairing_createsPendingRecord() = runTest {
        userPairingDao.insert(
            UserPairing(requesterId = "user-id-aaa", receiverId = "user-id-bbb")
        )

        val pending = userPairingDao.getPendingRequests("user-id-bbb")
        assertEquals(1, pending.size)
        assertEquals(UserPairing.STATUS_PENDING, pending[0].status)
        assertEquals("user-id-aaa", pending[0].requesterId)
    }

    @Test
    fun acceptPairing_updatesStatus() = runTest {
        userPairingDao.insert(
            UserPairing(requesterId = "user-id-aaa", receiverId = "user-id-bbb")
        )
        val pairing = userPairingDao.getPendingRequests("user-id-bbb")[0]
        userPairingDao.update(
            pairing.copy(status = UserPairing.STATUS_ACCEPTED, acceptedAt = System.currentTimeMillis())
        )

        val accepted = userPairingDao.getAcceptedPairings("user-id-aaa")
        assertEquals(1, accepted.size)
        assertEquals(UserPairing.STATUS_ACCEPTED, accepted[0].status)
        assertNotNull(accepted[0].acceptedAt)
    }

    @Test
    fun getAcceptedPairings_returnsForBothSides() = runTest {
        userPairingDao.insert(
            UserPairing(
                requesterId = "user-id-aaa",
                receiverId = "user-id-bbb",
                status = UserPairing.STATUS_ACCEPTED,
                acceptedAt = System.currentTimeMillis()
            )
        )

        val pairingsA = userPairingDao.getAcceptedPairings("user-id-aaa")
        assertEquals(1, pairingsA.size)

        val pairingsB = userPairingDao.getAcceptedPairings("user-id-bbb")
        assertEquals(1, pairingsB.size)
    }

    @Test
    fun pendingSentRequests_onlyShowsRequesterSide() = runTest {
        userPairingDao.insert(
            UserPairing(requesterId = "user-id-aaa", receiverId = "user-id-bbb")
        )

        val sentA = userPairingDao.getPendingSentRequests("user-id-aaa")
        assertEquals(1, sentA.size)

        val sentB = userPairingDao.getPendingSentRequests("user-id-bbb")
        assertEquals(0, sentB.size)
    }

    @Test
    fun pendingReceivedRequests_onlyShowsReceiverSide() = runTest {
        userPairingDao.insert(
            UserPairing(requesterId = "user-id-aaa", receiverId = "user-id-bbb")
        )

        val receivedB = userPairingDao.getPendingRequests("user-id-bbb")
        assertEquals(1, receivedB.size)

        val receivedA = userPairingDao.getPendingRequests("user-id-aaa")
        assertEquals(0, receivedA.size)
    }

    @Test
    fun findPairingBetween_findsBothDirections() = runTest {
        userPairingDao.insert(
            UserPairing(requesterId = "user-id-aaa", receiverId = "user-id-bbb", status = UserPairing.STATUS_ACCEPTED)
        )

        val forward = userPairingDao.findPairingBetween("user-id-aaa", "user-id-bbb")
        assertNotNull(forward)

        val reverse = userPairingDao.findPairingBetween("user-id-bbb", "user-id-aaa")
        assertNotNull(reverse)

        assertEquals(forward!!.id, reverse!!.id)
    }

    @Test
    fun deletePairing_removesRecord() = runTest {
        userPairingDao.insert(
            UserPairing(requesterId = "user-id-aaa", receiverId = "user-id-bbb", status = UserPairing.STATUS_ACCEPTED)
        )

        val pairing = userPairingDao.getAcceptedPairings("user-id-aaa")[0]
        userPairingDao.delete(pairing)

        assertEquals(0, userPairingDao.getAcceptedPairings("user-id-aaa").size)
        assertEquals(0, userPairingDao.getAcceptedPairings("user-id-bbb").size)
    }

    @Test
    fun deleteById_removesCorrectRecord() = runTest {
        userPairingDao.insert(
            UserPairing(requesterId = "user-id-aaa", receiverId = "user-id-bbb")
        )
        val pairing = userPairingDao.getPendingRequests("user-id-bbb")[0]
        userPairingDao.deleteById(pairing.id)

        assertEquals(0, userPairingDao.getPendingRequests("user-id-bbb").size)
    }

    // ── PairingRepository tests ───────────────────────────────────────────

    @Test
    fun searchUsers_findsByUsername() = runTest {
        userDao.insert(userA)
        userDao.insert(userB)

        val results = pairingRepository.searchUsers("ali", userB.id)
        assertEquals(1, results.size)
        assertEquals("alice", results[0].username)
    }

    @Test
    fun searchUsers_findsByEmail() = runTest {
        userDao.insert(userA)
        userDao.insert(userB)

        val results = pairingRepository.searchUsers("bob@", userA.id)
        assertEquals(1, results.size)
        assertEquals("bob", results[0].username)
    }

    @Test
    fun searchUsers_excludesCurrentUser() = runTest {
        userDao.insert(userA)
        userDao.insert(userB)

        val results = pairingRepository.searchUsers("alice", userA.id)
        assertEquals(0, results.size)
    }

    @Test
    fun searchUsers_emptyQuery_returnsEmpty() = runTest {
        userDao.insert(userA)
        val results = pairingRepository.searchUsers("", userA.id)
        assertEquals(0, results.size)
    }

    @Test
    fun sendPairingRequest_createsRecord() = runTest {
        userDao.insert(userA)
        userDao.insert(userB)

        val result = pairingRepository.sendPairingRequest(userA.id, userB.id)
        assertTrue(result.isSuccess)

        val pending = pairingRepository.getPendingReceivedRequests(userB.id)
        assertEquals(1, pending.size)
    }

    @Test
    fun sendPairingRequest_duplicateFails() = runTest {
        userDao.insert(userA)
        userDao.insert(userB)

        pairingRepository.sendPairingRequest(userA.id, userB.id)
        val result = pairingRepository.sendPairingRequest(userA.id, userB.id)
        assertTrue(result.isFailure)
    }

    @Test
    fun sendPairingRequest_reverseDirectionAlsoFails() = runTest {
        userDao.insert(userA)
        userDao.insert(userB)

        pairingRepository.sendPairingRequest(userA.id, userB.id)
        val result = pairingRepository.sendPairingRequest(userB.id, userA.id)
        assertTrue(result.isFailure)
    }

    @Test
    fun acceptPairingRequest_marksAccepted() = runTest {
        userDao.insert(userA)
        userDao.insert(userB)

        pairingRepository.sendPairingRequest(userA.id, userB.id)
        val pendingRequest = pairingRepository.getPendingReceivedRequests(userB.id)[0]

        val result = pairingRepository.acceptPairingRequest(pendingRequest.id, userB.id)
        assertTrue(result.isSuccess)

        val partners = pairingRepository.getPartners(userA.id)
        assertEquals(1, partners.size)
        assertEquals("bob", partners[0].username)
    }

    @Test
    fun acceptPairingRequest_wrongReceiver_fails() = runTest {
        userDao.insert(userA)
        userDao.insert(userB)

        pairingRepository.sendPairingRequest(userA.id, userB.id)
        val pendingRequest = pairingRepository.getPendingReceivedRequests(userB.id)[0]

        val result = pairingRepository.acceptPairingRequest(pendingRequest.id, userA.id)
        assertTrue(result.isFailure)
    }

    @Test
    fun rejectPairingRequest_deletesRecord() = runTest {
        userDao.insert(userA)
        userDao.insert(userB)

        pairingRepository.sendPairingRequest(userA.id, userB.id)
        val pendingRequest = pairingRepository.getPendingReceivedRequests(userB.id)[0]
        pairingRepository.rejectPairingRequest(pendingRequest.id)

        assertEquals(0, pairingRepository.getPendingReceivedRequests(userB.id).size)
        assertEquals(0, pairingRepository.getPendingSentRequests(userA.id).size)
    }

    @Test
    fun unpair_removesSpecificPairing() = runTest {
        userDao.insert(userA)
        userDao.insert(userB)
        userDao.insert(userC)

        // A paired with B and C
        createAcceptedPairing(userA.id, userB.id)
        createAcceptedPairing(userA.id, userC.id)

        pairingRepository.unpair(userA.id, userB.id)

        val partnersA = pairingRepository.getPartners(userA.id)
        assertEquals(1, partnersA.size)
        assertEquals("carol", partnersA[0].username)

        // B should have no partners
        assertEquals(0, pairingRepository.getPartners(userB.id).size)
    }

    @Test
    fun getPartners_returnsAllAcceptedPartners() = runTest {
        userDao.insert(userA)
        userDao.insert(userB)
        userDao.insert(userC)

        createAcceptedPairing(userA.id, userB.id)
        createAcceptedPairing(userA.id, userC.id)

        val partners = pairingRepository.getPartners(userA.id)
        assertEquals(2, partners.size)
        val usernames = partners.map { it.username }.toSet()
        assertTrue(usernames.contains("bob"))
        assertTrue(usernames.contains("carol"))
    }

    @Test
    fun getPartners_doesNotIncludePendingRequests() = runTest {
        userDao.insert(userA)
        userDao.insert(userB)

        pairingRepository.sendPairingRequest(userA.id, userB.id)

        assertEquals(0, pairingRepository.getPartners(userA.id).size)
        assertEquals(0, pairingRepository.getPartners(userB.id).size)
    }

    // ── Guid resolution tests ─────────────────────────────────────────────

    @Test
    fun getPairedUserGuids_noPairings_returnsOnlyOwnGuid() = runTest {
        userDao.insert(userA)

        val guids = pairingRepository.getPairedUserGuids(userA.id, userA.userGuid)
        assertEquals(1, guids.size)
        assertEquals(userA.userGuid, guids[0])
    }

    @Test
    fun getPairedUserGuids_onePartner_returnsBothGuids() = runTest {
        userDao.insert(userA)
        userDao.insert(userB)
        createAcceptedPairing(userA.id, userB.id)

        val guidsA = pairingRepository.getPairedUserGuids(userA.id, userA.userGuid)
        assertEquals(2, guidsA.size)
        assertTrue(guidsA.contains(userA.userGuid))
        assertTrue(guidsA.contains(userB.userGuid))
    }

    @Test
    fun getPairedUserGuids_multiplePartners_returnsAllGuids() = runTest {
        userDao.insert(userA)
        userDao.insert(userB)
        userDao.insert(userC)
        createAcceptedPairing(userA.id, userB.id)
        createAcceptedPairing(userA.id, userC.id)

        val guidsA = pairingRepository.getPairedUserGuids(userA.id, userA.userGuid)
        assertEquals(3, guidsA.size)
        assertTrue(guidsA.contains(userA.userGuid))
        assertTrue(guidsA.contains(userB.userGuid))
        assertTrue(guidsA.contains(userC.userGuid))
    }

    @Test
    fun getPairedUserGuids_ownGuidIsAlwaysFirst() = runTest {
        userDao.insert(userA)
        userDao.insert(userB)
        createAcceptedPairing(userA.id, userB.id)

        val guids = pairingRepository.getPairedUserGuids(userA.id, userA.userGuid)
        assertEquals(userA.userGuid, guids[0])
    }

    @Test
    fun getPairedUserGuids_asymmetricPairings() = runTest {
        // A paired with B and C. B only paired with A (not C).
        userDao.insert(userA)
        userDao.insert(userB)
        userDao.insert(userC)
        createAcceptedPairing(userA.id, userB.id)
        createAcceptedPairing(userA.id, userC.id)

        // A sees A+B+C
        val guidsA = pairingRepository.getPairedUserGuids(userA.id, userA.userGuid)
        assertEquals(3, guidsA.size)

        // B sees A+B only (not C)
        val guidsB = pairingRepository.getPairedUserGuids(userB.id, userB.userGuid)
        assertEquals(2, guidsB.size)
        assertTrue(guidsB.contains(userB.userGuid))
        assertTrue(guidsB.contains(userA.userGuid))
        assertFalse(guidsB.contains(userC.userGuid))

        // C sees A+C only (not B)
        val guidsC = pairingRepository.getPairedUserGuids(userC.id, userC.userGuid)
        assertEquals(2, guidsC.size)
        assertTrue(guidsC.contains(userC.userGuid))
        assertTrue(guidsC.contains(userA.userGuid))
        assertFalse(guidsC.contains(userB.userGuid))
    }

    // ── Combined data queries with guid list ──────────────────────────────

    @Test
    fun expenses_guidList_returnsCombinedData() = runTest {
        expenseDao.insert(Expense(title = "Rent", amount = 1200.0, category = "Housing", userGuid = "guid-aaa"))
        expenseDao.insert(Expense(title = "Coffee", amount = 5.0, category = "Food", userGuid = "guid-bbb"))
        expenseDao.insert(Expense(title = "Dinner", amount = 40.0, category = "Food", userGuid = "guid-ccc"))

        // Paired A+B should see 2 expenses
        val expensesAB = expenseDao.getAllExpenses(listOf("guid-aaa", "guid-bbb")).first()
        assertEquals(2, expensesAB.size)

        // Paired A+B+C should see all 3
        val expensesABC = expenseDao.getAllExpenses(listOf("guid-aaa", "guid-bbb", "guid-ccc")).first()
        assertEquals(3, expensesABC.size)

        // Single user still sees only their own
        val expensesA = expenseDao.getAllExpenses(listOf("guid-aaa")).first()
        assertEquals(1, expensesA.size)
    }

    @Test
    fun expenses_guidList_totalAmount_combinesAll() = runTest {
        expenseDao.insert(Expense(title = "A", amount = 100.0, category = "Food", userGuid = "guid-aaa"))
        expenseDao.insert(Expense(title = "B", amount = 200.0, category = "Food", userGuid = "guid-bbb"))
        expenseDao.insert(Expense(title = "C", amount = 300.0, category = "Food", userGuid = "guid-ccc"))

        val totalAB = expenseDao.getTotalAmount(listOf("guid-aaa", "guid-bbb")).first()
        assertEquals(300.0, totalAB ?: 0.0, 0.01)

        val totalABC = expenseDao.getTotalAmount(listOf("guid-aaa", "guid-bbb", "guid-ccc")).first()
        assertEquals(600.0, totalABC ?: 0.0, 0.01)
    }

    @Test
    fun expenses_guidList_recurringExpenses_combinesAll() = runTest {
        expenseDao.insert(Expense(title = "Gym", amount = 30.0, category = "Health", recurringType = RecurringType.MONTHLY, userGuid = "guid-aaa"))
        expenseDao.insert(Expense(title = "Netflix", amount = 15.0, category = "Entertainment", recurringType = RecurringType.MONTHLY, userGuid = "guid-bbb"))
        expenseDao.insert(Expense(title = "Lunch", amount = 12.0, category = "Food", recurringType = RecurringType.NONE, userGuid = "guid-aaa"))

        val recurring = expenseDao.getRecurringExpenses(listOf("guid-aaa", "guid-bbb")).first()
        assertEquals(2, recurring.size)
        assertTrue(recurring.any { it.title == "Gym" })
        assertTrue(recurring.any { it.title == "Netflix" })
    }

    @Test
    fun expenses_guidList_byRecurringType_combinesAll() = runTest {
        expenseDao.insert(Expense(title = "A-Weekly", amount = 10.0, category = "Food", recurringType = RecurringType.WEEKLY, userGuid = "guid-aaa"))
        expenseDao.insert(Expense(title = "B-Weekly", amount = 20.0, category = "Food", recurringType = RecurringType.WEEKLY, userGuid = "guid-bbb"))
        expenseDao.insert(Expense(title = "B-Monthly", amount = 50.0, category = "Food", recurringType = RecurringType.MONTHLY, userGuid = "guid-bbb"))

        val weekly = expenseDao.getExpensesByRecurringType(listOf("guid-aaa", "guid-bbb"), RecurringType.WEEKLY.name).first()
        assertEquals(2, weekly.size)
    }

    @Test
    fun expenses_guidList_monthlyRecurringOnce_combinesAll() = runTest {
        expenseDao.insert(Expense(title = "Rent", amount = 1200.0, category = "Housing", recurringType = RecurringType.MONTHLY, userGuid = "guid-aaa"))
        expenseDao.insert(Expense(title = "Insurance", amount = 200.0, category = "Bills", recurringType = RecurringType.MONTHLY, userGuid = "guid-bbb"))

        val templates = expenseDao.getMonthlyRecurringExpensesOnce(listOf("guid-aaa", "guid-bbb"))
        assertEquals(2, templates.size)
    }

    @Test
    fun income_guidList_returnsCombinedData() = runTest {
        incomeDao.insert(Income(title = "Salary A", amount = 5000.0, source = "Salary", userGuid = "guid-aaa"))
        incomeDao.insert(Income(title = "Salary B", amount = 6000.0, source = "Salary", userGuid = "guid-bbb"))
        incomeDao.insert(Income(title = "Freelance C", amount = 1000.0, source = "Freelance", userGuid = "guid-ccc"))

        val incomeAB = incomeDao.getAllIncome(listOf("guid-aaa", "guid-bbb")).first()
        assertEquals(2, incomeAB.size)

        val incomeABC = incomeDao.getAllIncome(listOf("guid-aaa", "guid-bbb", "guid-ccc")).first()
        assertEquals(3, incomeABC.size)
    }

    @Test
    fun income_guidList_totalAmount_combinesAll() = runTest {
        incomeDao.insert(Income(title = "A", amount = 3000.0, source = "Salary", userGuid = "guid-aaa"))
        incomeDao.insert(Income(title = "B", amount = 4000.0, source = "Salary", userGuid = "guid-bbb"))

        val total = incomeDao.getTotalAmount(listOf("guid-aaa", "guid-bbb")).first()
        assertEquals(7000.0, total ?: 0.0, 0.01)
    }

    @Test
    fun income_guidList_recurringIncome_combinesAll() = runTest {
        incomeDao.insert(Income(title = "Weekly A", amount = 500.0, source = "Freelance", recurringType = RecurringType.WEEKLY, userGuid = "guid-aaa"))
        incomeDao.insert(Income(title = "Monthly B", amount = 5000.0, source = "Salary", recurringType = RecurringType.MONTHLY, userGuid = "guid-bbb"))
        incomeDao.insert(Income(title = "OneTime A", amount = 100.0, source = "Gifts", recurringType = RecurringType.NONE, userGuid = "guid-aaa"))

        val recurring = incomeDao.getRecurringIncome(listOf("guid-aaa", "guid-bbb")).first()
        assertEquals(2, recurring.size)
    }

    @Test
    fun income_guidList_bySource_combinesAll() = runTest {
        incomeDao.insert(Income(title = "Job A", amount = 5000.0, source = "Salary", userGuid = "guid-aaa"))
        incomeDao.insert(Income(title = "Job B", amount = 6000.0, source = "Salary", userGuid = "guid-bbb"))
        incomeDao.insert(Income(title = "Side B", amount = 1000.0, source = "Freelance", userGuid = "guid-bbb"))

        val salaries = incomeDao.getIncomeBySource(listOf("guid-aaa", "guid-bbb"), "Salary").first()
        assertEquals(2, salaries.size)
    }

    @Test
    fun income_guidList_byRecurringType_combinesAll() = runTest {
        incomeDao.insert(Income(title = "A", amount = 100.0, source = "Salary", recurringType = RecurringType.MONTHLY, userGuid = "guid-aaa"))
        incomeDao.insert(Income(title = "B", amount = 200.0, source = "Salary", recurringType = RecurringType.MONTHLY, userGuid = "guid-bbb"))

        val monthly = incomeDao.getIncomeByRecurringType(listOf("guid-aaa", "guid-bbb"), RecurringType.MONTHLY.name).first()
        assertEquals(2, monthly.size)
    }

    @Test
    fun categories_guidList_returnsCombinedCustomCategories() = runTest {
        categoryDao.insert(Category(name = "Pets", isDefault = false, userGuid = "guid-aaa"))
        categoryDao.insert(Category(name = "Crypto", isDefault = false, userGuid = "guid-bbb"))
        categoryDao.insert(Category(name = "Travel", isDefault = false, userGuid = "guid-ccc"))

        val categoriesAB = categoryDao.getAllCategories(listOf("guid-aaa", "guid-bbb")).first()
        assertTrue(categoriesAB.any { it.name == "Pets" })
        assertTrue(categoriesAB.any { it.name == "Crypto" })
        assertFalse(categoriesAB.any { it.name == "Travel" })
    }

    @Test
    fun categories_guidList_includesDefaults() = runTest {
        categoryDao.insert(Category(name = "Food & Dining", isDefault = true, userGuid = ""))
        categoryDao.insert(Category(name = "Pets", isDefault = false, userGuid = "guid-aaa"))

        val categories = categoryDao.getAllCategories(listOf("guid-aaa", "guid-bbb")).first()
        assertTrue(categories.any { it.name == "Food & Dining" })
        assertTrue(categories.any { it.name == "Pets" })
    }

    @Test
    fun categoryNames_guidList_returnsCombined() = runTest {
        categoryDao.insert(Category(name = "Pets", isDefault = false, userGuid = "guid-aaa"))
        categoryDao.insert(Category(name = "Crypto", isDefault = false, userGuid = "guid-bbb"))

        val names = categoryDao.getAllCategoryNames(listOf("guid-aaa", "guid-bbb")).first()
        assertTrue(names.contains("Pets"))
        assertTrue(names.contains("Crypto"))
    }

    @Test
    fun budgetLimits_guidList_returnsCombined() = runTest {
        budgetLimitDao.insertOrUpdate(BudgetLimit(category = "Food", limitAmount = 500.0, userGuid = "guid-aaa"))
        budgetLimitDao.insertOrUpdate(BudgetLimit(category = "Food", limitAmount = 300.0, userGuid = "guid-bbb"))
        budgetLimitDao.insertOrUpdate(BudgetLimit(category = "Transport", limitAmount = 200.0, userGuid = "guid-aaa"))

        val limits = budgetLimitDao.getAllLimits(listOf("guid-aaa", "guid-bbb")).first()
        assertEquals(3, limits.size)
    }

    @Test
    fun budgetLimits_guidList_getAllLimitsOnce_returnsCombined() = runTest {
        budgetLimitDao.insertOrUpdate(BudgetLimit(category = "Food", limitAmount = 500.0, userGuid = "guid-aaa"))
        budgetLimitDao.insertOrUpdate(BudgetLimit(category = "Food", limitAmount = 300.0, userGuid = "guid-bbb"))

        val limits = budgetLimitDao.getAllLimitsOnce(listOf("guid-aaa", "guid-bbb"))
        assertEquals(2, limits.size)
    }

    @Test
    fun budgetLimits_guidList_getLimitForCategory() = runTest {
        budgetLimitDao.insertOrUpdate(BudgetLimit(category = "Food", limitAmount = 500.0, userGuid = "guid-aaa"))

        val limit = budgetLimitDao.getLimitForCategory(listOf("guid-aaa", "guid-bbb"), "Food")
        assertNotNull(limit)
        assertEquals(500.0, limit!!.limitAmount, 0.01)
    }

    // ── Full end-to-end pairing scenarios ─────────────────────────────────

    @Test
    fun fullScenario_pairAndSeeCombinedData() = runTest {
        userDao.insert(userA)
        userDao.insert(userB)

        // Each user adds their own data
        expenseDao.insert(Expense(title = "Rent A", amount = 1200.0, category = "Housing", userGuid = userA.userGuid))
        expenseDao.insert(Expense(title = "Rent B", amount = 900.0, category = "Housing", userGuid = userB.userGuid))
        incomeDao.insert(Income(title = "Salary A", amount = 5000.0, source = "Salary", userGuid = userA.userGuid))
        incomeDao.insert(Income(title = "Salary B", amount = 4000.0, source = "Salary", userGuid = userB.userGuid))

        // Before pairing: each user sees only their own data
        val guidsABefore = pairingRepository.getPairedUserGuids(userA.id, userA.userGuid)
        assertEquals(1, guidsABefore.size)
        assertEquals(1, expenseDao.getAllExpenses(guidsABefore).first().size)

        // Pair A with B
        createAcceptedPairing(userA.id, userB.id)

        // After pairing: both see combined data
        val guidsAAfter = pairingRepository.getPairedUserGuids(userA.id, userA.userGuid)
        assertEquals(2, guidsAAfter.size)
        assertEquals(2, expenseDao.getAllExpenses(guidsAAfter).first().size)
        assertEquals(2100.0, expenseDao.getTotalAmount(guidsAAfter).first() ?: 0.0, 0.01)
        assertEquals(9000.0, incomeDao.getTotalAmount(guidsAAfter).first() ?: 0.0, 0.01)

        val guidsBAfter = pairingRepository.getPairedUserGuids(userB.id, userB.userGuid)
        assertEquals(2, guidsBAfter.size)
        assertEquals(2, expenseDao.getAllExpenses(guidsBAfter).first().size)
    }

    @Test
    fun fullScenario_threeUsersAsymmetricPairings() = runTest {
        userDao.insert(userA)
        userDao.insert(userB)
        userDao.insert(userC)

        // Each user adds data
        expenseDao.insert(Expense(title = "A-Expense", amount = 100.0, category = "Food", userGuid = userA.userGuid))
        expenseDao.insert(Expense(title = "B-Expense", amount = 200.0, category = "Food", userGuid = userB.userGuid))
        expenseDao.insert(Expense(title = "C-Expense", amount = 300.0, category = "Food", userGuid = userC.userGuid))

        // A pairs with B and C
        createAcceptedPairing(userA.id, userB.id)
        createAcceptedPairing(userA.id, userC.id)

        // A sees all three
        val guidsA = pairingRepository.getPairedUserGuids(userA.id, userA.userGuid)
        assertEquals(3, expenseDao.getAllExpenses(guidsA).first().size)
        assertEquals(600.0, expenseDao.getTotalAmount(guidsA).first() ?: 0.0, 0.01)

        // B sees only A+B (not C, since B is not paired with C)
        val guidsB = pairingRepository.getPairedUserGuids(userB.id, userB.userGuid)
        assertEquals(2, expenseDao.getAllExpenses(guidsB).first().size)
        assertEquals(300.0, expenseDao.getTotalAmount(guidsB).first() ?: 0.0, 0.01)

        // C sees only A+C (not B)
        val guidsC = pairingRepository.getPairedUserGuids(userC.id, userC.userGuid)
        assertEquals(2, expenseDao.getAllExpenses(guidsC).first().size)
        assertEquals(400.0, expenseDao.getTotalAmount(guidsC).first() ?: 0.0, 0.01)
    }

    @Test
    fun fullScenario_newExpenseVisibleToPairedUsers() = runTest {
        userDao.insert(userA)
        userDao.insert(userB)
        createAcceptedPairing(userA.id, userB.id)

        // A adds an expense
        expenseDao.insert(Expense(title = "New Expense", amount = 50.0, category = "Food", userGuid = userA.userGuid))

        // B should see it
        val guidsB = pairingRepository.getPairedUserGuids(userB.id, userB.userGuid)
        val expensesB = expenseDao.getAllExpenses(guidsB).first()
        assertEquals(1, expensesB.size)
        assertEquals("New Expense", expensesB[0].title)
        assertEquals(userA.userGuid, expensesB[0].userGuid)
    }

    @Test
    fun fullScenario_unpairShrinksCombinedData() = runTest {
        userDao.insert(userA)
        userDao.insert(userB)
        createAcceptedPairing(userA.id, userB.id)

        expenseDao.insert(Expense(title = "A-Expense", amount = 100.0, category = "Food", userGuid = userA.userGuid))
        expenseDao.insert(Expense(title = "B-Expense", amount = 200.0, category = "Food", userGuid = userB.userGuid))

        // Before unpair: both see 2 expenses
        val guidsBefore = pairingRepository.getPairedUserGuids(userA.id, userA.userGuid)
        assertEquals(2, expenseDao.getAllExpenses(guidsBefore).first().size)

        // Unpair
        pairingRepository.unpair(userA.id, userB.id)

        // After unpair: each sees only their own
        val guidsAAfter = pairingRepository.getPairedUserGuids(userA.id, userA.userGuid)
        assertEquals(1, guidsAAfter.size)
        assertEquals(1, expenseDao.getAllExpenses(guidsAAfter).first().size)
        assertEquals("A-Expense", expenseDao.getAllExpenses(guidsAAfter).first()[0].title)

        val guidsBAfter = pairingRepository.getPairedUserGuids(userB.id, userB.userGuid)
        assertEquals(1, guidsBAfter.size)
        assertEquals(1, expenseDao.getAllExpenses(guidsBAfter).first().size)
        assertEquals("B-Expense", expenseDao.getAllExpenses(guidsBAfter).first()[0].title)
    }

    @Test
    fun fullScenario_unpairOnePartnerKeepsOthers() = runTest {
        userDao.insert(userA)
        userDao.insert(userB)
        userDao.insert(userC)
        createAcceptedPairing(userA.id, userB.id)
        createAcceptedPairing(userA.id, userC.id)

        expenseDao.insert(Expense(title = "A-Expense", amount = 100.0, category = "Food", userGuid = userA.userGuid))
        expenseDao.insert(Expense(title = "B-Expense", amount = 200.0, category = "Food", userGuid = userB.userGuid))
        expenseDao.insert(Expense(title = "C-Expense", amount = 300.0, category = "Food", userGuid = userC.userGuid))

        // Unpair A from B only
        pairingRepository.unpair(userA.id, userB.id)

        // A should still see A+C
        val guidsA = pairingRepository.getPairedUserGuids(userA.id, userA.userGuid)
        assertEquals(2, guidsA.size)
        val expensesA = expenseDao.getAllExpenses(guidsA).first()
        assertEquals(2, expensesA.size)
        assertEquals(400.0, expenseDao.getTotalAmount(guidsA).first() ?: 0.0, 0.01)

        // B should see only B
        val guidsB = pairingRepository.getPairedUserGuids(userB.id, userB.userGuid)
        assertEquals(1, guidsB.size)
        assertEquals(1, expenseDao.getAllExpenses(guidsB).first().size)
    }

    @Test
    fun fullScenario_dataUntouchedThroughPairAndUnpair() = runTest {
        userDao.insert(userA)
        userDao.insert(userB)

        val expenseA = Expense(title = "Rent A", amount = 1200.0, category = "Housing", userGuid = userA.userGuid)
        val expenseB = Expense(title = "Rent B", amount = 900.0, category = "Housing", userGuid = userB.userGuid)
        expenseDao.insert(expenseA)
        expenseDao.insert(expenseB)

        // Pair
        createAcceptedPairing(userA.id, userB.id)

        // Verify A's expense still has A's guid
        val guidsA = pairingRepository.getPairedUserGuids(userA.id, userA.userGuid)
        val allExpenses = expenseDao.getAllExpenses(guidsA).first()
        val aExpense = allExpenses.find { it.title == "Rent A" }
        assertEquals(userA.userGuid, aExpense!!.userGuid)
        val bExpense = allExpenses.find { it.title == "Rent B" }
        assertEquals(userB.userGuid, bExpense!!.userGuid)

        // Unpair
        pairingRepository.unpair(userA.id, userB.id)

        // Data still intact with original guids
        val aOnly = expenseDao.getAllExpenses(listOf(userA.userGuid)).first()
        assertEquals(1, aOnly.size)
        assertEquals(1200.0, aOnly[0].amount, 0.01)
        assertEquals(userA.userGuid, aOnly[0].userGuid)

        val bOnly = expenseDao.getAllExpenses(listOf(userB.userGuid)).first()
        assertEquals(1, bOnly.size)
        assertEquals(900.0, bOnly[0].amount, 0.01)
        assertEquals(userB.userGuid, bOnly[0].userGuid)
    }

    @Test
    fun fullScenario_writeOperationsUseOwnGuidOnly() = runTest {
        userDao.insert(userA)
        userDao.insert(userB)
        createAcceptedPairing(userA.id, userB.id)

        // When A adds an expense, it's stamped with A's guid
        expenseDao.insert(Expense(title = "A's Lunch", amount = 15.0, category = "Food", userGuid = userA.userGuid))

        // When B adds an expense, it's stamped with B's guid
        expenseDao.insert(Expense(title = "B's Dinner", amount = 25.0, category = "Food", userGuid = userB.userGuid))

        // Both see both expenses
        val guidsA = pairingRepository.getPairedUserGuids(userA.id, userA.userGuid)
        val all = expenseDao.getAllExpenses(guidsA).first()
        assertEquals(2, all.size)

        // But each expense retains its creator's guid
        val aExpense = all.find { it.title == "A's Lunch" }!!
        assertEquals(userA.userGuid, aExpense.userGuid)
        val bExpense = all.find { it.title == "B's Dinner" }!!
        assertEquals(userB.userGuid, bExpense.userGuid)
    }

    // ── Helper ─────────────────────────────────────────────────────────────

    private suspend fun createAcceptedPairing(requesterId: String, receiverId: String) {
        userPairingDao.insert(
            UserPairing(
                requesterId = requesterId,
                receiverId = receiverId,
                status = UserPairing.STATUS_ACCEPTED,
                acceptedAt = System.currentTimeMillis()
            )
        )
    }
}
