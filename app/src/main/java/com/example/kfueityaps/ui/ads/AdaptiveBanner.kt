package com.example.kfueityaps.ui.ads

import android.app.Activity
import android.util.Log
import android.view.View
import android.widget.FrameLayout
import androidx.core.view.doOnLayout
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError

object AdaptiveBanner {

    private const val TAG = "AdaptiveBanner"

    fun load(
        activity: Activity,
        container: FrameLayout,
        adUnitId: String,
    ): AdView {
        AdMobInitializer.ensureInitialized(activity)

        Log.d(TAG, "Loading banner: unitId=$adUnitId")

        container.visibility = View.INVISIBLE
        container.removeAllViews()

        val adView = AdView(activity)
        adView.adUnitId = adUnitId

        container.addView(adView)

        container.doOnLayout {
            val density = activity.resources.displayMetrics.density
            val containerWidthPx = container.width
            val screenWidthPx = activity.resources.displayMetrics.widthPixels
            val adWidthPx = if (containerWidthPx > 0) containerWidthPx else screenWidthPx
            val adWidthDp = (adWidthPx / density).toInt().coerceAtLeast(320)

            val adSize = AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(activity, adWidthDp)
            Log.d(TAG, "Computed adaptive size: ${adSize.width}x${adSize.height} for widthDp=$adWidthDp")

            adView.setAdSize(adSize)
            adView.adListener = object : AdListener() {
                override fun onAdLoaded() {
                    Log.d(TAG, "Banner loaded")
                    container.visibility = View.VISIBLE
                }

                override fun onAdImpression() {
                    Log.d(TAG, "Banner impression")
                }

                override fun onAdClicked() {
                    Log.d(TAG, "Banner clicked")
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    Log.w(TAG, "Banner failed: code=${loadAdError.code}, message=${loadAdError.message}, domain=${loadAdError.domain}")
                    container.visibility = View.GONE
                }
            }

            val request = AdRequest.Builder().build()
            Log.d(TAG, "Requesting banner ad")
            adView.loadAd(request)
        }

        return adView
    }
}
