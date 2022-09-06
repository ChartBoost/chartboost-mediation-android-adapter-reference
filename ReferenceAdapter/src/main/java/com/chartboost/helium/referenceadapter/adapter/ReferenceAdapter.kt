package com.chartboost.helium.referenceadapter.adapter

import android.content.Context
import android.util.Size
import com.chartboost.helium.referenceadapter.BuildConfig
import com.chartboost.helium.referenceadapter.sdk.ReferenceBanner
import com.chartboost.helium.referenceadapter.sdk.ReferenceFullscreenAd
import com.chartboost.helium.referenceadapter.sdk.ReferenceFullscreenAd.ReferenceFullscreenAdFormat
import com.chartboost.helium.referenceadapter.sdk.ReferenceFullscreenAd.ReferenceFullscreenAdFormat.INTERSTITIAL
import com.chartboost.helium.referenceadapter.sdk.ReferenceFullscreenAd.ReferenceFullscreenAdFormat.REWARDED
import com.chartboost.helium.referenceadapter.sdk.ReferenceSdk
import com.chartboost.heliumsdk.domain.*
import com.chartboost.heliumsdk.utils.PartnerLogController
import com.chartboost.heliumsdk.utils.PartnerLogController.PartnerAdapterEvents.*
import com.chartboost.heliumsdk.utils.PartnerLogController.PartnerAdapterFailureEvents.SHOW_FAILED
import com.chartboost.heliumsdk.utils.PartnerLogController.PartnerAdapterSuccessEvents.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * INTERNAL. FOR DEMO AND TESTING PURPOSES ONLY. DO NOT USE DIRECTLY.
 *
 * An adapter that is used for reference purposes. It is designed to showcase and test the
 * mediation contract of the Helium SDK.
 *
 * Implementations of the Helium mediation interface may roughly model their own design after this class,
 * but do NOT call this adapter directly.
 */
class ReferenceAdapter : PartnerAdapter {
    /**
     * A map of Helium's listeners for the corresponding Helium placements.
     */
    private val listeners = mutableMapOf<String, PartnerAdListener>()

    /**
     * Override this value to return the version of the partner SDK.
     */
    override val partnerSdkVersion: String
        get() = ReferenceSdk.getVersion()

    /**
     * Override this value to return the version of the mediation adapter.
     * To determine the version, use the following scheme to indicate compatibility:
     *
     * [Helium SDK Major Version].[Partner SDK Major Version].[Partner SDK Minor Version].[Partner SDK Patch Version].[Adapter Version]
     *
     * For example, if this adapter is compatible with Helium SDK 2.x.y and partner SDK 1.0.0, and
     * this is its initial release, then its version should be 2.1.0.0.0.
     */
    override val adapterVersion: String
        get() = BuildConfig.HELIUM_REFERENCE_ADAPTER_VERSION

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
     * @param partnerConfiguration The necessary initialization data provided by Helium.
     *
     * @return Result.success(Unit) if the initialization was successful, Result.failure(Exception) otherwise.
     */
    override suspend fun setUp(
        context: Context,
        partnerConfiguration: PartnerConfiguration
    ): Result<Unit> {
        PartnerLogController.log(SETUP_STARTED)

        // For simplicity, the reference adapter always assumes successes.
        ReferenceSdk.initialize()
        return Result.success(PartnerLogController.log(SETUP_SUCCEEDED))
    }

