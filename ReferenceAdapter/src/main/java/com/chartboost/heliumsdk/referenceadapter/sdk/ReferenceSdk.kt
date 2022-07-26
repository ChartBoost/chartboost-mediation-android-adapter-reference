package com.chartboost.heliumsdk.referenceadapter.sdk

import java.util.*

/**
 * INTERNAL. FOR DEMO AND TESTING PURPOSES ONLY. DO NOT USE DIRECTLY.
 *
 * A dummy SDK designed to support the reference adapter. Do NOT copy.
 */
class ReferenceSdk {
    companion object {
        private const val REFERENCE_SDK_VERSION = "1.0.0"

        fun getVersion(): String {
            return REFERENCE_SDK_VERSION
        }

        /**
         * Simulate a partner SDK initialization that does nothing.
         * Do NOT copy.
         */
        fun initialize() {
        }

        /**
         * Simulate a partner SDK computation of a big token.
         * Using the random UUID as an example. Do NOT copy.
         */
        fun getBidToken(): String {
            return UUID.randomUUID().toString()
        }
    }
}
