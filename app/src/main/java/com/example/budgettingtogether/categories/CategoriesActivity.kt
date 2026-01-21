package com.example.budgettingtogether.categories

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.budgettingtogether.R
import com.example.budgettingtogether.core.AppDatabase
import com.example.budgettingtogether.databinding.ActivityCategoriesBinding
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class CategoriesActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCategoriesBinding
    private lateinit var categoryDao: CategoryDao
    private lateinit var adapter: CategoryAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCategoriesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val database = AppDatabase.getDatabase(this)
        categoryDao = database.categoryDao()

        setupToolbar()
        setupRecyclerView()
        setupAddButton()
        observeCategories()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.manage_categories)
        binding.toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }
    }

    private fun setupRecyclerView() {
        adapter = CategoryAdapter { category ->
            deleteCategory(category)
        }
        binding.recyclerViewCategories.apply {
            layoutManager = LinearLayoutManager(this@CategoriesActivity)
            adapter = this@CategoriesActivity.adapter
        }
    }

    private fun setupAddButton() {
        binding.buttonAddCategory.setOnClickListener {
            val categoryName = binding.editTextNewCategory.text.toString().trim()
            if (categoryName.isNotEmpty()) {
                addCategory(categoryName)
                binding.editTextNewCategory.text?.clear()
            } else {
                Toast.makeText(this, R.string.enter_category_name, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun observeCategories() {
        lifecycleScope.launch {
            categoryDao.getAllCategories().collectLatest { categories ->
                adapter.updateList(categories)
            }
        }
    }

    private fun addCategory(name: String) {
        lifecycleScope.launch {
            categoryDao.insert(Category(name, false))
        }
    }

    private fun deleteCategory(category: Category) {
        lifecycleScope.launch {
            categoryDao.delete(category)
        }
    }
}
