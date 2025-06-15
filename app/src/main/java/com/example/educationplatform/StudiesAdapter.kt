package com.veducation.app

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.*

class StudiesAdapter(
    private val studies: MutableList<Study>,
    private val onStudyClick: (Study) -> Unit,
    private val onVoteClick: (Study, String) -> Unit,
    private val onCommentsClick: (Study) -> Unit,
    private val onShareClick: (Study) -> Unit,
    private val onSaveClick: (Study) -> Unit
) : RecyclerView.Adapter<StudiesAdapter.StudyViewHolder>() {

    class StudyViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val btnUpvote: ImageView = view.findViewById(R.id.btnUpvote)
        val btnDownvote: ImageView = view.findViewById(R.id.btnDownvote)
        val tvVoteCount: TextView = view.findViewById(R.id.tvVoteCount)
        val tvStudyType: TextView = view.findViewById(R.id.tvStudyType)
        val tvAuthor: TextView = view.findViewById(R.id.tvAuthor)
        val tvTimeAgo: TextView = view.findViewById(R.id.tvTimeAgo)
        val tvTitle: TextView = view.findViewById(R.id.tvTitle)
        val tvDescription: TextView = view.findViewById(R.id.tvDescription)
        val btnComments: LinearLayout = view.findViewById(R.id.btnComments)
        val tvCommentsCount: TextView = view.findViewById(R.id.tvCommentsCount)
        val btnShare: LinearLayout = view.findViewById(R.id.btnShare)
        val btnSave: ImageView = view.findViewById(R.id.btnSave)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StudyViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_study, parent, false)
        return StudyViewHolder(view)
    }

    override fun onBindViewHolder(holder: StudyViewHolder, position: Int) {
        val study = studies[position]
        
        // Set basic info
        holder.tvTitle.text = study.title
        holder.tvDescription.text = study.description ?: ""
        holder.tvAuthor.text = "by ${study.authorName}"
        holder.tvCommentsCount.text = study.commentsCount.toString()
        
        // Set study type
        holder.tvStudyType.text = study.studyType.replaceFirstChar { 
            if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() 
        }
        
        // Set time ago
        holder.tvTimeAgo.text = getTimeAgo(study.createdAt)
        
        // Set vote count and colors
        val voteCount = study.upvotesCount - study.downvotesCount
        holder.tvVoteCount.text = voteCount.toString()
        
        // Update vote button states
        when (study.userVote) {
            "upvote" -> {
                holder.btnUpvote.setColorFilter(Color.parseColor("#FF6B35"))
                holder.btnDownvote.setColorFilter(Color.parseColor("#666666"))
            }
            "downvote" -> {
                holder.btnUpvote.setColorFilter(Color.parseColor("#666666"))
                holder.btnDownvote.setColorFilter(Color.parseColor("#6B73FF"))
            }
            else -> {
                holder.btnUpvote.setColorFilter(Color.parseColor("#666666"))
                holder.btnDownvote.setColorFilter(Color.parseColor("#666666"))
            }
        }
        
        // Set save button state
        if (study.isSaved) {
            holder.btnSave.setColorFilter(Color.parseColor("#FFD700"))
        } else {
            holder.btnSave.setColorFilter(Color.parseColor("#666666"))
        }
        
        // Set click listeners
        holder.itemView.setOnClickListener { onStudyClick(study) }
        
        holder.btnUpvote.setOnClickListener { 
            val voteType = if (study.userVote == "upvote") "none" else "upvote"
            onVoteClick(study, voteType) 
        }
        
        holder.btnDownvote.setOnClickListener { 
            val voteType = if (study.userVote == "downvote") "none" else "downvote"
            onVoteClick(study, voteType) 
        }
        
        holder.btnComments.setOnClickListener { onCommentsClick(study) }
        holder.btnShare.setOnClickListener { onShareClick(study) }
        holder.btnSave.setOnClickListener { onSaveClick(study) }
    }

    override fun getItemCount() = studies.size
    
    fun updateStudy(updatedStudy: Study) {
        val index = studies.indexOfFirst { it.id == updatedStudy.id }
        if (index != -1) {
            studies[index] = updatedStudy
            notifyItemChanged(index)
        }
    }
    
    private fun getTimeAgo(createdAt: String): String {
        return try {
            val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
            val date = format.parse(createdAt)
            val now = Date()
            val diff = now.time - (date?.time ?: 0)
            
            val seconds = diff / 1000
            val minutes = seconds / 60
            val hours = minutes / 60
            val days = hours / 24
            
            when {
                days > 0 -> "${days}d ago"
                hours > 0 -> "${hours}h ago"
                minutes > 0 -> "${minutes}m ago"
                else -> "now"
            }
        } catch (e: Exception) {
            "recently"
        }
    }
}
