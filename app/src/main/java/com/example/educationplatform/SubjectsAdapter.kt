package com.veducation.app

import android.graphics.Color
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy

class SubjectsAdapter(
    private val subjects: List<Subject>,
    private val onSubjectClick: (Subject) -> Unit,
    private val onFollowClick: (Subject) -> Unit
) : RecyclerView.Adapter<SubjectsAdapter.SubjectViewHolder>() {

    class SubjectViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val cardView: CardView = view.findViewById(R.id.cardSubject)
        val tvSubjectName: TextView = view.findViewById(R.id.tvSubjectName)
        val tvFollowers: TextView = view.findViewById(R.id.tvFollowers)
        val btnFollow: ImageView = view.findViewById(R.id.btnFollow)
        val ivSubjectImage: ImageView = view.findViewById(R.id.ivSubjectImage)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SubjectViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_subject, parent, false)
        return SubjectViewHolder(view)
    }

    override fun onBindViewHolder(holder: SubjectViewHolder, position: Int) {
        val subject = subjects[position]

        holder.tvSubjectName.text = subject.name
        holder.tvFollowers.text = "${subject.followersCount} followers"

        // Load image with Glide
        if (!subject.imageUrl.isNullOrEmpty()) {
            Log.d("SubjectsAdapter", "Loading image: ${subject.imageUrl}")
            Glide.with(holder.itemView.context)
                .load(subject.imageUrl)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .placeholder(R.drawable.ic_subject_default)
                .error(R.drawable.ic_subject_default)
                .into(holder.ivSubjectImage)
        } else {
            Log.d("SubjectsAdapter", "No image URL for ${subject.name}, using default")
            holder.ivSubjectImage.setImageResource(R.drawable.ic_subject_default)
        }

        // Set card background color
        holder.cardView.setCardBackgroundColor(Color.WHITE)

        // Set follow button state
        if (subject.isFollowed) {
            holder.btnFollow.setImageResource(R.drawable.ic_check)
            holder.btnFollow.setColorFilter(Color.parseColor("#4CAF50"))
        } else {
            holder.btnFollow.setImageResource(R.drawable.ic_add)
            holder.btnFollow.setColorFilter(Color.parseColor("#FA7E01"))
        }

        // Handle follow button click
        holder.btnFollow.setOnClickListener {
            onFollowClick(subject)
        }

        // Handle card click
        holder.cardView.setOnClickListener {
            onSubjectClick(subject)
        }
    }

    override fun getItemCount() = subjects.size
}
