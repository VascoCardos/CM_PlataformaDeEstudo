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
    
    suspend fun signUp(email: String, password: String, name: String): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                val url = URL("$SUPABASE_URL/auth/v1/signup")
                val connection = url.openConnection() as HttpURLConnection
                
                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "application/json")
                connection.setRequestProperty("apikey", SUPABASE_ANON_KEY)
                connection.doOutput = true
                
                // Create JSON body
                val jsonBody = JSONObject().apply {
                    put("email", email)
                    put("password", password)
                    put("data", JSONObject().apply {
                        put("name", name)
                    })
                }
                
                // Send request
                val writer = OutputStreamWriter(connection.outputStream)
                writer.write(jsonBody.toString())
                writer.flush()
                writer.close()
                
                // Read response
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
                        Result.success("Conta criada com sucesso!")
                    }
                } else {
                    val jsonResponse = JSONObject(response)
                    val errorMessage = if (jsonResponse.has("error_description")) {
                        jsonResponse.getString("error_description")
                    } else if (jsonResponse.has("message")) {
                        jsonResponse.getString("message")
                    } else {
                        "Erro ao criar conta"
                    }
                    Result.failure(Exception(errorMessage))
                }
            } catch (e: Exception) {
                Log.e("SupabaseClient", "SignUp Error", e)
                Result.failure(Exception("Erro de conexão: ${e.message}"))
            }
        }
    }
    
    suspend fun signIn(email: String, password: String): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                val url = URL("$SUPABASE_URL/auth/v1/token?grant_type=password")
                val connection = url.openConnection() as HttpURLConnection
                
                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "application/json")
                connection.setRequestProperty("apikey", SUPABASE_ANON_KEY)
                connection.doOutput = true
                
                // Create JSON body
                val jsonBody = JSONObject().apply {
                    put("email", email)
                    put("password", password)
                }
                
                // Send request
                val writer = OutputStreamWriter(connection.outputStream)
                writer.write(jsonBody.toString())
                writer.flush()
                writer.close()
                
                // Read response
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
                        Result.success("Login realizado com sucesso!")
                    } else if (jsonResponse.has("error")) {
                        val errorMessage = jsonResponse.getJSONObject("error").getString("message")
                        Result.failure(Exception(errorMessage))
                    } else {
                        Result.failure(Exception("Credenciais inválidas"))
                    }
                } else {
                    val jsonResponse = JSONObject(response)
                    val errorMessage = if (jsonResponse.has("error_description")) {
                        jsonResponse.getString("error_description")
                    } else if (jsonResponse.has("message")) {
                        jsonResponse.getString("message")
                    } else {
                        "Erro no login"
                    }
                    Result.failure(Exception(errorMessage))
                }
            } catch (e: Exception) {
                Log.e("SupabaseClient", "SignIn Error", e)
                Result.failure(Exception("Erro de conexão: ${e.message}"))
            }
        }
    }
}
