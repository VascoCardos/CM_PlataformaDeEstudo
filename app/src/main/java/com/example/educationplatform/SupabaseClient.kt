package com.veducation.app

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

object SupabaseClient {
    private const val SUPABASE_URL = "https://dwrmjzupjmgmjtuiuysh.supabase.co"
    private const val SUPABASE_ANON_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImR3cm1qenVwam1nbWp0dWl1eXNoIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NDk4OTc1ODQsImV4cCI6MjA2NTQ3MzU4NH0.OUBsCjdRDTZwOub6r8Q6AGKEY-wOuLS7KRFrYDRcJSY"

    // Store tokens globally
    private var accessToken: String? = null
    private var refreshToken: String? = null
    private var tokenExpiresAt: Long = 0

    fun setTokens(accessToken: String, refreshToken: String, expiresIn: Int) {
        this.accessToken = accessToken
        this.refreshToken = refreshToken
        this.tokenExpiresAt = System.currentTimeMillis() + (expiresIn * 1000L)
        Log.d("SupabaseClient", "✅ Tokens set successfully. Expires at: $tokenExpiresAt")
    }

    fun getAccessToken(): String? {
        return accessToken
    }

    fun getRefreshToken(): String? {
        return refreshToken
    }

    private fun isTokenExpired(): Boolean {
        return System.currentTimeMillis() >= (tokenExpiresAt - 60000) // Refresh 1 minute before expiry
    }

