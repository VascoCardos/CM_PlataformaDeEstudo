package com.veducation.app

import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

class MainActivity : AppCompatActivity() {

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var btnMenu: ImageView
    private lateinit var btnProfile: ImageView
    private lateinit var etSearch: EditText
    private lateinit var rvPopular: RecyclerView
    private lateinit var rvCategories: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var btnLogout: Button
    private lateinit var sessionManager: SessionManager

    // Drawer elements
    private lateinit var btnCloseDrawer: ImageView
    private lateinit var tvUserName: TextView
    private lateinit var tvUserEmail: TextView
    private lateinit var btnEditProfile: TextView
    private lateinit var btnDrawerLogout: Button

    // Menu items
    private lateinit var menuMyProfile: LinearLayout
    private lateinit var menuSettings: LinearLayout
    private lateinit var menuStatistics: LinearLayout
    private lateinit var menuHelp: LinearLayout
    private lateinit var menuActivityHistory: LinearLayout
    private lateinit var menuPrivacyPolicy: LinearLayout

    private lateinit var popularAdapter: PopularSubjectsAdapter
    private lateinit var categoriesAdapter: CategoriesAdapter
    private val popularSubjects = mutableListOf<Subject>()
    private val categoriesList = mutableListOf<Category>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Force portrait orientation
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

        setContentView(R.layout.activity_main)

        sessionManager = SessionManager(this)

        // Check if user is logged in
        if (!sessionManager.isLoggedIn()) {
            Log.d("MainActivity", "User not logged in, redirecting to login")
            redirectToLogin()
            return
        }

        // Restore tokens manually
        restoreTokens()

