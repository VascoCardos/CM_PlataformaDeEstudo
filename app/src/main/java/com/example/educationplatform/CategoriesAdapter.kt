package com.veducation.app

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class CategoriesAdapter(
    private val categories: List<Category>,
    private val onSubjectClick: (Subject) -> Unit,
    private val onFollowClick: (Subject) -> Unit
) : RecyclerView.Adapter<CategoriesAdapter.CategoryViewHolder>() {

    class CategoryViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvCategoryName: TextView = view.findViewById(R.id.tvCategoryName)
        val tvViewAll: TextView = view.findViewById(R.id.tvViewAll)
        val rvSubjects: RecyclerView = view.findViewById(R.id.rvSubjects)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_category, parent, false)
        return CategoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        val category = categories[position]
        
        holder.tvCategoryName.text = category.name
        holder.tvViewAll.text = "View all (${category.subjects.size})"
        
        // Set up subjects RecyclerView
        val subjectsAdapter = SubjectsAdapter(category.subjects, onSubjectClick, onFollowClick)
        holder.rvSubjects.layoutManager = LinearLayoutManager(
            holder.itemView.context, 
            LinearLayoutManager.HORIZONTAL, 
            false
        )
        holder.rvSubjects.adapter = subjectsAdapter
        
        // Handle "View all" click
        holder.tvViewAll.setOnClickListener {
            // TODO: Navigate to category detail screen
        }
    }

    override fun getItemCount() = categories.size
}