    private suspend fun refreshAccessToken(): Result<Boolean> {
        return withContext(Dispatchers.IO) {
            try {
                val currentRefreshToken = refreshToken ?: return@withContext Result.failure(Exception("No refresh token available"))

                Log.d("SupabaseClient", "🔄 Refreshing access token...")

                val url = URL("$SUPABASE_URL/auth/v1/token?grant_type=refresh_token")
                val connection = url.openConnection() as HttpURLConnection

                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "application/json")
                connection.setRequestProperty("apikey", SUPABASE_ANON_KEY)
                connection.doOutput = true

                val jsonBody = JSONObject().apply {
                    put("refresh_token", currentRefreshToken)
                }

                val writer = OutputStreamWriter(connection.outputStream)
                writer.write(jsonBody.toString())
                writer.flush()
                writer.close()

                val responseCode = connection.responseCode
                val inputStream = if (responseCode >= 400) {
                    connection.errorStream
                } else {
                    connection.inputStream
                }

                val reader = BufferedReader(InputStreamReader(inputStream))
                val response = reader.readText()
                reader.close()

                Log.d("SupabaseClient", "Refresh token response: $response")

                if (responseCode in 200..299) {
                    val jsonResponse = JSONObject(response)
                    if (jsonResponse.has("access_token")) {
                        val newAccessToken = jsonResponse.getString("access_token")
                        val newRefreshToken = jsonResponse.getString("refresh_token")
                        val expiresIn = jsonResponse.getInt("expires_in")

                        setTokens(newAccessToken, newRefreshToken, expiresIn)
                        Log.d("SupabaseClient", "✅ Token refreshed successfully")
                        Result.success(true)
                    } else {
                        Log.e("SupabaseClient", "❌ No access token in refresh response")
                        Result.failure(Exception("Failed to refresh token"))
                    }
                } else {
                    Log.e("SupabaseClient", "❌ Refresh token failed: $responseCode - $response")
                    Result.failure(Exception("Token refresh failed: $responseCode"))
                }
            } catch (e: Exception) {
                Log.e("SupabaseClient", "❌ Refresh token error", e)
                Result.failure(Exception("Token refresh error: ${e.message}"))
            }
        }
    }

    private suspend fun ensureValidToken(): Result<String> {
        return if (accessToken == null) {
            Result.failure(Exception("No access token available"))
        } else if (isTokenExpired()) {
            Log.d("SupabaseClient", "⏰ Token expired, refreshing...")
            val refreshResult = refreshAccessToken()
            if (refreshResult.isSuccess) {
                Result.success(accessToken!!)
            } else {
                refreshResult.map { accessToken!! }
            }
        } else {
            Result.success(accessToken!!)
        }
    }

    suspend fun signUp(email: String, password: String, name: String): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d("SupabaseClient", "=== SIGN UP REQUEST ===")
                Log.d("SupabaseClient", "Email: $email")
                Log.d("SupabaseClient", "Name: $name")

                val url = URL("$SUPABASE_URL/auth/v1/signup")
                val connection = url.openConnection() as HttpURLConnection

                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "application/json")
                connection.setRequestProperty("apikey", SUPABASE_ANON_KEY)
                connection.doOutput = true

                val jsonBody = JSONObject().apply {
                    put("email", email)
                    put("password", password)
                    put("data", JSONObject().apply {
                        put("name", name)
                    })
                }

                Log.d("SupabaseClient", "Request body: $jsonBody")

                val writer = OutputStreamWriter(connection.outputStream)
                writer.write(jsonBody.toString())
                writer.flush()
                writer.close()

                val responseCode = connection.responseCode
                val inputStream = if (responseCode >= 400) {
                    connection.errorStream
                } else {
                    connection.inputStream
                }

                val reader = BufferedReader(InputStreamReader(inputStream))
                val response = reader.readText()
                reader.close()

                Log.d("SupabaseClient", "SignUp Response Code: $responseCode")
                Log.d("SupabaseClient", "SignUp Response: $response")

                if (responseCode in 200..299) {
                    val jsonResponse = JSONObject(response)
                    if (jsonResponse.has("error")) {
                        val errorMessage = jsonResponse.getJSONObject("error").getString("message")
                        Result.failure(Exception(errorMessage))
                    } else {
                        Result.success("Account created successfully!")
                    }
                } else {
                    val jsonResponse = JSONObject(response)
                    val errorMessage = if (jsonResponse.has("error_description")) {
                        jsonResponse.getString("error_description")
                    } else if (jsonResponse.has("message")) {
                        jsonResponse.getString("message")
                    } else {
                        "Error creating account"
                    }
                    Result.failure(Exception(errorMessage))
                }
            } catch (e: Exception) {
                Log.e("SupabaseClient", "❌ SignUp Error", e)
                Result.failure(Exception("Connection error: ${e.message}"))
            }
        }
    }

    suspend fun signIn(email: String, password: String): Result<SignInResponse> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d("SupabaseClient", "=== SIGN IN REQUEST ===")
                Log.d("SupabaseClient", "Email: $email")

                val url = URL("$SUPABASE_URL/auth/v1/token?grant_type=password")
                val connection = url.openConnection() as HttpURLConnection

                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "application/json")
                connection.setRequestProperty("apikey", SUPABASE_ANON_KEY)
                connection.doOutput = true

                val jsonBody = JSONObject().apply {
                    put("email", email)
                    put("password", password)
                }

                Log.d("SupabaseClient", "Request body: $jsonBody")

                val writer = OutputStreamWriter(connection.outputStream)
                writer.write(jsonBody.toString())
                writer.flush()
                writer.close()

                val responseCode = connection.responseCode
                val inputStream = if (responseCode >= 400) {
                    connection.errorStream
                } else {
                    connection.inputStream
                }

                val reader = BufferedReader(InputStreamReader(inputStream))
                val response = reader.readText()
                reader.close()

                Log.d("SupabaseClient", "SignIn Response Code: $responseCode")
                Log.d("SupabaseClient", "SignIn Response: $response")

                if (responseCode in 200..299) {
                    val jsonResponse = JSONObject(response)
                    if (jsonResponse.has("access_token")) {
                        val token = jsonResponse.getString("access_token")
                        val refreshToken = jsonResponse.getString("refresh_token")
                        val expiresIn = jsonResponse.getInt("expires_in")
                        val user = jsonResponse.getJSONObject("user")
                        val userEmail = user.getString("email")
                        val userName = if (user.has("user_metadata") &&
                            user.getJSONObject("user_metadata").has("name")) {
                            user.getJSONObject("user_metadata").getString("name")
                        } else {
                            userEmail.substringBefore("@")
                        }

                        // Store the tokens
                        setTokens(token, refreshToken, expiresIn)

                        Result.success(SignInResponse(token, refreshToken, userEmail, userName))
                    } else if (jsonResponse.has("error")) {
                        val errorMessage = jsonResponse.getJSONObject("error").getString("message")
                        Result.failure(Exception(errorMessage))
                    } else {
                        Result.failure(Exception("Invalid credentials"))
                    }
                } else {
                    val jsonResponse = JSONObject(response)
                    val errorMessage = if (jsonResponse.has("error_description")) {
                        jsonResponse.getString("error_description")
                    } else if (jsonResponse.has("message")) {
                        jsonResponse.getString("message")
                    } else {
                        "Login error"
                    }
                    Result.failure(Exception(errorMessage))
                }
            } catch (e: Exception) {
                Log.e("SupabaseClient", "❌ SignIn Error", e)
                Result.failure(Exception("Connection error: ${e.message}"))
            }
        }
    }

    suspend fun changePassword(currentPassword: String, newPassword: String): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d("SupabaseClient", "=== CHANGE PASSWORD REQUEST ===")

                val tokenResult = ensureValidToken()
                if (tokenResult.isFailure) {
                    return@withContext Result.failure(tokenResult.exceptionOrNull() ?: Exception("Token validation failed"))
                }

                val validToken = tokenResult.getOrNull()!!

                val url = URL("$SUPABASE_URL/auth/v1/user")
                val connection = url.openConnection() as HttpURLConnection

                connection.requestMethod = "PUT"
                connection.setRequestProperty("Content-Type", "application/json")
                connection.setRequestProperty("apikey", SUPABASE_ANON_KEY)
                connection.setRequestProperty("Authorization", "Bearer $validToken")
                connection.doOutput = true

                val jsonBody = JSONObject().apply {
                    put("password", newPassword)
                }

                val writer = OutputStreamWriter(connection.outputStream)
                writer.write(jsonBody.toString())
                writer.flush()
                writer.close()

                val responseCode = connection.responseCode
                val inputStream = if (responseCode >= 400) {
                    connection.errorStream
                } else {
                    connection.inputStream
                }

                val reader = BufferedReader(InputStreamReader(inputStream))
                val response = reader.readText()
                reader.close()

                Log.d("SupabaseClient", "Change Password Response Code: $responseCode")
                Log.d("SupabaseClient", "Change Password Response: $response")

                if (responseCode in 200..299) {
                    Result.success("Password changed successfully")
                } else {
                    Result.failure(Exception("Failed to change password: $responseCode - $response"))
                }
            } catch (e: Exception) {
                Log.e("SupabaseClient", "❌ Change Password Error", e)
                Result.failure(Exception("Connection error: ${e.message}"))
            }
        }
    }

    suspend fun getSubjectsWithCategories(): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d("SupabaseClient", "=== GET SUBJECTS REQUEST ===")

                // Ensure we have a valid token
                val tokenResult = ensureValidToken()
                if (tokenResult.isFailure) {
                    return@withContext Result.failure(tokenResult.exceptionOrNull() ?: Exception("Token validation failed"))
                }

                val validToken = tokenResult.getOrNull()!!
                Log.d("SupabaseClient", "Using valid token for request")

                // Try different endpoints to see which one works
                val endpoints = listOf(
                    "subjects_with_details",
                    "subjects",
                    "rpc/get_subjects_with_categories"
                )

                for (endpoint in endpoints) {
                    Log.d("SupabaseClient", "Trying endpoint: $endpoint")

                    val url = URL("$SUPABASE_URL/rest/v1/$endpoint?select=*")
                    val connection = url.openConnection() as HttpURLConnection

                    connection.requestMethod = "GET"
                    connection.setRequestProperty("apikey", SUPABASE_ANON_KEY)
                    connection.setRequestProperty("Authorization", "Bearer $validToken")
                    connection.setRequestProperty("Content-Type", "application/json")

                    val responseCode = connection.responseCode
                    val inputStream = if (responseCode >= 400) {
                        connection.errorStream
                    } else {
                        connection.inputStream
                    }

                    val reader = BufferedReader(InputStreamReader(inputStream))
                    val response = reader.readText()
                    reader.close()

                    Log.d("SupabaseClient", "Endpoint: $endpoint")
                    Log.d("SupabaseClient", "Response Code: $responseCode")
                    Log.d("SupabaseClient", "Response Length: ${response.length}")
                    Log.d("SupabaseClient", "Response Preview: ${response.take(200)}")

                    if (responseCode in 200..299) {
                        Log.d("SupabaseClient", "✅ SUCCESS with endpoint: $endpoint")
                        return@withContext Result.success(response)
                    } else if (responseCode == 401) {
                        Log.w("SupabaseClient", "⚠️ 401 Unauthorized - token might be invalid")
                        // Token might be invalid, try to refresh
                        val refreshResult = refreshAccessToken()
                        if (refreshResult.isSuccess) {
                            Log.d("SupabaseClient", "🔄 Token refreshed, retrying request...")
                            continue // Retry with new token
                        }
                    } else {
                        Log.w("SupabaseClient", "⚠️ Failed with endpoint: $endpoint - $responseCode")
                        Log.w("SupabaseClient", "Error response: $response")
                    }
                }

                // If all endpoints fail
                Log.e("SupabaseClient", "❌ All endpoints failed")
                Result.failure(Exception("Unable to fetch data from any endpoint"))

            } catch (e: Exception) {
                Log.e("SupabaseClient", "❌ GetSubjects Error", e)
                Result.failure(Exception("Connection error: ${e.message}"))
            }
        }
    }

    suspend fun toggleSubjectFollow(subjectId: String): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d("SupabaseClient", "=== TOGGLE FOLLOW REQUEST ===")
                Log.d("SupabaseClient", "Subject ID: $subjectId")

                // Ensure we have a valid token
                val tokenResult = ensureValidToken()
                if (tokenResult.isFailure) {
                    return@withContext Result.failure(tokenResult.exceptionOrNull() ?: Exception("Token validation failed"))
                }

                val validToken = tokenResult.getOrNull()!!

                val url = URL("$SUPABASE_URL/rest/v1/rpc/toggle_subject_follow")
                val connection = url.openConnection() as HttpURLConnection

                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "application/json")
                connection.setRequestProperty("apikey", SUPABASE_ANON_KEY)
                connection.setRequestProperty("Authorization", "Bearer $validToken")
                connection.doOutput = true

                val jsonBody = JSONObject().apply {
                    put("subject_uuid", subjectId)
                }

                Log.d("SupabaseClient", "Request body: $jsonBody")

                val writer = OutputStreamWriter(connection.outputStream)
                writer.write(jsonBody.toString())
                writer.flush()
                writer.close()

                val responseCode = connection.responseCode
                val inputStream = if (responseCode >= 400) {
                    connection.errorStream
                } else {
                    connection.inputStream
                }

                val reader = BufferedReader(InputStreamReader(inputStream))
                val response = reader.readText()
                reader.close()

                Log.d("SupabaseClient", "ToggleFollow Response Code: $responseCode")
                Log.d("SupabaseClient", "ToggleFollow Response: $response")

                if (responseCode in 200..299) {
                    Result.success(response)
                } else if (responseCode == 401) {
                    Log.w("SupabaseClient", "⚠️ 401 Unauthorized - attempting token refresh")
                    val refreshResult = refreshAccessToken()
                    if (refreshResult.isSuccess) {
                        // Retry the request with new token
                        return@withContext toggleSubjectFollow(subjectId)
                    } else {
                        Result.failure(Exception("Authentication failed"))
                    }
                } else {
                    Result.failure(Exception("Error toggling follow: $responseCode - $response"))
                }
            } catch (e: Exception) {
                Log.e("SupabaseClient", "❌ ToggleFollow Error", e)
                Result.failure(Exception("Connection error: ${e.message}"))
            }
        }
    }

    suspend fun getUserProfile(): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d("SupabaseClient", "=== GET USER PROFILE REQUEST ===")

                val tokenResult = ensureValidToken()
                if (tokenResult.isFailure) {
                    return@withContext Result.failure(tokenResult.exceptionOrNull() ?: Exception("Token validation failed"))
                }

                val validToken = tokenResult.getOrNull()!!

                val url = URL("$SUPABASE_URL/rest/v1/rpc/get_user_profile")
                val connection = url.openConnection() as HttpURLConnection

                connection.requestMethod = "POST"
                connection.setRequestProperty("apikey", SUPABASE_ANON_KEY)
                connection.setRequestProperty("Authorization", "Bearer $validToken")
                connection.setRequestProperty("Content-Type", "application/json")
                connection.doOutput = true

                val writer = OutputStreamWriter(connection.outputStream)
                writer.write("{}")
                writer.flush()
                writer.close()

                val responseCode = connection.responseCode
                val inputStream = if (responseCode >= 400) {
                    connection.errorStream
                } else {
                    connection.inputStream
                }

                val reader = BufferedReader(InputStreamReader(inputStream))
                val response = reader.readText()
                reader.close()

                Log.d("SupabaseClient", "Get Profile Response Code: $responseCode")
                Log.d("SupabaseClient", "Get Profile Response: $response")

                if (responseCode in 200..299) {
                    Result.success(response)
                } else {
                    Result.failure(Exception("Failed to get profile: $responseCode - $response"))
                }
            } catch (e: Exception) {
                Log.e("SupabaseClient", "❌ Get Profile Error", e)
                Result.failure(Exception("Connection error: ${e.message}"))
            }
        }
    }

    suspend fun updateUserProfile(name: String, bio: String, profileImageUrl: String?): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d("SupabaseClient", "=== UPDATE USER PROFILE REQUEST ===")

                val tokenResult = ensureValidToken()
                if (tokenResult.isFailure) {
                    return@withContext Result.failure(tokenResult.exceptionOrNull() ?: Exception("Token validation failed"))
                }

                val validToken = tokenResult.getOrNull()!!

                val url = URL("$SUPABASE_URL/rest/v1/rpc/update_user_profile")
                val connection = url.openConnection() as HttpURLConnection

                connection.requestMethod = "POST"
                connection.setRequestProperty("apikey", SUPABASE_ANON_KEY)
                connection.setRequestProperty("Authorization", "Bearer $validToken")
                connection.setRequestProperty("Content-Type", "application/json")
                connection.doOutput = true

                val jsonBody = JSONObject().apply {
                    put("user_name", name)
                    put("user_bio", bio)
                    if (profileImageUrl != null) {
                        put("profile_image_url", profileImageUrl)
                    }
                }

                Log.d("SupabaseClient", "Update Profile Request: $jsonBody")

                val writer = OutputStreamWriter(connection.outputStream)
                writer.write(jsonBody.toString())
                writer.flush()
                writer.close()

                val responseCode = connection.responseCode
                val inputStream = if (responseCode >= 400) {
                    connection.errorStream
                } else {
                    connection.inputStream
                }

                val reader = BufferedReader(InputStreamReader(inputStream))
                val response = reader.readText()
                reader.close()

                Log.d("SupabaseClient", "Update Profile Response Code: $responseCode")
                Log.d("SupabaseClient", "Update Profile Response: $response")

                if (responseCode in 200..299) {
                    Result.success(response)
                } else {
                    Result.failure(Exception("Failed to update profile: $responseCode - $response"))
                }
            } catch (e: Exception) {
                Log.e("SupabaseClient", "❌ Update Profile Error", e)
                Result.failure(Exception("Connection error: ${e.message}"))
            }
        }
    }

    suspend fun uploadProfileImage(imageBytes: ByteArray): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d("SupabaseClient", "=== UPLOAD PROFILE IMAGE REQUEST ===")
                Log.d("SupabaseClient", "Image size: ${imageBytes.size} bytes")

                val tokenResult = ensureValidToken()
                if (tokenResult.isFailure) {
                    return@withContext Result.failure(tokenResult.exceptionOrNull() ?: Exception("Token validation failed"))
                }

                val validToken = tokenResult.getOrNull()!!
                val fileName = "profile_${System.currentTimeMillis()}.jpg"

                Log.d("SupabaseClient", "Uploading file: $fileName")

                // Upload directly to the bucket
                val url = URL("$SUPABASE_URL/storage/v1/object/profile-images/$fileName")
                val connection = url.openConnection() as HttpURLConnection

                connection.requestMethod = "POST"
                connection.setRequestProperty("apikey", SUPABASE_ANON_KEY)
                connection.setRequestProperty("Authorization", "Bearer $validToken")
                connection.setRequestProperty("Content-Type", "image/jpeg")
                connection.setRequestProperty("Cache-Control", "3600")
                connection.setRequestProperty("x-upsert", "true")
                connection.doOutput = true
                connection.doInput = true

                Log.d("SupabaseClient", "Writing image bytes...")
                val outputStream = connection.outputStream
                outputStream.write(imageBytes)
                outputStream.flush()
                outputStream.close()

                val responseCode = connection.responseCode
                Log.d("SupabaseClient", "Upload response code: $responseCode")

                val inputStream = if (responseCode >= 400) {
                    connection.errorStream
                } else {
                    connection.inputStream
                }

                val reader = BufferedReader(InputStreamReader(inputStream))
                val response = reader.readText()
                reader.close()

                Log.d("SupabaseClient", "Upload Image Response Code: $responseCode")
                Log.d("SupabaseClient", "Upload Image Response: $response")

                if (responseCode in 200..299) {
                    val publicUrl = "$SUPABASE_URL/storage/v1/object/public/profile-images/$fileName"
                    Log.d("SupabaseClient", "✅ Image uploaded successfully: $publicUrl")
                    Result.success(publicUrl)
                } else {
                    Log.e("SupabaseClient", "❌ Upload failed: $responseCode - $response")
                    Result.failure(Exception("Failed to upload image: $responseCode - $response"))
                }
            } catch (e: Exception) {
                Log.e("SupabaseClient", "❌ Upload Image Error", e)
                Result.failure(Exception("Connection error: ${e.message}"))
            }
        }
    }

    suspend fun uploadStudyFile(fileBytes: ByteArray, fileName: String, mimeType: String): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d("SupabaseClient", "=== ULTRA SIMPLE UPLOAD ===")
                Log.d("SupabaseClient", "📁 File: $fileName")
                Log.d("SupabaseClient", "📊 Size: ${fileBytes.size} bytes")

                // Basic validations
                if (fileBytes.isEmpty()) {
                    return@withContext Result.failure(Exception("File is empty"))
                }

                val tokenResult = ensureValidToken()
                if (tokenResult.isFailure) {
                    return@withContext Result.failure(Exception("Please login again"))
                }

                val validToken = tokenResult.getOrNull()!!
                
                // Super clean filename
                val cleanFileName = fileName.replace("[^a-zA-Z0-9._-]".toRegex(), "_")
                val uniqueFileName = "${System.currentTimeMillis()}_$cleanFileName"
                
                Log.d("SupabaseClient", "🔧 Uploading as: $uniqueFileName")

                // MINIMAL APPROACH - Just like profile images but even simpler
                val url = URL("$SUPABASE_URL/storage/v1/object/study-files/$uniqueFileName")
                val connection = url.openConnection() as HttpURLConnection

                // ABSOLUTE MINIMUM headers
                connection.requestMethod = "POST"
                connection.setRequestProperty("apikey", SUPABASE_ANON_KEY)
                connection.setRequestProperty("Authorization", "Bearer $validToken")
                connection.doOutput = true

                Log.d("SupabaseClient", "⬆️ Uploading...")
                
                // Write file
                connection.outputStream.use { it.write(fileBytes) }

                val responseCode = connection.responseCode
                val response = if (responseCode >= 400) {
                    connection.errorStream?.bufferedReader()?.readText() ?: "No error details"
                } else {
                    connection.inputStream?.bufferedReader()?.readText() ?: "No response"
                }

                Log.d("SupabaseClient", "📥 Response Code: $responseCode")
                Log.d("SupabaseClient", "📝 Response Body: $response")

                if (responseCode in 200..299) {
                    val publicUrl = "$SUPABASE_URL/storage/v1/object/public/study-files/$uniqueFileName"
                    Log.d("SupabaseClient", "✅ SUCCESS! URL: $publicUrl")
                    Result.success(publicUrl)
                } else {
                    Log.e("SupabaseClient", "❌ Upload failed: $responseCode")
                    Log.e("SupabaseClient", "❌ Error details: $response")
                    
                    val errorMsg = when (responseCode) {
                        400 -> "Invalid file or bucket configuration. Check bucket setup."
                        401 -> "Authentication failed. Please login again."
                        403 -> "Permission denied. Check bucket policies."
                        404 -> "Bucket not found. Run the SQL script first."
                        413 -> "File too large. Maximum 50MB allowed."
                        else -> "Upload failed with code $responseCode"
                    }
                    
                    Result.failure(Exception(errorMsg))
                }
            } catch (e: Exception) {
                Log.e("SupabaseClient", "❌ Upload exception", e)
                Result.failure(Exception("Upload error: ${e.message}"))
            }
        }
    }

    // VERSÃO SIMPLIFICADA - Criar estudo diretamente na tabela
    suspend fun createStudy(
        title: String,
        content: String,
        description: String? = null,
        status: String = "public",
        studyType: String = "other",
        subjectId: String,
        fileUrls: List<String> = emptyList()
    ): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d("SupabaseClient", "=== CREATE STUDY SIMPLE ===")
                Log.d("SupabaseClient", "📚 Title: $title")
                Log.d("SupabaseClient", "🎯 Subject: $subjectId")
                Log.d("SupabaseClient", "📎 Files: ${fileUrls.size}")

                val tokenResult = ensureValidToken()
                if (tokenResult.isFailure) {
                    return@withContext Result.failure(Exception("Authentication required"))
                }

                val validToken = tokenResult.getOrNull()!!
                val userId = getCurrentUserId()
                
                if (userId == null) {
                    return@withContext Result.failure(Exception("Unable to get user ID"))
                }

                Log.d("SupabaseClient", "👤 User ID: $userId")

                // INSERIR DIRETAMENTE NA TABELA studies
                val url = URL("$SUPABASE_URL/rest/v1/studies")
                val connection = url.openConnection() as HttpURLConnection

                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "application/json")
                connection.setRequestProperty("apikey", SUPABASE_ANON_KEY)
                connection.setRequestProperty("Authorization", "Bearer $validToken")
                connection.setRequestProperty("Prefer", "return=representation")
                connection.doOutput = true

                val jsonBody = JSONObject().apply {
                    put("title", title)
                    put("content", content)
                    put("subject_id", subjectId)
                    put("author_id", userId)
                    if (description != null) put("description", description)
                    put("status", status)
                    put("study_type", studyType)
                }

                Log.d("SupabaseClient", "📤 Request body: $jsonBody")

                val writer = OutputStreamWriter(connection.outputStream)
                writer.write(jsonBody.toString())
                writer.flush()
                writer.close()

                val responseCode = connection.responseCode
                val inputStream = if (responseCode >= 400) {
                    connection.errorStream
                } else {
                    connection.inputStream
                }

                val reader = BufferedReader(InputStreamReader(inputStream))
                val response = reader.readText()
                reader.close()

                Log.d("SupabaseClient", "📥 Response Code: $responseCode")
                Log.d("SupabaseClient", "📝 Response Body: $response")

                if (responseCode in 200..299) {
                    Log.d("SupabaseClient", "✅ Study created successfully!")
                    
                    // Se há arquivos, adicionar eles também
                    if (fileUrls.isNotEmpty()) {
                        val studyResponse = JSONArray(response)
                        if (studyResponse.length() > 0) {
                            val studyId = studyResponse.getJSONObject(0).getString("id")
                            Log.d("SupabaseClient", "📎 Adding ${fileUrls.size} files to study $studyId")
                            
                            // Adicionar arquivos (mas não falhar se der erro)
                            try {
                                addFilesToStudy(studyId, fileUrls, userId)
                            } catch (e: Exception) {
                                Log.w("SupabaseClient", "⚠️ Failed to add files, but study was created: ${e.message}")
                            }
                        }
                    }
                    
                    Result.success(response)
                } else {
                    Log.e("SupabaseClient", "❌ Failed to create study: $responseCode")
                    Result.failure(Exception("Failed to create study: $responseCode - $response"))
                }
            } catch (e: Exception) {
                Log.e("SupabaseClient", "❌ Create Study Error", e)
                Result.failure(Exception("Error creating study: ${e.message}"))
            }
        }
    }

    suspend fun getStudiesBySubject(subjectId: String, sortBy: String = "hot"): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d("SupabaseClient", "=== GET STUDIES BY SUBJECT ===")
                Log.d("SupabaseClient", "Subject ID: $subjectId, Sort: $sortBy")

                val tokenResult = ensureValidToken()
                if (tokenResult.isFailure) {
                    return@withContext Result.failure(tokenResult.exceptionOrNull() ?: Exception("Token validation failed"))
                }

                val validToken = tokenResult.getOrNull()!!

                val url = URL("$SUPABASE_URL/rest/v1/rpc/get_studies_by_subject")
                val connection = url.openConnection() as HttpURLConnection

                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "application/json")
                connection.setRequestProperty("apikey", SUPABASE_ANON_KEY)
                connection.setRequestProperty("Authorization", "Bearer $validToken")
                connection.doOutput = true

                val jsonBody = JSONObject().apply {
                    put("subject_uuid", subjectId)
                    put("sort_by", sortBy)
                }

                val writer = OutputStreamWriter(connection.outputStream)
                writer.write(jsonBody.toString())
                writer.flush()
                writer.close()

                val responseCode = connection.responseCode
                val inputStream = if (responseCode >= 400) {
                    connection.errorStream
                } else {
                    connection.inputStream
                }

                val reader = BufferedReader(InputStreamReader(inputStream))
                val response = reader.readText()
                reader.close()

                Log.d("SupabaseClient", "Get Studies Response Code: $responseCode")
                Log.d("SupabaseClient", "Get Studies Response: $response")

                if (responseCode in 200..299) {
                    Result.success(response)
                } else {
                    Result.failure(Exception("Failed to get studies: $responseCode - $response"))
                }
            } catch (e: Exception) {
                Log.e("SupabaseClient", "❌ Get Studies Error", e)
                Result.failure(Exception("Connection error: ${e.message}"))
            }
        }
    }

    suspend fun voteStudy(studyId: String, voteType: String): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d("SupabaseClient", "=== VOTE STUDY ===")
                Log.d("SupabaseClient", "Study ID: $studyId, Vote: $voteType")

                val tokenResult = ensureValidToken()
                if (tokenResult.isFailure) {
                    return@withContext Result.failure(tokenResult.exceptionOrNull() ?: Exception("Token validation failed"))
                }

                val validToken = tokenResult.getOrNull()!!

                val url = URL("$SUPABASE_URL/rest/v1/rpc/vote_study")
                val connection = url.openConnection() as HttpURLConnection

                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "application/json")
                connection.setRequestProperty("apikey", SUPABASE_ANON_KEY)
                connection.setRequestProperty("Authorization", "Bearer $validToken")
                connection.doOutput = true

                val jsonBody = JSONObject().apply {
                    put("study_uuid", studyId)
                    put("vote_type", voteType)
                }

                val writer = OutputStreamWriter(connection.outputStream)
                writer.write(jsonBody.toString())
                writer.flush()
                writer.close()

                val responseCode = connection.responseCode
                val inputStream = if (responseCode >= 400) {
                    connection.errorStream
                } else {
                    connection.inputStream
                }

                val reader = BufferedReader(InputStreamReader(inputStream))
                val response = reader.readText()
                reader.close()

                Log.d("SupabaseClient", "Vote Study Response Code: $responseCode")
                Log.d("SupabaseClient", "Vote Study Response: $response")

                if (responseCode in 200..299) {
                    Result.success(response)
                } else {
                    Result.failure(Exception("Failed to vote: $responseCode - $response"))
                }
            } catch (e: Exception) {
                Log.e("SupabaseClient", "❌ Vote Study Error", e)
                Result.failure(Exception("Connection error: ${e.message}"))
            }
        }
    }

    suspend fun toggleStudySave(studyId: String): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d("SupabaseClient", "=== TOGGLE STUDY SAVE ===")
                Log.d("SupabaseClient", "Study ID: $studyId")

                val tokenResult = ensureValidToken()
                if (tokenResult.isFailure) {
                    return@withContext Result.failure(tokenResult.exceptionOrNull() ?: Exception("Token validation failed"))
                }

                val validToken = tokenResult.getOrNull()!!

                val url = URL("$SUPABASE_URL/rest/v1/rpc/toggle_study_save")
                val connection = url.openConnection() as HttpURLConnection

                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "application/json")
                connection.setRequestProperty("apikey", SUPABASE_ANON_KEY)
                connection.setRequestProperty("Authorization", "Bearer $validToken")
                connection.doOutput = true

                val jsonBody = JSONObject().apply {
                    put("study_uuid", studyId)
                }

                val writer = OutputStreamWriter(connection.outputStream)
                writer.write(jsonBody.toString())
                writer.flush()
                writer.close()

                val responseCode = connection.responseCode
                val inputStream = if (responseCode >= 400) {
                    connection.errorStream
                } else {
                    connection.inputStream
                }

                val reader = BufferedReader(InputStreamReader(inputStream))
                val response = reader.readText()
                reader.close()

                Log.d("SupabaseClient", "Toggle Save Response Code: $responseCode")
                Log.d("SupabaseClient", "Toggle Save Response: $response")

                if (responseCode in 200..299) {
                    Result.success(response)
                } else {
                    Result.failure(Exception("Failed to toggle save: $responseCode - $response"))
                }
            } catch (e: Exception) {
                Log.e("SupabaseClient", "❌ Toggle Save Error", e)
                Result.failure(Exception("Connection error: ${e.message}"))
            }
        }
    }

    suspend fun getMyStudies(sortBy: String = "new"): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d("SupabaseClient", "=== GET MY STUDIES ===")
                Log.d("SupabaseClient", "Sort: $sortBy")

                val tokenResult = ensureValidToken()
                if (tokenResult.isFailure) {
                    return@withContext Result.failure(tokenResult.exceptionOrNull() ?: Exception("Token validation failed"))
                }

                val validToken = tokenResult.getOrNull()!!

                val url = URL("$SUPABASE_URL/rest/v1/rpc/get_my_studies")
                val connection = url.openConnection() as HttpURLConnection

                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "application/json")
                connection.setRequestProperty("apikey", SUPABASE_ANON_KEY)
                connection.setRequestProperty("Authorization", "Bearer $validToken")
                connection.doOutput = true

                val jsonBody = JSONObject().apply {
                    put("sort_by", sortBy)
                }

                val writer = OutputStreamWriter(connection.outputStream)
                writer.write(jsonBody.toString())
                writer.flush()
                writer.close()

                val responseCode = connection.responseCode
                val inputStream = if (responseCode >= 400) {
                    connection.errorStream
                } else {
                    connection.inputStream
                }

                val reader = BufferedReader(InputStreamReader(inputStream))
                val response = reader.readText()
                reader.close()

                Log.d("SupabaseClient", "Get My Studies Response Code: $responseCode")
                Log.d("SupabaseClient", "Get My Studies Response: $response")

                if (responseCode in 200..299) {
                    Result.success(response)
                } else {
                    Result.failure(Exception("Failed to get my studies: $responseCode - $response"))
                }
            } catch (e: Exception) {
                Log.e("SupabaseClient", "❌ Get My Studies Error", e)
                Result.failure(Exception("Connection error: ${e.message}"))
            }
        }
    }

    private suspend fun addFilesToStudy(studyId: String, fileUrls: List<String>, userId: String) {
        return withContext(Dispatchers.IO) {
            try {
                val tokenResult = ensureValidToken()
                if (tokenResult.isFailure) return@withContext

                val validToken = tokenResult.getOrNull()!!

                for (fileUrl in fileUrls) {
                    val url = URL("$SUPABASE_URL/rest/v1/study_files")
                    val connection = url.openConnection() as HttpURLConnection

                    connection.requestMethod = "POST"
                    connection.setRequestProperty("Content-Type", "application/json")
                    connection.setRequestProperty("apikey", SUPABASE_ANON_KEY)
                    connection.setRequestProperty("Authorization", "Bearer $validToken")
                    connection.doOutput = true

                    val fileName = fileUrl.substringAfterLast("/")
                    val jsonBody = JSONObject().apply {
                        put("study_id", studyId)
                        put("file_url", fileUrl)
                        put("file_name", fileName)
                        put("uploaded_by", userId)
                    }

                    val writer = OutputStreamWriter(connection.outputStream)
                    writer.write(jsonBody.toString())
                    writer.flush()
                    writer.close()

                    val responseCode = connection.responseCode
                    Log.d("SupabaseClient", "📎 File added: $responseCode")
                }
            } catch (e: Exception) {
                Log.w("SupabaseClient", "⚠️ Error adding files: ${e.message}")
            }
        }
    }

    private fun getCurrentUserId(): String? {
        return try {
            val token = accessToken ?: return null
            val parts = token.split(".")
            if (parts.size != 3) return null
        
            val payload = parts[1]
            val decodedBytes = android.util.Base64.decode(payload, android.util.Base64.URL_SAFE)
            val decodedString = String(decodedBytes)
            val jsonObject = JSONObject(decodedString)
        
            jsonObject.getString("sub") // 'sub' contains the user ID in JWT
        } catch (e: Exception) {
            Log.e("SupabaseClient", "Error extracting user ID from token", e)
            null
        }
    }

    suspend fun getStudyComments(studyId: String): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d("SupabaseClient", "=== GET STUDY COMMENTS ===")
                Log.d("SupabaseClient", "Study ID: $studyId")

                val tokenResult = ensureValidToken()
                if (tokenResult.isFailure) {
                    return@withContext Result.failure(tokenResult.exceptionOrNull() ?: Exception("Token validation failed"))
                }

                val validToken = tokenResult.getOrNull()!!

                val url = URL("$SUPABASE_URL/rest/v1/rpc/get_study_comments")
                val connection = url.openConnection() as HttpURLConnection

                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "application/json")
                connection.setRequestProperty("apikey", SUPABASE_ANON_KEY)
                connection.setRequestProperty("Authorization", "Bearer $validToken")
                connection.doOutput = true

                val jsonBody = JSONObject().apply {
                    put("study_uuid", studyId)
                }

                val writer = OutputStreamWriter(connection.outputStream)
                writer.write(jsonBody.toString())
                writer.flush()
                writer.close()

                val responseCode = connection.responseCode
                val inputStream = if (responseCode >= 400) {
                    connection.errorStream
                } else {
                    connection.inputStream
                }

                val reader = BufferedReader(InputStreamReader(inputStream))
                val response = reader.readText()
                reader.close()

                Log.d("SupabaseClient", "Get Comments Response Code: $responseCode")
                Log.d("SupabaseClient", "Get Comments Response: $response")

                if (responseCode in 200..299) {
                    Result.success(response)
                } else {
                    Result.failure(Exception("Failed to get comments: $responseCode - $response"))
                }
            } catch (e: Exception) {
                Log.e("SupabaseClient", "❌ Get Comments Error", e)
                Result.failure(Exception("Connection error: ${e.message}"))
            }
        }
    }

    suspend fun postComment(studyId: String, content: String): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d("SupabaseClient", "=== POST COMMENT ===")
                Log.d("SupabaseClient", "Study ID: $studyId")

                val tokenResult = ensureValidToken()
                if (tokenResult.isFailure) {
                    return@withContext Result.failure(tokenResult.exceptionOrNull() ?: Exception("Token validation failed"))
                }

                val validToken = tokenResult.getOrNull()!!

                val url = URL("$SUPABASE_URL/rest/v1/rpc/post_comment")
                val connection = url.openConnection() as HttpURLConnection

                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "application/json")
                connection.setRequestProperty("apikey", SUPABASE_ANON_KEY)
                connection.setRequestProperty("Authorization", "Bearer $validToken")
                connection.doOutput = true

                val jsonBody = JSONObject().apply {
                    put("study_uuid", studyId)
                    put("comment_content", content)
                }

                val writer = OutputStreamWriter(connection.outputStream)
                writer.write(jsonBody.toString())
                writer.flush()
                writer.close()

                val responseCode = connection.responseCode
                val inputStream = if (responseCode >= 400) {
                    connection.errorStream
                } else {
                    connection.inputStream
                }

                val reader = BufferedReader(InputStreamReader(inputStream))
                val response = reader.readText()
                reader.close()

                Log.d("SupabaseClient", "Post Comment Response Code: $responseCode")
                Log.d("SupabaseClient", "Post Comment Response: $response")

                if (responseCode in 200..299) {
                    Result.success(response)
                } else {
                    Result.failure(Exception("Failed to post comment: $responseCode - $response"))
                }
            } catch (e: Exception) {
                Log.e("SupabaseClient", "❌ Post Comment Error", e)
                Result.failure(Exception("Connection error: ${e.message}"))
            }
        }
    }

    suspend fun voteComment(commentId: String, voteType: String): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d("SupabaseClient", "=== VOTE COMMENT ===")
                Log.d("SupabaseClient", "Comment ID: $commentId, Vote: $voteType")

                val tokenResult = ensureValidToken()
                if (tokenResult.isFailure) {
                    return@withContext Result.failure(tokenResult.exceptionOrNull() ?: Exception("Token validation failed"))
                }

                val validToken = tokenResult.getOrNull()!!

                val url = URL("$SUPABASE_URL/rest/v1/rpc/vote_comment")
                val connection = url.openConnection() as HttpURLConnection

                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "application/json")
                connection.setRequestProperty("apikey", SUPABASE_ANON_KEY)
                connection.setRequestProperty("Authorization", "Bearer $validToken")
                connection.doOutput = true

                val jsonBody = JSONObject().apply {
                    put("comment_uuid", commentId)
                    put("vote_type_param", voteType)
                }

                val writer = OutputStreamWriter(connection.outputStream)
                writer.write(jsonBody.toString())
                writer.flush()
                writer.close()

                val responseCode = connection.responseCode
                val inputStream = if (responseCode >= 400) {
                    connection.errorStream
                } else {
                    connection.inputStream
                }

                val reader = BufferedReader(InputStreamReader(inputStream))
                val response = reader.readText()
                reader.close()

                Log.d("SupabaseClient", "Vote Comment Response Code: $responseCode")
                Log.d("SupabaseClient", "Vote Comment Response: $response")

                if (responseCode in 200..299) {
                    Result.success(response)
                } else {
                    Result.failure(Exception("Failed to vote comment: $responseCode - $response"))
                }
            } catch (e: Exception) {
                Log.e("SupabaseClient", "❌ Vote Comment Error", e)
                Result.failure(Exception("Connection error: ${e.message}"))
            }
        }
    }

    fun clearTokens() {
        accessToken = null
        refreshToken = null
        tokenExpiresAt = 0
        Log.d("SupabaseClient", "🧹 Tokens cleared")
    }
}

data class SignInResponse(
    val accessToken: String,
    val refreshToken: String,
    val email: String,
    val name: String
)
