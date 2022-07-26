package com.chartboost.heliumsdk.referenceadapter.sdk

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.MotionEvent
import android.view.View
import android.view.View.INVISIBLE
import android.view.ViewConfiguration
import android.webkit.WebView
import android.widget.LinearLayout
import com.chartboost.heliumsdk.utils.LogController

/**
 * INTERNAL. FOR DEMO AND TESTING PURPOSES ONLY. DO NOT USE DIRECTLY.
 *
 * A dummy SDK designed to support the reference adapter. Do NOT copy.
 */
@SuppressLint("ViewConstructor")
class ReferenceBanner(
    private val context: Context,
    private val adUnitId: String, private val size: Size
) {
    enum class Size(val width: Int, val height: Int, val resUrl: String) {
        BANNER(
            320,
            50,
            "https://chartboost.s3.amazonaws.com/helium/creatives/creative-320x50.png"
        ),
        MEDIUM_RECTANGLE(
            300,
            250,
            "https://chartboost.s3.amazonaws.com/helium/creatives/creative-300x250.png"
        ),
        LEADERBOARD(
            728,
            90,
            "https://chartboost.s3.amazonaws.com/helium/creatives/creative-728x90.png"
        );
    }

    private var bannerAd: WebView? = null
    private var clickThroughUrl = "https://www.chartboost.com/helium/"

    @SuppressLint("ClickableViewAccessibility")
    fun load(
        adm: String?,
        onAdImpression: () -> Unit,
        onAdClicked: () -> Unit,
    ) {
        LogController.i(
            "Loading reference banner for ad unit ID $adUnitId with ad markup $adm" +
                    "and size ${size.width}x${size.height}"
        )

        bannerAd = destroy().run { createBannerAd() }
        bannerAd?.setOnTouchListener(object : View.OnTouchListener {
            var startTime: Long = 0

            override fun onTouch(v: View?, event: MotionEvent): Boolean {
                if (event.action == MotionEvent.ACTION_DOWN) {
                    startTime = System.currentTimeMillis()
                }

                if (event.action == MotionEvent.ACTION_UP) {
                    // Trivial way to rule out other touch events (e.g. swiping)
                    if (System.currentTimeMillis() - startTime < ViewConfiguration.getTapTimeout()) {
                        clickthrough()
                        onAdClicked()
                    }
                }

                return true
            }
        }) ?: run {
            throw IllegalStateException("Unable to load banner ad. It is null.")
        }

        onAdImpression()
    }

    fun destroy() {
        bannerAd?.run {
            this.visibility = INVISIBLE
            this.destroy()
            bannerAd = null
        }
    }

    private fun clickthrough() {
        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(clickThroughUrl)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(browserIntent)
    }

    private fun createBannerAd(): WebView {
        return WebView(context).run {
            this.layoutParams = LinearLayout.LayoutParams(size.width, size.height)
            this.loadUrl(size.resUrl)
            this
        }
    }
}
