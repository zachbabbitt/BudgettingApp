package com.example.budgettingtogether.currency

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.budgettingtogether.databinding.ItemExchangeRateBinding

class ExchangeRateAdapter : RecyclerView.Adapter<ExchangeRateAdapter.ViewHolder>() {

    private var rates: List<ExchangeRate> = emptyList()

    fun updateRates(newRates: List<ExchangeRate>) {
        rates = newRates
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemExchangeRateBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(rates[position])
    }

    override fun getItemCount(): Int = rates.size

    class ViewHolder(private val binding: ItemExchangeRateBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(rate: ExchangeRate) {
            binding.textViewCode.text = rate.currencyCode
            binding.textViewSymbol.text = rate.symbol
            binding.textViewName.text = rate.currencyName
            binding.textViewRate.text = String.format("%.4f", rate.rateToUsd)
        }
    }
}
