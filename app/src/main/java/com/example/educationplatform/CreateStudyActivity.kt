package com.veducation.app

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.ActivityInfo
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.OpenableColumns
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.webkit.MimeTypeMap
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.InputStream

class CreateStudyActivity : AppCompatActivity() {

    private lateinit var btnBack: ImageView
    private lateinit var btnSave: ImageView
    private lateinit var etTitle: EditText
    private lateinit var etDescription: EditText
    private lateinit var tvDescriptionCount: TextView
    private lateinit var spinnerStudyType: Spinner
    private lateinit var spinnerSubject: Spinner
    private lateinit var rgStatus: RadioGroup
    private lateinit var rbPublic: RadioButton
    private lateinit var rbPrivate: RadioButton
    private lateinit var etContent: EditText
    private lateinit var tvContentCount: TextView
    private lateinit var btnAddFile: LinearLayout
    private lateinit var rvFiles: RecyclerView
    private lateinit var btnCreateStudy: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var tvUploadProgress: TextView
    private lateinit var tvPageTitle: TextView

    private lateinit var sessionManager: SessionManager
    private lateinit var attachedFilesAdapter: AttachedFilesAdapter
    private val attachedFiles = mutableListOf<AttachedFile>()
    private val subjects = mutableListOf<Subject>()
    
    private lateinit var filePickerLauncher: ActivityResultLauncher<Intent>
    private lateinit var permissionLauncher: ActivityResultLauncher<Array<String>>

    // Edit mode variables
    private var isEditMode = false
    private var editStudyId: String? = null
    private var studySubjectId: String? = null // Store the study's subject ID

    // Study types
    private val studyTypes = arrayOf(
        "Question" to "question",
        "Need Help" to "need_help", 
        "Documentation" to "documentation",
        "Summary" to "summary",
        "Other" to "other"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Force portrait orientation
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        
        setContentView(R.layout.activity_create_study)
        
        sessionManager = SessionManager(this)
        
        // Check if user is logged in
        if (!sessionManager.isLoggedIn()) {
            Log.w("CreateStudy", "User not logged in, redirecting to login")
            finish()
            return
        }
        
        // Check if this is edit mode
        checkEditMode()
        
        initViews()
        setupActivityResultLaunchers()
        setupListeners()
        setupSpinners()
        setupFilesRecyclerView()
        loadSubjects()
    }

    private fun checkEditMode() {
        isEditMode = intent.getBooleanExtra("EDIT_MODE", false)
        editStudyId = intent.getStringExtra("STUDY_ID")
        
        Log.d("CreateStudy", "Edit mode: $isEditMode, Study ID: $editStudyId")
    }

    private fun initViews() {
        btnBack = findViewById(R.id.btnBack)
        btnSave = findViewById(R.id.btnSave)
        etTitle = findViewById(R.id.etTitle)
        etDescription = findViewById(R.id.etDescription)
        tvDescriptionCount = findViewById(R.id.tvDescriptionCount)
        spinnerStudyType = findViewById(R.id.spinnerStudyType)
        spinnerSubject = findViewById(R.id.spinnerSubject)
        rgStatus = findViewById(R.id.rgStatus)
        rbPublic = findViewById(R.id.rbPublic)
        rbPrivate = findViewById(R.id.rbPrivate)
        etContent = findViewById(R.id.etContent)
        tvContentCount = findViewById(R.id.tvContentCount)
        btnAddFile = findViewById(R.id.btnAddFile)
        rvFiles = findViewById(R.id.rvFiles)
        btnCreateStudy = findViewById(R.id.btnCreateStudy)
        progressBar = findViewById(R.id.progressBar)
        tvUploadProgress = findViewById(R.id.tvUploadProgress)
        tvPageTitle = findViewById(R.id.tvPageTitle)
        
        // Update UI based on mode
        if (isEditMode) {
            tvPageTitle.text = "Edit Study"
            btnCreateStudy.text = "Update Study"
        } else {
            tvPageTitle.text = "Create Study"
            btnCreateStudy.text = "Create Study"
        }
    }

