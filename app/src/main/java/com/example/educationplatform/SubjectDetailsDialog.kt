package com.veducation.app

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.ViewGroup
import android.view.Window
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy

class SubjectDetailsDialog(
    context: Context,
    private val subject: Subject,
    private val onFollowClick: (Subject) -> Unit
) : Dialog(context) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Remove default dialog styling
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.dialog_subject_details)

        // Make dialog responsive to content
        window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )

        // Set dialog background to transparent so our custom background shows
        window?.setBackgroundDrawableResource(android.R.color.transparent)

        setupDialog()
    }

    private fun setupDialog() {
        val ivSubjectImage = findViewById<ImageView>(R.id.ivSubjectImage)
        val tvSubjectName = findViewById<TextView>(R.id.tvSubjectName)
        val tvDescription = findViewById<TextView>(R.id.tvDescription)
        val tvCategory = findViewById<TextView>(R.id.tvCategory)
        val tvFollowers = findViewById<TextView>(R.id.tvFollowers)
        val tvDifficulty = findViewById<TextView>(R.id.tvDifficulty)
        val tvEstimatedHours = findViewById<TextView>(R.id.tvEstimatedHours)
        val btnFollow = findViewById<Button>(R.id.btnFollow)
        val btnClose = findViewById<Button>(R.id.btnClose)

        // Set subject data
        tvSubjectName.text = subject.name
        tvDescription.text = subject.description
        tvCategory.text = subject.categoryName
        tvFollowers.text = "${subject.followersCount} followers"
        tvDifficulty.text = "Difficulty: ${getDifficultyText(subject.difficultyLevel)}"
        tvEstimatedHours.text = "Estimated: ${subject.estimatedHours}h"

        // Load image with Glide
        if (!subject.imageUrl.isNullOrEmpty()) {
            Log.d("SubjectDialog", "Loading image: ${subject.imageUrl}")
            Glide.with(context)
                .load(subject.imageUrl)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .placeholder(R.drawable.ic_subject_default)
                .error(R.drawable.ic_subject_default)
                .into(ivSubjectImage)
        } else {
            Log.d("SubjectDialog", "No image URL for ${subject.name}, using default")
            ivSubjectImage.setImageResource(R.drawable.ic_subject_default)
        }

        // Set follow button
        updateFollowButton(btnFollow)

        // Handle clicks
        btnFollow.setOnClickListener {
            onFollowClick(subject)
            updateFollowButton(btnFollow)
        }

        btnClose.setOnClickListener {
            dismiss()
        }
    }

    private fun updateFollowButton(btnFollow: Button) {
        if (subject.isFollowed) {
            btnFollow.text = "Unfollow"
            btnFollow.setBackgroundResource(R.drawable.button_unfollow)
        } else {
            btnFollow.text = "Follow"
            btnFollow.setBackgroundResource(R.drawable.gradient_button)
        }
    }

    private fun getDifficultyText(level: Int): String {
        return when (level) {
            1 -> "Beginner"
            2 -> "Easy"
            3 -> "Intermediate"
            4 -> "Advanced"
            5 -> "Expert"
            else -> "Unknown"
        }
    }
}
