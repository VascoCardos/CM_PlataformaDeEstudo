package com.veducation.app

import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import de.hdodenhof.circleimageview.CircleImageView
import kotlinx.coroutines.launch
import org.json.JSONObject

class SearchActivity : AppCompatActivity() {
    
    private lateinit var btnBack: ImageView
    private lateinit var btnProfilePhoto: CircleImageView
    private lateinit var etSearch: EditText
    private lateinit var rvSearchHistory: RecyclerView
    private lateinit var tvNoHistory: TextView
    
    private lateinit var searchHistoryManager: SearchHistoryManager
    private lateinit var searchHistoryAdapter: SearchHistoryAdapter
    private lateinit var sessionManager: SessionManager
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Force portrait orientation
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        
        setContentView(R.layout.activity_search)
        
        searchHistoryManager = SearchHistoryManager(this)
        sessionManager = SessionManager(this)
        
        initViews()
        setupSearchHistory()
        setupListeners()
        setupProfileButton()
        loadUserProfileImage()
        
        // Focus on search field and show keyboard
        etSearch.requestFocus()
    }
    
    private fun initViews() {
        btnBack = findViewById(R.id.btnBack)
        btnProfilePhoto = findViewById(R.id.btnProfilePhoto)
        etSearch = findViewById(R.id.etSearch)
        rvSearchHistory = findViewById(R.id.rvSearchHistory)
        tvNoHistory = findViewById(R.id.tvNoHistory)
    }
    
    private fun setupProfileButton() {
        btnProfilePhoto.setOnClickListener {
            val intent = Intent(this, ProfileActivity::class.java)
            startActivity(intent)
        }
    }
    
    private fun loadUserProfileImage() {
        Log.d("SearchActivity", "Loading user profile image")
        
        lifecycleScope.launch {
            try {
                val result = SupabaseClient.getUserProfile()
                
                result.onSuccess { data ->
                    Log.d("SearchActivity", "User profile loaded successfully")
                    parseUserProfile(data)
                }.onFailure { error ->
                    Log.e("SearchActivity", "Failed to load user profile: ${error.message}")
                    btnProfilePhoto.setImageResource(R.drawable.ic_person)
                }
                
            } catch (e: Exception) {
                Log.e("SearchActivity", "Exception loading user profile", e)
                btnProfilePhoto.setImageResource(R.drawable.ic_person)
            }
        }
    }
    
    private fun parseUserProfile(jsonData: String) {
        try {
            val jsonObject = JSONObject(jsonData)
            val profileImageUrl = jsonObject.optString("profile_image_url", null)
            
            // Load profile image
            if (!profileImageUrl.isNullOrEmpty() && profileImageUrl != "null") {
                loadProfileImage(profileImageUrl)
            } else {
                btnProfilePhoto.setImageResource(R.drawable.ic_person)
            }
            
            Log.d("SearchActivity", "User profile parsed successfully")
            
        } catch (e: Exception) {
            Log.e("SearchActivity", "Error parsing user profile data", e)
            btnProfilePhoto.setImageResource(R.drawable.ic_person)
        }
    }
    
    private fun loadProfileImage(imageUrl: String) {
        Glide.with(this)
            .load(imageUrl)
            .placeholder(R.drawable.ic_person)
            .error(R.drawable.ic_person)
            .into(btnProfilePhoto)
    }
    
    private fun setupSearchHistory() {
        val searchHistory = searchHistoryManager.getSearchHistory().toMutableList()
        
        searchHistoryAdapter = SearchHistoryAdapter(
            searchTerms = searchHistory,
            onSearchClick = { searchTerm ->
                performSearch(searchTerm)
            },
            onRemoveClick = { searchTerm ->
                removeSearchTerm(searchTerm)
            }
        )
        
        rvSearchHistory.layoutManager = LinearLayoutManager(this)
        rvSearchHistory.adapter = searchHistoryAdapter
        
        updateHistoryVisibility()
    }
    
    private fun setupListeners() {
        btnBack.setOnClickListener {
            finish()
        }
        
        // Handle search when user presses enter or search button
        etSearch.setOnEditorActionListener { _, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH || 
                (event != null && event.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN)) {
                val query = etSearch.text.toString().trim()
                if (query.isNotEmpty()) {
                    performSearch(query)
                }
                true
            } else {
                false
            }
        }
    }
    
    private fun performSearch(query: String) {
        // Add to search history
        searchHistoryManager.addSearchTerm(query)
        
        // Navigate to search results
        val intent = Intent(this, SearchResultsActivity::class.java)
        intent.putExtra("search_query", query)
        startActivity(intent)
    }
    
    private fun removeSearchTerm(searchTerm: String) {
        searchHistoryManager.removeSearchTerm(searchTerm)
        searchHistoryAdapter.removeItem(searchTerm)
        updateHistoryVisibility()
        showToast("Removed from history")
    }
    
    private fun updateHistoryVisibility() {
        val hasHistory = searchHistoryManager.getSearchHistory().isNotEmpty()
        rvSearchHistory.visibility = if (hasHistory) View.VISIBLE else View.GONE
        tvNoHistory.visibility = if (hasHistory) View.GONE else View.VISIBLE
    }
    
    override fun onResume() {
        super.onResume()
        // Refresh search history when returning from search results
        val updatedHistory = searchHistoryManager.getSearchHistory()
        searchHistoryAdapter.updateHistory(updatedHistory)
        updateHistoryVisibility()
        
        // Refresh profile image
        loadUserProfileImage()
    }
    
    private fun showToast(message: String) {
        android.widget.Toast.makeText(this, message, android.widget.Toast.LENGTH_SHORT).show()
    }
}
