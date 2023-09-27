/*
 * Copyright 2022-2023 Chartboost, Inc.
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE file.
 */

package com.chartboost.mediation.referenceadapter.sdk

import android.content.Context

object ReferenceSettings {
    var appContext: Context? = null

    private const val REFERENCE_ADAPTER_SETTINGS = "REFERENCE_ADAPTER_SETTINGS"
    private const val REFERENCE_ADAPTER_INIT_STATUS = "REFERENCE_ADAPTER_INIT_STATUS"
    private const val REFERENCE_ADAPTER_TOKEN_FETCH_STATUS = "REFERENCE_ADAPTER_TOKEN_FETCH_STATUS"
    private const val REFERENCE_ADAPTER_AD_LOAD_STATUS = "REFERENCE_ADAPTER_AD_LOAD_STATUS"
    private const val REFERENCE_ADAPTER_AD_LOAD_SHOULD_RETURN_OVERSIZED_AD = "REFERENCE_ADAPTER_AD_LOAD_SHOULD_RETURN_OVERSIZED_AD"
    private const val REFERENCE_ADAPTER_AD_SHOW_STATUS = "REFERENCE_ADAPTER_AD_SHOW_STATUS"
    private const val REFERENCE_ADAPTER_AD_INVALIDATE_STATUS = "REFERENCE_ADAPTER_AD_INVALIDATE_STATUS"
    private const val REFERENCE_ADAPTER_AD_CLOSE_STATUS = "REFERENCE_ADAPTER_AD_CLOSE_STATUS"
    private const val REFERENCE_ADAPTER_AD_LOAD_CONTINUATION_STATUS = "REFERENCE_ADAPTER_AD_LOAD_CONTINUATION_STATUS"
    private const val REFERENCE_ADAPTER_AD_SHOW_CONTINUATION_STATUS = "REFERENCE_ADAPTER_AD_SHOW_CONTINUATION_STATUS"

    var initializationShouldSucceed: Boolean
        get() = getSetting(REFERENCE_ADAPTER_INIT_STATUS, true)
        set(value) = applySetting(REFERENCE_ADAPTER_INIT_STATUS, value)

    var tokenFetchShouldSucceed: Boolean
        get() = getSetting(REFERENCE_ADAPTER_TOKEN_FETCH_STATUS, true)
        set(value) = applySetting(REFERENCE_ADAPTER_TOKEN_FETCH_STATUS, value)

    var adLoadShouldSucceed: Boolean
        get() = getSetting(REFERENCE_ADAPTER_AD_LOAD_STATUS, true)
        set(value) = applySetting(REFERENCE_ADAPTER_AD_LOAD_STATUS, value)

    var adLoadShouldReturnOversizedAd: Boolean
        get() = getSetting(REFERENCE_ADAPTER_AD_LOAD_SHOULD_RETURN_OVERSIZED_AD, true)
        set(value) = applySetting(REFERENCE_ADAPTER_AD_LOAD_SHOULD_RETURN_OVERSIZED_AD, value)

    var adShowShouldSucceed: Boolean
        get() = getSetting(REFERENCE_ADAPTER_AD_SHOW_STATUS, true)
        set(value) = applySetting(REFERENCE_ADAPTER_AD_SHOW_STATUS, value)

    var adInvalidateShouldSucceed: Boolean
        get() = getSetting(REFERENCE_ADAPTER_AD_INVALIDATE_STATUS, true)
        set(value) = applySetting(REFERENCE_ADAPTER_AD_INVALIDATE_STATUS, value)

    var adCloseShouldSucceed: Boolean
        get() = getSetting(REFERENCE_ADAPTER_AD_CLOSE_STATUS, true)
        set(value) = applySetting(REFERENCE_ADAPTER_AD_CLOSE_STATUS, value)

    var adLoadContinuationShouldResumeMoreThanOnce: Boolean
        get() = getSetting(REFERENCE_ADAPTER_AD_LOAD_CONTINUATION_STATUS, false)
        set(value) = applySetting(REFERENCE_ADAPTER_AD_LOAD_CONTINUATION_STATUS, value)

    var adShowContinuationShouldResumeMoreThanOnce: Boolean
        get() = getSetting(REFERENCE_ADAPTER_AD_SHOW_CONTINUATION_STATUS, false)
        set(value) = applySetting(REFERENCE_ADAPTER_AD_SHOW_CONTINUATION_STATUS, value)

    private fun getSetting(key: String, defaultValue: Boolean): Boolean {
        return appContext?.getSharedPreferences(REFERENCE_ADAPTER_SETTINGS, Context.MODE_PRIVATE)
            ?.getBoolean(key, defaultValue) ?: defaultValue
    }

    private fun applySetting(key: String, value: Boolean) {
        appContext?.getSharedPreferences(REFERENCE_ADAPTER_SETTINGS, Context.MODE_PRIVATE)
            ?.edit()
            ?.putBoolean(key, value)
            ?.apply()
    }
}
