package com.veducation.app

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

class MyStudiesActivity : AppCompatActivity() {

    private lateinit var btnBack: ImageView
    private lateinit var tvStudiesCount: TextView
    private lateinit var btnSortNew: TextView
    private lateinit var btnSortHot: TextView
    private lateinit var btnSortTop: TextView
    private lateinit var rvStudies: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var emptyState: LinearLayout
    private lateinit var btnCreateFirstStudy: Button
    private lateinit var fabCreateStudy: FloatingActionButton

    private lateinit var studiesAdapter: StudiesAdapter
    private val studies = mutableListOf<Study>()

    private var currentSort: String = "new"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_my_studies)

        initViews()
        setupRecyclerView()
        setupSortButtons()
        loadMyStudies()
    }

    private fun initViews() {
        btnBack = findViewById(R.id.btnBack)
        tvStudiesCount = findViewById(R.id.tvStudiesCount)
        btnSortNew = findViewById(R.id.btnSortNew)
        btnSortHot = findViewById(R.id.btnSortHot)
        btnSortTop = findViewById(R.id.btnSortTop)
        rvStudies = findViewById(R.id.rvStudies)
        progressBar = findViewById(R.id.progressBar)
        emptyState = findViewById(R.id.emptyState)
        btnCreateFirstStudy = findViewById(R.id.btnCreateFirstStudy)
        fabCreateStudy = findViewById(R.id.fabCreateStudy)

        btnBack.setOnClickListener { finish() }

        fabCreateStudy.setOnClickListener {
            val intent = Intent(this, CreateStudyActivity::class.java)
            startActivity(intent)
        }

        btnCreateFirstStudy.setOnClickListener {
            val intent = Intent(this, CreateStudyActivity::class.java)
            startActivity(intent)
        }
    }

    private fun setupRecyclerView() {
        studiesAdapter = StudiesAdapter(
            studies = studies,
            onStudyClick = { study -> openStudyDetails(study) },
            onVoteClick = { study, voteType -> voteOnStudy(study, voteType) },
            onCommentsClick = { study -> openComments(study) },
            onShareClick = { study -> shareStudy(study) },
            onSaveClick = { study -> toggleSaveStudy(study) }
        )

        rvStudies.layoutManager = LinearLayoutManager(this)
        rvStudies.adapter = studiesAdapter
    }

    private fun setupSortButtons() {
        btnSortNew.setOnClickListener { changeSortOrder("new") }
        btnSortHot.setOnClickListener { changeSortOrder("hot") }
        btnSortTop.setOnClickListener { changeSortOrder("top") }
    }

    private fun changeSortOrder(sortType: String) {
        if (currentSort == sortType) return

        currentSort = sortType
        updateSortButtonsUI()
        loadMyStudies()
    }

    private fun updateSortButtonsUI() {
        // Reset all buttons
        btnSortNew.setTextColor(getColor(android.R.color.darker_gray))
        btnSortHot.setTextColor(getColor(android.R.color.darker_gray))
        btnSortTop.setTextColor(getColor(android.R.color.darker_gray))

        // Highlight selected button
        when (currentSort) {
            "new" -> btnSortNew.setTextColor(getColor(R.color.orange_start))
            "hot" -> btnSortHot.setTextColor(getColor(R.color.orange_start))
            "top" -> btnSortTop.setTextColor(getColor(R.color.orange_start))
        }
    }

    private fun loadMyStudies() {
        Log.d("MyStudies", "Loading my studies, sort: $currentSort")

        progressBar.visibility = View.VISIBLE
        emptyState.visibility = View.GONE

        lifecycleScope.launch {
            try {
                val result = SupabaseClient.getMyStudies(currentSort)

                result.onSuccess { data ->
                    Log.d("MyStudies", "✅ My studies loaded successfully")
                    parseAndDisplayStudies(data)
                    progressBar.visibility = View.GONE
                }.onFailure { error ->
                    Log.e("MyStudies", "❌ Failed to load my studies: ${error.message}")
                    Toast.makeText(this@MyStudiesActivity, "Error loading studies: ${error.message}", Toast.LENGTH_SHORT).show()
                    progressBar.visibility = View.GONE
                    showEmptyState()
                }

            } catch (e: Exception) {
                Log.e("MyStudies", "❌ Exception loading my studies", e)
                Toast.makeText(this@MyStudiesActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                progressBar.visibility = View.GONE
                showEmptyState()
            }
        }
    }

    private fun parseAndDisplayStudies(jsonData: String) {
        try {
            val jsonArray = JSONArray(jsonData)
            studies.clear()

            for (i in 0 until jsonArray.length()) {
                val item = jsonArray.getJSONObject(i)

                val study = Study(
                    id = item.getString("id"),
                    title = item.getString("title"),
                    description = if (item.isNull("description")) null else item.getString("description"),
                    content = item.getString("content"),
                    studyType = item.getString("study_type"),
                    subjectId = if (item.isNull("subject_id")) "" else item.getString("subject_id"),
                    authorId = item.getString("author_id"),
                    authorName = item.getString("author_name"),
                    authorImageUrl = if (item.isNull("author_image_url")) "" else item.getString("author_image_url"),
                    upvotesCount = item.getInt("upvotes_count"),
                    downvotesCount = item.getInt("downvotes_count"),
                    commentsCount = item.getInt("comments_count"),
                    viewsCount = item.getInt("views_count"),
                    createdAt = item.getString("created_at"),
                    updatedAt = item.getString("updated_at"),
                    userVote = if (item.isNull("user_vote")) "" else item.getString("user_vote"),
                    isSaved = item.getBoolean("is_saved")
                )

                studies.add(study)
            }

            tvStudiesCount.text = "${studies.size} studies"

            if (studies.isEmpty()) {
                showEmptyState()
            } else {
                emptyState.visibility = View.GONE
                studiesAdapter.notifyDataSetChanged()
            }

            Log.d("MyStudies", "✅ Parsed ${studies.size} studies")

        } catch (e: Exception) {
            Log.e("MyStudies", "❌ Error parsing studies", e)
            Toast.makeText(this, "Error parsing studies: ${e.message}", Toast.LENGTH_SHORT).show()
            showEmptyState()
        }
    }

    private fun showEmptyState() {
        emptyState.visibility = View.VISIBLE
        rvStudies.visibility = View.GONE
    }

    private fun voteOnStudy(study: Study, voteType: String) {
        lifecycleScope.launch {
            try {
                val result = SupabaseClient.voteStudy(study.id, voteType)

                result.onSuccess { response ->
                    val jsonResponse = JSONObject(response)
                    if (jsonResponse.getBoolean("success")) {
                        study.upvotesCount = jsonResponse.getInt("upvotes_count")
                        study.downvotesCount = jsonResponse.getInt("downvotes_count")
                        study.userVote = if (jsonResponse.isNull("user_vote")) null else jsonResponse.getString("user_vote")

                        studiesAdapter.updateStudy(study)
                    }
                }.onFailure { error ->
                    Toast.makeText(this@MyStudiesActivity, "Error voting: ${error.message}", Toast.LENGTH_SHORT).show()
                }

            } catch (e: Exception) {
                Toast.makeText(this@MyStudiesActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun toggleSaveStudy(study: Study) {
        lifecycleScope.launch {
            try {
                val result = SupabaseClient.toggleStudySave(study.id)

                result.onSuccess { response ->
                    val jsonResponse = JSONObject(response)
                    if (jsonResponse.getBoolean("success")) {
                        study.isSaved = jsonResponse.getBoolean("is_saved")
                        studiesAdapter.updateStudy(study)

                        val message = if (study.isSaved) "Study saved!" else "Study unsaved!"
                        Toast.makeText(this@MyStudiesActivity, message, Toast.LENGTH_SHORT).show()
                    }
                }.onFailure { error ->
                    Toast.makeText(this@MyStudiesActivity, "Error saving: ${error.message}", Toast.LENGTH_SHORT).show()
                }

            } catch (e: Exception) {
                Toast.makeText(this@MyStudiesActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun openStudyDetails(study: Study) {
        Toast.makeText(this, "Opening study: ${study.title}", Toast.LENGTH_SHORT).show()
        // TODO: Navigate to study details activity
    }

    private fun openComments(study: Study) {
        Toast.makeText(this, "Comments: ${study.commentsCount} comments", Toast.LENGTH_SHORT).show()
        // TODO: Implement comments screen later
    }

    private fun shareStudy(study: Study) {
        val shareIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, "Check out my study: ${study.title}")
            type = "text/plain"
        }
        startActivity(Intent.createChooser(shareIntent, "Share Study"))
    }

    override fun onResume() {
        super.onResume()
        // Refresh studies when returning to this activity
        loadMyStudies()
    }
}
