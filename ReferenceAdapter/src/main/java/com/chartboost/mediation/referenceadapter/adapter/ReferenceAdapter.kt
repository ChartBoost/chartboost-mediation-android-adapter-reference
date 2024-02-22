/*
 * Copyright 2023-2024 Chartboost, Inc.
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE file.
 */

package com.chartboost.mediation.referenceadapter.adapter

import android.content.Context
import android.util.Size
import com.chartboost.chartboostmediationsdk.domain.*
import com.chartboost.chartboostmediationsdk.utils.LogController
import com.chartboost.chartboostmediationsdk.utils.PartnerLogController
import com.chartboost.chartboostmediationsdk.utils.PartnerLogController.PartnerAdapterEvents.*
import com.chartboost.mediation.referenceadapter.BuildConfig
import com.chartboost.mediation.referenceadapter.sdk.ReferenceBanner
import com.chartboost.mediation.referenceadapter.sdk.ReferenceFullscreenAd
import com.chartboost.mediation.referenceadapter.sdk.ReferenceFullscreenAd.ReferenceFullscreenAdFormat.*
import com.chartboost.mediation.referenceadapter.sdk.ReferenceSdk
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * INTERNAL. FOR DEMO AND TESTING PURPOSES ONLY. DO NOT USE DIRECTLY.
 *
 * An adapter that is used for reference purposes. It is designed to showcase and test the
 * mediation contract of the Chartboost Mediation SDK.
 *
 * Implementations of the Chartboost Mediation interface may roughly model their own design after this class,
 * but do NOT call this adapter directly.
 */
class ReferenceAdapter : PartnerAdapter {
    /**
     * A map of Chartboost Mediation's listeners for the corresponding load identifier.
     */
    private val listeners = mutableMapOf<String, PartnerAdListener>()

    /**
     * Override this value to return the version of the partner SDK.
     */
    override val partnerSdkVersion: String
        get() = ReferenceSdk.REFERENCE_SDK_VERSION

    /**
     * Override this value to return the version of the mediation adapter.
     *
     * You may version the adapter using any preferred convention, but it is recommended to apply the
     * following format if the adapter will be published by Chartboost Mediation:
     *
     * Chartboost Mediation.Partner.Adapter
     *
     * "Chartboost Mediation" represents the Chartboost Mediation SDK’s major version that is compatible with this adapter. This must be 1 digit.
     * "Partner" represents the partner SDK’s major.minor.patch.x (where x is optional) version that is compatible with this adapter. This can be 3-4 digits.
     * "Adapter" represents this adapter’s version (starting with 0), which resets to 0 when the partner SDK’s version changes. This must be 1 digit.
     */
    override val adapterVersion: String
        get() = BuildConfig.CHARTBOOST_MEDIATION_REFERENCE_ADAPTER_VERSION

    /**
     * Override this value to return the name of the partner SDK.
     */
    override val partnerId: String
        get() = "reference"

    /**
     * Override this value to return the display name of the partner SDK.
     */
    override val partnerDisplayName: String
        get() = "Reference"

    /**
     * Override this method to initialize the partner SDK so that it's ready to request and display ads.
     *
     * @param context The current Context.
     * @param partnerConfiguration The necessary initialization data provided by Chartboost Mediation.
     *
     * @return Result.success(Unit) if the initialization was successful, Result.failure(Exception) otherwise.
     */
    override suspend fun setUp(
        context: Context,
        partnerConfiguration: PartnerConfiguration,
    ): Result<Unit> {
        PartnerLogController.log(SETUP_STARTED)

        return ReferenceSdk.initialize().fold(
            onSuccess = {
                Result.success(PartnerLogController.log(SETUP_SUCCEEDED))
            },
            onFailure = {
                PartnerLogController.log(SETUP_FAILED)
                Result.failure(it)
            },
        )
    }

    /**
     * Override this method to make an ad request to the partner SDK for the given ad format.
     *
     * @param context The current Context.
     * @param request The relevant data associated with the current ad load call.
     * @param partnerAdListener The listener to notify the Chartboost Mediation SDK of the partner ad events.
     *
     * @return Result.success(PartnerAd) if the request was successful, Result.failure(Exception) otherwise.
     */
    override suspend fun load(
        context: Context,
        request: PartnerAdLoadRequest,
        partnerAdListener: PartnerAdListener,
    ): Result<PartnerAd> {
        PartnerLogController.log(LOAD_STARTED)

        // Save the listener for later use.
        listeners[request.identifier] = partnerAdListener

        delay(1000L)

        return when (request.format.key) {
            AdFormat.BANNER.key, "adaptive_banner" -> {
                loadBannerAd(context, request)
            }
            AdFormat.INTERSTITIAL.key, AdFormat.REWARDED.key -> loadFullscreenAd(context, request)
            else -> {
                if (request.format.key == "rewarded_interstitial") {
                    loadFullscreenAd(context, request)
                } else {
                    PartnerLogController.log(LOAD_FAILED)
                    Result.failure(ChartboostMediationAdException(ChartboostMediationError.CM_LOAD_FAILURE_UNSUPPORTED_AD_FORMAT))
                }
            }
        }
    }

