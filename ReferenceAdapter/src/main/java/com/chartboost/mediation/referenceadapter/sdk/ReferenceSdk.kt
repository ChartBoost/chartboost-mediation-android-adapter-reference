/*
 * Copyright 2023-2024 Chartboost, Inc.
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE file.
 */

package com.chartboost.mediation.referenceadapter.sdk

import android.util.Size
import kotlinx.coroutines.delay
import java.util.UUID

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
         *
         * @return Result.success(Unit) if initialization succeeds, Result.failure(Exception) otherwise.
         */
        suspend fun initialize(): Result<Unit> {
            delay(500L)

            return if (ReferenceSettings.initializationShouldSucceed) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Initialization failed"))
            }
        }

        /**
         * Simulate a partner SDK computation of a bid token.
         * Using the random UUID as an example. Do NOT copy.
         *
         * @return A bid token if the token fetch succeeds, an empty string otherwise.
         */
        fun getBidToken(): String {
            return if (ReferenceSettings.tokenFetchShouldSucceed) {
                UUID.randomUUID().toString()
            } else {
                ""
            }
        }

        /**
         * Simulate an oversized ad being returned for an adaptive banner placement.
         * Do NOT copy.
         *
         * @return A size with dimensions larger than requested.
         */
        fun getOversizedAdSize(requestedSize: Size): Size {
            return if (ReferenceSettings.adLoadShouldReturnOversizedAd) {
                Size(requestedSize.width + 1, requestedSize.height + 1)
            } else {
                requestedSize
            }
        }
    }
}
