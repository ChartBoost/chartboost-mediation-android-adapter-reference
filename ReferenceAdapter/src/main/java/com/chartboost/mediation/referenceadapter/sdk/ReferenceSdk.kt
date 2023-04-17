/*
 * Copyright 2022-2023 Chartboost, Inc.
 * 
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE file.
 */

package com.chartboost.mediation.referenceadapter.sdk

import android.content.Context
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

        lateinit var settings: ReferenceSettings

        /**
         * Simulate a partner SDK initialization that does nothing and completes after 500 ms.
         * Do NOT copy.
         *
         * @param context The current [Context].
         *
         * @return Result.success(Unit) if initialization succeeds, Result.failure(Exception) otherwise.
         */
        suspend fun initialize(context: Context): Result<Unit> {
            settings = ReferenceSettings.getInstance(context)

            return if (settings.initializationShouldSucceed) {
                delay(500L)
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
            return if (settings.tokenFetchShouldSucceed) {
                UUID.randomUUID().toString()
            } else {
                ""
            }
        }
    }
}