    /**
     * Override this method to discard current ad objects and release resources.
     *
     * @param partnerAd The partner ad to be discarded.
     *
     * @return Result.success(PartnerAd) if the ad was successfully discarded, Result.failure(Exception) otherwise.
     */
    override suspend fun invalidate(partnerAd: PartnerAd): Result<PartnerAd> {
        PartnerLogController.log(INVALIDATE_STARTED)

        destroyBannerAd(partnerAd)
        destroyFullscreenAd(partnerAd)

        listeners.remove(partnerAd.request.identifier)

        // For simplicity, the reference adapter always assumes successes.
        PartnerLogController.log(INVALIDATE_SUCCEEDED)
        return Result.success(partnerAd)
    }

    /**
     * Override this method to show the partner ad.
     *
     * @param context The current Context.
     * @param partnerAd The partner ad to be shown.
     *
     * @return Result.success(PartnerAd) if the ad was successfully shown, Result.failure(Exception) otherwise.
     */
    override suspend fun show(
        context: Context,
        partnerAd: PartnerAd,
    ): Result<PartnerAd> {
        PartnerLogController.log(SHOW_STARTED)

        return when (partnerAd.request.format.key) {
            AdFormat.BANNER.key, "adaptive_banner" -> {
                // Banners do not have a "show" mechanism.
                PartnerLogController.log(SHOW_SUCCEEDED)
                Result.success(partnerAd)
            }

            AdFormat.INTERSTITIAL.key, AdFormat.REWARDED.key -> {
                showFullscreenAd(partnerAd)
            }

            else -> {
                if (partnerAd.request.format.key == "rewarded_interstitial") {
                    showFullscreenAd(partnerAd)
                } else {
                    PartnerLogController.log(SHOW_FAILED)
                    Result.failure(ChartboostMediationAdException(ChartboostMediationError.CM_SHOW_FAILURE_UNSUPPORTED_AD_FORMAT))
                }
            }
        }
    }

    /**
     * Override this method to compute and return a bid token for the bid request.
     *
     * @param context The current Context.
     * @param request The necessary data associated with the current bid request.
     *
     * @return A Map<String, String> of a biddable token keyed by an identifier.
     */
    override suspend fun fetchBidderInformation(
        context: Context,
        request: PreBidRequest,
    ): Map<String, String> {
        PartnerLogController.log(BIDDER_INFO_FETCH_STARTED)

        val token = ReferenceSdk.getBidToken()

        PartnerLogController.log(if (token.isNotEmpty()) BIDDER_INFO_FETCH_SUCCEEDED else BIDDER_INFO_FETCH_FAILED)
        return mapOf("bid_token" to token)
    }

    /**
     * Override this method to notify your partner SDK of GDPR applicability as determined by
     * the Chartboost Mediation SDK.
     *
     * @param context The current [Context].
     * @param applies True if GDPR applies, false otherwise.
     * @param gdprConsentStatus The user's GDPR consent status.
     */
    override fun setGdpr(
        context: Context,
        applies: Boolean?,
        gdprConsentStatus: GdprConsentStatus,
    ) {
        PartnerLogController.log(
            when (applies) {
                true -> GDPR_APPLICABLE
                false -> GDPR_NOT_APPLICABLE
                else -> GDPR_UNKNOWN
            },
        )

        PartnerLogController.log(
            when (gdprConsentStatus) {
                GdprConsentStatus.GDPR_CONSENT_UNKNOWN -> GDPR_CONSENT_UNKNOWN
                GdprConsentStatus.GDPR_CONSENT_GRANTED -> GDPR_CONSENT_GRANTED
                GdprConsentStatus.GDPR_CONSENT_DENIED -> GDPR_CONSENT_DENIED
            },
        )
    }

    /**
     * Override this method to notify your partner SDK of the CCPA privacy String as supplied by
     * the Chartboost Mediation SDK.
     *
     * @param context The current [Context].
     * @param hasGrantedCcpaConsent True if the user has granted CCPA consent, false otherwise.
     * @param privacyString The CCPA privacy String.
     */
    override fun setCcpaConsent(
        context: Context,
        hasGrantedCcpaConsent: Boolean,
        privacyString: String,
    ) {
        PartnerLogController.log(
            if (hasGrantedCcpaConsent) {
                CCPA_CONSENT_GRANTED
            } else {
                CCPA_CONSENT_DENIED
            },
        )
    }