        initViews()
        setupDrawer()
        setupRecyclerViews()
        setupSearch()
        loadUserData()
        loadData()
    }

    private fun restoreTokens() {
        val accessToken = sessionManager.getAccessToken()
        val refreshToken = sessionManager.getRefreshToken()
        val expiresAt = sessionManager.getTokenExpiresAt()

        if (accessToken != null && refreshToken != null && expiresAt > 0) {
            val expiresIn = ((expiresAt - System.currentTimeMillis()) / 1000).toInt()
            if (expiresIn > 0) {
                SupabaseClient.setTokens(accessToken, refreshToken, expiresIn)
                Log.d("MainActivity", "✅ Tokens restored successfully")
            } else {
                Log.w("MainActivity", "⚠️ Stored tokens are expired")
                // Tokens are expired, redirect to login
                redirectToLogin()
                return
            }
        } else {
            Log.w("MainActivity", "⚠️ No valid tokens found")
            redirectToLogin()
            return
        }
    }

    private fun redirectToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun initViews() {
        // Main views
        drawerLayout = findViewById(R.id.drawerLayout)
        btnMenu = findViewById(R.id.btnMenu)
        btnProfile = findViewById(R.id.btnProfile)
        etSearch = findViewById(R.id.etSearch)
        rvPopular = findViewById(R.id.rvPopular)
        rvCategories = findViewById(R.id.rvCategories)
        progressBar = findViewById(R.id.progressBar)
        btnLogout = findViewById(R.id.btnLogout)

        // Drawer views
        btnCloseDrawer = findViewById(R.id.btnCloseDrawer)
        tvUserName = findViewById(R.id.tvUserName)
        tvUserEmail = findViewById(R.id.tvUserEmail)
        btnEditProfile = findViewById(R.id.btnEditProfile)
        btnDrawerLogout = findViewById(R.id.btnDrawerLogout)

        // Menu items
        menuMyProfile = findViewById(R.id.menuMyProfile)
        menuSettings = findViewById(R.id.menuSettings)
        menuStatistics = findViewById(R.id.menuStatistics)
        menuHelp = findViewById(R.id.menuHelp)
        menuActivityHistory = findViewById(R.id.menuActivityHistory)
        menuPrivacyPolicy = findViewById(R.id.menuPrivacyPolicy)
    }

    private fun setupSearch() {
        etSearch.setOnClickListener {
            // Navigate to search activity
            val intent = Intent(this, SearchActivity::class.java)
            startActivity(intent)
        }

        etSearch.isFocusable = false
        etSearch.isClickable = true
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
            showToast("My Profile clicked")
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

        btnEditProfile.setOnClickListener {
            showToast("Edit Profile clicked")
            drawerLayout.closeDrawer(GravityCompat.START)
        }

        // Logout buttons
        btnLogout.setOnClickListener { logout() }
        btnDrawerLogout.setOnClickListener { logout() }

        btnProfile.setOnClickListener {
            val userName = sessionManager.getUserName() ?: "User"
            showToast("Hello, $userName!")
        }
    }

    private fun loadUserData() {
        val userName = sessionManager.getUserName() ?: "User"
        val userEmail = sessionManager.getUserEmail() ?: "user@example.com"

        tvUserName.text = userName
        tvUserEmail.text = userEmail
    }

    private fun setupRecyclerViews() {
        // Popular subjects (horizontal)
        popularAdapter = PopularSubjectsAdapter(
            subjects = popularSubjects,
            onSubjectClick = { subject -> showSubjectDetailsDialog(subject) },
            onFollowClick = { subject -> toggleFollowSubject(subject) }
        )
        rvPopular.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        rvPopular.adapter = popularAdapter

        // Categories (vertical)
        categoriesAdapter = CategoriesAdapter(
            categories = categoriesList,
            onSubjectClick = { subject -> showSubjectDetailsDialog(subject) },
            onFollowClick = { subject -> toggleFollowSubject(subject) }
        )
        rvCategories.layoutManager = LinearLayoutManager(this)
        rvCategories.adapter = categoriesAdapter
    }

    private fun loadData() {
        Log.d("MainActivity", "=== STARTING DATA LOAD ===")
        Log.d("MainActivity", "Access token available: ${SupabaseClient.getAccessToken() != null}")
        Log.d("MainActivity", "User logged in: ${sessionManager.isLoggedIn()}")

        progressBar.visibility = View.VISIBLE

        lifecycleScope.launch {
            try {
                Log.d("MainActivity", "Making request to Supabase...")
                val result = SupabaseClient.getSubjectsWithCategories()

                result.onSuccess { data ->
                    Log.d("MainActivity", "✅ SUCCESS: Data loaded successfully")
                    Log.d("MainActivity", "Response length: ${data.length} characters")
                    Log.d("MainActivity", "First 200 chars: ${data.take(200)}")

                    if (data.trim().isEmpty()) {
                        Log.w("MainActivity", "⚠️ WARNING: Response is empty")
                        showEmptyState()
                    } else {
                        parseAndDisplayData(data)
                    }
                    progressBar.visibility = View.GONE

                }.onFailure { error ->
                    Log.e("MainActivity", "❌ ERROR: Failed to load data")
                    Log.e("MainActivity", "Error message: ${error.message}")
                    Log.e("MainActivity", "Error type: ${error.javaClass.simpleName}")

                    showToast("Erro ao carregar dados: ${error.message}")
                    progressBar.visibility = View.GONE
                    showEmptyState()
                }

            } catch (e: Exception) {
                Log.e("MainActivity", "❌ EXCEPTION: Unexpected error occurred")
                Log.e("MainActivity", "Exception message: ${e.message}")
                Log.e("MainActivity", "Exception type: ${e.javaClass.simpleName}")
                e.printStackTrace()

                showToast("Erro inesperado: ${e.message}")
                progressBar.visibility = View.GONE
                showEmptyState()
            }
        }
    }

    private fun showEmptyState() {
        Log.d("MainActivity", "Showing empty state - no data to display")
        popularSubjects.clear()
        categoriesList.clear()
        popularAdapter.notifyDataSetChanged()
        categoriesAdapter.notifyDataSetChanged()
    }

    private fun parseAndDisplayData(jsonData: String) {
        try {
            Log.d("MainActivity", "=== PARSING JSON DATA ===")
            val jsonArray = JSONArray(jsonData)
            Log.d("MainActivity", "Found ${jsonArray.length()} subjects in response")

            if (jsonArray.length() == 0) {
                Log.w("MainActivity", "⚠️ JSON array is empty")
                showEmptyState()
                return
            }

            val categoryMap = mutableMapOf<String, Category>()
            val allSubjects = mutableListOf<Subject>()

            for (i in 0 until jsonArray.length()) {
                val item = jsonArray.getJSONObject(i)

                try {
                    val categoryName = item.getString("category_name")
                    val categoryColor = item.getString("category_color")

                    Log.d("MainActivity", "Processing subject ${i + 1}: ${item.getString("name")}")

                    if (!categoryMap.containsKey(categoryName)) {
                        categoryMap[categoryName] = Category(
                            name = categoryName,
                            color = categoryColor,
                            subjects = mutableListOf()
                        )
                        Log.d("MainActivity", "Created new category: $categoryName")
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
                        categoryName = categoryName,
                        categoryColor = categoryColor
                    )

                    categoryMap[categoryName]?.subjects?.add(subject)
                    allSubjects.add(subject)

                } catch (e: Exception) {
                    Log.e("MainActivity", "Error parsing subject $i: ${e.message}")
                    continue
                }
            }

            // Sort subjects by followers count for popular section
            popularSubjects.clear()
            popularSubjects.addAll(allSubjects.sortedByDescending { it.followersCount }.take(10))

            categoriesList.clear()
            categoriesList.addAll(categoryMap.values)

            Log.d("MainActivity", "✅ PARSING COMPLETE")
            Log.d("MainActivity", "Total subjects: ${allSubjects.size}")
            Log.d("MainActivity", "Categories: ${categoriesList.size}")
            Log.d("MainActivity", "Popular subjects: ${popularSubjects.size}")

            popularAdapter.notifyDataSetChanged()
            categoriesAdapter.notifyDataSetChanged()

        } catch (e: Exception) {
            Log.e("MainActivity", "❌ ERROR PARSING DATA")
            Log.e("MainActivity", "Parse error: ${e.message}")
            e.printStackTrace()
            showToast("Erro ao processar dados: ${e.message}")
            showEmptyState()
        }
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
                Log.d("MainActivity", "Toggling follow for subject: ${subject.name}")
                val result = SupabaseClient.toggleSubjectFollow(subject.id)

                result.onSuccess { response ->
                    Log.d("MainActivity", "Toggle follow response: $response")
                    val jsonResponse = JSONObject(response)

                    if (jsonResponse.getBoolean("success")) {
                        subject.isFollowed = jsonResponse.getBoolean("is_following")
                        subject.followersCount = jsonResponse.getInt("followers_count")

                        popularAdapter.notifyDataSetChanged()
                        categoriesAdapter.notifyDataSetChanged()

                        val action = if (subject.isFollowed) "followed" else "unfollowed"
                        showToast("${subject.name} $action!")
                    }
                }.onFailure { error ->
                    Log.e("MainActivity", "Error toggling follow: ${error.message}")
                    showToast("Erro: ${error.message}")
                }
            } catch (e: Exception) {
                Log.e("MainActivity", "Exception toggling follow: ${e.message}")
                showToast("Erro: ${e.message}")
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

    override fun onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}

// Updated data classes
data class Category(
    val name: String,
    val color: String,
    val subjects: MutableList<Subject>
)

data class Subject(
    val id: String,
    val name: String,
    val description: String,
    val imageUrl: String?,
    var followersCount: Int,
    var isFollowed: Boolean,
    val isFeatured: Boolean,
    val difficultyLevel: Int,
    val estimatedHours: Int,
    val categoryName: String,
    val categoryColor: String
)
