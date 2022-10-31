package com.chartboost.helium.referenceadapter.sdk

import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.ActivityInfo
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
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

/**
 * INTERNAL. FOR DEMO AND TESTING PURPOSES ONLY. DO NOT USE DIRECTLY.
 *
 * A dummy SDK designed to support the reference adapter. Do NOT copy.
 */
class ReferenceFullscreenActivity : AppCompatActivity() {
    companion object {
        private var onAdShown: () -> Unit = {}
        private var onAdShowFailed: (String) -> Unit = {}
        private var onAdRewarded: (Int, String) -> Unit = { _, _ -> }
        private var onAdClicked: () -> Unit = {}
        private var onAdDismissed: () -> Unit = {}

        fun subscribe(
            shown: () -> Unit,
            showFailed: (String) -> Unit,
            rewarded: (Int, String) -> Unit,
            clicked: () -> Unit,
            dismissed: () -> Unit
        ) {
            onAdShown = {
                shown()
            }
            onAdShowFailed = { error ->
                showFailed(error)
            }
            onAdRewarded = { amount, currency ->
                rewarded(amount, currency)
            }
            onAdClicked = {
                clicked()
            }
            onAdDismissed = {
                dismissed()
            }
        }
    }

    private lateinit var binding: ActivityReferenceFullscreenBinding

    private var clickThroughUrl = "https://www.chartboost.com/helium/"
    private var isAdRewarded = false
    private var videoView: VideoView? = null
    private var webView: WebView? = null
    private var videoPlaybackHandler: Handler? = Handler(Looper.getMainLooper())
    private var adDismissTracked = false
    private var adShowTracked = false

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityReferenceFullscreenBinding.inflate(layoutInflater)
        setContentView(binding.root)

        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

        isAdRewarded = intent.getBooleanExtra(IS_REWARDED_KEY, false)
        val adUrl = intent.getStringExtra(FULLSCREEN_AD_URL) ?: run {
            onAdShowFailed("No creative URL provided")
            finish()
            return
        }

        if (isAdRewarded) {
            showRewardedAd(adUrl)
        } else {
            showInterstitialAd(adUrl)
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
    private fun showInterstitialAd(url: String) {
        webView = findViewById<View>(R.id.reference_fullscreen_webview) as? WebView
            ?: run {
                onAdShowFailed("Failed to load WebView")
                finish()
                return
            }
        webView?.run {
            this.visibility = View.VISIBLE
            this.webChromeClient = WebChromeClient()
            this.loadUrl(url)
            this.setOnTouchListener(object : OnTouchListener {
                var startTime: Long = 0

                override fun onTouch(view: View, event: MotionEvent): Boolean {
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

            onAdShown()
        }
    }

    /**
     * Create and show a rewarded ad.
     */
    @SuppressLint("ClickableViewAccessibility")
    private fun showRewardedAd(url: String) {
        videoView = findViewById<View>(R.id.reference_fullscreen_videoview) as VideoView?
            ?: run {
                onAdShowFailed("Failed to load WebView")
                finish()
                return
            }
        videoView?.run {
            this.visibility = View.VISIBLE
            this.setVideoPath(url)
            this.start()
            this.setOnTouchListener(object : OnTouchListener {
                var startTime: Long = 0

                override fun onTouch(view: View, event: MotionEvent): Boolean {
                    if (event.action == MotionEvent.ACTION_DOWN) {
                        startTime = System.currentTimeMillis()
                    }

                    if (event.action == MotionEvent.ACTION_UP) {
                        // Trivial way to rule out other touch events (e.g. swiping)
                        if (System.currentTimeMillis() - startTime < ViewConfiguration.getTapTimeout()) {
                            // Deliberately terminate ad playback upon clickthrough.
                            // As such, when the user comes back from the landing page, they will also be exiting the ad experience.
                            clickthrough()
                            cleanUp()
                        }
                    }
                    return true
                }
            })

            videoPlaybackHandler?.post(object : Runnable {
                override fun run() {
                    if (isPlaying && !adShowTracked) {
                        onAdShown()
                        adShowTracked = true
                    }

                    if (!isPlaying && currentPosition > 0 && duration != -1) {
                        videoPlaybackHandler?.removeCallbacksAndMessages(null)
                        videoPlaybackHandler = null
                        onAdRewarded(1, "coin")
                    }

                    videoPlaybackHandler?.postDelayed(this, 250)
                }
            })
        }
    }

    private fun clickthrough() {
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(clickThroughUrl)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        })

        onAdClicked()
    }

    private fun cleanUp() {
        webView?.destroy()
        webView = null

        videoView?.stopPlayback()
        videoView?.clearAnimation()
        videoView?.suspend()
        videoView = null

        if (!adDismissTracked) {
            onAdDismissed()
            adDismissTracked = true
        }
    }
}
