# Chartboost Mediation Reference Adapter

The Chartboost Mediation Reference adapter mediates the Reference SDK via the Chartboost Mediation SDK.

## Minimum Requirements

| Plugin                   | Version |
| ------------------------ | ------- |
| Chartboost Mediation SDK | 4.0.0+  |
| Android API              | 21+     |

## Integration

In your `build.gradle`, add the following entry:
```groovy
    implementation "com.chartboost:chartboost-mediation-adapter-reference:4.1.0.1.0"
```

## Chartboost Mediation Custom Adapter Implementation Guidelines

This guide is intended to help you implement custom mediation adapters that work with the Chartboost
Mediation SDK on Android.

> [!IMPORTANT]
> Chartboost Mediation will not be providing official support for any custom adapters. For official adapters that we develop and support, [visit this site](https://adapters.chartboost.com).

> [!NOTE]
> The following instructions are available in Kotlin. If your adapter is going to be published and maintained by Chartboost Mediation, ensure that it is written in Kotlin.

1. Create a new class and implement Chartboost Mediation's `PartnerAdapter` interface.

    ```kotlin
    class ReferenceAdapter : PartnerAdapter
    ```

2. Override `val partnerSdkVersion: String` with the version of the partner SDK.

    ```kotlin
    override val partnerSdkVersion: String
        get() = ReferenceSdk.REFERENCE_SDK_VERSION
    ```

3. Override `val adapterVersion: String` with the version of the mediation adapter.
    - You may version the adapter using any preferred convention, but it is recommended to apply the following format if the adapter will be published by Chartboost Mediation: `[Chartboost Mediation][Partner][Adapter]`, where `[Chartboost Mediation]` represents the Chartboost Mediation SDK’s major version that is compatible with this adapter, `[Partner]` represents the partner SDK’s version, and `[Adapter]` represents this adapter’s version (starting with 0).
        - For example, if this adapter is compatible with Chartboost Mediation SDK 4.x and partner SDK 1.2.3.x (where x is optional), and this is its initial release, then adapterVersion is 4.1.2.3.x.0.

    ```kotlin
    override val adapterVersion: String
        get() = BuildConfig.CHARTBOOST_MEDIATION_REFERENCE_ADAPTER_VERSION
    ```

4. Override `val partnerId: String` with the internal identifier that the Chartboost Mediation SDK can use to refer to the current partner.

    ```kotlin
    override val partnerId: String
        get() = "reference"
    ```

5. Override `val partnerDisplayName: String` with the official/external name of the current partner.

    ```kotlin
    override val partnerDisplayName: String
        get() = "Reference"
    ```

6. Override `fun setGdpr(context: Context, applies: Boolean?, gdprConsentStatus: GdprConsentStatus)` to notify the partner SDK of the GDPR applicability as needed and to notify the partner SDK of the GDPR consent status as needed.

    ```kotlin
    override fun setGdpr(
        context: Context,
        applies: Boolean?,
        gdprConsentStatus: GdprConsentStatus
    ) {
        PartnerLogController.log(
            when (applies) {
                true -> GDPR_APPLICABLE
                false -> GDPR_NOT_APPLICABLE
                else -> GDPR_UNKNOWN
            }
        )

        PartnerLogController.log(
            when (gdprConsentStatus) {
                GdprConsentStatus.GDPR_CONSENT_UNKNOWN -> GDPR_CONSENT_UNKNOWN
                GdprConsentStatus.GDPR_CONSENT_GRANTED -> GDPR_CONSENT_GRANTED
                GdprConsentStatus.GDPR_CONSENT_DENIED -> GDPR_CONSENT_DENIED
            }
        )

        // Implement your SDK's GDPR methods here.
    }
    ```

7. Override `fun setCcpaConsent(context: Context, hasGrantedCcpaConsent: Boolean, privacyString: String)` to notify the partner SDK of the CCPA consent as needed.

    ```kotlin
    override fun setCcpaConsent(
        context: Context,
        hasGrantedCcpaConsent: Boolean,
        privacyString: String
    ) {
        PartnerLogController.log(
            if (hasGrantedCcpaConsent) CCPA_CONSENT_GRANTED
            else CCPA_CONSENT_DENIED
        )

        // Implement your SDK's CCPA consent methods here.
    }
    ```

8. Override `fun setUserSubjectToCoppa(context: Context, isSubjectToCoppa: Boolean)` to notify the partner SDK of whether the user is subject to COPPA as needed.

    ```kotlin
    override fun setUserSubjectToCoppa(context: Context, isSubjectToCoppa: Boolean) {
        PartnerLogController.log(
            if (isSubjectToCoppa) COPPA_SUBJECT
            else COPPA_NOT_SUBJECT
        )
        
        // Implement your SDK's COPPA methods here.
    }
    ```

9. Override `suspend fun setUp(context: Context, partnerConfiguration: PartnerConfiguration): Result<Unit>` to initialize the partner SDK and perform any necessary setup in order to request and serve ads.
    - If the operation succeeds, return `Result.success`. Otherwise, return `Result.failure(Exception)`.

    > [!NOTE]
    > This operation may time out as deemed necessary by the Chartboost Mediation SDK. However, the partner SDK’s initialization attempt will not be canceled and may continue until completion.

    ```kotlin
    override suspend fun setUp(
        context: Context,
        partnerConfiguration: PartnerConfiguration
    ): Result<Unit> {
        PartnerLogController.log(SETUP_STARTED)

        return ReferenceSdk.initialize().fold(
            onSuccess = {
                Result.success(PartnerLogController.log(SETUP_SUCCEEDED))
            },
            onFailure = {
                PartnerLogController.log(SETUP_FAILED)
                Result.failure(it)
            }
        )
    }
    ```

10. Override `suspend fun fetchBidderInformation(context: Context, request: PreBidRequest): Map<String, String>` to compute and return a `Map<String, String>` of biddable token Strings. If network bidding is not supported by the current partner, return an empty `Map`.

    > [!NOTE]
    > This operation may time out as deemed necessary by the Chartboost Mediation SDK.

    ```kotlin
    override suspend fun fetchBidderInformation(
        context: Context,
        request: PreBidRequest
    ): Map<String, String> {
        PartnerLogController.log(BIDDER_INFO_FETCH_STARTED)

        val token = ReferenceSdk.getBidToken()

        PartnerLogController.log(if (token.isNotEmpty()) BIDDER_INFO_FETCH_SUCCEEDED else BIDDER_INFO_FETCH_FAILED)
        return mapOf("bid_token" to token)
    }
    ```

11. Override `suspend fun load(context: Context, request: PartnerAdLoadRequest, partnerAdListener: PartnerAdListener): Result<PartnerAd>` to make an ad request.
    - `PartnerAd(val ad: Any?, val details: Map<String, String>, val request: PartnerAdLoadRequest)` is a data class that holds your partner’s ad object along with data relevant to the current ad request. You may populate and pass a `Map<String, String>` of data that you’d like to persist between calls.
    - If an ad is successfully loaded, return `Result.success(PartnerAd)`. Otherwise, return `Result.failure(Exception)`.

    > [!NOTE]
    > This operation may time out as deemed necessary by the Chartboost Mediation SDK.

    ```kotlin
    override suspend fun load(
        context: Context,
        request: PartnerAdLoadRequest,
        partnerAdListener: PartnerAdListener
    ): Result<PartnerAd> {
        PartnerLogController.log(LOAD_STARTED)
        // Implement your SDK load calls here.
    }
    ```

12. Override `suspend fun show(context: Context, partnerAd: PartnerAd): Result<PartnerAd>` to show the loaded ad, if one is available.
    - Since banner ads render upon load and do not have a separate “show” stage, you will only need to handle non-banner ads in this call.
    - If an ad is successfully shown, return `Result.success(PartnerAd)`. Otherwise, return `Result.failure(Exception)`.

    > [!NOTE]
    > This operation may time out as deemed necessary by the Chartboost Mediation SDK.

    ```kotlin
    override suspend fun show(context: Context, partnerAd: PartnerAd): Result<PartnerAd> {
        PartnerLogController.log(SHOW_STARTED)
        // Implement your SDK show calls here.
    }
    ```

13. Override `suspend fun invalidate(partnerAd: PartnerAd): Result<PartnerAd>` to discard unnecessary ad objects and release resources.
    - This is required to destroy any `Views` from a specific `PartnerAd`, especially for banners.
    - If the ad is successfully discarded, return `Result.success(PartnerAd)`. Otherwise, return `Result.failure(Exception)`.

    ```kotlin
    override suspend fun invalidate(partnerAd: PartnerAd): Result<PartnerAd> {
        PartnerLogController.log(INVALIDATE_STARTED)
        // Implement your SDK destroy calls here.
    }
    ```

14. Notify the `PartnerAdListener` of the following ad lifecycle events as applicable:
    - `onPartnerAdImpression(partnerAd: PartnerAd)` when the partner SDK registers an impression for the currently showing ad.
    - `onPartnerAdClicked(partnerAd: PartnerAd)` when the partner ad has been clicked as the result of a user action.
    - `onPartnerAdRewarded(partnerAd: PartnerAd, reward: Reward)` when a reward has been given for watching a video ad.
    - `onPartnerAdDismissed(partnerAd: PartnerAd, error: ChartboostMediationAdException?)` when the partner ad has been dismissed as the result of a user action.
    - `onPartnerAdExpired(partnerAd: PartnerAd)` when the partner ad has expired as determined by the partner SDK.

    ```kotlin
    interface PartnerAdListener {
        fun onPartnerAdImpression(partnerAd: PartnerAd)
        fun onPartnerAdClicked(partnerAd: PartnerAd)
        fun onPartnerAdRewarded(partnerAd: PartnerAd)
        fun onPartnerAdDismissed(partnerAd: PartnerAd, error: ChartboostMediationAdException?)
        fun onPartnerAdExpired(partnerAd: PartnerAd)
    }
    ```

15. Refer to the Chartboost Mediation SDK’s PartnerAdapterEvents enum for a complete list of log events to be used.

## Custom Mediation Adapter Best Practices

1. Ensure that your adapter follows a similar structure:
   - Adapter Class
     - Companion Object
     - Private Values and Variables
     - Overridden Chartboost Mediation APIs
     - Private Functions

2. Use the companion object to expose your public SDK APIs to publishers that are integrating your adapter.
    - Some usages include enabling/disabling test modes, logging, SDK settings, or other SDK methods you want to expose to publishers.

    ```kotlin
        companion object {
            /**
            * Sample code for an SDK that has a test mode API.
            */
            var testMode = SampleSDK.getTestMode()
                set(value) {
                    field = value
                    SampleSDK.setTestMode(value)
                    PartnerLogController.log(
                        CUSTOM,
                        "SampleSDK test mode is ${if (value) "enabled" else "disabled"}."
                    )
                }

            /**
            * Sample code for an SDK that has a Log level option.
            */
            var logLevel = Logger.Level.info
                set(value) {
                    SampleSDK.setLogLevel(value)
                    field = value
                    PartnerLogController.log(CUSTOM, "Sample SDK log level set to $value.")
                }
        }
    ```

3. Determine if your SDK will be integrated with Mediation, Programmatic Bidding, or both.
    - This will determine if you need to implement the `fetchBidderInformation` as well as passing the adm from the request returned. Otherwise, you simply just need to pass the placement to your SDK.

    > [!WARNING]
    > This is sample code, don't use the code below for your own adapter. Please refer to the code in our actual adapters for real usage samples.

    ```kotlin
    // Example in which a distinction is made for a programmatic or mediation ad load request.
    // Make the load distinction at load time.
        if (request.adm.isNullOrEmpty()) {
            // This is a mediation request. Pass the partner placement name to your sdk.
            PartnerSDK.load(request.partnerPlacement)
        } else {
            // This is a programmatic request. Pass the returned adm to your sdk.
            PartnerSDK.load(request.adm)
        }
    ```

4. Determine if your SDK supports the currently supported Chartboost Mediation ad formats:
    - Banner
    - Interstitial
    - Rewarded
    - Rewarded Interstitial

        > [!IMPORTANT]
        > To determine the ad format for Rewarded Interstitial, you will need to do the following check:
        >
        > ```kotlin
        > request.format.key == "rewarded_interstitial"
        >```

5. Log your adapter with the `PartnerLogController` class and its appropriate `PartnerAdapterEvents` enums.

6. When using suspendable coroutines, it is preferable to use the `CancellableCoroutine` class and provide a `continuation.isActive` check to prevent continuation crashes on multiple calls.

    > [!WARNING]
    > This is sample code, don't use the code below for your own adapter. Please refer to the code in our actual adapters for real usage samples.

    ```kotlin
    /**
     * Example usage of a suspendable coroutine.
     */
    override suspend fun setUp(
        context: Context,
        partnerConfiguration: PartnerConfiguration
    ): Result<Unit> {
        PartnerLogController.log(SETUP_STARTED)

        return suspendCancellableCoroutine { continuation ->
            fun resumeOnce(result: Result<Unit>) {
                if (continuation.isActive) {
                    continuation.resume(result)
                }
            }

            Chartboost.startWithAppId(
                context.applicationContext,
                appId,
                app_signature,
            ) { startError ->

                startError?.let {
                    PartnerLogController.log(SETUP_FAILED, "${it.code}")
                    resumeOnce(
                        Result.failure(
                            ChartboostMediationAdException(
                                getChartboostMediationError(it),
                            )
                        )
                    )
                } ?: run {
                    PartnerLogController.log(SETUP_SUCCEEDED)
                    resumeOnce(
                        Result.success(PartnerLogController.log(SETUP_SUCCEEDED))
                    )
                }
            }
        }
    }
    ```

7. Depending on your SDK, if you have show callbacks that need to be mapped with Chartboost Mediation's callbacks at show time, you will need to save the `PartnerAdListener` listener you used at load time.
    - It is recommended to map the `PartnerAdListener` listener with the appropriate load identifier to prevent listeners from being lost or being triggered wrongly. To get the request identifier, it can be found via `PartnerAdLoadRequest.identifier`

    > [!WARNING]
    > This is sample code, don't use the code below for your own adapter. Please refer to the code in our actual adapters for real usage samples.

    ```kotlin
    /**
     * A map of Chartboost Mediation's listeners for the corresponding load identifier.
     */
    private val listeners = mutableMapOf<String, PartnerAdListener>()

    // During setup, clear the map.
    override suspend fun setUp(
        context: Context,
        partnerConfiguration: PartnerConfiguration
    ): Result<Unit> {
        // ...
        listeners.clear()
        // ...
    }

    // During load, save the listener.
    override suspend fun load(
        context: Context,
        request: PartnerAdLoadRequest,
        partnerAdListener: PartnerAdListener
    ): Result<PartnerAd> {
        // ... 
        // Save the listener for later usage.
        listeners[request.identifier] = partnerAdListener
        // ...
    }

    // During show, get the listener from the map and invoke it where it will be used.
    override suspend fun load(
        context: Context,
        request: PartnerAdLoadRequest,
        partnerAdListener: PartnerAdListener
    ): Result<PartnerAd> {
        // ... 
         val listener = listeners[partnerAd.request.identifier]
         val partnerSDKShowListener = object : ShowListener() {
            override fun onAdImpression() {
                PartnerLogController.log(DID_TRACK_IMPRESSION)
                listener?.onPartnerAdImpression(partnerAd) ?: PartnerLogController.log(
                    CUSTOM,
                    "Unable to fire onPartnerAdImpression for sample adapter."
                )
            }
         }
         // ...
    }

    // Once you have or done and destroyed an ad, remove the listener from the map.
    override suspend fun invalidate(partnerAd: PartnerAd): Result<PartnerAd> {
        // ...
        listeners.remove(partnerAd.request.identifier)
        // ...
    }
    ```

8. Map your SDK's error codes with Chartboost Mediation's in a separate function and as relevant as possible.

    > [!WARNING]
    > This is sample code, don't use the code below for your own adapter. Please refer to the code in our actual adapters for real usage samples.

    ```kotlin
    /**
     * Example usage from the Chartboost Monetization Adapter.
     */
    private fun getChartboostMediationError(error: CBError) = when (error) {
        is StartError -> {
            when (error.code) {
                StartError.Code.INVALID_CREDENTIALS -> ChartboostMediationError.CM_INITIALIZATION_FAILURE_INVALID_CREDENTIALS
                StartError.Code.NETWORK_FAILURE -> ChartboostMediationError.CM_AD_SERVER_ERROR
                else -> ChartboostMediationError.CM_INITIALIZATION_FAILURE_UNKNOWN
            }
        }
        is CacheError -> {
            when (error.code) {
                CacheError.Code.INTERNET_UNAVAILABLE -> ChartboostMediationError.CM_NO_CONNECTIVITY
                CacheError.Code.NO_AD_FOUND -> ChartboostMediationError.CM_LOAD_FAILURE_NO_FILL
                CacheError.Code.SESSION_NOT_STARTED -> ChartboostMediationError.CM_INITIALIZATION_FAILURE_UNKNOWN
                CacheError.Code.NETWORK_FAILURE, CacheError.Code.SERVER_ERROR -> ChartboostMediationError.CM_AD_SERVER_ERROR
                else -> ChartboostMediationError.CM_PARTNER_ERROR
            }
        }
        is ShowError -> {
            when (error.code) {
                ShowError.Code.INTERNET_UNAVAILABLE -> ChartboostMediationError.CM_NO_CONNECTIVITY
                ShowError.Code.NO_CACHED_AD -> ChartboostMediationError.CM_SHOW_FAILURE_AD_NOT_READY
                ShowError.Code.SESSION_NOT_STARTED -> ChartboostMediationError.CM_SHOW_FAILURE_NOT_INITIALIZED
                else -> ChartboostMediationError.CM_PARTNER_ERROR
            }
        }
        else -> ChartboostMediationError.CM_UNKNOWN_ERROR
    }
    ```

9. Convert requested Chartboost Mediation banner sizes to those with your SDK banner sizes.

    > [!WARNING]
    > This is sample code, don't use the code below for your own adapter. Please refer to the code in our actual adapters for real usage samples.

    ```kotlin
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
    ```

10. Use the currently supported Chartboost Mediation SDK's gradle and Kotlin version. Any higher gradle & Kotlin versions are currently not supported.

    ```text
    Current Gradle Version: 7.6.0
    Current Kotlin Version: 1.7.20
    ```

## Contributions

We are committed to a fully transparent development process and highly appreciate any contributions. Our team regularly monitors and investigates all submissions for the inclusion in our official adapter releases.

Please refer to our [CONTRIBUTING](https://github.com/ChartBoost/chartboost-mediation-android-adapter-reference/blob/main/CONTRIBUTING.md) file for more information on how to contribute.

## License

Please refer to our [LICENSE](https://github.com/ChartBoost/chartboost-mediation-android-adapter-reference/blob/main/LICENSE.md) file for more information.
