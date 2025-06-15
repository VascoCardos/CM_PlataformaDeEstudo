package com.veducation.app

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageView
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

class SubjectStudiesActivity : AppCompatActivity() {
    
    private lateinit var btnBack: ImageView
    private lateinit var tvSubjectName: TextView
    private lateinit var tvStudiesCount: TextView
    private lateinit var btnSortHot: TextView
    private lateinit var btnSortNew: TextView
    private lateinit var btnSortTop: TextView
    private lateinit var rvStudies: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var fabCreateStudy: FloatingActionButton
    
    private lateinit var studiesAdapter: StudiesAdapter
    private val studies = mutableListOf<Study>()
    
    private var subjectId: String = ""
    private var subjectName: String = ""
    private var currentSort: String = "hot"
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_subject_studies)
        
        // Get subject info from intent
        subjectId = intent.getStringExtra("subject_id") ?: ""
        subjectName = intent.getStringExtra("subject_name") ?: "Studies"
        
        if (subjectId.isEmpty()) {
            Toast.makeText(this, "Error: Subject not found", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        
        initViews()
        setupRecyclerView()
        setupSortButtons()
        loadStudies()
    }
    
    private fun initViews() {
        btnBack = findViewById(R.id.btnBack)
        tvSubjectName = findViewById(R.id.tvSubjectName)
        tvStudiesCount = findViewById(R.id.tvStudiesCount)
        btnSortHot = findViewById(R.id.btnSortHot)
        btnSortNew = findViewById(R.id.btnSortNew)
        btnSortTop = findViewById(R.id.btnSortTop)
        rvStudies = findViewById(R.id.rvStudies)
        progressBar = findViewById(R.id.progressBar)
        fabCreateStudy = findViewById(R.id.fabCreateStudy)
        
        tvSubjectName.text = "$subjectName Studies"
        
        btnBack.setOnClickListener { finish() }
        
        fabCreateStudy.setOnClickListener {
            val intent = Intent(this, CreateStudyActivity::class.java)
            intent.putExtra("subject_id", subjectId)
            intent.putExtra("subject_name", subjectName)
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
        btnSortHot.setOnClickListener { changeSortOrder("hot") }
        btnSortNew.setOnClickListener { changeSortOrder("new") }
        btnSortTop.setOnClickListener { changeSortOrder("top") }
    }
    
    private fun changeSortOrder(sortType: String) {
        if (currentSort == sortType) return
        
        currentSort = sortType
        updateSortButtonsUI()
        loadStudies()
    }
    
    private fun updateSortButtonsUI() {
        // Reset all buttons
        btnSortHot.setTextColor(getColor(android.R.color.darker_gray))
        btnSortNew.setTextColor(getColor(android.R.color.darker_gray))
        btnSortTop.setTextColor(getColor(android.R.color.darker_gray))
        
        // Highlight selected button
        when (currentSort) {
            "hot" -> btnSortHot.setTextColor(getColor(R.color.orange_start))
            "new" -> btnSortNew.setTextColor(getColor(R.color.orange_start))
            "top" -> btnSortTop.setTextColor(getColor(R.color.orange_start))
        }
    }
    
    private fun loadStudies() {
        Log.d("SubjectStudies", "Loading studies for subject: $subjectId, sort: $currentSort")
        
        progressBar.visibility = View.VISIBLE
        
        lifecycleScope.launch {
            try {
                val result = SupabaseClient.getStudiesBySubject(subjectId, currentSort)
                
                result.onSuccess { data ->
                    Log.d("SubjectStudies", "✅ Studies loaded successfully")
                    parseAndDisplayStudies(data)
                    progressBar.visibility = View.GONE
                }.onFailure { error ->
                    Log.e("SubjectStudies", "❌ Failed to load studies: ${error.message}")
                    Toast.makeText(this@SubjectStudiesActivity, "Error loading studies: ${error.message}", Toast.LENGTH_SHORT).show()
                    progressBar.visibility = View.GONE
                }
                
            } catch (e: Exception) {
                Log.e("SubjectStudies", "❌ Exception loading studies", e)
                Toast.makeText(this@SubjectStudiesActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                progressBar.visibility = View.GONE
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
                    subjectId = item.getString("subject_id"),
                    authorId = item.getString("author_id"),
                    authorName = item.getString("author_name"),
                    authorImageUrl = if (item.isNull("author_image_url")) null else item.getString("author_image_url"),
                    upvotesCount = item.getInt("upvotes_count"),
                    downvotesCount = item.getInt("downvotes_count"),
                    commentsCount = item.getInt("comments_count"),
                    viewsCount = item.getInt("views_count"),
                    createdAt = item.getString("created_at"),
                    updatedAt = item.getString("updated_at"),
                    userVote = if (item.isNull("user_vote")) null else item.getString("user_vote"),
                    isSaved = item.getBoolean("is_saved")
                )
                
                studies.add(study)
            }
            
            tvStudiesCount.text = "${studies.size} studies"
            studiesAdapter.notifyDataSetChanged()
            
            Log.d("SubjectStudies", "✅ Parsed ${studies.size} studies")
            
        } catch (e: Exception) {
            Log.e("SubjectStudies", "❌ Error parsing studies", e)
            Toast.makeText(this, "Error parsing studies: ${e.message}", Toast.LENGTH_SHORT).show()
        }
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
                    Toast.makeText(this@SubjectStudiesActivity, "Error voting: ${error.message}", Toast.LENGTH_SHORT).show()
                }
                
            } catch (e: Exception) {
                Toast.makeText(this@SubjectStudiesActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
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
                        Toast.makeText(this@SubjectStudiesActivity, message, Toast.LENGTH_SHORT).show()
                    }
                }.onFailure { error ->
                    Toast.makeText(this@SubjectStudiesActivity, "Error saving: ${error.message}", Toast.LENGTH_SHORT).show()
                }
                
            } catch (e: Exception) {
                Toast.makeText(this@SubjectStudiesActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
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
            putExtra(Intent.EXTRA_TEXT, "Check out this study: ${study.title}")
            type = "text/plain"
        }
        startActivity(Intent.createChooser(shareIntent, "Share Study"))
    }
    
    override fun onResume() {
        super.onResume()
        // Refresh studies when returning to this activity
        loadStudies()
    }
}
