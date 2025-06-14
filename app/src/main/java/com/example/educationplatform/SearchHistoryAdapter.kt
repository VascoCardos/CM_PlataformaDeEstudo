package com.veducation.app

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class SearchHistoryAdapter(
    private val searchTerms: MutableList<String>,
    private val onSearchClick: (String) -> Unit,
    private val onRemoveClick: (String) -> Unit
) : RecyclerView.Adapter<SearchHistoryAdapter.SearchHistoryViewHolder>() {

    class SearchHistoryViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvSearchTerm: TextView = view.findViewById(R.id.tvSearchTerm)
        val btnRemove: ImageView = view.findViewById(R.id.btnRemove)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SearchHistoryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_search_history, parent, false)
        return SearchHistoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: SearchHistoryViewHolder, position: Int) {
        val searchTerm = searchTerms[position]
        
        holder.tvSearchTerm.text = searchTerm
        
        // Handle search term click
        holder.itemView.setOnClickListener {
            onSearchClick(searchTerm)
        }
        
        // Handle remove click
        holder.btnRemove.setOnClickListener {
            onRemoveClick(searchTerm)
        }
    }

    override fun getItemCount() = searchTerms.size
    
    fun updateHistory(newHistory: List<String>) {
        searchTerms.clear()
        searchTerms.addAll(newHistory)
        notifyDataSetChanged()
    }
    
    fun removeItem(searchTerm: String) {
        val position = searchTerms.indexOf(searchTerm)
        if (position != -1) {
            searchTerms.removeAt(position)
            notifyItemRemoved(position)
        }
    }
}
