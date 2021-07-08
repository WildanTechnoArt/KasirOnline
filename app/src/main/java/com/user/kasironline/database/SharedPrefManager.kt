package com.user.kasironline.database

import android.content.Context

class SharedPrefManager private constructor(private val context: Context) {

    companion object {
        private const val USER = "user"

        private const val USERNAME = "username"
        private const val STATUS = "status"

        @Synchronized
        fun getInstance(context: Context?): SharedPrefManager? {
            var mInstance: SharedPrefManager? = null
            if (mInstance == null)
                mInstance = context?.let { SharedPrefManager(it) }
            return mInstance
        }
    }

    val getUsername: String?
        get() {
            val preferences = context.getSharedPreferences(USER, Context.MODE_PRIVATE)
            return preferences.getString(USERNAME, null)
        }

    val getStatus: Boolean
        get() {
            val preferences = context.getSharedPreferences(USER, Context.MODE_PRIVATE)
            return preferences.getBoolean(STATUS, false)
        }

    fun logoutUser(): Boolean {
        val preferences = context.getSharedPreferences(USER, Context.MODE_PRIVATE)
        val editor = preferences.edit()
        editor.clear()
        return editor.commit()
    }

    fun setUsername(username: String): Boolean {
        val preferences = context.getSharedPreferences(USER, Context.MODE_PRIVATE)
        val editor = preferences.edit()
        editor.putString(USERNAME, username)
        editor.apply()
        return true
    }

    fun setStatus(status: Boolean): Boolean {
        val preferences = context.getSharedPreferences(USER, Context.MODE_PRIVATE)
        val editor = preferences.edit()
        editor.putBoolean(STATUS, status)
        editor.apply()
        return true
    }
}