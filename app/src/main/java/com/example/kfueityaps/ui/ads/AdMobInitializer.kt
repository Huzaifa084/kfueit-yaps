package com.example.kfueityaps.ui.ads

import android.content.Context
import android.util.Log
import com.google.android.gms.ads.MobileAds
import java.util.concurrent.atomic.AtomicBoolean

object AdMobInitializer {
    private val initialized = AtomicBoolean(false)
    private const val TAG = "AdMobInitializer"

    fun ensureInitialized(context: Context) {
        if (initialized.compareAndSet(false, true)) {
            Log.d(TAG, "Initializing MobileAds")
            MobileAds.initialize(context) { status ->
                Log.d(TAG, "MobileAds initialized. adapters=${status.adapterStatusMap.keys}")
            }
        }
    }
}
