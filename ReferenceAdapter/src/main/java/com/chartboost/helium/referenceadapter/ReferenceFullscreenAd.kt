package com.chartboost.helium.referenceadapter

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.util.Log
import android.webkit.WebView
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.registerForActivityResult
import androidx.appcompat.app.AppCompatActivity
import com.chartboost.heliumsdk.utils.LogController
import kotlinx.coroutines.*
import kotlinx.coroutines.Dispatchers.Main

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
) : AppCompatActivity() {

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

    /**
     * In this example, there are no "load" and "destroy" actions as the fullscreen ad is tied
     * to the presenting Activity's lifecycle, so those actions will be managed there.
     *
     * See [ReferenceFullscreenActivity] for more information.
     */
    fun load(adm: String?) {
        LogController.i(
            "Loading reference $adFormat ad for ad unit ID $adUnitId " +
                    "with ad markup $adm"
        )
    }

    suspend fun show(
        onFullScreenAdImpression: () -> Unit,
        onFullScreenAdClicked: () -> Unit,
        onFullScreenAdRewarded: (Int, String) -> Unit,
        onFullScreenAdDismissed: () -> Unit,
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

        // TODO: There might be weird lifecycle crashes here where `registerForActivityResult`
        //  needs to be assigned to a variable. Check when ready.
        withTimeoutOrNull(5000) {
            // Launch a new Activity where the fullscreen ad will be displayed.
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == RESULT_CANCELED) {
                    onFullScreenAdDismissed()
                }
            }.launch(fullScreenActivity)
        } ?: onFullScreenAdExpired()
    }

    /**
     * In this example, there are no "load" and "destroy" actions as the fullscreen ad is tied
     * to the presenting Activity's lifecycle, so those actions will be managed there.
     *
     * See [ReferenceFullscreenActivity] for more information.
     */
    fun destroy() {
        LogController.i(
            "Destroying reference $adFormat ad for ad unit ID $adUnitId"
        )
    }
}
