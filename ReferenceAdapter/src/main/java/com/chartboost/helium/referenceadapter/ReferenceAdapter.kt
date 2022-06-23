package com.chartboost.helium.referenceadapter;

import android.content.Context
import android.util.Size
import com.chartboost.helium.referenceadapter.ReferenceFullscreenAd.ReferenceFullscreenAdFormat
import com.chartboost.helium.referenceadapter.ReferenceFullscreenAd.ReferenceFullscreenAdFormat.INTERSTITIAL
import com.chartboost.helium.referenceadapter.ReferenceFullscreenAd.ReferenceFullscreenAdFormat.REWARDED
import com.chartboost.heliumsdk.domain.*
import com.chartboost.heliumsdk.utils.LogController

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
    companion object {
        private const val ADAPTER_NAME = "[ReferenceAdapter]"
        private const val ADAPTER_VERSION = BuildConfig.VERSION_NAME
        private const val PARTNER_NAME = "ReferenceNetwork"
        private const val PARTNER_DISPLAY_NAME = "Reference Network"
    }

    /**
     * A listener to notify the Helium SDK of the partner ad events.
     */
    private var listener: PartnerAdListener? = null

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
        get() = ADAPTER_VERSION

    /**
     * Override this value to return the name of the partner SDK.
     */
    override val partnerName: String
        get() = PARTNER_NAME

    /**
     * Override this value to return the display name of the partner SDK.
     */
    override val partnerDisplayName: String
        get() = PARTNER_DISPLAY_NAME

    /**
     * Override this method to initialize the partner SDK so that it's ready to request and display ads.
     *
     * @param context The current Context.
     * @param partnerInitData The necessary initialization data provided by Helium.
     *
     * @return Result.success(Unit) if the initialization was successful, Result.failure(Unit) otherwise.
     */
    override suspend fun setUp(context: Context, partnerInitData: PartnerInitData): Result<Unit> {
        var succeeded: Boolean? = null
        ReferenceSdk.initialize { succeeded = it }

        return if (succeeded == true)
            Result.success(LogController.i("The reference SDK has been initialized."))
        else
            Result.failure(Exception("The reference SDK has failed to initialize."))
    }

    /**
     * Override this method to make an ad request to the partner SDK for the given ad format.
     *
     * @param context The current Context.
     * @param adData The necessary data associated with the current ad.
     * @param partnerSettings The partner-specific settings for this ad request.
     * @param adm The ad markup to be rendered by the partner SDK.
     * @param partnerAdListener The listener to notify the Helium SDK of the partner ad events.
     *
     * @return Result.success(PartnerAd) if the request was successful, Result.failure(PartnerAd) otherwise.
     */
    override suspend fun load(
        context: Context,
        adData: AdData,
        partnerSettings: Map<String, String>,
        adm: String?,
        partnerAdListener: PartnerAdListener
    ): Result<PartnerAd> {
        // Save the listener for later use.
        listener = partnerAdListener

        // For simplicity, the reference adapter always assumes successes.
        return Result.success(
            when (adData.format) {
                AdFormat.BANNER -> {
                    loadBannerAd(context, adm, adData)
                }

                // For simplicity, this example uses a unified API for both interstitial and rewarded
                // ads. Your implementations may differ.
                AdFormat.INTERSTITIAL, AdFormat.REWARDED -> {
                    loadFullscreenAd(context, adm, adData)
                }
            }
        )
    }

    /**
     * Override this method to discard current ad objects and release resources.
     *
     * @param partnerAd The partner ad to be discarded.
     *
     * @return Result.success(PartnerAd) if the ad was successfully discarded, Result.failure(PartnerAd) otherwise.
     */
    override suspend fun invalidate(partnerAd: PartnerAd): Result<PartnerAd> {
        destroyBannerAd(partnerAd)
        destroyFullscreenAd(partnerAd)

        // For simplicity, the reference adapter always assumes successes.
        return Result.success(partnerAd)
    }

    /**
     * Override this method to show the partner ad.
     *
     * @param context The current Context.
     * @param partnerAd The partner ad to be shown.
     *
     * @return Result.success(PartnerAd) if the ad was successfully shown, Result.failure(PartnerAd) otherwise.
     */
    override suspend fun show(context: Context, partnerAd: PartnerAd): Result<PartnerAd> {
        // Only show fullscreen ad formats. Banners do not have a "show" mechanism.
        return if (partnerAd.adData.format == AdFormat.INTERSTITIAL || partnerAd.adData.format == AdFormat.REWARDED) {
            showFullscreenAd(partnerAd)
        } else {
            Result.failure(Exception("Unsupported ad format"))
        }
    }

    /**
     * Override this method to compute and return a bid token for the bid request.
     *
     * @param context The current Context.
     * @param adData The necessary data associated with the current ad.
     *
     * @return A Map<String, String> of a biddable token keyed by an identifier.
     */
    override suspend fun fetchBidderInformation(
        context: Context,
        adData: AdData
    ): Map<String, String> {
        return mapOf("bid_token" to ReferenceSdk.getBidToken())
    }

    /**
     * Override this method to notify your partner SDK of GDPR applicability as determined by
     * the Helium SDK.
     *
     * @param gdprApplies True if GDPR applies, false if GDPR does not apply, and null if the
     *                   Helium SDK has not yet determined whether GDPR applies.
     */
    override fun setGdprApplies(gdprApplies: Boolean) {
        // NO-OP for the purpose of this adapter
    }

    /**
     * Override this method to notify your partner SDK of the GDPR consent status as determined by
     * the Helium SDK.
     *
     * @param gdprConsentStatus The consent status of the user (unknown, denied, or granted).
     */
    override fun setGdprConsentStatus(gdprConsentStatus: GdprConsentStatus) {
        // NO-OP for the purpose of this adapter
    }

    /**
     * Override this method to notify your partner SDK of the CCPA privacy String as supplied by
     * the Helium SDK.
     *
     * @param privacyString The privacy string, if available.
     */
    override fun setCcpaPrivacyString(privacyString: String?) {
        // NO-OP for the purpose of this adapter
    }

    /**
     * Override this method to notify your partner SDK of the COPPA subjectivity as determined by
     * the Helium SDK.
     *
     * @param isSubjectToCoppa True if the user is subject to COPPA, false if the user is not subject to COPPA, and null if the
     *                        Helium SDK has not yet determined whether the user is subject to COPPA.
     */
    override fun setUserSubjectToCoppa(isSubjectToCoppa: Boolean) {
        // NO-OP for the purpose of this adapter
    }

    /**
     * Ask the reference SDK to load a banner ad.
     *
     * @param context The current Context.
     * @param adm The ad markup to be rendered by the reference SDK.
     * @param adData The necessary data associated with the current ad.
     *
     * @return The loaded [PartnerAd] ad.
     */
    private fun loadBannerAd(
        context: Context,
        adm: String?,
        adData: AdData
    ): PartnerAd {
        val ad = ReferenceBanner(
            context, adData.placementId,
            heliumToReferenceBannerSize(adData.size)
        )
        val partnerAd = PartnerAd(ad, null, mapOf("foo" to "bar"), adData)

        ad.load(
            adm,
            onAdImpression = { listener?.onPartnerAdImpression(partnerAd) },
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
        return when (size) {
            Size(320, 50) -> ReferenceBanner.Size.BANNER
            Size(300, 250) -> ReferenceBanner.Size.MEDIUM_RECTANGLE
            Size(728, 90) -> ReferenceBanner.Size.LEADERBOARD
            else -> ReferenceBanner.Size.BANNER
        }
    }

    /**
     * Ask the reference SDK to load a fullscreen ad.
     * This method will randomly decide the ad format (interstitial or rewarded).
     *
     * @param context The current Context.
     * @param adm The ad markup.
     * @param adData The necessary data associated with the current ad.
     *
     * @return The loaded [PartnerAd] ad.
     */
    private fun loadFullscreenAd(
        context: Context,
        adm: String?,
        adData: AdData
    ): PartnerAd {
        val ad = ReferenceFullscreenAd(context, adData.placementId, getRandomFullscreenAdFormat())
        ad.load(adm)

        return PartnerAd(ad, null, mapOf("foo" to "bar"), adData)
    }

    /**
     * Show the current fullscreen ad.
     *
     * @param partnerAd The partner ad to be shown.
     *
     * @return Result.success(PartnerAd) if the ad was successfully shown, Result.failure(PartnerAd) otherwise.
     */
    private fun showFullscreenAd(partnerAd: PartnerAd): Result<PartnerAd> {
        partnerAd.ad?.let {
            if (it is ReferenceFullscreenAd) {
                it.show(
                    onFullScreenAdImpression = {
                        listener?.onPartnerAdImpression(partnerAd)
                    },
                    onFullScreenAdFinished = {
                        listener?.onPartnerAdFinished(partnerAd, null)
                    },
                    onFullScreenAdRewarded = { amount, label ->
                        listener?.onPartnerAdRewarded(partnerAd, Reward(amount, label))
                    },
                    onFullScreenAdClicked = {
                        listener?.onPartnerAdClicked(partnerAd)
                    },
                    onFullScreenAdExpired = {
                        listener?.onPartnerAdExpired(partnerAd)
                    }
                )
            }
        }

        // For simplicity, the reference adapter always assumes successes.
        return Result.success(partnerAd)
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
