package com.veducation.app

import android.content.Context
import android.content.SharedPreferences
import android.util.Log

class SessionManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREF_NAME = "user_session"
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
        private const val KEY_USER_EMAIL = "user_email"
        private const val KEY_USER_NAME = "user_name"
        private const val KEY_ACCESS_TOKEN = "access_token"
        private const val KEY_REFRESH_TOKEN = "refresh_token"
        private const val KEY_TOKEN_EXPIRES_AT = "token_expires_at"
    }

    fun createLoginSession(email: String, name: String, accessToken: String, refreshToken: String, expiresIn: Int) {
        val editor = prefs.edit()
        val expiresAt = System.currentTimeMillis() + (expiresIn * 1000L)

        editor.putBoolean(KEY_IS_LOGGED_IN, true)
        editor.putString(KEY_USER_EMAIL, email)
        editor.putString(KEY_USER_NAME, name)
        editor.putString(KEY_ACCESS_TOKEN, accessToken)
        editor.putString(KEY_REFRESH_TOKEN, refreshToken)
        editor.putLong(KEY_TOKEN_EXPIRES_AT, expiresAt)
        editor.apply()

        // Also set tokens in SupabaseClient
        SupabaseClient.setTokens(accessToken, refreshToken, expiresIn)

        Log.d("SessionManager", "✅ Login session created for: $email")
    }

    // Overload for backward compatibility (without refresh token)
    fun createLoginSession(email: String, name: String, accessToken: String) {
        val editor = prefs.edit()

        editor.putBoolean(KEY_IS_LOGGED_IN, true)
        editor.putString(KEY_USER_EMAIL, email)
        editor.putString(KEY_USER_NAME, name)
        editor.putString(KEY_ACCESS_TOKEN, accessToken)
        editor.apply()

        Log.d("SessionManager", "✅ Login session created for: $email (without refresh token)")
    }

    fun isLoggedIn(): Boolean {
        return prefs.getBoolean(KEY_IS_LOGGED_IN, false)
    }

    fun getUserEmail(): String? {
        return prefs.getString(KEY_USER_EMAIL, null)
    }

    fun getUserName(): String? {
        return prefs.getString(KEY_USER_NAME, null)
    }

    fun getAccessToken(): String? {
        return prefs.getString(KEY_ACCESS_TOKEN, null)
    }

    fun getRefreshToken(): String? {
        return prefs.getString(KEY_REFRESH_TOKEN, null)
    }

    fun getTokenExpiresAt(): Long {
        return prefs.getLong(KEY_TOKEN_EXPIRES_AT, 0)
    }

    fun updateTokens(accessToken: String, refreshToken: String, expiresIn: Int) {
        val editor = prefs.edit()
        val expiresAt = System.currentTimeMillis() + (expiresIn * 1000L)

        editor.putString(KEY_ACCESS_TOKEN, accessToken)
        editor.putString(KEY_REFRESH_TOKEN, refreshToken)
        editor.putLong(KEY_TOKEN_EXPIRES_AT, expiresAt)
        editor.apply()

        Log.d("SessionManager", "✅ Tokens updated in session")
    }

    fun logout() {
        val editor = prefs.edit()
        editor.clear()
        editor.apply()

        // Clear tokens from SupabaseClient
        SupabaseClient.clearTokens()

        Log.d("SessionManager", "🚪 User logged out")
    }
}
