package com.chartboost.helium.referenceadapter.sdk

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.view.View.OnTouchListener
import android.view.ViewConfiguration
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.widget.VideoView
import androidx.appcompat.app.AppCompatActivity
import com.chartboost.helium.referenceadapter.R
import com.chartboost.helium.referenceadapter.databinding.ActivityReferenceFullscreenBinding
import com.chartboost.helium.referenceadapter.sdk.ReferenceFullscreenAd.Companion.FULLSCREEN_AD_URL
import com.chartboost.helium.referenceadapter.sdk.ReferenceFullscreenAd.Companion.IS_REWARDED_KEY
import com.chartboost.heliumsdk.utils.PartnerLogController
import com.chartboost.heliumsdk.utils.PartnerLogController.PartnerAdapterFailureEvents.SHOW_FAILED

/**
 * INTERNAL. FOR DEMO AND TESTING PURPOSES ONLY. DO NOT USE DIRECTLY.
 *
 * A dummy SDK designed to support the reference adapter. Do NOT copy.
 */
class ReferenceFullscreenActivity : AppCompatActivity() {
    private lateinit var binding: ActivityReferenceFullscreenBinding

    private var clickThroughUrl = "https://www.chartboost.com/helium/"
    private var isAdRewarded = false
    private var videoView: VideoView? = null
    private var webView: WebView? = null

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityReferenceFullscreenBinding.inflate(layoutInflater)
        setContentView(binding.root)

        isAdRewarded = intent.getBooleanExtra(IS_REWARDED_KEY, false)
        val adUrl = intent.getStringExtra(FULLSCREEN_AD_URL)
            ?: throw IllegalArgumentException("No creative URL provided")

        /**
         * Since we are only locally detecting show failures (no signals back to the adapter),
         * logging is sufficient.
         *
         * We also don't need to relay any other ad events since [ReferenceFullscreenAd.show]
         * has already fired all callbacks.
         */
        if (isAdRewarded) {
            showRewardedAd(adUrl,
                onShowFailure = {
                    PartnerLogController.log(SHOW_FAILED)
                })
        } else {
            showInterstitialAd(adUrl,
                onShowFailure = {
                    PartnerLogController.log(SHOW_FAILED)
                }
            )
        }
    }

    override fun onResume() {
        super.onResume()

        /**
         * If the current creative is a rewarded ad, it's already deliberately terminated when the use
         * clicks through (videoView is null). Now that the user is back, skip the ad Activity.
         */
        if (isAdRewarded && videoView == null) {
            cleanUp().also { finish() }
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        cleanUp()
    }

    /**
     * Create and show an interstitial ad.
     */
    @SuppressLint("ClickableViewAccessibility")
    private fun showInterstitialAd(
        url: String,
        onShowFailure: () -> Unit
    ) {
        webView = findViewById<View>(R.id.reference_fullscreen_webview) as WebView?
            ?: run {
                onShowFailure()
                throw IllegalStateException("Unable to show interstitial ad. WebView not found.")
            }
        webView?.run {
            this.visibility = View.VISIBLE
            this.webChromeClient = WebChromeClient()
            this.loadUrl(url)
            this.setOnTouchListener(object : OnTouchListener {
                var startTime: Long = 0

                override fun onTouch(v: View, event: MotionEvent): Boolean {
                    if (event.action == MotionEvent.ACTION_DOWN) {
                        startTime = System.currentTimeMillis()
                    }

                    if (event.action == MotionEvent.ACTION_UP) {
                        // Trivial way to rule out other touch events (e.g. swiping)
                        if (System.currentTimeMillis() - startTime < ViewConfiguration.getTapTimeout()) {
                            clickthrough()
                        }
                    }
                    return true
                }
            })
        }
    }

    /**
     * Create and show a rewarded ad.
     */
    @SuppressLint("ClickableViewAccessibility")
    private fun showRewardedAd(url: String, onShowFailure: () -> Unit) {
        videoView = findViewById<View>(R.id.reference_fullscreen_videoview) as VideoView?
            ?: run {
                onShowFailure()
                throw IllegalStateException("Unable to show rewarded ad. VideoView not found.")
            }
        videoView?.run {
            this.visibility = View.VISIBLE
            this.setVideoPath(url)
            this.start()
            this.setOnTouchListener(object : OnTouchListener {
                var startTime: Long = 0

                override fun onTouch(v: View?, event: MotionEvent): Boolean {
                    if (event.action == MotionEvent.ACTION_DOWN) {
                        startTime = System.currentTimeMillis()
                    }

                    if (event.action == MotionEvent.ACTION_UP) {
                        // Trivial way to rule out other touch events (e.g. swiping)
                        if (System.currentTimeMillis() - startTime < ViewConfiguration.getTapTimeout()) {
                            // Deliberately terminate ad playback upon clickthrough.
                            // As such, when the user comes back from the landing page, they will also be exiting the ad experience.
                            cleanUp()
                            clickthrough()
                        }
                    }
                    return true
                }
            })
        }
    }

    private fun clickthrough() {
        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(clickThroughUrl)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        startActivity(browserIntent)
    }

    private fun cleanUp() {
        webView?.destroy()
        webView = null

        videoView?.stopPlayback()
        videoView?.clearAnimation()
        videoView?.suspend()
        videoView = null
    }
}
