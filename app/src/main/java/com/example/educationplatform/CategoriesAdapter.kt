package com.veducation.app

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class CategoriesAdapter(
    private val categories: List<Category>,
    private val onSubjectClick: (Subject) -> Unit,
    private val onFollowClick: (Subject) -> Unit,
    private val onViewStudiesClick: (Subject) -> Unit,
    private val onViewSessionsClick: (Subject) -> Unit
) : RecyclerView.Adapter<CategoriesAdapter.CategoryViewHolder>() {

    class CategoryViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvCategoryName: TextView = view.findViewById(R.id.tvCategoryName)
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
        
        // Setup horizontal RecyclerView for subjects
        val subjectsAdapter = SubjectsAdapter(
            subjects = category.subjects,
            onSubjectClick = onSubjectClick,
            onFollowClick = onFollowClick,
            onViewStudiesClick = onViewStudiesClick,
            onViewSessionsClick = onViewSessionsClick
        )
        
        holder.rvSubjects.layoutManager = LinearLayoutManager(
            holder.itemView.context, 
            LinearLayoutManager.HORIZONTAL, 
            false
        )
        holder.rvSubjects.adapter = subjectsAdapter
    }

    override fun getItemCount() = categories.size
}
