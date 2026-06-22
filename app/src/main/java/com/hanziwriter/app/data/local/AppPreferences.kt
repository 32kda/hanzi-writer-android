package com.hanziwriter.app.data.local

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /** The directory name of the last-selected character set, or null on first launch. */
    var selectedSetName: String?
        get() = prefs.getString(KEY_SELECTED_SET, null)
        set(value) = prefs.edit().putString(KEY_SELECTED_SET, value).commit()

    companion object {
        private const val PREFS_NAME = "hanzi_writer_prefs"
        private const val KEY_SELECTED_SET = "selected_set_name"
    }
}
