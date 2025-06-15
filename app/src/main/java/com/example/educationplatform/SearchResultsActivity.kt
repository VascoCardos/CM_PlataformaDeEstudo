package com.veducation.app

import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import de.hdodenhof.circleimageview.CircleImageView
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

class SearchResultsActivity : AppCompatActivity() {
    
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var btnMenu: ImageView
    private lateinit var btnProfilePhoto: CircleImageView
    private lateinit var tvSearchQuery: TextView
    private lateinit var tvResultsCount: TextView
    private lateinit var rvSearchResults: RecyclerView
    private lateinit var noResultsLayout: LinearLayout
    private lateinit var progressBar: ProgressBar
    
    // Bottom navigation
    private lateinit var btnHome: LinearLayout
    private lateinit var btnSearch: LinearLayout
    private lateinit var btnCreate: LinearLayout
    private lateinit var btnBookmarks: LinearLayout
    private lateinit var btnProfile: LinearLayout
    
    // Drawer elements
    private lateinit var btnCloseDrawer: ImageView
    private lateinit var ivDrawerProfileImage: CircleImageView
    private lateinit var tvDrawerUserName: TextView
    private lateinit var tvDrawerUserEmail: TextView
    private lateinit var btnDrawerEditProfile: TextView
    private lateinit var btnDrawerLogout: Button
    