    /**
     * Override this method to notify your partner SDK of the COPPA subjectivity as determined by
     * the Chartboost Mediation SDK.
     *
     * @param context The current [Context].
     * @param isSubjectToCoppa True if the user is subject to COPPA, false otherwise.
     */
    override fun setUserSubjectToCoppa(
        context: Context,
        isSubjectToCoppa: Boolean,
    ) {
        PartnerLogController.log(
            if (isSubjectToCoppa) {
                COPPA_SUBJECT
            } else {
                COPPA_NOT_SUBJECT
            },
        )
    }

    /**
     * Ask the reference SDK to load a banner ad.
     *
     * @param context The current Context.
     * @param request The relevant data associated with the current ad load call.
     *
     * @return The loaded [PartnerAd] ad.
     */
    private fun loadBannerAd(
        context: Context,
        request: PartnerAdLoadRequest,
    ): Result<PartnerAd> {
        val listener = listeners[request.identifier]
        val ad =
            ReferenceBanner(
                context,
                request.partnerPlacement,
                chartboostMediationToReferenceBannerSize(request.size),
            )

        return ad.load(
            request.adm,
            onAdImpression = {
                PartnerLogController.log(LOAD_SUCCEEDED)
                listener?.onPartnerAdImpression(createPartnerAd(ad, request))
                    ?: LogController.d("Unable to notify partner ad impression. Listener is null.")
            },
            onAdClicked = {
                PartnerLogController.log(DID_CLICK)
                listener?.onPartnerAdClicked(createPartnerAd(ad, request))
                    ?: LogController.d("Unable to notify partner ad click. Listener is null.")
            },
        ).fold(
            onSuccess = {
                Result.success(createPartnerAd(ad, request))
            },
            onFailure = {
                PartnerLogController.log(LOAD_FAILED)
                Result.failure(it)
            },
        )
    }

    /**
     * Destroy the current banner ad.
     *
     * @param partnerAd The partner ad to be destroyed.
     */
    private fun destroyBannerAd(partnerAd: PartnerAd) {
        partnerAd.ad?.let {
            if (it is ReferenceBanner) {
                it.destroy()
            }
        }
    }

    /**
     * Map Chartboost Mediation's banner sizes to the reference SDK's supported sizes.
     *
     * @param size The Chartboost Mediation banner [Size]
     *
     * @return The reference SDK's equivalent [ReferenceBanner.Size].
     */
    private fun chartboostMediationToReferenceBannerSize(size: Size?): ReferenceBanner.Size {
        return size?.height?.let {
            when {
                it in 50 until 90 -> ReferenceBanner.Size.BANNER
                it in 90 until 250 -> ReferenceBanner.Size.LEADERBOARD
                it >= 250 -> ReferenceBanner.Size.MEDIUM_RECTANGLE
                else -> ReferenceBanner.Size.BANNER
            }
        } ?: ReferenceBanner.Size.BANNER
    }

    /**
     * Ask the reference SDK to load a fullscreen ad.
     *
     * @param context The current Context.
     * @param request The relevant data associated with the current ad load call.
     *
     * @return The loaded [PartnerAd] ad.
     */
    private suspend fun loadFullscreenAd(
        context: Context,
        request: PartnerAdLoadRequest,
    ): Result<PartnerAd> {
        val ad =
            ReferenceFullscreenAd(
                context = context,
                adUnitId = request.partnerPlacement,
                adFormat =
                    when (request.format.key) {
                        AdFormat.INTERSTITIAL.key -> INTERSTITIAL
                        AdFormat.REWARDED.key -> REWARDED
                        else -> {
                            if (request.format.key == "rewarded_interstitial") {
                                REWARDED_INTERSTITIAL
                            } else {
                                PartnerLogController.log(LOAD_FAILED)
                                return Result.failure(
                                    ChartboostMediationAdException(ChartboostMediationError.CM_LOAD_FAILURE_UNSUPPORTED_AD_FORMAT),
                                )
                            }
                        }
                    },
            )

        return suspendCancellableCoroutine { continuation ->
            fun resumeOnce(result: Result<PartnerAd>) {
                if (continuation.isActive) {
                    continuation.resume(result)
                }
            }
            ad.load(request.adm, onFullScreenAdLoaded = {
                resumeOnce(Result.success(createPartnerAd(ad, request)))
            }, onFullScreenAdLoadFailed = {
                resumeOnce(Result.failure(it))
            })
        }
    }

