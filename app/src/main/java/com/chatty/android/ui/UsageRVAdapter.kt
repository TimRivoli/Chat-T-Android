package com.chatty.android.ui

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.chatty.android.R
import java.text.SimpleDateFormat
import com.chatty.android.etc.DataClasses.*

class UsageRVAdapter(private val context: Context, private var usageList: List<ChatUsage>, private val onItemClick: (String) -> Unit) :
    RecyclerView.Adapter<UsageRVAdapter.ViewHolder>(){

        inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val chatUsagePromptTokens: TextView = itemView.findViewById(R.id.chatUsagePromptTokens)
            val chatUsageCompletionTokens: TextView = itemView.findViewById(R.id.chatUsageCompletionTokens)
            val chatUsageTotalToken: TextView = itemView.findViewById(R.id.chatUsageTotalTokens)
            val chatUsageUserID: TextView = itemView.findViewById(R.id.chatUsageUserID)
            val chatUsageTimeStamp: TextView = itemView.findViewById(R.id.chatUsageTimeStamp)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.usage_entry, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val sdf = SimpleDateFormat("MM/dd/yy HH:mm")
            val u: ChatUsage = usageList[position]
            holder.chatUsagePromptTokens.text = u.promptTokens.toString()
            holder.chatUsageCompletionTokens.text = u.completionTokens.toString()
            holder.chatUsageTotalToken.text = u.totalTokens.toString()
            holder.chatUsageUserID.text = u.conversationID.toString()
            holder.chatUsageTimeStamp.text = sdf.format(u.timeStamp)
        }

        override fun getItemCount(): Int {
            return usageList.size
        }

        fun SetMessages(newMessages :List<ChatUsage>){
            usageList = newMessages
            notifyDataSetChanged()
        }
}