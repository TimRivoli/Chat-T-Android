package com.chatty.android.ui

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.chatty.android.R
import com.chatty.android.etc.DataClasses.PriceWorkingSetEntry
import kotlin.reflect.KFunction1

class PriceWorkingSetRVAdapter(
    private val context: Context,
    private var priceList: List<PriceWorkingSetEntry>,
    private val onItemClick: KFunction1<String, Unit>
) : RecyclerView.Adapter<PriceWorkingSetRVAdapter.ViewHolder>() {

    private val TAG = "PriceWorkingSetRVAdapter"

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val txtTicker: TextView = itemView.findViewById(R.id.txtTicker)
        val txtCurrentPrice: TextView = itemView.findViewById(R.id.txtCurrentPrice)
        val txtCompanyName: TextView = itemView.findViewById(R.id.txtCompanyName)
        val txtSector: TextView = itemView.findViewById(R.id.txtSector)
        val txtPriceChanges: TextView = itemView.findViewById(R.id.txtPriceChanges)
        val txtTargetHoldings: TextView = itemView.findViewById(R.id.txtTargetHoldings)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.price_working_set_entry, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        var lastClickTime = 0L
        val COOLDOWN_TIME: Long = 3000
        val entry = priceList[position]
        holder.txtTicker.text = entry.ticker
        holder.txtCurrentPrice.text = "$%.2f".format(entry.currentPrice)
        holder.txtCompanyName.text = entry.companyName
        holder.txtSector.text = entry.sector
        holder.txtPriceChanges.text = "1D: ${formatPct(entry.pc1Day)}  1M: ${formatPct(entry.pc1Month)}  1Y: ${formatPct(entry.pc1Year)}"
        holder.txtTargetHoldings.text = "Target: ${"%.2f".format(entry.targetHoldings)}"
        val clickListener = View.OnClickListener {
            it.isClickable = false
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastClickTime >= COOLDOWN_TIME) {
                lastClickTime = currentTime
                onItemClick(entry.ticker)
            } else {
                Log.d(TAG, "Suppressing spam clicking...")
            }
            it.postDelayed({ it.isClickable = true }, COOLDOWN_TIME)
        }
        holder.txtTicker.setOnClickListener(clickListener)
        holder.txtCompanyName.setOnClickListener(clickListener)
        holder.txtSector.setOnClickListener(clickListener)
        holder.txtPriceChanges.setOnClickListener(clickListener)
        holder.txtCurrentPrice.setOnClickListener(clickListener)
        holder.txtTargetHoldings.setOnClickListener(clickListener)
    }

    override fun getItemCount(): Int = priceList.size

    fun updateData(newList: List<PriceWorkingSetEntry>) {
        Log.d(TAG, "Refreshing RV contents")
        priceList = newList
        notifyDataSetChanged()
    }

    private fun formatPct(value: Double): String {
        val pct = value * 100
        val sign = if (pct >= 0) "+" else ""
        return "$sign${"%.1f".format(pct)}%"
    }
}
