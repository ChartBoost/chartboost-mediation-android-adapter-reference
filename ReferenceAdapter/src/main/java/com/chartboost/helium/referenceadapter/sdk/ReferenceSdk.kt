// Copyright 2022-2023 Chartboost, Inc.
// 
// Use of this source code is governed by an MIT-style
// license that can be found in the LICENSE file.

package com.chartboost.helium.referenceadapter.sdk

import kotlinx.coroutines.delay
import java.util.*

/**
 * INTERNAL. FOR DEMO AND TESTING PURPOSES ONLY. DO NOT USE DIRECTLY.
 *
 * A dummy SDK designed to support the reference adapter. Do NOT copy.
 */
class ReferenceSdk {
    companion object {
        const val REFERENCE_SDK_VERSION = "1.0.0"

        /**
         * Simulate a partner SDK initialization that does nothing and completes after 500 ms.
         * Do NOT copy.
         */
        suspend fun initialize() {
            delay(500L)
        }

        /**
         * Simulate a partner SDK computation of a bid token.
         * Using the random UUID as an example. Do NOT copy.
         */
        fun getBidToken(): String {
            return UUID.randomUUID().toString()
        }
    }
}
