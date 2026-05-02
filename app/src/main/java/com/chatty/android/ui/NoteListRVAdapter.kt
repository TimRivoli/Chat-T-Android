package com.chatty.android.ui

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.chatty.android.R
import java.text.SimpleDateFormat
import kotlin.reflect.KFunction1
import com.chatty.android.etc.DataClasses.*

class NoteListRVAdapter(private val context: Context, private var noteList: List<NoteEntry>, private val onItemClick: KFunction1<Long, Unit>) :
    RecyclerView.Adapter<NoteListRVAdapter.ViewHolder>(){
    private val TAG = "NoteListRVAdapter"
    private val maxContentLength = 100

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val noteTitle: TextView = itemView.findViewById(R.id.txtTitleNoteList)
            val noteSummary: TextView = itemView.findViewById(R.id.txtContentNoteList)
            val noteDateModified: TextView = itemView.findViewById(R.id.txtLastModifiedNoteList)
        }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.note_list_entry, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        var lastClickTime = 0L
        val COOLDOWN_TIME: Long = 3000
        val sdf = SimpleDateFormat("MM/dd/yy HH:mm")
        val note: NoteEntry = noteList[position]
        holder.noteTitle.text = note.categoryName + ": " + note.title
        if (note.content.length > maxContentLength){
            holder.noteSummary.text = note.content.take(maxContentLength) + "..."
        } else {
            holder.noteSummary.text = note.content
        }
        holder.noteDateModified.text = sdf.format(note.dateModified)//conv.dateModified.toString()
        holder.noteTitle.setOnClickListener{
            it.isClickable = false
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastClickTime >= COOLDOWN_TIME) {
                lastClickTime = currentTime
                onItemClick(note.noteID)
            } else {
                Log.d(TAG, "Suppressing spam clicking...")
            }
            it.postDelayed({ it.isClickable = true }, COOLDOWN_TIME)
        }
    }

    override fun getItemCount(): Int {
        return noteList.size
    }

    fun updateData(newList:List<NoteEntry>){
        Log.d(TAG, "Refreshing RV contents")
        noteList = newList
        notifyDataSetChanged()
    }
}