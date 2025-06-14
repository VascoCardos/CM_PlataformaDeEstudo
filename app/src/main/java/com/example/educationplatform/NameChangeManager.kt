package com.veducation.app

import android.content.Context
import android.content.SharedPreferences
import java.util.Calendar
import java.util.Date

class NameChangeManager(context: Context) {
    
    private val prefs: SharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    
    companion object {
        private const val PREF_NAME = "name_change_manager"
        private const val KEY_LAST_CHANGE_DATE = "last_change_date"
    }
    
    fun canChangeName(): Boolean {
        val lastChangeDate = getLastChangeDate()
        if (lastChangeDate == null) {
            return true // Never changed before
        }
        
        val calendar = Calendar.getInstance()
        calendar.time = lastChangeDate
        calendar.add(Calendar.MONTH, 1)
        
        val nextAllowedDate = calendar.time
        val currentDate = Date()
        
        return currentDate.after(nextAllowedDate) || currentDate.equals(nextAllowedDate)
    }
    
    fun getLastChangeDate(): Date? {
        val timestamp = prefs.getLong(KEY_LAST_CHANGE_DATE, -1)
        return if (timestamp == -1L) null else Date(timestamp)
    }
    
    fun getNextChangeDate(): Date {
        val lastChangeDate = getLastChangeDate() ?: Date()
        val calendar = Calendar.getInstance()
        calendar.time = lastChangeDate
        calendar.add(Calendar.MONTH, 1)
        return calendar.time
    }
    
    fun recordNameChange() {
        val editor = prefs.edit()
        editor.putLong(KEY_LAST_CHANGE_DATE, System.currentTimeMillis())
        editor.apply()
    }
    
    fun getDaysUntilNextChange(): Int {
        if (canChangeName()) return 0
        
        val nextChangeDate = getNextChangeDate()
        val currentDate = Date()
        val diffInMillis = nextChangeDate.time - currentDate.time
        return (diffInMillis / (1000 * 60 * 60 * 24)).toInt()
    }
}
