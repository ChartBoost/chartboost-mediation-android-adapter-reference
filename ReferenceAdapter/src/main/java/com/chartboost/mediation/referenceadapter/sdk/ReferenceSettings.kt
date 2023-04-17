package com.chartboost.mediation.referenceadapter.sdk

import android.content.Context
import java.lang.ref.WeakReference

class ReferenceSettings private constructor(context: Context) {
    companion object {
        const val REFERENCE_ADAPTER_SETTINGS = "REFERENCE_ADAPTER_SETTINGS"
        const val REFERENCE_ADAPTER_INIT_STATUS = "REFERENCE_ADAPTER_INIT_STATUS"
        const val REFERENCE_ADAPTER_TOKEN_FETCH_STATUS = "REFERENCE_ADAPTER_TOKEN_FETCH_STATUS"
        const val REFERENCE_ADAPTER_AD_LOAD_STATUS = "REFERENCE_ADAPTER_AD_LOAD_STATUS"
        const val REFERENCE_ADAPTER_AD_SHOW_STATUS = "REFERENCE_ADAPTER_AD_SHOW_STATUS"
        const val REFERENCE_ADAPTER_AD_INVALIDATE_STATUS = "REFERENCE_ADAPTER_AD_INVALIDATE_STATUS"

        @Volatile
        private var INSTANCE: ReferenceSettings? = null

        fun getInstance(context: Context): ReferenceSettings {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: ReferenceSettings(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    private val weakContext = WeakReference(context)

    var initializationShouldSucceed: Boolean
        get() = getSetting(REFERENCE_ADAPTER_INIT_STATUS, true)
        set(value) = setSetting(REFERENCE_ADAPTER_INIT_STATUS, value)

    var tokenFetchShouldSucceed: Boolean
        get() = getSetting(REFERENCE_ADAPTER_TOKEN_FETCH_STATUS, true)
        set(value) = setSetting(REFERENCE_ADAPTER_TOKEN_FETCH_STATUS, value)

    var adLoadShouldSucceed: Boolean
        get() = getSetting(REFERENCE_ADAPTER_AD_LOAD_STATUS, true)
        set(value) = setSetting(REFERENCE_ADAPTER_AD_LOAD_STATUS, value)

    var adShowShouldSucceed: Boolean
        get() = getSetting(REFERENCE_ADAPTER_AD_SHOW_STATUS, true)
        set(value) = setSetting(REFERENCE_ADAPTER_AD_SHOW_STATUS, value)

    var adInvalidateShouldSucceed: Boolean
        get() = getSetting(REFERENCE_ADAPTER_AD_INVALIDATE_STATUS, true)
        set(value) = setSetting(REFERENCE_ADAPTER_AD_INVALIDATE_STATUS, value)

    private fun getSetting(key: String, defaultValue: Boolean): Boolean {
        val context = weakContext.get()
        return context?.getSharedPreferences(REFERENCE_ADAPTER_SETTINGS, Context.MODE_PRIVATE)
            ?.getBoolean(key, defaultValue) ?: defaultValue
    }

    private fun setSetting(key: String, value: Boolean) {
        val context = weakContext.get()
        context?.getSharedPreferences(REFERENCE_ADAPTER_SETTINGS, Context.MODE_PRIVATE)
            ?.edit()
            ?.putBoolean(key, value)
            ?.apply()
    }
}