    /**
     * Show the current fullscreen ad.
     *
     * @param partnerAd The partner ad to be shown.
     *
     * @return Result.success(PartnerAd) if the ad was successfully shown, Result.failure(Exception) otherwise.
     */
    private suspend fun showFullscreenAd(partnerAd: PartnerAd): Result<PartnerAd> {
        partnerAd.ad?.let { ad ->
            if (ad is ReferenceFullscreenAd) {
                val listener = listeners[partnerAd.request.identifier]

                return suspendCancellableCoroutine { continuation ->
                    fun resumeOnce(result: Result<PartnerAd>) {
                        if (continuation.isActive) {
                            continuation.resume(result)
                        }
                    }

                    ad.show(
                        onFullScreenAdImpression = {
                            listener?.let {
                                PartnerLogController.log(DID_TRACK_IMPRESSION)
                                it.onPartnerAdImpression(partnerAd)
                            } ?: PartnerLogController.log(
                                CUSTOM,
                                "Unable to notify partner ad impression. Listener is null.",
                            )

                            resumeOnce(Result.success(partnerAd))
                        },
                        onFullScreenAdShowFailed = {
                            PartnerLogController.log(SHOW_FAILED, it)
                            resumeOnce(
                                Result.failure(
                                    ChartboostMediationAdException(
                                        ChartboostMediationError.CM_SHOW_FAILURE_UNKNOWN,
                                    ),
                                ),
                            )
                        },
                        onFullScreenAdDismissed = { exception ->
                            listener?.let {
                                PartnerLogController.log(DID_DISMISS, exception?.message ?: "")
                                it.onPartnerAdDismissed(partnerAd, exception)
                            } ?: run {
                                PartnerLogController.log(
                                    CUSTOM,
                                    "Unable to notify partner ad dismissal. Listener is null.",
                                )
                            }
                        },
                        onFullScreenAdRewarded = { _, _ ->
                            listener?.let {
                                PartnerLogController.log(DID_REWARD)
                                it.onPartnerAdRewarded(partnerAd)
                            } ?: run {
                                PartnerLogController.log(
                                    CUSTOM,
                                    "Unable to notify partner ad reward. Listener is null.",
                                )
                            }
                        },
                        onFullScreenAdClicked = {
                            listener?.let {
                                PartnerLogController.log(DID_CLICK)
                                it.onPartnerAdClicked(partnerAd)
                            } ?: run {
                                PartnerLogController.log(
                                    CUSTOM,
                                    "Unable to notify partner ad click. Listener is null.",
                                )
                            }
                        },
                        onFullScreenAdExpired = {
                            listener?.let {
                                PartnerLogController.log(DID_EXPIRE)
                                it.onPartnerAdExpired(partnerAd)
                            } ?: run {
                                PartnerLogController.log(
                                    CUSTOM,
                                    "Unable to notify partner ad expiration. Listener is null.",
                                )
                            }
                        },
                    )
                }
            } else {
                PartnerLogController.log(SHOW_FAILED, "Ad is not a ReferenceFullscreenAd.")
                return Result.failure(ChartboostMediationAdException(ChartboostMediationError.CM_SHOW_FAILURE_WRONG_RESOURCE_TYPE))
            }
        } ?: run {
            PartnerLogController.log(SHOW_FAILED, "Ad is null.")
            return Result.failure(ChartboostMediationAdException(ChartboostMediationError.CM_SHOW_FAILURE_AD_NOT_FOUND))
        }
    }

    /**
     * Destroy the current fullscreen ad.
     *
     * @param partnerAd The partner ad to be destroyed.
     */
    private fun destroyFullscreenAd(partnerAd: PartnerAd) {
        partnerAd.ad?.let {
            if (it is ReferenceFullscreenAd) {
                it.destroy()
            }
        }
    }

    /**
     * Create a [PartnerAd] from the given data.
     *
     * @param ad The reference SDK's ad object.
     * @param request The relevant data associated with the current ad load call.
     *
     * @return The [PartnerAd] instance for the current ad call.
     */
    private fun createPartnerAd(
        ad: Any,
        request: PartnerAdLoadRequest,
    ): PartnerAd {
        val adSize = ReferenceSdk.getOversizedAdSize(request.size ?: Size(1, 1))
        return PartnerAd(
            ad,
            mapOf(
                "foo" to "bar",
                "banner_width_dips" to "${adSize.width}",
                "banner_height_dips" to "${adSize.height}",
            ),
            request,
        )
    }
}
