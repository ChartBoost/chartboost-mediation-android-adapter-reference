package com.chartboost.helium.referenceadapter.sdk

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.chartboost.heliumsdk.utils.PartnerLogController
import com.chartboost.heliumsdk.utils.PartnerLogController.PartnerAdapterEvents.*
import com.chartboost.heliumsdk.utils.PartnerLogController.PartnerAdapterFailureEvents.*
import com.chartboost.heliumsdk.utils.PartnerLogController.PartnerAdapterSuccessEvents.*
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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
        PartnerLogController.log(
            CUSTOM,
            "Loading reference $adFormat ad for ad unit ID $adUnitId " +
                    "with ad markup $adm"
        )
    }

    fun show(
        onFullScreenAdImpression: () -> Unit,
        onFullScreenAdClicked: () -> Unit,
        onFullScreenAdRewarded: (Int, String) -> Unit,
        onFullScreenAdDismissed: () -> Unit,
        onFullScreenAdExpired: (error: String) -> Unit
    ) {
        val fullScreenActivity = Intent(context, ReferenceFullscreenActivity::class.java).apply {
            putExtra(FULLSCREEN_AD_URL, adFormat.resUrl)
            putExtra(IS_REWARDED_KEY, adFormat == ReferenceFullscreenAdFormat.REWARDED)
        }

        // Launch a new Activity where the fullscreen ad will be displayed.
        CoroutineScope(Main).launch(CoroutineExceptionHandler { _, error ->
            onFullScreenAdExpired(error.message ?: "Unknown error")
        }) {
            // TODO: There might be weird lifecycle crashes where `registerForActivityResult` must be assigned to a variable. Check when ready.
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == RESULT_CANCELED) {
                    onFullScreenAdRewarded(1, "coin")
                    onFullScreenAdDismissed()
                }
            }.launch(fullScreenActivity)

            // Since we lose control of the ad obj and events when we launch the new Activity, for the sake of
            // simplicity we'll fire the following callbacks after a delay.
            delay(1000L)
            onFullScreenAdImpression()
            onFullScreenAdClicked()
        }
    }

    /**
     * In this example, there are no "load" and "destroy" actions as the fullscreen ad is tied
     * to the presenting Activity's lifecycle, so those actions will be managed there.
     *
     * See [ReferenceFullscreenActivity] for more information.
     */
    fun destroy() {
        PartnerLogController.log(
            CUSTOM,
            "Destroying reference $adFormat ad for ad unit ID $adUnitId"
        )
    }
}
