package com.example.budgettingtogether.currency

import android.R
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Filter
import android.widget.TextView

class CurrencyDropdownAdapter(
    context: Context,
    private val recentCurrencies: List<String>,
    private val allCurrencies: List<String>,
    private val useDisplayNames: Boolean = false
) : ArrayAdapter<String>(context, R.layout.simple_dropdown_item_1line) {

    companion object {
        private const val TYPE_CURRENCY = 0
        private const val TYPE_HEADER = 1
    }

    private val originalItems: List<CurrencyItem>
    private var filteredItems: List<CurrencyItem>
    private var filteredPositionToCode: Map<Int, String>

    init {
        val itemList = mutableListOf<CurrencyItem>()
        val positionMap = mutableMapOf<Int, String>()
        var position = 0

        // Add recent currencies header and items
        if (recentCurrencies.isNotEmpty()) {
            itemList.add(CurrencyItem.Header(com.example.budgettingtogether.R.string.recent_currencies))
            position++

            for (code in recentCurrencies) {
                itemList.add(CurrencyItem.Currency(code))
                positionMap[position] = code
                position++
            }

            // Add "All Currencies" header
            itemList.add(CurrencyItem.Header(com.example.budgettingtogether.R.string.all_currencies))
            position++
        }

        // Add remaining currencies (excluding recent ones)
        val remainingCurrencies = allCurrencies.filter { it !in recentCurrencies }
        for (code in remainingCurrencies) {
            itemList.add(CurrencyItem.Currency(code))
            positionMap[position] = code
            position++
        }

        originalItems = itemList
        filteredItems = itemList
        filteredPositionToCode = positionMap
    }

    override fun getCount(): Int = filteredItems.size

    override fun getItem(position: Int): String? {
        return when (val item = filteredItems[position]) {
            is CurrencyItem.Currency -> item.code
            is CurrencyItem.Header -> null
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when (filteredItems[position]) {
            is CurrencyItem.Currency -> TYPE_CURRENCY
            is CurrencyItem.Header -> TYPE_HEADER
        }
    }

    override fun getViewTypeCount(): Int = 2

    override fun isEnabled(position: Int): Boolean {
        return filteredItems[position] is CurrencyItem.Currency
    }

    override fun areAllItemsEnabled(): Boolean = false

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        return createView(position, convertView, parent)
    }

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
        return createView(position, convertView, parent)
    }

    private fun createView(position: Int, convertView: View?, parent: ViewGroup): View {
        val item = filteredItems[position]

        return when (item) {
            is CurrencyItem.Currency -> {
                val view = convertView?.takeIf { it.tag == TYPE_CURRENCY }
                    ?: LayoutInflater.from(context)
                        .inflate(R.layout.simple_dropdown_item_1line, parent, false)
                        .also { it.tag = TYPE_CURRENCY }

                val displayText = if (useDisplayNames) {
                    CurrencyData.getDisplayName(item.code)
                } else {
                    item.code
                }
                (view as TextView).text = displayText
                view
            }
            is CurrencyItem.Header -> {
                val view = convertView?.takeIf { it.tag == TYPE_HEADER }
                    ?: LayoutInflater.from(context)
                        .inflate(com.example.budgettingtogether.R.layout.item_currency_divider, parent, false)
                        .also { it.tag = TYPE_HEADER }
                (view as TextView).text = context.getString(item.stringResId)
                view
            }
        }
    }

    fun getCurrencyCodeAtPosition(position: Int): String? {
        return filteredPositionToCode[position]
    }

    override fun getFilter(): Filter {
        return object : Filter() {
            override fun performFiltering(constraint: CharSequence?): FilterResults {
                val query = constraint?.toString()?.trim()?.lowercase() ?: ""

                val results = FilterResults()

                if (query.isEmpty()) {
                    // No filter - show original list with headers
                    results.values = FilterData(originalItems, buildPositionMap(originalItems))
                    results.count = originalItems.size
                } else {
                    // Filter currencies (no headers when filtering)
                    val filtered = allCurrencies.filter { code ->
                        val displayName = CurrencyData.getDisplayName(code).lowercase()
                        code.lowercase().contains(query) || displayName.contains(query)
                    }.map { CurrencyItem.Currency(it) }

                    results.values = FilterData(filtered, buildPositionMap(filtered))
                    results.count = filtered.size
                }

                return results
            }

            override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
                val data = results?.values as? FilterData
                if (data != null) {
                    filteredItems = data.items
                    filteredPositionToCode = data.positionMap
                    notifyDataSetChanged()
                }
            }

            private fun buildPositionMap(items: List<CurrencyItem>): Map<Int, String> {
                val map = mutableMapOf<Int, String>()
                items.forEachIndexed { index, item ->
                    if (item is CurrencyItem.Currency) {
                        map[index] = item.code
                    }
                }
                return map
            }
        }
    }

    private data class FilterData(
        val items: List<CurrencyItem>,
        val positionMap: Map<Int, String>
    )

    sealed class CurrencyItem {
        data class Currency(val code: String) : CurrencyItem()
        data class Header(val stringResId: Int) : CurrencyItem()
    }
}
