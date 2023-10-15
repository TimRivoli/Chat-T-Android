package com.chatty.android.ui

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.chatty.android.R
import java.text.SimpleDateFormat
import kotlin.reflect.KFunction1
import com.chatty.android.etc.ChatManager
import com.chatty.android.etc.DataClasses.*

class ConversationListRVAdapterSwipeCallBack(private val adapter: RecyclerView.Adapter<*>, private val swipeListener: ConversationListRVAdapter.SwipeListener) :
    ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT ) {

    override fun onMove(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        target: RecyclerView.ViewHolder
    ): Boolean {
        return false
    }

    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
        if (viewHolder is ConversationListRVAdapter.ViewHolder) {
            val position = viewHolder.adapterPosition
            val conversationID: String = viewHolder.conversationID.text.toString()
            adapter.notifyItemRemoved(position)
            swipeListener?.onItemSwiped(position, conversationID)
        }
    }
}

class ConversationListRVAdapter(private val context: Context, private var conversationList: List<Conversation>, private val onItemClick: KFunction1<Long, Unit>) :
    RecyclerView.Adapter<ConversationListRVAdapter.ViewHolder>(){
    private val TAG = "ConversationListRVAdapterSwipeCallBack"
    var swipeListener: ConversationListRVAdapter.SwipeListener? = null

    interface SwipeListener {
        fun onItemSwiped(position: Int, conversationID: String)
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val conversationTitle: TextView = itemView.findViewById(R.id.chatConversationTitle)
            val conversationSummary: TextView = itemView.findViewById(R.id.chatConversationSummary)
            val conversationDateModified: TextView = itemView.findViewById(R.id.chatConversationDateModified)
            val conversationID: TextView = itemView.findViewById(R.id.chatConversationID)
        }
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.conversation_entry, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            var lastClickTime = 0L
            val COOLDOWN_TIME: Long = 3000
            val sdf = SimpleDateFormat("MM/dd/yy HH:mm")
            val conv: Conversation = conversationList[position]
            holder.conversationTitle.text = conv.title
            if (conv.summary.length > 400){
                holder.conversationSummary.text = conv.summary.take(400) + "..."
            } else {
                holder.conversationSummary.text = conv.summary
            }
            holder.conversationDateModified.text = sdf.format(conv.dateModified)//conv.dateModified.toString()
            holder.conversationID.text = conv.conversationID.toString()
            holder.conversationTitle.setOnClickListener{
                it.isClickable = false
                val currentTime = System.currentTimeMillis()
                if (currentTime - lastClickTime >= COOLDOWN_TIME) {
                    lastClickTime = currentTime
                    onItemClick(conv.conversationID)
                } else {
                    Log.d(TAG, "Suppressing spam clicking...")
                }
                it.postDelayed({ it.isClickable = true }, COOLDOWN_TIME)
            }
        }

        override fun getItemCount(): Int {
            return conversationList.size
        }

        fun SetMessages(newMessages:List<Conversation>){
            conversationList = newMessages
            ChatManager.clearConversation()
            notifyDataSetChanged()
        }
}