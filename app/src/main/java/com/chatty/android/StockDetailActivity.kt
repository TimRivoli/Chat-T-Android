package com.chatty.android

import android.graphics.Typeface
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.chatty.android.etc.DataClasses.PriceWorkingSetEntry
import com.chatty.android.etc.StorageManager
import java.text.SimpleDateFormat
import java.util.Locale

class StockDetailActivity : AppCompatActivity() {

    private val TAG = "StockDetailActivity"

    private lateinit var tvTicker: TextView
    private lateinit var tvCurrentPrice: TextView
    private lateinit var tvCompanyName: TextView
    private lateinit var tvSector: TextView
    private lateinit var llContent: LinearLayout
    private lateinit var btnBack: ImageButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_stock_detail)

        tvTicker = findViewById(R.id.tvTicker)
        tvCurrentPrice = findViewById(R.id.tvCurrentPrice)
        tvCompanyName = findViewById(R.id.tvCompanyName)
        tvSector = findViewById(R.id.tvSector)
        llContent = findViewById(R.id.llContent)
        btnBack = findViewById(R.id.btnBack)

        btnBack.setOnClickListener { finish() }

        val ticker = intent.getStringExtra("ticker") ?: ""
        val entry = StorageManager.getPricesWorkingSet().firstOrNull { it.ticker == ticker }
        if (entry != null) {
            bindEntry(entry)
        } else {
            Log.e(TAG, "No entry found for ticker: $ticker")
            tvTicker.text = ticker
            tvCurrentPrice.text = ""
            tvCompanyName.text = "Not found"
            tvSector.text = ""
        }
    }

    private fun bindEntry(e: PriceWorkingSetEntry) {
        tvTicker.text = e.ticker
        tvCurrentPrice.text = formatPrice(e.currentPrice)
        tvCompanyName.text = e.companyName
        tvSector.text = e.sector

        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        addRow("Price Updated", sdf.format(e.latestEntry))

        addSection("Performance")
        addRow("1-Day", formatPct(e.pc1Day))
        addRow("1-Month", formatPct(e.pc1Month))
        addRow("2-Month", formatPct(e.pc2Month))
        addRow("3-Month", formatPct(e.pc3Month))
        addRow("6-Month", formatPct(e.pc6Month))
        addRow("1-Year", formatPct(e.pc1Year))
        addRow("2-Year", formatPct(e.pc2Year))

        addSection("Technical")
        addRow("2-Day Moving Avg", formatPrice(e.average2Day))
        addRow("5-Day Moving Avg", formatPrice(e.average5Day))
        addRow("Monthly Gain", formatPct(e.gainMonthly))
        addRow("1Y Loss Std Dev", formatPct(e.lossStd1Year))
        addRow("Point Value", e.pointValue.toString())

        addSection("Portfolio")
        addRow("Target Holdings", "%.2f%%".format(e.targetHoldings * 100))
        addRow("S&P 500 Listed", if (e.sp500Listed) "Yes" else "No")

        addSection("Income Statement")
        addRow("Revenue", formatMoney(e.revenue))
        addRow("Net Income", formatMoney(e.netIncome))
        addRow("Operating Expense", formatMoney(e.operatingExpense))
        addRow("Net Profit Margin", formatPct(e.netProfitMargin))
        addRow("Earnings Per Share", "$%.4f".format(e.earningsPerShare))
        addRow("Company Size", e.companySize.toString())
        addRow("Market Cap", formatMoney(e.marketCap))

        addSection("Balance Sheet")
        addRow("Cash & Short-Term Investments", formatMoney(e.cashShortTermInvestments))
        addRow("Total Assets", formatMoney(e.totalAssets))
        addRow("Total Liabilities", formatMoney(e.totalLiabilities))
        addRow("Net Worth", formatMoney(e.netWorth))
        addRow("Total Equity", formatMoney(e.totalEquity))
        addRow("Shares Outstanding", formatMoney(e.sharesOutstanding))

        addSection("Ratios")
        addRow("Price to Book", "%.4f".format(e.priceToBook))
        addRow("Return on Assets", formatPct(e.returnOnAssets))
        addRow("Return on Capital", formatPct(e.returnOnCapital))

        addSection("Cash Flow")
        addRow("From Operations", formatMoney(e.cashFromOperations))
        addRow("From Investing", formatMoney(e.cashFromInvesting))
        addRow("From Financing", formatMoney(e.cashFromFinancing))
        addRow("Net Change in Cash", formatMoney(e.netChangeInCash))
        addRow("Free Cash Flow", formatMoney(e.freeCashFlow))

        if (e.comments.isNotBlank()) {
            addSection("Comments")
            val tv = TextView(this)
            tv.text = e.comments
            tv.setPadding(dp(16), dp(6), dp(16), dp(12))
            tv.textSize = 13f
            llContent.addView(tv)
        }

        addSection("Meta")
        addRow("Latest Entry", sdf.format(e.latestEntry))
    }

    private fun addSection(label: String) {
        val divider = View(this)
        val divLp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(1))
        divider.layoutParams = divLp
        divider.setBackgroundColor(ContextCompat.getColor(this, R.color.divider))
        llContent.addView(divider)

        val tv = TextView(this)
        tv.text = label
        tv.setPadding(dp(16), dp(10), dp(16), dp(4))
        tv.setTypeface(null, Typeface.BOLD)
        tv.setTextColor(ContextCompat.getColor(this, R.color.colorTextSecondary))
        tv.textSize = 13f
        llContent.addView(tv)
    }

    private fun addRow(label: String, value: String) {
        val row = LinearLayout(this)
        row.orientation = LinearLayout.HORIZONTAL
        row.setPadding(dp(16), dp(5), dp(16), dp(5))

        val tvLabel = TextView(this)
        tvLabel.text = label
        tvLabel.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        tvLabel.setTextColor(ContextCompat.getColor(this, R.color.colorTextSecondary))
        tvLabel.textSize = 13f

        val tvValue = TextView(this)
        tvValue.text = value
        tvValue.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        tvValue.textAlignment = View.TEXT_ALIGNMENT_TEXT_END
        tvValue.textSize = 13f

        row.addView(tvLabel)
        row.addView(tvValue)
        llContent.addView(row)
    }

    private fun formatPrice(value: Double) = "$%.2f".format(value)

    private fun formatPct(value: Double): String {
        val pct = value * 100
        val sign = if (pct >= 0) "+" else ""
        return "$sign${"%.2f".format(pct)}%"
    }

    private fun formatMoney(value: Double): String {
        val abs = Math.abs(value)
        val sign = if (value < 0) "-" else ""
        return when {
            abs >= 1_000_000_000.0 -> "${sign}${"%.2f".format(abs / 1_000_000_000.0)}B"
            abs >= 1_000_000.0 -> "${sign}${"%.2f".format(abs / 1_000_000.0)}M"
            abs >= 1_000.0 -> "${sign}${"%.2f".format(abs / 1_000.0)}K"
            else -> "$%.2f".format(value)
        }
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
