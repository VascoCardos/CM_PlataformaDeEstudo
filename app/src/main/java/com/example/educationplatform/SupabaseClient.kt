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

    // Store access token globally
    private var accessToken: String? = null

    fun setAccessToken(token: String) {
        accessToken = token
        Log.d("SupabaseClient", "Access token set: ${token.take(20)}...")
    }

    fun getAccessToken(): String? {
        return accessToken
    }

    suspend fun signUp(email: String, password: String, name: String): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
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
                Log.e("SupabaseClient", "SignUp Error", e)
                Result.failure(Exception("Connection error: ${e.message}"))
            }
        }
    }

    suspend fun signIn(email: String, password: String): Result<SignInResponse> {
        return withContext(Dispatchers.IO) {
            try {
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

                Log.d("SupabaseClient", "SignIn Response: $response")

                if (responseCode in 200..299) {
                    val jsonResponse = JSONObject(response)
                    if (jsonResponse.has("access_token")) {
                        val token = jsonResponse.getString("access_token")
                        val user = jsonResponse.getJSONObject("user")
                        val userEmail = user.getString("email")
                        val userName = if (user.has("user_metadata") &&
                            user.getJSONObject("user_metadata").has("name")) {
                            user.getJSONObject("user_metadata").getString("name")
                        } else {
                            userEmail.substringBefore("@")
                        }

                        // Store the access token
                        setAccessToken(token)

                        Result.success(SignInResponse(token, userEmail, userName))
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
                Log.e("SupabaseClient", "SignIn Error", e)
                Result.failure(Exception("Connection error: ${e.message}"))
            }
        }
    }

    suspend fun getSubjectsWithCategories(): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d("SupabaseClient", "Getting subjects with token: ${getAccessToken()?.take(20)}...")

                val url = URL("$SUPABASE_URL/rest/v1/subjects_with_details?select=*")
                val connection = url.openConnection() as HttpURLConnection

                connection.requestMethod = "GET"
                connection.setRequestProperty("apikey", SUPABASE_ANON_KEY)
                connection.setRequestProperty("Authorization", "Bearer ${getAccessToken()}")
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

                Log.d("SupabaseClient", "GetSubjects Response Code: $responseCode")
                Log.d("SupabaseClient", "GetSubjects Response: $response")

                if (responseCode in 200..299) {
                    Result.success(response)
                } else {
                    Result.failure(Exception("Error fetching subjects: $responseCode - $response"))
                }
            } catch (e: Exception) {
                Log.e("SupabaseClient", "GetSubjects Error", e)
                Result.failure(Exception("Connection error: ${e.message}"))
            }
        }
    }

    suspend fun toggleSubjectFollow(subjectId: String): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d("SupabaseClient", "Toggling follow for subject: $subjectId")

                val url = URL("$SUPABASE_URL/rest/v1/rpc/toggle_subject_follow")
                val connection = url.openConnection() as HttpURLConnection

                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "application/json")
                connection.setRequestProperty("apikey", SUPABASE_ANON_KEY)
                connection.setRequestProperty("Authorization", "Bearer ${getAccessToken()}")
                connection.doOutput = true

                val jsonBody = JSONObject().apply {
                    put("subject_uuid", subjectId)
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

                Log.d("SupabaseClient", "ToggleFollow Response: $response")

                if (responseCode in 200..299) {
                    Result.success(response)
                } else {
                    Result.failure(Exception("Error toggling follow: $responseCode - $response"))
                }
            } catch (e: Exception) {
                Log.e("SupabaseClient", "ToggleFollow Error", e)
                Result.failure(Exception("Connection error: ${e.message}"))
            }
        }
    }
}

data class SignInResponse(
    val accessToken: String,
    val email: String,
    val name: String
)
