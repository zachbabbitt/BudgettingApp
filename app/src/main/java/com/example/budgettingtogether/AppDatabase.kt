package com.example.budgettingtogether

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Date

@Database(
    entities = [
        Expense::class,
        Income::class,
        BudgetLimit::class,
        Category::class,
        UserPreferences::class,
        ExchangeRate::class
    ],
    version = 6,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun expenseDao(): ExpenseDao
    abstract fun incomeDao(): IncomeDao
    abstract fun budgetLimitDao(): BudgetLimitDao
    abstract fun categoryDao(): CategoryDao
    abstract fun userPreferencesDao(): UserPreferencesDao
    abstract fun exchangeRateDao(): ExchangeRateDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS categories (
                        name TEXT NOT NULL PRIMARY KEY,
                        isDefault INTEGER NOT NULL DEFAULT 0
                    )
                """.trimIndent())
            }
        }

        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Add currency columns to expenses
                db.execSQL("ALTER TABLE expenses ADD COLUMN originalAmount REAL")
                db.execSQL("ALTER TABLE expenses ADD COLUMN originalCurrency TEXT")

                // Add currency columns to income
                db.execSQL("ALTER TABLE income ADD COLUMN originalAmount REAL")
                db.execSQL("ALTER TABLE income ADD COLUMN originalCurrency TEXT")

                // Create user_preferences table
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS user_preferences (
                        id INTEGER NOT NULL PRIMARY KEY,
                        defaultCurrencyCode TEXT NOT NULL DEFAULT 'USD',
                        lastRatesUpdate INTEGER NOT NULL DEFAULT 0
                    )
                """.trimIndent())

                // Create exchange_rates table
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS exchange_rates (
                        currencyCode TEXT NOT NULL PRIMARY KEY,
                        rateToUsd REAL NOT NULL,
                        currencyName TEXT NOT NULL,
                        symbol TEXT NOT NULL
                    )
                """.trimIndent())

                // Insert default preferences
                db.execSQL("INSERT OR IGNORE INTO user_preferences (id, defaultCurrencyCode, lastRatesUpdate) VALUES (1, 'USD', 0)")
            }
        }

        private val DEFAULT_CATEGORIES = listOf(
            Category("Food & Dining", true),
            Category("Transportation", true),
            Category("Shopping", true),
            Category("Entertainment", true),
            Category("Bills & Utilities", true),
            Category("Health", true),
            Category("Other", true)
        )

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "budget_database"
                )
                .addMigrations(MIGRATION_4_5, MIGRATION_5_6)
                .addCallback(object : Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        INSTANCE?.let { database ->
                            CoroutineScope(Dispatchers.IO).launch {
                                database.categoryDao().insertAll(DEFAULT_CATEGORIES)
                                // Insert default preferences on fresh install
                                database.userPreferencesDao().savePreferences(UserPreferences())
                            }
                        }
                    }
                })
                .build()
                INSTANCE = instance
                // Insert default categories if needed
                CoroutineScope(Dispatchers.IO).launch {
                    instance.categoryDao().insertAll(DEFAULT_CATEGORIES)
                }
                instance
            }
        }
    }
}

class Converters {
    @TypeConverter
    fun fromTimestamp(value: Long?): Date? {
        return value?.let { Date(it) }
    }

    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? {
        return date?.time
    }

    @TypeConverter
    fun fromRecurringType(value: RecurringType): String {
        return value.name
    }

    @TypeConverter
    fun toRecurringType(value: String): RecurringType {
        return RecurringType.valueOf(value)
    }
}
