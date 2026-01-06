package com.example.kfueityaps.data.prefs

import android.content.Context

object TocAcceptanceStore {
    private const val PREFS_NAME = "kfueityaps_prefs"
    private const val KEY_TOC_ACCEPTED = "toc_accepted"

    fun isAccepted(context: Context): Boolean {
        return context
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_TOC_ACCEPTED, false)
    }

    fun setAccepted(context: Context, accepted: Boolean) {
        context
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_TOC_ACCEPTED, accepted)
            .apply()
    }
}
