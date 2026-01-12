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

@RunWith(AndroidJUnit4::class)
class CategoryDaoTest {

    private lateinit var database: AppDatabase
    private lateinit var categoryDao: CategoryDao

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        categoryDao = database.categoryDao()
    }

    @After
    fun teardown() {
        database.close()
    }

    @Test
    fun insertCategory_addsToDatabase() = runTest {
        val category = Category(name = "Travel", isDefault = false)

        categoryDao.insert(category)

        val categories = categoryDao.getAllCategories().first()
        assertEquals(1, categories.size)
        assertEquals("Travel", categories[0].name)
        assertFalse(categories[0].isDefault)
    }

    @Test
    fun insertMultipleCategories_allAddedToDatabase() = runTest {
        val category1 = Category(name = "Travel", isDefault = false)
        val category2 = Category(name = "Hobbies", isDefault = false)
        val category3 = Category(name = "Pets", isDefault = false)

        categoryDao.insert(category1)
        categoryDao.insert(category2)
        categoryDao.insert(category3)

        val categories = categoryDao.getAllCategories().first()
        assertEquals(3, categories.size)
    }

    @Test
    fun insertAll_addsMultipleCategories() = runTest {
        val categories = listOf(
            Category(name = "Travel", isDefault = false),
            Category(name = "Hobbies", isDefault = false),
            Category(name = "Pets", isDefault = false)
        )

        categoryDao.insertAll(categories)

        val result = categoryDao.getAllCategories().first()
        assertEquals(3, result.size)
    }

    @Test
    fun insertDuplicateCategory_isIgnored() = runTest {
        val category1 = Category(name = "Travel", isDefault = false)
        val category2 = Category(name = "Travel", isDefault = true)

        categoryDao.insert(category1)
        categoryDao.insert(category2)

        val categories = categoryDao.getAllCategories().first()
        assertEquals(1, categories.size)
        // First insert wins, so isDefault should be false
        assertFalse(categories[0].isDefault)
    }

    @Test
    fun deleteCategory_removesFromDatabase() = runTest {
        val category = Category(name = "Travel", isDefault = false)

        categoryDao.insert(category)
        var categories = categoryDao.getAllCategories().first()
        assertEquals(1, categories.size)

        categoryDao.delete(category)
        categories = categoryDao.getAllCategories().first()
        assertEquals(0, categories.size)
    }

    @Test
    fun deleteByName_removesNonDefaultCategory() = runTest {
        val category = Category(name = "Travel", isDefault = false)

        categoryDao.insert(category)
        assertEquals(1, categoryDao.getAllCategories().first().size)

        categoryDao.deleteByName("Travel")
        assertEquals(0, categoryDao.getAllCategories().first().size)
    }

    @Test
    fun deleteByName_doesNotRemoveDefaultCategory() = runTest {
        val defaultCategory = Category(name = "Food & Dining", isDefault = true)

        categoryDao.insert(defaultCategory)
        assertEquals(1, categoryDao.getAllCategories().first().size)

        categoryDao.deleteByName("Food & Dining")
        // Default category should still exist
        assertEquals(1, categoryDao.getAllCategories().first().size)
    }

    @Test
    fun getAllCategories_orderedByDefaultThenName() = runTest {
        categoryDao.insert(Category(name = "Zebra", isDefault = false))
        categoryDao.insert(Category(name = "Apple", isDefault = true))
        categoryDao.insert(Category(name = "Banana", isDefault = false))
        categoryDao.insert(Category(name = "Cherry", isDefault = true))

        val categories = categoryDao.getAllCategories().first()

        // Default categories should come first, then sorted by name
        assertEquals(4, categories.size)
        // First two should be defaults (Apple, Cherry) sorted by name
        assertTrue(categories[0].isDefault)
        assertTrue(categories[1].isDefault)
        assertEquals("Apple", categories[0].name)
        assertEquals("Cherry", categories[1].name)
        // Last two should be non-defaults (Banana, Zebra) sorted by name
        assertFalse(categories[2].isDefault)
        assertFalse(categories[3].isDefault)
        assertEquals("Banana", categories[2].name)
        assertEquals("Zebra", categories[3].name)
    }

    @Test
    fun getAllCategoryNames_returnsOnlyNames() = runTest {
        categoryDao.insert(Category(name = "Food", isDefault = true))
        categoryDao.insert(Category(name = "Travel", isDefault = false))

        val names = categoryDao.getAllCategoryNames().first()

        assertEquals(2, names.size)
        assertTrue(names.contains("Food"))
        assertTrue(names.contains("Travel"))
    }

    @Test
    fun getAllCategoryNames_orderedCorrectly() = runTest {
        categoryDao.insert(Category(name = "Zebra", isDefault = false))
        categoryDao.insert(Category(name = "Apple", isDefault = true))

        val names = categoryDao.getAllCategoryNames().first()

        // Default first, then alphabetical
        assertEquals("Apple", names[0])
        assertEquals("Zebra", names[1])
    }

    @Test
    fun categoryFields_persistCorrectly() = runTest {
        val category = Category(name = "Custom Category", isDefault = false)

        categoryDao.insert(category)
        val retrieved = categoryDao.getAllCategories().first()[0]

        assertEquals("Custom Category", retrieved.name)
        assertFalse(retrieved.isDefault)
    }

    @Test
    fun defaultCategoryFields_persistCorrectly() = runTest {
        val category = Category(name = "Default Category", isDefault = true)

        categoryDao.insert(category)
        val retrieved = categoryDao.getAllCategories().first()[0]

        assertEquals("Default Category", retrieved.name)
        assertTrue(retrieved.isDefault)
    }

    @Test
    fun emptyDatabase_returnsEmptyList() = runTest {
        val categories = categoryDao.getAllCategories().first()
        assertTrue(categories.isEmpty())

        val names = categoryDao.getAllCategoryNames().first()
        assertTrue(names.isEmpty())
    }
}
