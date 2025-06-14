package com.veducation.app

import android.Manifest
import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.content.pm.ActivityInfo
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.util.Log
import android.view.View
import android.view.Window
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import de.hdodenhof.circleimageview.CircleImageView
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ProfileActivity : AppCompatActivity() {

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var btnMenu: ImageView
    private lateinit var btnEditProfile: ImageView
    private lateinit var ivProfilePhoto: CircleImageView
    private lateinit var tvUserName: TextView
    private lateinit var tvUserEmail: TextView
    private lateinit var tvUserBio: TextView
    private lateinit var progressBar: ProgressBar

    // Bottom Navigation
    private lateinit var btnHomepage: LinearLayout
    private lateinit var btnCreate: LinearLayout
    private lateinit var btnMyStudies: LinearLayout

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

    private lateinit var sessionManager: SessionManager
    private lateinit var nameChangeManager: NameChangeManager

    private var selectedImageUri: Uri? = null

    // Activity Result Launchers
    private lateinit var cameraLauncher: ActivityResultLauncher<Intent>
    private lateinit var galleryLauncher: ActivityResultLauncher<Intent>
    private lateinit var permissionLauncher: ActivityResultLauncher<Array<String>>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Force portrait orientation
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        
        setContentView(R.layout.activity_profile)
        
        sessionManager = SessionManager(this)
        nameChangeManager = NameChangeManager(this)
        
        initViews()
        setupActivityResultLaunchers()
        setupListeners()
        setupDrawer()
        setupBottomNavigation()
        loadUserProfile()
    }

    private fun initViews() {
        drawerLayout = findViewById(R.id.drawerLayout)
        btnMenu = findViewById(R.id.btnMenu)
        btnEditProfile = findViewById(R.id.btnEditProfile)
        ivProfilePhoto = findViewById(R.id.ivProfilePhoto)
        tvUserName = findViewById(R.id.tvUserName)
        tvUserEmail = findViewById(R.id.tvUserEmail)
        tvUserBio = findViewById(R.id.tvUserBio)
        progressBar = findViewById(R.id.progressBar)

        // Bottom Navigation
        btnHomepage = findViewById(R.id.btnHomepage)
        btnCreate = findViewById(R.id.btnCreate)
        btnMyStudies = findViewById(R.id.btnMyStudies)

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
    }

    private fun setupBottomNavigation() {
        btnHomepage.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            startActivity(intent)
            finish()
        }
        
        btnCreate.setOnClickListener {
            showToast("Create clicked")
        }
        
        btnMyStudies.setOnClickListener {
            showToast("My Studies clicked")
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
            drawerLayout.closeDrawer(GravityCompat.START)
            // Already on profile, just close drawer
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
            showEditOptionsDialog()
            drawerLayout.closeDrawer(GravityCompat.START)
        }
        
        btnDrawerLogout.setOnClickListener { 
            logout()
        }
    }

    private fun setupActivityResultLaunchers() {
        // Launcher para câmera
        cameraLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val imageBitmap = result.data?.extras?.get("data") as? Bitmap
                if (imageBitmap != null) {
                    selectedImageUri = getImageUriFromBitmap(imageBitmap)
                    uploadAndUpdatePhoto()
                }
            }
        }
        
        // Launcher para galeria
        galleryLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val imageUri = result.data?.data
                if (imageUri != null) {
                    selectedImageUri = imageUri
                    uploadAndUpdatePhoto()
                }
            }
        }
        
        // Launcher para permissões
        permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            val cameraGranted = permissions[Manifest.permission.CAMERA] ?: false
            val storageGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                permissions[Manifest.permission.READ_MEDIA_IMAGES] ?: false
            } else {
                permissions[Manifest.permission.READ_EXTERNAL_STORAGE] ?: false
            }
            
            when {
                cameraGranted && storageGranted -> showImagePickerDialog()
                cameraGranted -> {
                    showToast("Permissão da galeria negada")
                    openCamera()
                }
                storageGranted -> {
                    showToast("Permissão da câmera negada")
                    openGallery()
                }
                else -> showToast("Permissões necessárias foram negadas")
            }
        }
    }

    private fun setupListeners() {
        btnEditProfile.setOnClickListener { showEditOptionsDialog() }
    }

    private fun showEditOptionsDialog() {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_edit_options)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        
        val btnChangeName = dialog.findViewById<LinearLayout>(R.id.btnChangeName)
        val btnChangeBio = dialog.findViewById<LinearLayout>(R.id.btnChangeBio)
        val btnChangePhoto = dialog.findViewById<LinearLayout>(R.id.btnChangePhoto)
        val btnChangePassword = dialog.findViewById<LinearLayout>(R.id.btnChangePassword)
        val btnCancel = dialog.findViewById<Button>(R.id.btnCancel)
        val tvNameStatus = dialog.findViewById<TextView>(R.id.tvNameStatus)
        
        // Update name status
        updateNameStatus(tvNameStatus)
        
        btnChangeName.setOnClickListener {
            dialog.dismiss()
            showChangeNameDialog()
        }
        
        btnChangeBio.setOnClickListener {
            dialog.dismiss()
            showChangeBioDialog()
        }
        
        btnChangePhoto.setOnClickListener {
            dialog.dismiss()
            checkPermissionsAndShowPicker()
        }
        
        btnChangePassword.setOnClickListener {
            dialog.dismiss()
            showChangePasswordDialog()
        }
        
        btnCancel.setOnClickListener { dialog.dismiss() }
        
        dialog.show()
    }

    private fun updateNameStatus(tvNameStatus: TextView) {
        if (nameChangeManager.canChangeName()) {
            tvNameStatus.text = "Available to change"
            tvNameStatus.setTextColor(ContextCompat.getColor(this, R.color.orange_start))
        } else {
            val nextChangeDate = nameChangeManager.getNextChangeDate()
            val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            tvNameStatus.text = "Next change available: ${dateFormat.format(nextChangeDate)}"
            tvNameStatus.setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_dark))
        }
    }

    private fun showChangeNameDialog() {
        if (!nameChangeManager.canChangeName()) {
            val nextChangeDate = nameChangeManager.getNextChangeDate()
            val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            showToast("You can only change your name once per month. Next change available: ${dateFormat.format(nextChangeDate)}")
            return
        }

        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_change_name)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        
        val etNewName = dialog.findViewById<EditText>(R.id.etNewName)
        val btnCancelName = dialog.findViewById<Button>(R.id.btnCancelName)
        val btnConfirmName = dialog.findViewById<Button>(R.id.btnConfirmName)
        val tvNameInfo = dialog.findViewById<TextView>(R.id.tvNameInfo)
        
        // Set current name
        etNewName.setText(tvUserName.text.toString())
        
        // Update info text
        val lastChangeDate = nameChangeManager.getLastChangeDate()
        if (lastChangeDate != null) {
            val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            tvNameInfo.text = "Last changed: ${dateFormat.format(lastChangeDate)}"
        }
        
        btnCancelName.setOnClickListener { dialog.dismiss() }
        
        btnConfirmName.setOnClickListener {
            val newName = etNewName.text.toString().trim()
            if (newName.isEmpty()) {
                showToast("Nome não pode estar vazio")
                return@setOnClickListener
            }
            
            if (newName == tvUserName.text.toString()) {
                showToast("O nome é igual ao atual")
                return@setOnClickListener
            }
            
            changeName(newName) { success ->
                if (success) {
                    nameChangeManager.recordNameChange()
                    updateUserInfo(newName, tvUserEmail.text.toString(), tvUserBio.text.toString(), null)
                    sessionManager.updateUserName(newName)
                    showToast("Nome alterado com sucesso!")
                    dialog.dismiss()
                } else {
                    showToast("Erro ao alterar nome")
                }
            }
        }
        
        dialog.show()
    }

    private fun showChangeBioDialog() {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_change_bio)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        
        val etNewBio = dialog.findViewById<EditText>(R.id.etNewBio)
        val btnCancelBio = dialog.findViewById<Button>(R.id.btnCancelBio)
        val btnConfirmBio = dialog.findViewById<Button>(R.id.btnConfirmBio)
        val tvCharacterCount = dialog.findViewById<TextView>(R.id.tvCharacterCount)
        
        // Set current bio
        val currentBio = if (tvUserBio.text.toString() == "O utilizador não adicionou bio") {
            ""
        } else {
            tvUserBio.text.toString()
        }
        etNewBio.setText(currentBio)
        
        // Update character count
        updateCharacterCount(etNewBio.text.toString(), tvCharacterCount)
        
        // Add text watcher for character count
        etNewBio.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                updateCharacterCount(s.toString(), tvCharacterCount)
            }
        })
        
        btnCancelBio.setOnClickListener { dialog.dismiss() }
        
        btnConfirmBio.setOnClickListener {
            val newBio = etNewBio.text.toString().trim()
            
            if (newBio.length > 500) {
                showToast("Bio não pode ter mais de 500 caracteres")
                return@setOnClickListener
            }
            
            changeBio(newBio) { success ->
                if (success) {
                    val displayBio = if (newBio.isEmpty()) "O utilizador não adicionou bio" else newBio
                    updateUserInfo(tvUserName.text.toString(), tvUserEmail.text.toString(), displayBio, null)
                    showToast("Bio alterada com sucesso!")
                    dialog.dismiss()
                } else {
                    showToast("Erro ao alterar bio")
                }
            }
        }
        
        dialog.show()
    }

    private fun updateCharacterCount(text: String, tvCharacterCount: TextView) {
        val count = text.length
        tvCharacterCount.text = "$count/500"
        
        // Change color based on character count
        when {
            count > 500 -> tvCharacterCount.setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_dark))
            count > 450 -> tvCharacterCount.setTextColor(ContextCompat.getColor(this, android.R.color.holo_orange_dark))
            else -> tvCharacterCount.setTextColor(ContextCompat.getColor(this, android.R.color.darker_gray))
        }
    }

    private fun showChangePasswordDialog() {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_change_password)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        
        val etCurrentPassword = dialog.findViewById<EditText>(R.id.etCurrentPassword)
        val etNewPassword = dialog.findViewById<EditText>(R.id.etNewPassword)
        val etConfirmNewPassword = dialog.findViewById<EditText>(R.id.etConfirmNewPassword)
        val btnCancelPassword = dialog.findViewById<Button>(R.id.btnCancelPassword)
        val btnConfirmPassword = dialog.findViewById<Button>(R.id.btnConfirmPassword)
        
        // Password visibility toggles
        setupPasswordToggle(dialog, R.id.ivToggleCurrentPassword, etCurrentPassword)
        setupPasswordToggle(dialog, R.id.ivToggleNewPassword, etNewPassword)
        setupPasswordToggle(dialog, R.id.ivToggleConfirmPassword, etConfirmNewPassword)
        
        btnCancelPassword.setOnClickListener { dialog.dismiss() }
        
        btnConfirmPassword.setOnClickListener {
            val currentPassword = etCurrentPassword.text.toString()
            val newPassword = etNewPassword.text.toString()
            val confirmPassword = etConfirmNewPassword.text.toString()
            
            if (currentPassword.isEmpty() || newPassword.isEmpty() || confirmPassword.isEmpty()) {
                showToast("Preencha todos os campos")
                return@setOnClickListener
            }
            
            if (newPassword != confirmPassword) {
                showToast("As passwords não coincidem")
                return@setOnClickListener
            }
            
            if (newPassword.length < 6) {
                showToast("A nova password deve ter pelo menos 6 caracteres")
                return@setOnClickListener
            }
            
            changePassword(currentPassword, newPassword) { success ->
                if (success) {
                    showToast("Password alterada com sucesso!")
                    dialog.dismiss()
                } else {
                    showToast("Erro ao alterar password")
                }
            }
        }
        
        dialog.show()
    }

    private fun checkPermissionsAndShowPicker() {
        val permissions = mutableListOf<String>()
        
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.CAMERA)
        }
        
        val storagePermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
        
        if (ContextCompat.checkSelfPermission(this, storagePermission) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            permissions.add(storagePermission)
        }
        
        if (permissions.isNotEmpty()) {
            permissionLauncher.launch(permissions.toTypedArray())
        } else {
            showImagePickerDialog()
        }
    }

    private fun showImagePickerDialog() {
        val options = arrayOf("📷 Tirar Foto", "🖼️ Escolher da Galeria", "❌ Cancelar")
        
        AlertDialog.Builder(this)
            .setTitle("Selecionar Foto de Perfil")
            .setItems(options) { dialog, which ->
                when (which) {
                    0 -> openCamera()
                    1 -> openGallery()
                    2 -> dialog.dismiss()
                }
            }
            .show()
    }

    private fun openCamera() {
        try {
            val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            if (intent.resolveActivity(packageManager) != null) {
                cameraLauncher.launch(intent)
            } else {
                showToast("Câmera não disponível")
            }
        } catch (e: Exception) {
            showToast("Erro ao abrir câmera: ${e.message}")
        }
    }

    private fun openGallery() {
        try {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI).apply {
                type = "image/*"
            }
            
            if (intent.resolveActivity(packageManager) != null) {
                galleryLauncher.launch(intent)
            } else {
                val fallbackIntent = Intent(Intent.ACTION_GET_CONTENT).apply {
                    type = "image/*"
                }
                if (fallbackIntent.resolveActivity(packageManager) != null) {
                    galleryLauncher.launch(fallbackIntent)
                } else {
                    showToast("Galeria não disponível")
                }
            }
        } catch (e: Exception) {
            showToast("Erro ao abrir galeria: ${e.message}")
        }
    }

    private fun uploadAndUpdatePhoto() {
        if (selectedImageUri == null) return
        
        progressBar.visibility = View.VISIBLE
        
        lifecycleScope.launch {
            try {
                val inputStream = contentResolver.openInputStream(selectedImageUri!!)
                val bytes = inputStream?.readBytes()
                inputStream?.close()
                
                if (bytes != null) {
                    val uploadResult = SupabaseClient.uploadProfileImage(bytes)
                    uploadResult.onSuccess { imageUrl ->
                        val updateResult = SupabaseClient.updateUserProfile(
                            tvUserName.text.toString(),
                            if (tvUserBio.text.toString() == "O utilizador não adicionou bio") "" else tvUserBio.text.toString(),
                            imageUrl
                        )
                        
                        updateResult.onSuccess {
                            loadProfileImage(imageUrl)
                            loadDrawerProfileImage(imageUrl)
                            showToast("Foto atualizada com sucesso!")
                        }.onFailure { error ->
                            showToast("Erro ao atualizar perfil: ${error.message}")
                        }
                    }.onFailure { error ->
                        showToast("Erro no upload: ${error.message}")
                    }
                }
            } catch (e: Exception) {
                showToast("Erro: ${e.message}")
            }
            
            progressBar.visibility = View.GONE
        }
    }

    private fun changeName(newName: String, callback: (Boolean) -> Unit) {
        lifecycleScope.launch {
            try {
                val result = SupabaseClient.updateUserProfile(
                    newName,
                    if (tvUserBio.text.toString() == "O utilizador não adicionou bio") "" else tvUserBio.text.toString(),
                    null
                )
                result.onSuccess {
                    callback(true)
                }.onFailure {
                    callback(false)
                }
            } catch (e: Exception) {
                callback(false)
            }
        }
    }

    private fun changeBio(newBio: String, callback: (Boolean) -> Unit) {
        lifecycleScope.launch {
            try {
                val result = SupabaseClient.updateUserProfile(
                    tvUserName.text.toString(),
                    newBio,
                    null
                )
                result.onSuccess {
                    callback(true)
                }.onFailure {
                    callback(false)
                }
            } catch (e: Exception) {
                callback(false)
            }
        }
    }

    private fun changePassword(currentPassword: String, newPassword: String, callback: (Boolean) -> Unit) {
        lifecycleScope.launch {
            try {
                val result = SupabaseClient.changePassword(currentPassword, newPassword)
                result.onSuccess {
                    callback(true)
                }.onFailure {
                    callback(false)
                }
            } catch (e: Exception) {
                callback(false)
            }
        }
    }

    private fun setupPasswordToggle(dialog: Dialog, toggleId: Int, editText: EditText) {
        val toggle = dialog.findViewById<ImageView>(toggleId)
        var isVisible = false
        
        toggle.setOnClickListener {
            isVisible = !isVisible
            if (isVisible) {
                editText.transformationMethod = HideReturnsTransformationMethod.getInstance()
                toggle.alpha = 1.0f
            } else {
                editText.transformationMethod = PasswordTransformationMethod.getInstance()
                toggle.alpha = 0.5f
            }
            editText.setSelection(editText.text.length)
        }
    }

    private fun loadUserProfile() {
        Log.d("ProfileActivity", "Loading user profile")
        progressBar.visibility = View.VISIBLE
        
        lifecycleScope.launch {
            try {
                val result = SupabaseClient.getUserProfile()
                
                result.onSuccess { data ->
                    Log.d("ProfileActivity", "Profile loaded successfully")
                    parseUserProfile(data)
                }.onFailure { error ->
                    Log.e("ProfileActivity", "Failed to load profile: ${error.message}")
                    showToast("Failed to load profile: ${error.message}")
                    loadDefaultProfile()
                }
                
            } catch (e: Exception) {
                Log.e("ProfileActivity", "Exception loading profile", e)
                showToast("Error loading profile: ${e.message}")
                loadDefaultProfile()
            }
            
            progressBar.visibility = View.GONE
        }
    }

    private fun parseUserProfile(jsonData: String) {
        try {
            val jsonObject = JSONObject(jsonData)
            
            val name = jsonObject.optString("name", sessionManager.getUserName() ?: "")
            val email = jsonObject.optString("email", sessionManager.getUserEmail() ?: "")
            val bio = jsonObject.optString("bio", "")
            val profileImageUrl = jsonObject.optString("profile_image_url", null)
            
            val displayBio = if (bio.isNotEmpty()) bio else "O utilizador não adicionou bio"
            
            updateUserInfo(name, email, displayBio, profileImageUrl)
            
            Log.d("ProfileActivity", "Profile parsed successfully")
            
        } catch (e: Exception) {
            Log.e("ProfileActivity", "Error parsing profile data", e)
            loadDefaultProfile()
        }
    }

    private fun updateUserInfo(name: String, email: String, bio: String, profileImageUrl: String?) {
        // Update main profile views
        tvUserName.text = if (name.isNotEmpty()) name else (sessionManager.getUserName() ?: "User")
        tvUserEmail.text = if (email.isNotEmpty()) email else (sessionManager.getUserEmail() ?: "")
        tvUserBio.text = bio
        
        // Update drawer views
        tvDrawerUserName.text = tvUserName.text
        tvDrawerUserEmail.text = tvUserEmail.text
        
        // Load profile images
        if (!profileImageUrl.isNullOrEmpty() && profileImageUrl != "null") {
            loadProfileImage(profileImageUrl)
            loadDrawerProfileImage(profileImageUrl)
        } else {
            ivProfilePhoto.setImageResource(R.drawable.ic_person)
            ivDrawerProfileImage.setImageResource(R.drawable.ic_person)
        }
    }

    private fun loadDefaultProfile() {
        val name = sessionManager.getUserName() ?: "User"
        val email = sessionManager.getUserEmail() ?: ""
        
        updateUserInfo(name, email, "O utilizador não adicionou bio", null)
    }

    private fun loadProfileImage(imageUrl: String) {
        Glide.with(this)
            .load(imageUrl)
            .placeholder(R.drawable.ic_person)
            .error(R.drawable.ic_person)
            .into(ivProfilePhoto)
    }

    private fun loadDrawerProfileImage(imageUrl: String) {
        Glide.with(this)
            .load(imageUrl)
            .placeholder(R.drawable.ic_person)
            .error(R.drawable.ic_person)
            .into(ivDrawerProfileImage)
    }

    private fun getImageUriFromBitmap(bitmap: Bitmap): Uri? {
        return try {
            val bytes = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bytes)
            val path = MediaStore.Images.Media.insertImage(contentResolver, bitmap, "Profile Image", null)
            Uri.parse(path)
        } catch (e: Exception) {
            Log.e("ProfileActivity", "Error converting bitmap to URI", e)
            null
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