    /**
     * Override this method to make an ad request to the partner SDK for the given ad format.
     *
     * @param context The current Context.
     * @param request The relevant data associated with the current ad load call.
     * @param partnerAdListener The listener to notify the Helium SDK of the partner ad events.
     *
     * @return Result.success(PartnerAd) if the request was successful, Result.failure(Exception) otherwise.
     */
    override suspend fun load(
        context: Context,
        request: PartnerAdLoadRequest,
        partnerAdListener: PartnerAdListener
    ): Result<PartnerAd> {
        PartnerLogController.log(LOAD_STARTED)

        // Save the listener for later use.
        listeners[request.heliumPlacement] = partnerAdListener

        delay(1000L)

        // For simplicity, the reference adapter always assumes successes.
        return Result.success(
            when (request.format) {
                AdFormat.BANNER -> {
                    loadBannerAd(context, request)
                }

                // For simplicity, this example uses a unified API for both interstitial and rewarded
                // ads. Your implementations may differ.
                AdFormat.INTERSTITIAL, AdFormat.REWARDED -> {
                    loadFullscreenAd(context, request)
                }
            }
        )
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

        listeners.remove(partnerAd.request.partnerPlacement)

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
    override suspend fun show(context: Context, partnerAd: PartnerAd): Result<PartnerAd> {
        PartnerLogController.log(SHOW_STARTED)

        return when (partnerAd.request.format) {
            AdFormat.BANNER -> {
                // Banners do not have a "show" mechanism.
                PartnerLogController.log(SHOW_SUCCEEDED)
                Result.success(partnerAd)
            }
            AdFormat.INTERSTITIAL, AdFormat.REWARDED -> {
                showFullscreenAd(partnerAd)
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
        request: PreBidRequest
    ): Map<String, String> {
        PartnerLogController.log(BIDDER_INFO_FETCH_STARTED)
        PartnerLogController.log(BIDDER_INFO_FETCH_SUCCEEDED)

        return mapOf("bid_token" to ReferenceSdk.getBidToken())
    }

    /**
     * Override this method to notify your partner SDK of GDPR applicability as determined by
     * the Helium SDK.
     *
     * @param context The current [Context].
     * @param gdprApplies True if GDPR applies, false otherwise.
     */
    override fun setGdprApplies(context: Context, gdprApplies: Boolean) {
        PartnerLogController.log(
            CUSTOM,
            "The reference adapter has been notified that GDPR " +
                    (if (gdprApplies) "applies" else "does not apply.")
        )
    }

    /**
     * Override this method to notify your partner SDK of the GDPR consent status as determined by
     * the Helium SDK.
     *
     * @param context The current [Context].
     * @param gdprConsentStatus The user's current GDPR consent status.
     */
    override fun setGdprConsentStatus(context: Context, gdprConsentStatus: GdprConsentStatus) {
        PartnerLogController.log(
            CUSTOM,
            "The reference adapter has been notified that the user's GDPR consent status " +
                    "is ${gdprConsentStatus.name}"
        )
    }

    /**
     * Override this method to notify your partner SDK of the CCPA privacy String as supplied by
     * the Helium SDK.
     *
     * @param context The current [Context].
     * @param hasGivenCcpaConsent True if the user has given CCPA consent, false otherwise.
     * @param privacyString The CCPA privacy String.
     */
    override fun setCcpaConsent(
        context: Context,
        hasGivenCcpaConsent: Boolean,
        privacyString: String?
    ) {
        PartnerLogController.log(
            CUSTOM,
            "The reference adapter has been notified that the user's CCPA privacy string " +
                    "is $privacyString. And the user has ${if (hasGivenCcpaConsent) "given" else "not given"} CCPA consent."
        )
    }

    /**
     * Override this method to notify your partner SDK of the COPPA subjectivity as determined by
     * the Helium SDK.
     *
     * @param context The current [Context].
     * @param isSubjectToCoppa True if the user is subject to COPPA, false otherwise.
     */
    override fun setUserSubjectToCoppa(context: Context, isSubjectToCoppa: Boolean) {
        PartnerLogController.log(
            CUSTOM,
            "The reference adapter has been notified that the user is ${if (isSubjectToCoppa) "subject to" else "not subject to"} COPPA."
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
        request: PartnerAdLoadRequest
    ): PartnerAd {
        val ad = ReferenceBanner(
            context, request.partnerPlacement,
            heliumToReferenceBannerSize(request.size)
        )
        val partnerAd = PartnerAd(ad, mapOf("foo" to "bar"), request)
        val listener = listeners[request.partnerPlacement]

        ad.load(
            request.adm,
            onAdImpression = {
                PartnerLogController.log(LOAD_SUCCEEDED)
                listener?.onPartnerAdImpression(partnerAd)
            },
            onAdClicked = { listener?.onPartnerAdClicked(partnerAd) }
        )

        return partnerAd
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
     * Map Helium's banner sizes to the reference SDK's supported sizes.
     *
     * @param size The Helium banner [Size]
     *
     * @return The reference SDK's equivalent [ReferenceBanner.Size].
     */
    private fun heliumToReferenceBannerSize(size: Size?): ReferenceBanner.Size {
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
     * This method will randomly decide the ad format (interstitial or rewarded).
     *
     * @param context The current Context.
     * @param request The relevant data associated with the current ad load call.
     *
     * @return The loaded [PartnerAd] ad.
     */
    private fun loadFullscreenAd(
        context: Context,
        request: PartnerAdLoadRequest,
    ): PartnerAd {
        val ad =
            ReferenceFullscreenAd(context, request.partnerPlacement, getRandomFullscreenAdFormat())
        ad.load(request.adm)

        PartnerLogController.log(LOAD_SUCCEEDED)
        return PartnerAd(ad, mapOf("foo" to "bar"), request)
    }

    /**
     * Show the current fullscreen ad.
     *
     * @param partnerAd The partner ad to be shown.
     *
     * @return Result.success(PartnerAd) if the ad was successfully shown, Result.failure(Exception) otherwise.
     */
    private suspend fun showFullscreenAd(
        partnerAd: PartnerAd
    ): Result<PartnerAd> {
        partnerAd.ad?.let {
            if (it is ReferenceFullscreenAd) {
                val listener = listeners[partnerAd.request.partnerPlacement]

                return suspendCancellableCoroutine { continuation ->
                    it.show(
                        onFullScreenAdImpression = {
                            PartnerLogController.log(SHOW_SUCCEEDED)
                            listener?.onPartnerAdImpression(partnerAd)
                                ?: PartnerLogController.log(
                                    CUSTOM,
                                    "Unable to notify partner ad impression. Listener is null."
                                )

                            // For simplicity, the reference adapter always assumes successes.
                            continuation.resume(Result.success(partnerAd))
                        },
                        onFullScreenAdDismissed = {
                            listener?.onPartnerAdDismissed(partnerAd, null) ?: run {
                                PartnerLogController.log(
                                    CUSTOM,
                                    "Unable to notify partner ad dismissal. Listener is null."
                                )
                            }
                        },
                        onFullScreenAdRewarded = { amount, label ->
                            listener?.onPartnerAdRewarded(partnerAd, Reward(amount, label)) ?: run {
                                PartnerLogController.log(
                                    CUSTOM,
                                    "Unable to notify partner ad reward. Listener is null."
                                )
                            }
                        },
                        onFullScreenAdClicked = {
                            listener?.onPartnerAdClicked(partnerAd) ?: run {
                                PartnerLogController.log(
                                    CUSTOM,
                                    "Unable to notify partner ad click. Listener is null."
                                )
                            }
                        },
                        onFullScreenAdExpired = { error ->
                            PartnerLogController.log(SHOW_FAILED, error)
                            listener?.onPartnerAdExpired(partnerAd) ?: run {
                                PartnerLogController.log(
                                    CUSTOM,
                                    "Unable to notify partner ad expiration. Listener is null."
                                )
                            }
                        }
                    )
                }
            } else {
                PartnerLogController.log(SHOW_FAILED, "Ad is not a ReferenceFullscreenAd.")
                return Result.failure(HeliumAdException(HeliumErrorCode.INTERNAL))
            }
        } ?: run {
            PartnerLogController.log(SHOW_FAILED, "Ad is null.")
            return Result.failure(HeliumAdException(HeliumErrorCode.INTERNAL))
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
     * Return a random fullscreen ad format.
     *
     * @return A random ad format (interstitial or rewarded).
     */
    private fun getRandomFullscreenAdFormat(): ReferenceFullscreenAdFormat {
        return if (Math.random() < 0.5) INTERSTITIAL else REWARDED
    }
}
