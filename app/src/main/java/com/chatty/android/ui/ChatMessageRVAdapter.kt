package com.chatty.android.ui

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.chatty.android.R
import com.chatty.android.etc.DataClasses.*
import io.noties.markwon.Markwon

class ChatMessageRVAdapterSwipeCallBack(private val adapter: RecyclerView.Adapter<*>, private val swipeListener: ChatMessageRVAdapter.SwipeListener) :
    ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT ) {
    override fun onMove(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        target: RecyclerView.ViewHolder
    ): Boolean {
        return false
    }

    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
        val position = viewHolder.adapterPosition
        adapter.notifyItemRemoved(position)
        swipeListener?.onItemSwiped(position)
    }
}

class ChatMessageRVAdapter(private val context: Context, private var messageList: ArrayList<ChatMessageExtended>, private val onClickListener: (String, Boolean) -> Unit) :
    RecyclerView.Adapter<ChatMessageRVAdapter.ViewHolder>() {
    val TAG = "ChatMessageRVAdapter"
    private val markwon = Markwon.create(context)
    var swipeListener: SwipeListener? = null
    var lastClickTime = 0L
    val COOLDOWN_TIME: Long = 2500

    interface SwipeListener {
        fun onItemSwiped(position: Int)
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val chatMessageItem: TextView = itemView.findViewById(R.id.chatMessageItem)
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.chat_message, parent, false)
        return ViewHolder(view)
    }

    private fun onClickListener(it: View, text:String, isUser: Boolean) {
        it.isClickable = false
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastClickTime >= COOLDOWN_TIME) {
            lastClickTime = currentTime
            onClickListener(text, isUser)
        } else {
            Log.d(TAG, "Suppressing spam clicking...")
        }
        it.postDelayed({ it.isClickable = true }, COOLDOWN_TIME)

    }
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val text = messageList[position].content
        val isAssistant = messageList[position].role == "assistant"
        if (isAssistant) {
            holder.chatMessageItem.setBackgroundColor(ContextCompat.getColor(context, R.color.responseBackground))
            holder.chatMessageItem.textAlignment = View.TEXT_ALIGNMENT_TEXT_START
            markwon.setMarkdown(holder.chatMessageItem, text)
        } else {
            holder.chatMessageItem.textAlignment = View.TEXT_ALIGNMENT_CENTER
            holder.chatMessageItem.setBackgroundColor(ContextCompat.getColor(context, R.color.promptBackground))
            holder.chatMessageItem.text = text
        }
        holder.chatMessageItem.setOnClickListener { onClickListener(it, text, !isAssistant) }
    }

    override fun getItemCount(): Int {
        return messageList.size
    }

    fun SetMessages(newMessages :ArrayList<ChatMessageExtended>){
        messageList = newMessages
        notifyDataSetChanged()
    }
}