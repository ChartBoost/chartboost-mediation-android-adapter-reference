package com.chartboost.helium.referenceadapter

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.webkit.WebView
import com.chartboost.heliumsdk.utils.LogController

/**
 * INTERNAL. FOR DEMO AND TESTING PURPOSES ONLY. DO NOT USE DIRECTLY.
 *
 * A dummy SDK designed to support the reference adapter. Do NOT copy.
 */
@SuppressLint("ViewConstructor")
class ReferenceFullscreenAd(
    private val context: Context,
    private val adUnitId: String,
    private val adFormat: ReferenceFullscreenAdFormat
) {
    enum class ReferenceFullscreenAdFormat(val resUrl: String) {
        INTERSTITIAL("https://chartboost.s3.amazonaws.com/helium/creatives/creative-320x480.png"),
        REWARDED("https://chartboost.s3.amazonaws.com/helium/creatives/cbvideoad-portrait.mp4")
    }

    companion object {
        /**
         * Internal keys identifying the current fullscreen creative to be passed to the
         * [ReferenceFullscreenActivity].
         */
        const val FULLSCREEN_AD_URL = "reference_fullscreen_ad_url"
        const val IS_REWARDED_KEY = "reference_fullscreen_is_rewarded"
    }

    fun load(adm: String?) {
        LogController.i(
            "Loading reference $adFormat ad for ad unit ID $adUnitId " +
                    "with ad markup $adm"
        )
    }

    fun show(
        onFullScreenAdImpression: () -> Unit,
        onFullScreenAdClicked: () -> Unit,
        onFullScreenAdRewarded: (Int, String) -> Unit,
        onFullScreenAdFinished: () -> Unit,
        onFullScreenAdExpired: () -> Unit
    ) {
        val fullScreenActivity = Intent(context, ReferenceFullscreenActivity::class.java).apply {
            putExtra(FULLSCREEN_AD_URL, adFormat.resUrl)
            putExtra(IS_REWARDED_KEY, adFormat == ReferenceFullscreenAdFormat.REWARDED)
        }

        // Since we lose control of the ad obj and events when we launch the new Activity, for the sake of
        // simplicity we'll fire all callbacks now.
        onFullScreenAdImpression()
        onFullScreenAdClicked()
        onFullScreenAdRewarded(1, "coin")
        onFullScreenAdFinished()
        onFullScreenAdExpired()

        // Launch a new Activity where the fullscreen ad will be displayed.
        context.startActivity(fullScreenActivity)
    }

    fun destroy() {
        // NO-OP
    }
}