    // Menu items
    private lateinit var menuMyProfile: LinearLayout
    private lateinit var menuSettings: LinearLayout
    private lateinit var menuStatistics: LinearLayout
    private lateinit var menuHelp: LinearLayout
    private lateinit var menuActivityHistory: LinearLayout
    private lateinit var menuPrivacyPolicy: LinearLayout
    
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
        setupDrawer()
        setupBottomNavigation()
        setupRecyclerView()
        setupListeners()
        setupProfileButton()
        loadUserData()
        loadSearchResults()
    }
    
    private fun initViews() {
        // Main views
        drawerLayout = findViewById(R.id.drawerLayout)
        btnMenu = findViewById(R.id.btnMenu)
        btnProfilePhoto = findViewById(R.id.btnProfilePhoto)
        tvSearchQuery = findViewById(R.id.tvSearchQuery)
        tvResultsCount = findViewById(R.id.tvResultsCount)
        rvSearchResults = findViewById(R.id.rvSearchResults)
        noResultsLayout = findViewById(R.id.noResultsLayout)
        progressBar = findViewById(R.id.progressBar)
        
        // Bottom navigation
        btnHome = findViewById(R.id.btnHome)
        btnSearch = findViewById(R.id.btnSearch)
        btnCreate = findViewById(R.id.btnCreate)
        btnBookmarks = findViewById(R.id.btnBookmarks)
        btnProfile = findViewById(R.id.btnProfile)
        
        // Drawer views
        btnCloseDrawer = findViewById(R.id.btnCloseDrawer)
        ivDrawerProfileImage = findViewById(R.id.ivDrawerProfileImage)
        tvDrawerUserName = findViewById(R.id.tvDrawerUserName)
        tvDrawerUserEmail = findViewById(R.id.tvDrawerUserEmail)
        btnDrawerEditProfile = findViewById(R.id.btnDrawerEditProfile)
        btnDrawerLogout = findViewById(R.id.btnDrawerLogout)
        
        // Menu items
        menuMyProfile = findViewById(R.id.menuMyProfile)
        menuSettings = findViewById(R.id.menuSettings)
        menuStatistics = findViewById(R.id.menuStatistics)
        menuHelp = findViewById(R.id.menuHelp)
        menuActivityHistory = findViewById(R.id.menuActivityHistory)
        menuPrivacyPolicy = findViewById(R.id.menuPrivacyPolicy)
        
        tvSearchQuery.text = "Search results for: \"$searchQuery\""
    }
    
    private fun setupBottomNavigation() {
        btnHome.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            startActivity(intent)
            finish()
        }
        
        btnSearch.setOnClickListener {
            val intent = Intent(this, SearchActivity::class.java)
            startActivity(intent)
        }
        
        btnCreate.setOnClickListener {
            showToast("Create feature coming soon!")
        }
        
        btnBookmarks.setOnClickListener {
            showToast("Bookmarks feature coming soon!")
        }
        
        btnProfile.setOnClickListener {
            val intent = Intent(this, ProfileActivity::class.java)
            startActivity(intent)
        }
    }
    
    private fun setupProfileButton() {
        btnProfilePhoto.setOnClickListener {
            val intent = Intent(this, ProfileActivity::class.java)
            startActivity(intent)
        }
    }
    
    private fun setupDrawer() {
        // Open drawer
        btnMenu.setOnClickListener {
            drawerLayout.openDrawer(GravityCompat.START)
        }
        
        // Close drawer
        btnCloseDrawer.setOnClickListener {
            drawerLayout.closeDrawer(GravityCompat.START)
        }
        
        // Menu item clicks
        menuMyProfile.setOnClickListener {
            val intent = Intent(this, ProfileActivity::class.java)
            startActivity(intent)
            drawerLayout.closeDrawer(GravityCompat.START)
        }
        
        menuSettings.setOnClickListener {
            showToast("Settings clicked")
            drawerLayout.closeDrawer(GravityCompat.START)
        }
        
        menuStatistics.setOnClickListener {
            showToast("Statistics clicked")
            drawerLayout.closeDrawer(GravityCompat.START)
        }
        
        menuHelp.setOnClickListener {
            showToast("Help clicked")
            drawerLayout.closeDrawer(GravityCompat.START)
        }
        
        menuActivityHistory.setOnClickListener {
            showToast("Activity History clicked")
            drawerLayout.closeDrawer(GravityCompat.START)
        }
        
        menuPrivacyPolicy.setOnClickListener {
            showToast("Privacy Policy clicked")
            drawerLayout.closeDrawer(GravityCompat.START)
        }
        
        btnDrawerEditProfile.setOnClickListener {
            val intent = Intent(this, ProfileActivity::class.java)
            startActivity(intent)
            drawerLayout.closeDrawer(GravityCompat.START)
        }
        
        // Logout do drawer
        btnDrawerLogout.setOnClickListener { logout() }
    }
    
    private fun loadUserData() {
        Log.d("SearchResultsActivity", "Loading user data from database")
        
        lifecycleScope.launch {
            try {
                val result = SupabaseClient.getUserProfile()
                
                result.onSuccess { data ->
                    Log.d("SearchResultsActivity", "User profile loaded successfully")
                    parseUserProfile(data)
                }.onFailure { error ->
                    Log.e("SearchResultsActivity", "Failed to load user profile: ${error.message}")
                    loadDefaultUserData()
                }
                
            } catch (e: Exception) {
                Log.e("SearchResultsActivity", "Exception loading user profile", e)
                loadDefaultUserData()
            }
        }
    }
    
    private fun parseUserProfile(jsonData: String) {
        try {
            val jsonObject = JSONObject(jsonData)
            
            val name = jsonObject.optString("name", sessionManager.getUserName() ?: "")
            val email = jsonObject.optString("email", sessionManager.getUserEmail() ?: "")
            val profileImageUrl = jsonObject.optString("profile_image_url", null)
            
            // Update drawer views
            tvDrawerUserName.text = if (name.isNotEmpty()) name else (sessionManager.getUserName() ?: "User")
            tvDrawerUserEmail.text = if (email.isNotEmpty()) email else (sessionManager.getUserEmail() ?: "")
            
            // Load profile images (both header and drawer)
            if (!profileImageUrl.isNullOrEmpty() && profileImageUrl != "null") {
                loadHeaderProfileImage(profileImageUrl)
                loadDrawerProfileImage(profileImageUrl)
            } else {
                btnProfilePhoto.setImageResource(R.drawable.ic_person)
                ivDrawerProfileImage.setImageResource(R.drawable.ic_person)
            }
            
            Log.d("SearchResultsActivity", "User profile parsed successfully")
            
        } catch (e: Exception) {
            Log.e("SearchResultsActivity", "Error parsing user profile data", e)
            loadDefaultUserData()
        }
    }
    
    private fun loadDefaultUserData() {
        val userName = sessionManager.getUserName() ?: "User"
        val userEmail = sessionManager.getUserEmail() ?: "user@example.com"
        
        tvDrawerUserName.text = userName
        tvDrawerUserEmail.text = userEmail
        btnProfilePhoto.setImageResource(R.drawable.ic_person)
        ivDrawerProfileImage.setImageResource(R.drawable.ic_person)
    }
    
    private fun loadHeaderProfileImage(imageUrl: String) {
        Glide.with(this)
            .load(imageUrl)
            .placeholder(R.drawable.ic_person)
            .error(R.drawable.ic_person)
            .into(btnProfilePhoto)
    }
    
    private fun loadDrawerProfileImage(imageUrl: String) {
        Glide.with(this)
            .load(imageUrl)
            .placeholder(R.drawable.ic_person)
            .error(R.drawable.ic_person)
            .into(ivDrawerProfileImage)
    }
    
    private fun setupRecyclerView() {
        categoriesAdapter = CategoriesAdapter(
            categories = categoriesList,
            onSubjectClick = { subject -> showSubjectDetailsDialog(subject) },
            onFollowClick = { subject -> toggleFollowSubject(subject) },
            onViewStudiesClick = { subject -> 
                val intent = Intent(this, SubjectStudiesActivity::class.java)
                intent.putExtra("subject_id", subject.id)
                intent.putExtra("subject_name", subject.name)
                startActivity(intent)
            },
            onViewSessionsClick = { subject -> 
                showToast("Sessions for ${subject.name} - Coming soon!")
            }
        )
        rvSearchResults.layoutManager = LinearLayoutManager(this)
        rvSearchResults.adapter = categoriesAdapter
    }
    
    private fun setupListeners() {
        // No additional listeners needed
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
    
    private fun logout() {
        // Clear session
        sessionManager.logout()
        
        // Redirect to login
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
        showToast("Logged out successfully")
    }
    
    override fun onResume() {
        super.onResume()
        // Refresh user data when returning to activity
        loadUserData()
    }
    
    override fun onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }
    
    private fun showToast(message: String) {
        android.widget.Toast.makeText(this, message, android.widget.Toast.LENGTH_SHORT).show()
    }
}