    private fun setupActivityResultLaunchers() {
        // File picker launcher
        filePickerLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                result.data?.data?.let { uri ->
                    handleSelectedFile(uri)
                }
            }
        }
        
        // Permission launcher
        permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            Log.d("CreateStudy", "Permission results: $permissions")
            
            val hasStoragePermission = when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
                    permissions[Manifest.permission.READ_MEDIA_IMAGES] == true ||
                    permissions[Manifest.permission.READ_MEDIA_VIDEO] == true ||
                    permissions[Manifest.permission.READ_MEDIA_AUDIO] == true
                }
                else -> {
                    permissions[Manifest.permission.READ_EXTERNAL_STORAGE] == true
                }
            }
            
            if (hasStoragePermission) {
                Log.d("CreateStudy", "Storage permission granted")
                openFilePicker()
            } else {
                Log.d("CreateStudy", "Storage permission denied")
                showToast("Storage permission is required to attach files. Please enable it in Settings.")
                showPermissionSettingsDialog()
            }
        }
    }

    private fun setupListeners() {
        btnBack.setOnClickListener { finish() }
        btnSave.setOnClickListener { saveStudy() }
        btnCreateStudy.setOnClickListener { saveStudy() }
        btnAddFile.setOnClickListener { checkPermissionsAndOpenFilePicker() }
        
        // Character counters
        etDescription.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                updateCharacterCount(s.toString(), tvDescriptionCount, 500)
            }
        })
        
        etContent.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                updateCharacterCount(s.toString(), tvContentCount, 10000)
            }
        })
    }

    private fun setupSpinners() {
        // Study Type Spinner
        val studyTypeAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            studyTypes.map { it.first }
        )
        studyTypeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerStudyType.adapter = studyTypeAdapter
        
        // Subject Spinner - will be populated when subjects are loaded
        val subjectAdapter = ArrayAdapter<String>(this, android.R.layout.simple_spinner_item)
        subjectAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerSubject.adapter = subjectAdapter
    }

    private fun setupFilesRecyclerView() {
        attachedFilesAdapter = AttachedFilesAdapter(attachedFiles) { file ->
            attachedFilesAdapter.removeFile(file)
            updateFilesVisibility()
        }
        rvFiles.layoutManager = LinearLayoutManager(this)
        rvFiles.adapter = attachedFilesAdapter
        updateFilesVisibility()
    }

    private fun loadSubjects() {
        Log.d("CreateStudy", "Loading subjects...")
        
        lifecycleScope.launch {
            try {
                val result = SupabaseClient.getSubjectsWithCategories()
                result.onSuccess { data ->
                    parseSubjects(data)
                    
                    // Load study data after subjects are loaded (if in edit mode)
                    if (isEditMode) {
                        loadStudyForEdit()
                    }
                }.onFailure { error ->
                    Log.e("CreateStudy", "Error loading subjects: ${error.message}")
                    showToast("Error loading subjects: ${error.message}")
                    
                    // If authentication failed, redirect to login
                    if (error.message?.contains("401") == true || error.message?.contains("authentication") == true) {
                        sessionManager.logout()
                        finish()
                    }
                }
            } catch (e: Exception) {
                Log.e("CreateStudy", "Exception loading subjects", e)
                showToast("Error loading subjects")
            }
        }
    }

    private fun parseSubjects(jsonData: String) {
        try {
            Log.d("CreateStudy", "Parsing subjects data: ${jsonData.take(200)}...")
            
            val jsonArray = JSONArray(jsonData)
            subjects.clear()
            
            for (i in 0 until jsonArray.length()) {
                val item = jsonArray.getJSONObject(i)
                
                try {
                    // Handle different possible response formats
                    val isFollowed = when {
                        item.has("is_followed_by_current_user") -> item.getBoolean("is_followed_by_current_user")
                        item.has("is_followed") -> item.getBoolean("is_followed")
                        else -> false
                    }
                    
                    // Add all subjects for edit mode, only followed for create mode
                    if (!isEditMode && !isFollowed) continue
                    
                    val subject = Subject(
                        id = item.getString("id"),
                        name = item.getString("name"),
                        description = item.optString("description", ""),
                        imageUrl = if (item.isNull("image_url")) null else item.getString("image_url"),
                        followersCount = item.optInt("followers_count", 0),
                        isFollowed = isFollowed,
                        isFeatured = item.optBoolean("is_featured", false),
                        difficultyLevel = item.optInt("difficulty_level", 1),
                        estimatedHours = item.optInt("estimated_hours", 0),
                        categoryName = item.optString("category_name", "General"),
                        categoryColor = item.optString("category_color", "#6366F1")
                    )
                    subjects.add(subject)
                    
                } catch (e: Exception) {
                    Log.w("CreateStudy", "Error parsing subject at index $i: ${e.message}")
                    continue
                }
            }
            
            // Update spinner
            runOnUiThread {
                updateSubjectSpinner()
            }
            
            Log.d("CreateStudy", "Loaded ${subjects.size} subjects")
            
        } catch (e: Exception) {
            Log.e("CreateStudy", "Error parsing subjects", e)
            runOnUiThread {
                showToast("Error parsing subjects data")
                // Add some default subjects for testing
                addDefaultSubjects()
            }
        }
    }
    
    private fun updateSubjectSpinner() {
        if (subjects.isEmpty()) {
            // Show message if user is not following any subjects
            val adapter = spinnerSubject.adapter as ArrayAdapter<String>
            adapter.clear()
            adapter.add("No subjects available")
            adapter.notifyDataSetChanged()
            
            // Disable the create button if no subjects are available
            btnCreateStudy.isEnabled = false
            btnSave.isEnabled = false
            
            if (!isEditMode) {
                showToast("You need to follow at least one subject to create a study")
            }
        } else {
            val subjectNames = subjects.map { it.name }
            val adapter = spinnerSubject.adapter as ArrayAdapter<String>
            adapter.clear()
            adapter.addAll(subjectNames)
            adapter.notifyDataSetChanged()
            
            // Re-enable buttons
            btnCreateStudy.isEnabled = true
            btnSave.isEnabled = true
            
            // Set subject selection if we have a stored subject ID
            studySubjectId?.let { subjectId ->
                setSubjectSelection(subjectId)
            }
        }
    }
    
    private fun addDefaultSubjects() {
        // Add some default subjects for testing
        subjects.clear()
        subjects.addAll(listOf(
            Subject(
                id = "default-1",
                name = "Mathematics",
                description = "Mathematics subject",
                imageUrl = null,
                followersCount = 100,
                isFollowed = true,
                isFeatured = false,
                difficultyLevel = 1,
                estimatedHours = 0,
                categoryName = "Science",
                categoryColor = "#6366F1"
            ),
            Subject(
                id = "default-2",
                name = "Computer Science",
                description = "Computer Science subject",
                imageUrl = null,
                followersCount = 150,
                isFollowed = true,
                isFeatured = false,
                difficultyLevel = 1,
                estimatedHours = 0,
                categoryName = "Technology",
                categoryColor = "#10B981"
            )
        ))
        updateSubjectSpinner()
    }

    private fun loadStudyForEdit() {
        if (editStudyId == null) {
            Log.e("CreateStudy", "No study ID provided for edit mode")
            showToast("Error: No study ID provided")
            finish()
            return
        }
        
        Log.d("CreateStudy", "Loading study for edit: $editStudyId")
        progressBar.visibility = View.VISIBLE
        tvUploadProgress.visibility = View.VISIBLE
        tvUploadProgress.text = "Loading study data..."
        
        lifecycleScope.launch {
            try {
                val result = SupabaseClient.getStudyForEdit(editStudyId!!)
                
                result.onSuccess { data ->
                    Log.d("CreateStudy", "✅ Study data loaded successfully")
                    Log.d("CreateStudy", "Raw response: $data")
                    parseAndFillStudyData(data)
                    progressBar.visibility = View.GONE
                    tvUploadProgress.visibility = View.GONE
                }.onFailure { error ->
                    Log.e("CreateStudy", "❌ Error loading study: ${error.message}")
                    showToast("Error loading study: ${error.message}")
                    progressBar.visibility = View.GONE
                    tvUploadProgress.visibility = View.GONE
                    
                    // If authentication failed, redirect to login
                    if (error.message?.contains("401") == true || error.message?.contains("authentication") == true) {
                        sessionManager.logout()
                        finish()
                    }
                }
                
            } catch (e: Exception) {
                Log.e("CreateStudy", "❌ Exception loading study", e)
                showToast("Error: ${e.message}")
                progressBar.visibility = View.GONE
                tvUploadProgress.visibility = View.GONE
            }
        }
    }

    private fun parseAndFillStudyData(jsonData: String) {
        try {
            Log.d("CreateStudy", "=== PARSING STUDY DATA ===")
            Log.d("CreateStudy", "Raw JSON: $jsonData")
            
            val studyData = JSONObject(jsonData)
            
            // Extract and log each field
            val title = studyData.optString("title", "")
            val description = studyData.optString("description", "")
            val content = studyData.optString("content", "")
            val studyType = studyData.optString("study_type", "other")
            val status = studyData.optString("status", "public")
            val subjectId = studyData.optString("subject_id", "")
            
            Log.d("CreateStudy", "📝 Title: '$title'")
            Log.d("CreateStudy", "📝 Description: '$description'")
            Log.d("CreateStudy", "📝 Content: '${content.take(50)}...'")
            Log.d("CreateStudy", "📝 Study Type: '$studyType'")
            Log.d("CreateStudy", "📝 Status: '$status'")
            Log.d("CreateStudy", "📝 Subject ID: '$subjectId'")
            
            // Fill basic fields
            runOnUiThread {
                Log.d("CreateStudy", "🔄 Filling UI fields...")
                
                etTitle.setText(title)
                etDescription.setText(description)
                etContent.setText(content)
                
                Log.d("CreateStudy", "✅ Title set to: '${etTitle.text}'")
                Log.d("CreateStudy", "✅ Description set to: '${etDescription.text}'")
                Log.d("CreateStudy", "✅ Content set to: '${etContent.text.toString().take(50)}...'")
                
                // Set study type
                val studyTypeIndex = studyTypes.indexOfFirst { it.second == studyType }
                if (studyTypeIndex >= 0) {
                    spinnerStudyType.setSelection(studyTypeIndex)
                    Log.d("CreateStudy", "✅ Study type set to index: $studyTypeIndex (${studyTypes[studyTypeIndex].first})")
                } else {
                    Log.w("CreateStudy", "⚠️ Study type not found: $studyType")
                }
                
                // Set status
                if (status == "private") {
                    rbPrivate.isChecked = true
                    Log.d("CreateStudy", "✅ Status set to private")
                } else {
                    rbPublic.isChecked = true
                    Log.d("CreateStudy", "✅ Status set to public")
                }
                
                // Store subject ID for later selection
                if (subjectId.isNotEmpty()) {
                    studySubjectId = subjectId
                    Log.d("CreateStudy", "✅ Subject ID stored: $subjectId")
                    
                    // Try to set subject selection if subjects are already loaded
                    if (subjects.isNotEmpty()) {
                        setSubjectSelection(subjectId)
                    }
                }
                
                // Update character counts
                updateCharacterCount(description, tvDescriptionCount, 500)
                updateCharacterCount(content, tvContentCount, 10000)
                
                Log.d("CreateStudy", "✅ Character counts updated")
            }
            
            // Load files if any
            if (studyData.has("files")) {
                val filesArray = studyData.getJSONArray("files")
                Log.d("CreateStudy", "📎 Loading ${filesArray.length()} files")
                loadExistingFiles(filesArray)
            }
            
            Log.d("CreateStudy", "✅ Study data filled successfully!")
            
        } catch (e: Exception) {
            Log.e("CreateStudy", "❌ Error parsing study data", e)
            Log.e("CreateStudy", "❌ JSON that failed: $jsonData")
            showToast("Error parsing study data: ${e.message}")
        }
    }

    private fun setSubjectSelection(subjectId: String) {
        Log.d("CreateStudy", "🎯 Setting subject selection for ID: $subjectId")
        
        // Find subject index
        val subjectIndex = subjects.indexOfFirst { it.id == subjectId }
        if (subjectIndex >= 0) {
            spinnerSubject.setSelection(subjectIndex)
            Log.d("CreateStudy", "✅ Subject selected: ${subjects[subjectIndex].name} (index: $subjectIndex)")
        } else {
            Log.w("CreateStudy", "⚠️ Subject not found in list: $subjectId")
            Log.d("CreateStudy", "Available subjects: ${subjects.map { "${it.id} -> ${it.name}" }}")
        }
    }

    private fun loadExistingFiles(filesArray: JSONArray) {
        try {
            attachedFiles.clear()
            
            for (i in 0 until filesArray.length()) {
                val fileObj = filesArray.getJSONObject(i)
                val fileName = fileObj.getString("file_name")
                val fileUrl = fileObj.getString("file_url")
                
                val attachedFile = AttachedFile(
                    name = fileName,
                    size = 0L, // Size not available for existing files
                    uri = fileUrl, // Use URL as URI for existing files
                    type = "application/octet-stream" // Default type
                )
                
                attachedFiles.add(attachedFile)
            }
            
            runOnUiThread {
                attachedFilesAdapter.notifyDataSetChanged()
                updateFilesVisibility()
            }
            
            Log.d("CreateStudy", "✅ Loaded ${attachedFiles.size} existing files")
            
        } catch (e: Exception) {
            Log.e("CreateStudy", "❌ Error loading existing files", e)
        }
    }

    private fun checkPermissionsAndOpenFilePicker() {
        val permissions = mutableListOf<String>()
        
        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                    permissions.add(Manifest.permission.READ_MEDIA_IMAGES)
                }
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_VIDEO) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                    permissions.add(Manifest.permission.READ_MEDIA_VIDEO)
                }
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_AUDIO) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                    permissions.add(Manifest.permission.READ_MEDIA_AUDIO)
                }
            }
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                    permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
                }
            }
            else -> {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                    permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
                }
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                    permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                }
            }
        }
        
        if (permissions.isNotEmpty()) {
            Log.d("CreateStudy", "Requesting permissions: $permissions")
            permissionLauncher.launch(permissions.toTypedArray())
        } else {
            Log.d("CreateStudy", "All permissions granted, opening file picker")
            openFilePicker()
        }
    }

    private fun openFilePicker() {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "*/*"
            addCategory(Intent.CATEGORY_OPENABLE)
            putExtra(Intent.EXTRA_MIME_TYPES, arrayOf(
                "application/pdf",
                "image/*",
                "text/*",
                "application/msword",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
            ))
        }
        
        try {
            filePickerLauncher.launch(Intent.createChooser(intent, "Select File"))
        } catch (e: Exception) {
            showToast("No file manager found")
        }
    }

    private fun handleSelectedFile(uri: Uri) {
        try {
            val cursor = contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    val sizeIndex = it.getColumnIndex(OpenableColumns.SIZE)
                    
                    val fileName = if (nameIndex >= 0) it.getString(nameIndex) else "Unknown"
                    val fileSize = if (sizeIndex >= 0) it.getLong(sizeIndex) else 0L
                    
                    // Check file size (50MB limit)
                    val maxSize = 50 * 1024 * 1024 // 50MB
                    if (fileSize > maxSize) {
                        showToast("File size must be less than 50MB")
                        return
                    }
                    
                    val fileType = getMimeType(uri) ?: "application/octet-stream"
                    
                    val attachedFile = AttachedFile(
                        name = fileName,
                        size = fileSize,
                        uri = uri.toString(),
                        type = fileType
                    )
                    
                    attachedFiles.add(attachedFile)
                    attachedFilesAdapter.notifyItemInserted(attachedFiles.size - 1)
                    updateFilesVisibility()
                    
                    showToast("File attached: $fileName")
                }
            }
        } catch (e: Exception) {
            Log.e("CreateStudy", "Error handling selected file", e)
            showToast("Error processing file")
        }
    }

    private fun updateCharacterCount(text: String, countView: TextView, maxLength: Int) {
        val count = text.length
        countView.text = "$count/$maxLength"
        
        when {
            count > maxLength -> countView.setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_dark))
            count > maxLength * 0.9 -> countView.setTextColor(ContextCompat.getColor(this, android.R.color.holo_orange_dark))
            else -> countView.setTextColor(ContextCompat.getColor(this, android.R.color.darker_gray))
        }
    }

    private fun updateFilesVisibility() {
        rvFiles.visibility = if (attachedFiles.isEmpty()) View.GONE else View.VISIBLE
    }

    private fun saveStudy() {
        val title = etTitle.text.toString().trim()
        val description = etDescription.text.toString().trim()
        val content = etContent.text.toString().trim()
        
        // Validation
        if (title.isEmpty()) {
            showToast("Title is required")
            etTitle.requestFocus()
            return
        }
        
        if (content.isEmpty()) {
            showToast("Content is required")
            etContent.requestFocus()
            return
        }
        
        if (subjects.isEmpty()) {
            showToast("No subjects available")
            return
        }
        
        if (spinnerSubject.selectedItemPosition == AdapterView.INVALID_POSITION || spinnerSubject.selectedItemPosition >= subjects.size) {
            showToast("Please select a valid subject")
            return
        }
        
        val selectedSubject = subjects[spinnerSubject.selectedItemPosition]
        val selectedStudyType = studyTypes[spinnerStudyType.selectedItemPosition].second
        val status = if (rbPublic.isChecked) "public" else "private"
        
        Log.d("CreateStudy", "Saving study: $title, Subject: ${selectedSubject.name}, Type: $selectedStudyType, Status: $status")
        
        progressBar.visibility = View.VISIBLE
        tvUploadProgress.visibility = View.VISIBLE
        btnCreateStudy.isEnabled = false
        btnSave.isEnabled = false
        
        if (isEditMode) {
            updateStudy(title, description, content, selectedStudyType, status, selectedSubject.id)
        } else {
            createNewStudy(title, description, content, selectedStudyType, status, selectedSubject.id)
        }
    }

    private fun createNewStudy(title: String, description: String, content: String, studyType: String, status: String, subjectId: String) {
        lifecycleScope.launch {
            try {
                val uploadedFileUrls = mutableListOf<String>()
                
                // Upload files first if any
                if (attachedFiles.isNotEmpty()) {
                    tvUploadProgress.text = "Uploading files..."
                    
                    for ((index, file) in attachedFiles.withIndex()) {
                        // Skip existing files (they already have URLs)
                        if (file.uri.startsWith("http")) {
                            uploadedFileUrls.add(file.uri)
                            continue
                        }
                        
                        tvUploadProgress.text = "Uploading file ${index + 1} of ${attachedFiles.size}..."
                        
                        try {
                            val uri = Uri.parse(file.uri)
                            val inputStream = contentResolver.openInputStream(uri)
                            val fileBytes = inputStream?.readBytes()
                            inputStream?.close()
                            
                            if (fileBytes != null) {
                                Log.d("CreateStudy", "Uploading file: ${file.name}, size: ${fileBytes.size}, type: ${file.type}")
                                
                                val uploadResult = SupabaseClient.uploadStudyFile(
                                    fileBytes = fileBytes,
                                    fileName = file.name,
                                    mimeType = file.type
                                )
                                
                                uploadResult.onSuccess { fileUrl ->
                                    uploadedFileUrls.add(fileUrl)
                                    Log.d("CreateStudy", "✅ File uploaded: ${file.name} -> $fileUrl")
                                    runOnUiThread {
                                        tvUploadProgress.text = "Uploaded ${file.name}"
                                    }
                                }.onFailure { error ->
                                    Log.e("CreateStudy", "❌ Failed to upload file ${file.name}: ${error.message}")
                                    runOnUiThread {
                                        showToast("Failed to upload ${file.name}: ${error.message}")
                                    }
                                }
                            } else {
                                Log.e("CreateStudy", "❌ Could not read file: ${file.name}")
                                runOnUiThread {
                                    showToast("Could not read file: ${file.name}")
                                }
                            }
                        } catch (e: Exception) {
                            Log.e("CreateStudy", "❌ Error processing file ${file.name}", e)
                            runOnUiThread {
                                showToast("Error processing ${file.name}: ${e.message}")
                            }
                        }
                    }
                }
                
                // Create study with uploaded file URLs
                tvUploadProgress.text = "Creating study..."

                val result = SupabaseClient.createStudy(
                    title = title,
                    content = content,
                    description = description.ifEmpty { null },
                    status = status,
                    studyType = studyType,
                    subjectId = subjectId,
                    fileUrls = uploadedFileUrls
                )
                
                result.onSuccess { response ->
                    Log.d("CreateStudy", "Study created successfully: $response")
                    runOnUiThread {
                        showToast("Study created successfully!")
                        finish()
                    }
                    
                }.onFailure { error ->
                    Log.e("CreateStudy", "Error creating study: ${error.message}")
                    runOnUiThread {
                        showToast("Error creating study: ${error.message}")
                        
                        // If authentication failed, redirect to login
                        if (error.message?.contains("401") == true || error.message?.contains("authentication") == true) {
                            sessionManager.logout()
                            finish()
                        }
                    }
                }
                
            } catch (e: Exception) {
                Log.e("CreateStudy", "Exception creating study", e)
                runOnUiThread {
                    showToast("Error: ${e.message}")
                }
            }
            
            runOnUiThread {
                progressBar.visibility = View.GONE
                tvUploadProgress.visibility = View.GONE
                btnCreateStudy.isEnabled = true
                btnSave.isEnabled = true
            }
        }
    }

    private fun updateStudy(title: String, description: String, content: String, studyType: String, status: String, subjectId: String) {
        lifecycleScope.launch {
            try {
                tvUploadProgress.text = "Updating study..."

                val result = SupabaseClient.updateStudy(
                    studyId = editStudyId!!,
                    title = title,
                    content = content,
                    description = description.ifEmpty { null },
                    status = status,
                    studyType = studyType,
                    subjectId = subjectId
                )
                
                result.onSuccess { response ->
                    Log.d("CreateStudy", "Study updated successfully: $response")
                    runOnUiThread {
                        showToast("Study updated successfully!")
                        finish()
                    }
                    
                }.onFailure { error ->
                    Log.e("CreateStudy", "Error updating study: ${error.message}")
                    runOnUiThread {
                        showToast("Error updating study: ${error.message}")
                        
                        // If authentication failed, redirect to login
                        if (error.message?.contains("401") == true || error.message?.contains("authentication") == true) {
                            sessionManager.logout()
                            finish()
                        }
                    }
                }
                
            } catch (e: Exception) {
                Log.e("CreateStudy", "Exception updating study", e)
                runOnUiThread {
                    showToast("Error: ${e.message}")
                }
            }
            
            runOnUiThread {
                progressBar.visibility = View.GONE
                tvUploadProgress.visibility = View.GONE
                btnCreateStudy.isEnabled = true
                btnSave.isEnabled = true
            }
        }
    }

    private fun getMimeType(uri: Uri): String? {
        return if (uri.scheme == "content") {
            contentResolver.getType(uri)
        } else {
            val fileExtension = MimeTypeMap.getFileExtensionFromUrl(uri.toString())
            MimeTypeMap.getSingleton().getMimeTypeFromExtension(fileExtension.lowercase())
        }
    }

    private fun InputStream.readBytes(): ByteArray {
        val buffer = ByteArrayOutputStream()
        val data = ByteArray(1024)
        var nRead: Int
        while (this.read(data, 0, data.size).also { nRead = it } != -1) {
            buffer.write(data, 0, nRead)
        }
        return buffer.toByteArray()
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun showPermissionSettingsDialog() {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Permission Required")
            .setMessage("Storage permission is needed to attach files. Please enable it in app settings.")
            .setPositiveButton("Go to Settings") { _, _ ->
                try {
                    val intent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    intent.data = Uri.parse("package:$packageName")
                    startActivity(intent)
                } catch (e: Exception) {
                    showToast("Could not open settings")
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
