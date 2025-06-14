package com.veducation.app

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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
