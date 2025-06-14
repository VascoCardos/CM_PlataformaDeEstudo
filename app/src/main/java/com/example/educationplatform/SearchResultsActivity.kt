package com.veducation.app

import android.content.pm.ActivityInfo
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

class SearchResultsActivity : AppCompatActivity() {
    
    private lateinit var btnBack: ImageView
    private lateinit var btnProfile: ImageView
    private lateinit var tvSearchQuery: TextView
    private lateinit var tvResultsCount: TextView
    private lateinit var rvSearchResults: RecyclerView
    private lateinit var noResultsLayout: LinearLayout
    private lateinit var progressBar: ProgressBar
    
    private lateinit var categoriesAdapter: CategoriesAdapter
    private lateinit var sessionManager: SessionManager
    private val categoriesList = mutableListOf<Category>()
    
    private var searchQuery: String = ""
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Force portrait orientation
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        
        setContentView(R.layout.activity_search_results)
        
        sessionManager = SessionManager(this)
        searchQuery = intent.getStringExtra("search_query") ?: ""
        
        initViews()
        setupRecyclerView()
        setupListeners()
        loadSearchResults()
    }
    
    private fun initViews() {
        btnBack = findViewById(R.id.btnBack)
        btnProfile = findViewById(R.id.btnProfile)
        tvSearchQuery = findViewById(R.id.tvSearchQuery)
        tvResultsCount = findViewById(R.id.tvResultsCount)
        rvSearchResults = findViewById(R.id.rvSearchResults)
        noResultsLayout = findViewById(R.id.noResultsLayout)
        progressBar = findViewById(R.id.progressBar)
        
        tvSearchQuery.text = "Search results for: \"$searchQuery\""
    }
    
    private fun setupRecyclerView() {
        categoriesAdapter = CategoriesAdapter(
            categories = categoriesList,
            onSubjectClick = { subject -> showSubjectDetailsDialog(subject) },
            onFollowClick = { subject -> toggleFollowSubject(subject) }
        )
        rvSearchResults.layoutManager = LinearLayoutManager(this)
        rvSearchResults.adapter = categoriesAdapter
    }
    
    private fun setupListeners() {
        btnBack.setOnClickListener {
            finish()
        }
        
        btnProfile.setOnClickListener {
            val userName = sessionManager.getUserName() ?: "User"
            showToast("Hello, $userName!")
        }
    }
    
    private fun loadSearchResults() {
        Log.d("SearchResults", "Loading search results for: $searchQuery")
        progressBar.visibility = View.VISIBLE
        
        lifecycleScope.launch {
            try {
                val result = SupabaseClient.getSubjectsWithCategories()
                result.onSuccess { data ->
                    Log.d("SearchResults", "Data loaded successfully")
                    parseAndFilterData(data)
                    progressBar.visibility = View.GONE
                }.onFailure { error ->
                    Log.e("SearchResults", "Error loading data: ${error.message}")
                    showToast("Error loading data: ${error.message}")
                    progressBar.visibility = View.GONE
                    showNoResults()
                }
            } catch (e: Exception) {
                Log.e("SearchResults", "Exception loading data", e)
                showToast("Error: ${e.message}")
                progressBar.visibility = View.GONE
                showNoResults()
            }
        }
    }
    
    private fun parseAndFilterData(jsonData: String) {
        try {
            Log.d("SearchResults", "Parsing and filtering data...")
            val jsonArray = JSONArray(jsonData)
            val categoryMap = mutableMapOf<String, Category>()
            var totalResults = 0
            
            for (i in 0 until jsonArray.length()) {
                val item = jsonArray.getJSONObject(i)
                val subjectName = item.getString("name").lowercase()
                val subjectDescription = item.getString("description").lowercase()
                val categoryName = item.getString("category_name").lowercase()
                val searchTerms = searchQuery.lowercase().split(" ")
                
                // Check if any search term matches subject name, description, or category
                val matches = searchTerms.any { term ->
                    subjectName.contains(term) || 
                    subjectDescription.contains(term) || 
                    categoryName.contains(term)
                }
                
                if (matches) {
                    val categoryDisplayName = item.getString("category_name")
                    val categoryColor = item.getString("category_color")
                    
                    if (!categoryMap.containsKey(categoryDisplayName)) {
                        categoryMap[categoryDisplayName] = Category(
                            name = categoryDisplayName,
                            color = categoryColor,
                            subjects = mutableListOf()
                        )
                    }
                    
                    val subject = Subject(
                        id = item.getString("id"),
                        name = item.getString("name"),
                        description = item.getString("description"),
                        imageUrl = if (item.isNull("image_url")) null else item.getString("image_url"),
                        followersCount = item.getInt("followers_count"),
                        isFollowed = item.getBoolean("is_followed_by_current_user"),
                        isFeatured = item.getBoolean("is_featured"),
                        difficultyLevel = item.getInt("difficulty_level"),
                        estimatedHours = if (item.isNull("estimated_hours")) 0 else item.getInt("estimated_hours"),
                        categoryName = categoryDisplayName,
                        categoryColor = categoryColor
                    )
                    
                    categoryMap[categoryDisplayName]?.subjects?.add(subject)
                    totalResults++
                }
            }
            
            categoriesList.clear()
            categoriesList.addAll(categoryMap.values)
            
            Log.d("SearchResults", "Found $totalResults results in ${categoriesList.size} categories")
            
            updateResultsDisplay(totalResults)
            categoriesAdapter.notifyDataSetChanged()
            
        } catch (e: Exception) {
            Log.e("SearchResults", "Error parsing data", e)
            showToast("Error parsing search results")
            showNoResults()
        }
    }
    
    private fun updateResultsDisplay(totalResults: Int) {
        tvResultsCount.text = "$totalResults results found"
        
        if (totalResults == 0) {
            showNoResults()
        } else {
            rvSearchResults.visibility = View.VISIBLE
            noResultsLayout.visibility = View.GONE
        }
    }
    
    private fun showNoResults() {
        rvSearchResults.visibility = View.GONE
        noResultsLayout.visibility = View.VISIBLE
        tvResultsCount.text = "0 results found"
    }
    
    private fun showSubjectDetailsDialog(subject: Subject) {
        val dialog = SubjectDetailsDialog(this, subject) { updatedSubject ->
            toggleFollowSubject(updatedSubject)
        }
        dialog.show()
    }
    
    private fun toggleFollowSubject(subject: Subject) {
        lifecycleScope.launch {
            try {
                val result = SupabaseClient.toggleSubjectFollow(subject.id)
                result.onSuccess { response ->
                    val jsonResponse = JSONObject(response)
                    if (jsonResponse.getBoolean("success")) {
                        subject.isFollowed = jsonResponse.getBoolean("is_following")
                        subject.followersCount = jsonResponse.getInt("followers_count")
                        
                        categoriesAdapter.notifyDataSetChanged()
                        
                        val action = if (subject.isFollowed) "followed" else "unfollowed"
                        showToast("${subject.name} $action!")
                    }
                }.onFailure { error ->
                    showToast("Error: ${error.message}")
                }
            } catch (e: Exception) {
                showToast("Error: ${e.message}")
            }
        }
    }
    
    private fun showToast(message: String) {
        android.widget.Toast.makeText(this, message, android.widget.Toast.LENGTH_SHORT).show()
    }
}
