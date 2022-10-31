package com.chartboost.helium.referenceadapter.sdk

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
import com.chartboost.heliumsdk.utils.PartnerLogController
import com.chartboost.heliumsdk.utils.PartnerLogController.PartnerAdapterEvents.CUSTOM

/**
 * INTERNAL. FOR DEMO AND TESTING PURPOSES ONLY. DO NOT USE DIRECTLY.
 *
 * A dummy SDK designed to support the reference adapter. Do NOT copy.
 */
@SuppressLint("ViewConstructor")
class ReferenceBanner(
    context: Context,
    private val adUnitId: String, private val size: Size
) : WebView(context) {
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

    private var clickThroughUrl = "https://www.chartboost.com/helium/"

    @SuppressLint("ClickableViewAccessibility")
    fun load(
        adm: String?,
        onAdImpression: () -> Unit,
        onAdClicked: () -> Unit,
    ) {
        PartnerLogController.log(
            CUSTOM,
            "Loading reference banner for ad unit ID $adUnitId with ad markup $adm" +
                    "and size ${size.width}x${size.height}"
        )

        layoutParams = LinearLayout.LayoutParams(size.width, size.height)
        loadUrl(size.resUrl)

        setOnTouchListener(object : OnTouchListener {
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
        })

        onAdImpression()
    }

    private fun clickthrough() {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(clickThroughUrl)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        })
    }
}
