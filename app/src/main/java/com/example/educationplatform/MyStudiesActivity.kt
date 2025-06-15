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

class MyStudiesActivity : AppCompatActivity() {
    
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var btnMenu: ImageView
    private lateinit var btnProfilePhoto: CircleImageView
    private lateinit var tvStudiesCount: TextView
    private lateinit var rvStudies: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var emptyState: LinearLayout
    private lateinit var btnCreateFirstStudy: Button
    private lateinit var sessionManager: SessionManager
    
    // Bottom Navigation
    private lateinit var btnHomepage: LinearLayout
    private lateinit var btnCreate: LinearLayout
    private lateinit var btnMyStudies: LinearLayout
    
    // Create Overlay
    private lateinit var createOverlay: LinearLayout
    private lateinit var btnCreateStudy: LinearLayout
    private lateinit var btnCreateSession: LinearLayout
    
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
    
    private lateinit var studiesAdapter: StudiesAdapter
    private val studies = mutableListOf<Study>()
    
    private var currentSort: String = "new"
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_my_studies)
        
        Log.d("MyStudies", "🚀 MyStudiesActivity started")
        
        sessionManager = SessionManager(this)
        
        initViews()
        setupDrawer()
        setupBottomNavigation()
        setupCreateOverlay()
        setupRecyclerView()
        setupProfileButton()
        loadUserData()
        loadMyStudies()
    }
    
    private fun initViews() {
        // Main views
        drawerLayout = findViewById(R.id.drawerLayout)
        btnMenu = findViewById(R.id.btnMenu)
        btnProfilePhoto = findViewById(R.id.btnProfilePhoto)
        tvStudiesCount = findViewById(R.id.tvStudiesCount)
        rvStudies = findViewById(R.id.rvStudies)
        progressBar = findViewById(R.id.progressBar)
        emptyState = findViewById(R.id.emptyState)
        btnCreateFirstStudy = findViewById(R.id.btnCreateFirstStudy)
        
        // Bottom Navigation
        btnHomepage = findViewById(R.id.btnHomepage)
        btnCreate = findViewById(R.id.btnCreate)
        btnMyStudies = findViewById(R.id.btnMyStudies)
        
        // Create Overlay
        createOverlay = findViewById(R.id.createOverlay)
        btnCreateStudy = findViewById(R.id.btnCreateStudy)
        btnCreateSession = findViewById(R.id.btnCreateSession)
        
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
        
        btnCreateFirstStudy.setOnClickListener {
            val intent = Intent(this, CreateStudyActivity::class.java)
            startActivity(intent)
        }
    }
    
    private fun setupProfileButton() {
        btnProfilePhoto.setOnClickListener {
            val intent = Intent(this, ProfileActivity::class.java)
            startActivity(intent)
        }
    }
    
    private fun setupBottomNavigation() {
        btnHomepage.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            startActivity(intent)
        }
        
        btnCreate.setOnClickListener {
            showCreateOverlay()
        }
        
        btnMyStudies.setOnClickListener {
            showToast("Already on My Studies")
        }
    }
    
    private fun setupCreateOverlay() {
        // Close overlay when clicking outside (on the background)
        createOverlay.setOnClickListener {
            hideCreateOverlay()
        }
        
        btnCreateStudy.setOnClickListener {
            hideCreateOverlay()
            val intent = Intent(this@MyStudiesActivity, CreateStudyActivity::class.java)
            startActivity(intent)
        }
        
        btnCreateSession.setOnClickListener {
            hideCreateOverlay()
            showToast("Create Session clicked")
            // TODO: Navigate to create session screen
        }
    }
    
    private fun showCreateOverlay() {
        createOverlay.visibility = View.VISIBLE
        createOverlay.alpha = 0f
        createOverlay.animate()
            .alpha(1f)
            .setDuration(200)
            .start()
    }
    
    private fun hideCreateOverlay() {
        createOverlay.animate()
            .alpha(0f)
            .setDuration(200)
            .withEndAction {
                createOverlay.visibility = View.GONE
            }
            .start()
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
        Log.d("MyStudies", "Loading user data from database")
        
        lifecycleScope.launch {
            try {
                val result = SupabaseClient.getUserProfile()
                
                result.onSuccess { data ->
                    Log.d("MyStudies", "User profile loaded successfully")
                    parseUserProfile(data)
                }.onFailure { error ->
                    Log.e("MyStudies", "Failed to load user profile: ${error.message}")
                    loadDefaultUserData()
                }
                
            } catch (e: Exception) {
                Log.e("MyStudies", "Exception loading user profile", e)
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
            
            Log.d("MyStudies", "User profile parsed successfully")
            
        } catch (e: Exception) {
            Log.e("MyStudies", "Error parsing user profile data", e)
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
        
        Log.d("MyStudies", "RecyclerView setup complete")
    }
    
    private fun loadMyStudies() {
        Log.d("MyStudies", "=== LOADING MY STUDIES ===")
        Log.d("MyStudies", "Access Token: ${SupabaseClient.getAccessToken()?.take(20)}...")
        
        progressBar.visibility = View.VISIBLE
        emptyState.visibility = View.GONE
        rvStudies.visibility = View.VISIBLE
        
        lifecycleScope.launch {
            try {
                val result = SupabaseClient.getMyStudies("new")
                
                result.onSuccess { data ->
                    Log.d("MyStudies", "✅ My studies loaded successfully")
                    Log.d("MyStudies", "Response data length: ${data.length}")
                    Log.d("MyStudies", "Response preview: ${data.take(200)}")
                    parseAndDisplayStudies(data)
                    progressBar.visibility = View.GONE
                }.onFailure { error ->
                    Log.e("MyStudies", "❌ Failed to load my studies")
                    Log.e("MyStudies", "Error message: ${error.message}")
                    Log.e("MyStudies", "Error details: $error")
                    
                    Toast.makeText(this@MyStudiesActivity, "Error: ${error.message}", Toast.LENGTH_LONG).show()
                    progressBar.visibility = View.GONE
                    showEmptyState()
                }
                
            } catch (e: Exception) {
                Log.e("MyStudies", "❌ Exception loading my studies", e)
                Toast.makeText(this@MyStudiesActivity, "Exception: ${e.message}", Toast.LENGTH_LONG).show()
                progressBar.visibility = View.GONE
                showEmptyState()
            }
        }
    }
    
    private fun parseAndDisplayStudies(jsonData: String) {
        try {
            Log.d("MyStudies", "Parsing studies data...")
            val jsonArray = JSONArray(jsonData)
            studies.clear()
            
            Log.d("MyStudies", "Found ${jsonArray.length()} studies")
            
            for (i in 0 until jsonArray.length()) {
                val item = jsonArray.getJSONObject(i)
                
                Log.d("MyStudies", "Parsing study $i: ${item.getString("title")}")
                
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
                Log.d("MyStudies", "No studies found, showing empty state")
                showEmptyState()
            } else {
                Log.d("MyStudies", "Displaying ${studies.size} studies")
                emptyState.visibility = View.GONE
                rvStudies.visibility = View.VISIBLE
                studiesAdapter.notifyDataSetChanged()
            }
            
            Log.d("MyStudies", "✅ Successfully parsed ${studies.size} studies")
            
        } catch (e: Exception) {
            Log.e("MyStudies", "❌ Error parsing studies", e)
            Toast.makeText(this, "Error parsing data: ${e.message}", Toast.LENGTH_LONG).show()
            showEmptyState()
        }
    }
    
    private fun showEmptyState() {
        Log.d("MyStudies", "Showing empty state")
        emptyState.visibility = View.VISIBLE
        rvStudies.visibility = View.GONE
    }
    
    private fun voteOnStudy(study: Study, voteType: String) {
        Log.d("MyStudies", "Voting on study: ${study.title}, vote: $voteType")
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
        Log.d("MyStudies", "Toggling save for study: ${study.title}")
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
        Log.d("MyStudies", "Activity resumed, refreshing studies and user data")
        loadUserData()
        loadMyStudies()
    }
    
    override fun onBackPressed() {
        if (createOverlay.visibility == View.VISIBLE) {
            hideCreateOverlay()
        } else if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }
    
    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
