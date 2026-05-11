package com.example.wq_learner1.network

import android.content.Context

class SharedPreferencesSessionStore(context: Context) {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    fun load(): StoredSession? {
        val session = StoredSession(
            accessToken = preferences.getString(KEY_ACCESS_TOKEN, "").orEmpty(),
            tokenType = preferences.getString(KEY_TOKEN_TYPE, "").orEmpty(),
            email = preferences.getString(KEY_EMAIL, "").orEmpty(),
        )
        return session.takeIf { it.isComplete }
    }

    fun save(session: StoredSession) {
        preferences.edit()
            .putString(KEY_ACCESS_TOKEN, session.accessToken)
            .putString(KEY_TOKEN_TYPE, session.tokenType)
            .putString(KEY_EMAIL, session.email)
            .apply()
    }

    fun clear() {
        preferences.edit().clear().apply()
    }

    private companion object {
        const val PREFERENCES_NAME = "wq_learner_session"
        const val KEY_ACCESS_TOKEN = "access_token"
        const val KEY_TOKEN_TYPE = "token_type"
        const val KEY_EMAIL = "email"
    }
}
