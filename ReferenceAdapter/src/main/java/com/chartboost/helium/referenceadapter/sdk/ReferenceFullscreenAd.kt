/*
 * Copyright 2022-2023 Chartboost, Inc.
 * 
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE file.
 */

package com.chartboost.helium.referenceadapter.sdk

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import com.chartboost.heliumsdk.utils.PartnerLogController
import com.chartboost.heliumsdk.utils.PartnerLogController.PartnerAdapterEvents.*
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
     * In this example, there are no meaningful "load" and "destroy" implementations as the fullscreen ad is tied
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
        onFullScreenAdShowFailed: (String) -> Unit,
        onFullScreenAdClicked: () -> Unit,
        onFullScreenAdRewarded: (Int, String) -> Unit,
        onFullScreenAdDismissed: () -> Unit,
        onFullScreenAdExpired: () -> Unit
    ) {
        // Launch a new Activity where the fullscreen ad will be displayed.
        CoroutineScope(Main).launch(CoroutineExceptionHandler { _, _ ->
            onFullScreenAdExpired()
        }) {
            ReferenceFullscreenActivity.subscribe(shown = {
                onFullScreenAdImpression()
            }, showFailed = {
                onFullScreenAdShowFailed(it)
            }, clicked = {
                onFullScreenAdClicked()
            }, rewarded = { amount, currency ->
                onFullScreenAdRewarded(amount, currency)
            }, dismissed = {
                onFullScreenAdDismissed()
            })

            context.startActivity(Intent(context, ReferenceFullscreenActivity::class.java).apply {
                putExtra(FULLSCREEN_AD_URL, adFormat.resUrl)
                putExtra(IS_REWARDED_KEY, adFormat == ReferenceFullscreenAdFormat.REWARDED)
            })
        }
    }

    /**
     * In this example, there are no meaningful "load" and "destroy" implementations as the fullscreen ad is tied
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
