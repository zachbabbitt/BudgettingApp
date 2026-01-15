package com.example.budgettingtogether

object RunningTotalCalculator {
    /**
     * Calculates running totals for a list of amounts.
     * Items are assumed to be sorted newest-first (descending by date),
     * so we calculate cumulative sums from end to start.
     *
     * The result is that the first item (newest) shows the grand total,
     * and the last item (oldest) shows just its own amount.
     *
     * @param amounts List of amounts in newest-first order
     * @return List of running totals, same size as input
     */
    fun calculate(amounts: List<Double>): List<Double> {
        if (amounts.isEmpty()) return emptyList()

        val totals = DoubleArray(amounts.size)
        var cumulative = 0.0
        for (i in amounts.indices.reversed()) {
            cumulative += amounts[i]
            totals[i] = cumulative
        }
        return totals.toList()
    }
}
