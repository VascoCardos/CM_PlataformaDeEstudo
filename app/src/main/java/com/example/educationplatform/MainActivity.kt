package com.veducation.app

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

class MainActivity : AppCompatActivity() {

    private lateinit var btnMenu: ImageView
    private lateinit var btnProfile: ImageView
    private lateinit var etSearch: EditText
    private lateinit var rvPopular: RecyclerView
    private lateinit var rvCategories: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var btnLogout: Button
    private lateinit var sessionManager: SessionManager

    private lateinit var popularAdapter: PopularSubjectsAdapter
    private lateinit var categoriesAdapter: CategoriesAdapter
    private val popularSubjects = mutableListOf<Subject>()
    private val categoriesList = mutableListOf<Category>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        sessionManager = SessionManager(this)

        // Check if user is logged in
        if (!sessionManager.isLoggedIn()) {
            Log.d("MainActivity", "User not logged in, redirecting to login")
            redirectToLogin()
            return
        }

        // Restore access token
        val accessToken = sessionManager.getAccessToken()
        if (accessToken != null) {
            Log.d("MainActivity", "Restoring access token")
            SupabaseClient.setAccessToken(accessToken)
        } else {
            Log.w("MainActivity", "No access token found")
        }

        initViews()
        setupRecyclerViews()
        loadData()
    }

    private fun redirectToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun initViews() {
        btnMenu = findViewById(R.id.btnMenu)
        btnProfile = findViewById(R.id.btnProfile)
        etSearch = findViewById(R.id.etSearch)
        rvPopular = findViewById(R.id.rvPopular)
        rvCategories = findViewById(R.id.rvCategories)
        progressBar = findViewById(R.id.progressBar)
        btnLogout = findViewById(R.id.btnLogout)

        btnLogout.setOnClickListener { logout() }
        btnProfile.setOnClickListener {
            val userName = sessionManager.getUserName() ?: "User"
            showToast("Hello, $userName!")
        }
        btnMenu.setOnClickListener {
            showToast("Menu clicked - to be implemented")
        }
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
        Log.d("MainActivity", "Loading data from database...")
        progressBar.visibility = View.VISIBLE

        lifecycleScope.launch {
            try {
                val result = SupabaseClient.getSubjectsWithCategories()
                result.onSuccess { data ->
                    Log.d("MainActivity", "Data loaded successfully: ${data.length} characters")
                    parseAndDisplayData(data)
                    progressBar.visibility = View.GONE
                }.onFailure { error ->
                    Log.e("MainActivity", "Error loading data: ${error.message}")
                    showToast("Error loading data: ${error.message}")
                    progressBar.visibility = View.GONE

                    // Show some test data if database fails
                    showTestData()
                }
            } catch (e: Exception) {
                Log.e("MainActivity", "Exception loading data", e)
                showToast("Error: ${e.message}")
                progressBar.visibility = View.GONE
                showTestData()
            }
        }
    }

    private fun showTestData() {
        Log.d("MainActivity", "Showing test data")
        // Add some test subjects if database fails
        popularSubjects.clear()
        categoriesList.clear()

        val testSubjects = listOf(
            Subject("1", "JavaScript", "Learn JavaScript programming", "https://cdn.jsdelivr.net/gh/devicons/devicon/icons/javascript/javascript-original.svg", 150, false, true, 2, 40, "Programming", "#FF6B35"),
            Subject("2", "Python", "Learn Python programming", "https://cdn.jsdelivr.net/gh/devicons/devicon/icons/python/python-original.svg", 200, false, true, 2, 35, "Programming", "#FF6B35"),
            Subject("3", "Mathematics", "Basic mathematics", "https://img.icons8.com/color/96/000000/math.png", 100, false, false, 3, 50, "Math", "#4ECDC4")
        )

        popularSubjects.addAll(testSubjects)

        val programmingCategory = Category("Programming", "#FF6B35", mutableListOf(testSubjects[0], testSubjects[1]))
        val mathCategory = Category("Math", "#4ECDC4", mutableListOf(testSubjects[2]))

        categoriesList.add(programmingCategory)
        categoriesList.add(mathCategory)

        popularAdapter.notifyDataSetChanged()
        categoriesAdapter.notifyDataSetChanged()
    }

    private fun parseAndDisplayData(jsonData: String) {
        try {
            Log.d("MainActivity", "Parsing JSON data...")
            val jsonArray = JSONArray(jsonData)
            Log.d("MainActivity", "Found ${jsonArray.length()} subjects")

            val categoryMap = mutableMapOf<String, Category>()
            val allSubjects = mutableListOf<Subject>()

            for (i in 0 until jsonArray.length()) {
                val item = jsonArray.getJSONObject(i)
                val categoryName = item.getString("category_name")
                val categoryColor = item.getString("category_color")

                // Log each subject data
                Log.d("MainActivity", "Subject ${i}: ${item.getString("name")}")
                Log.d("MainActivity", "Image URL: ${if (item.isNull("image_url")) "null" else item.getString("image_url")}")

                if (!categoryMap.containsKey(categoryName)) {
                    categoryMap[categoryName] = Category(
                        name = categoryName,
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
                    categoryName = categoryName,
                    categoryColor = categoryColor
                )

                categoryMap[categoryName]?.subjects?.add(subject)
                allSubjects.add(subject)
            }

            // Sort subjects by followers count for popular section
            popularSubjects.clear()
            popularSubjects.addAll(allSubjects.sortedByDescending { it.followersCount }.take(10))

            categoriesList.clear()
            categoriesList.addAll(categoryMap.values)

            Log.d("MainActivity", "Parsed ${allSubjects.size} subjects into ${categoriesList.size} categories")
            Log.d("MainActivity", "Popular subjects: ${popularSubjects.size}")

            popularAdapter.notifyDataSetChanged()
            categoriesAdapter.notifyDataSetChanged()

        } catch (e: Exception) {
            Log.e("MainActivity", "Error parsing data", e)
            showToast("Error parsing data: ${e.message}")
            showTestData()
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
                val result = SupabaseClient.toggleSubjectFollow(subject.id)
                result.onSuccess { response ->
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
