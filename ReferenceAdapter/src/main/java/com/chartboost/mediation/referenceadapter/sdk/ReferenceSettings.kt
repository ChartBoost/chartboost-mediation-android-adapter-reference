package com.chartboost.mediation.referenceadapter.sdk

import android.content.Context

object ReferenceSettings {
    lateinit var appContext: Context

    private const val REFERENCE_ADAPTER_SETTINGS = "REFERENCE_ADAPTER_SETTINGS"
    private const val REFERENCE_ADAPTER_INIT_STATUS = "REFERENCE_ADAPTER_INIT_STATUS"
    private const val REFERENCE_ADAPTER_TOKEN_FETCH_STATUS = "REFERENCE_ADAPTER_TOKEN_FETCH_STATUS"
    private const val REFERENCE_ADAPTER_AD_LOAD_STATUS = "REFERENCE_ADAPTER_AD_LOAD_STATUS"
    private const val REFERENCE_ADAPTER_AD_SHOW_STATUS = "REFERENCE_ADAPTER_AD_SHOW_STATUS"
    private const val REFERENCE_ADAPTER_AD_INVALIDATE_STATUS =
        "REFERENCE_ADAPTER_AD_INVALIDATE_STATUS"

    var initializationShouldSucceed: Boolean
        get() = getSetting(REFERENCE_ADAPTER_INIT_STATUS, true)
        set(value) = applySetting(REFERENCE_ADAPTER_INIT_STATUS, value)

    var tokenFetchShouldSucceed: Boolean
        get() = getSetting(REFERENCE_ADAPTER_TOKEN_FETCH_STATUS, true)
        set(value) = applySetting(REFERENCE_ADAPTER_TOKEN_FETCH_STATUS, value)

    var adLoadShouldSucceed: Boolean
        get() = getSetting(REFERENCE_ADAPTER_AD_LOAD_STATUS, true)
        set(value) = applySetting(REFERENCE_ADAPTER_AD_LOAD_STATUS, value)

    var adShowShouldSucceed: Boolean
        get() = getSetting(REFERENCE_ADAPTER_AD_SHOW_STATUS, true)
        set(value) = applySetting(REFERENCE_ADAPTER_AD_SHOW_STATUS, value)

    var adInvalidateShouldSucceed: Boolean
        get() = getSetting(REFERENCE_ADAPTER_AD_INVALIDATE_STATUS, true)
        set(value) = applySetting(REFERENCE_ADAPTER_AD_INVALIDATE_STATUS, value)

    private fun getSetting(key: String, defaultValue: Boolean): Boolean {
        return appContext.getSharedPreferences(REFERENCE_ADAPTER_SETTINGS, Context.MODE_PRIVATE)
            ?.getBoolean(key, defaultValue) ?: defaultValue
    }

    private fun applySetting(key: String, value: Boolean) {
        appContext.getSharedPreferences(REFERENCE_ADAPTER_SETTINGS, Context.MODE_PRIVATE)
            ?.edit()
            ?.putBoolean(key, value)
            ?.apply()
    }
}
