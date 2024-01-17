/*
 * Copyright 2022-2023 Chartboost, Inc.
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE file.
 */

package com.chartboost.mediation.referenceadapter.sdk

import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.ActivityInfo
import android.net.Uri
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.view.View
import android.view.View.OnTouchListener
import android.view.ViewConfiguration
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.VideoView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.chartboost.heliumsdk.domain.ChartboostMediationAdException
import com.chartboost.heliumsdk.domain.ChartboostMediationError
import com.chartboost.mediation.referenceadapter.R
import com.chartboost.mediation.referenceadapter.databinding.ActivityReferenceFullscreenBinding
import com.chartboost.mediation.referenceadapter.sdk.ReferenceFullscreenAd.Companion.FULLSCREEN_AD_TYPE
import com.chartboost.mediation.referenceadapter.sdk.ReferenceFullscreenAd.Companion.FULLSCREEN_AD_URL
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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
        private var onAdDismissed: (ChartboostMediationAdException?) -> Unit = {}

        fun subscribe(
            shown: () -> Unit,
            showFailed: (String) -> Unit,
            rewarded: (Int, String) -> Unit,
            clicked: () -> Unit,
            dismissed: (ChartboostMediationAdException?) -> Unit,
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
            onAdDismissed = { exception ->
                dismissed(exception)
            }
        }
    }

    private lateinit var binding: ActivityReferenceFullscreenBinding

    private val rewardedInterstitialPlaybackDuration = 5000L

    private var clickThroughUrl = "https://www.chartboost.com/mediate/"
    private var adType: ReferenceFullscreenAd.ReferenceFullscreenAdFormat? = null
    private var videoView: VideoView? = null
    private var webView: WebView? = null
    private var videoPlaybackHandler: Handler? = Handler(Looper.getMainLooper())
    private var adDismissTracked = false
    private var adShowTracked = false
    private var remainingTimeMillis = rewardedInterstitialPlaybackDuration

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityReferenceFullscreenBinding.inflate(layoutInflater)
        setContentView(binding.root)

        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

        adType = intent.getStringExtra(FULLSCREEN_AD_TYPE)?.let {
            ReferenceFullscreenAd.ReferenceFullscreenAdFormat.valueOf(it)
        } ?: run {
            onAdShowFailed("No fullscreen ad type provided")
            finish()
            return
        }

        val adUrl =
            intent.getStringExtra(FULLSCREEN_AD_URL) ?: run {
                onAdShowFailed("No creative URL provided")
                finish()
                return
            }

        when (adType) {
            ReferenceFullscreenAd.ReferenceFullscreenAdFormat.REWARDED -> {
                showRewardedAd(adUrl)
            }

            ReferenceFullscreenAd.ReferenceFullscreenAdFormat.INTERSTITIAL -> {
                showInterstitialAd(adUrl)
            }

            ReferenceFullscreenAd.ReferenceFullscreenAdFormat.REWARDED_INTERSTITIAL -> {
                showRewardedInterstitialAd(adUrl)
            }

            else -> {
                onAdShowFailed("Invalid fullscreen ad type provided")
                finish()
            }
        }
    }

    override fun onResume() {
        super.onResume()

        /**
         * If the current creative is a rewarded video ad, it's already deliberately terminated when the use
         * clicks through (videoView is null). Now that the user is back, skip the ad Activity.
         */
        if (adType == ReferenceFullscreenAd.ReferenceFullscreenAdFormat.REWARDED && videoView == null) {
            cleanUp()
            finish()
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
            this.setOnTouchListener(
                object : OnTouchListener {
                    var startTime: Long = 0

                    override fun onTouch(
                        view: View,
                        event: MotionEvent,
                    ): Boolean {
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
                },
            )

            if (adType == ReferenceFullscreenAd.ReferenceFullscreenAdFormat.REWARDED_INTERSTITIAL) {
                val timerView = binding.referenceFullscreenTimerview
                val closeButton = binding.referenceFullscreenClosebutton
                var timer: CountDownTimer? = null

                (timerView.parent as? LinearLayout).also { it?.visibility = View.VISIBLE }

                closeButton.setOnClickListener {
                    timer?.cancel()

                    if (remainingTimeMillis >= 1000) {
                        AlertDialog.Builder(this@ReferenceFullscreenActivity)
                            .setTitle("Skip Ad")
                            .setMessage("Are you sure you want to skip this ad?")
                            .setPositiveButton("Yes") { _, _ ->
                                if (ReferenceSettings.adCloseShouldSucceed) {
                                    onAdDismissed(null)
                                } else {
                                    onAdDismissed(
                                        ChartboostMediationAdException(
                                            ChartboostMediationError.CM_INTERNAL_ERROR,
                                        ),
                                    )
                                }
                                finish()
                            }
                            .setNegativeButton("No") { _, _ ->
                                timer =
                                    createCountdownTimer(
                                        millisInFuture = remainingTimeMillis,
                                        closeButton = closeButton,
                                        timerView = timerView,
                                    ).also { it.start() }
                            }
                            .create().show()
                    }
                }

                timer =
                    createCountdownTimer(
                        millisInFuture = rewardedInterstitialPlaybackDuration,
                        closeButton = closeButton,
                        timerView = timerView,
                    ).also { it.start() }
            }

            // Simulate multiple continuation resumes for testing purposes. This should not crash.
            if (ReferenceSettings.adShowContinuationShouldResumeMoreThanOnce) {
                CoroutineScope(Main).launch {
                    onAdShown()
                    delay(500L)
                    onAdShown()
                }
            } else {
                onAdShown()
            }
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
            this.setOnTouchListener(
                object : OnTouchListener {
                    var startTime: Long = 0

                    override fun onTouch(
                        view: View,
                        event: MotionEvent,
                    ): Boolean {
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
                },
            )

            videoPlaybackHandler?.post(
                object : Runnable {
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
                },
            )
        }
    }

    /**
     * Create and show a rewarded interstitial ad. For simplicity, this implementation reuses the
     * interstitial ad show logic but add rewarding capabilities. Your implementation may differ.
     */
    private fun showRewardedInterstitialAd(url: String) {
        showInterstitialAd(url)
    }

    private fun clickthrough() {
        startActivity(
            Intent(Intent.ACTION_VIEW, Uri.parse(clickThroughUrl)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            },
        )

        onAdClicked()
    }

    private fun createCountdownTimer(
        millisInFuture: Long,
        closeButton: Button,
        timerView: TextView,
    ): CountDownTimer {
        return object : CountDownTimer(millisInFuture, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                remainingTimeMillis = millisUntilFinished
                timerView.text =
                    getString(
                        R.string.reward_interstitial_timer_text,
                        millisUntilFinished / 1000,
                    )

                if (remainingTimeMillis < 1000) {
                    closeButton.visibility = View.GONE
                    timerView.visibility = View.GONE
                }
            }

            override fun onFinish() {
                onAdRewarded(1, "coin")
            }
        }
    }

    private fun cleanUp() {
        webView?.destroy()
        webView = null

        videoView?.stopPlayback()
        videoView?.clearAnimation()
        videoView?.suspend()
        videoView = null

        if (!adDismissTracked) {
            if (ReferenceSettings.adCloseShouldSucceed) {
                onAdDismissed(null)
            } else {
                onAdDismissed(ChartboostMediationAdException(ChartboostMediationError.CM_INTERNAL_ERROR))
            }
            adDismissTracked = true
        }
    }
}
