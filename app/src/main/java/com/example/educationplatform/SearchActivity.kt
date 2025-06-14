package com.veducation.app

import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class SearchActivity : AppCompatActivity() {
    
    private lateinit var btnBack: ImageView
    private lateinit var btnProfile: ImageView
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
        
        // Focus on search field and show keyboard
        etSearch.requestFocus()
    }
    
    private fun initViews() {
        btnBack = findViewById(R.id.btnBack)
        btnProfile = findViewById(R.id.btnProfile)
        etSearch = findViewById(R.id.etSearch)
        rvSearchHistory = findViewById(R.id.rvSearchHistory)
        tvNoHistory = findViewById(R.id.tvNoHistory)
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
        
        btnProfile.setOnClickListener {
            val userName = sessionManager.getUserName() ?: "User"
            showToast("Hello, $userName!")
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
    }
    
    private fun showToast(message: String) {
        android.widget.Toast.makeText(this, message, android.widget.Toast.LENGTH_SHORT).show()
    }
}
