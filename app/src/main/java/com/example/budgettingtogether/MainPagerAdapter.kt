package com.example.budgettingtogether

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.budgettingtogether.analysis.AnalysisFragment
import com.example.budgettingtogether.expenses.ExpensesFragment
import com.example.budgettingtogether.income.IncomeFragment

class MainPagerAdapter(fragmentActivity: FragmentActivity) : FragmentStateAdapter(fragmentActivity) {

    override fun getItemCount(): Int = 4

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> HomeFragment()
            1 -> ExpensesFragment()
            2 -> IncomeFragment()
            3 -> AnalysisFragment()
            else -> throw IllegalArgumentException("Invalid position: $position")
        }
    }
}
