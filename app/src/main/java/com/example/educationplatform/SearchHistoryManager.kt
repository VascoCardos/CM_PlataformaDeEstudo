package com.veducation.app

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class SearchHistoryManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()
    
    companion object {
        private const val PREF_NAME = "SearchHistory"
        private const val KEY_SEARCH_HISTORY = "searchHistory"
        private const val MAX_HISTORY_SIZE = 10
    }
    
    fun addSearchTerm(searchTerm: String) {
        val currentHistory = getSearchHistory().toMutableList()
        
        // Remove if already exists to avoid duplicates
        currentHistory.remove(searchTerm)
        
        // Add to the beginning
        currentHistory.add(0, searchTerm)
        
        // Keep only the last 10 searches
        if (currentHistory.size > MAX_HISTORY_SIZE) {
            currentHistory.removeAt(currentHistory.size - 1)
        }
        
        saveSearchHistory(currentHistory)
    }
    
    fun getSearchHistory(): List<String> {
        val json = prefs.getString(KEY_SEARCH_HISTORY, null) ?: return emptyList()
        val type = object : TypeToken<List<String>>() {}.type
        return gson.fromJson(json, type) ?: emptyList()
    }
    
    fun removeSearchTerm(searchTerm: String) {
        val currentHistory = getSearchHistory().toMutableList()
        currentHistory.remove(searchTerm)
        saveSearchHistory(currentHistory)
    }
    
    fun clearHistory() {
        prefs.edit().remove(KEY_SEARCH_HISTORY).apply()
    }
    
    private fun saveSearchHistory(history: List<String>) {
        val json = gson.toJson(history)
        prefs.edit().putString(KEY_SEARCH_HISTORY, json).apply()
    }
}
