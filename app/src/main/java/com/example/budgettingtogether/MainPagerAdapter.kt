package com.example.budgettingtogether

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.budgettingtogether.analysis.AnalysisFragment
import com.example.budgettingtogether.calendar.CalendarFragment
import com.example.budgettingtogether.expenses.ExpensesFragment

class MainPagerAdapter(fragmentActivity: FragmentActivity) : FragmentStateAdapter(fragmentActivity) {

    override fun getItemCount(): Int = 4

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> HomeFragment()
            1 -> ExpensesFragment()
            2 -> AnalysisFragment()
            3 -> CalendarFragment()
            else -> throw IllegalArgumentException("Invalid position: $position")
        }
    }
}
