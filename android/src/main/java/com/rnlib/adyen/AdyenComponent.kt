/*
 * Copyright (c) 2019 Adyen N.V.
 *
 * This file is open source and available under the MIT license. See the LICENSE file for more info.
 *
 * Created by caiof on 21/3/2019.
 */

package com.rnlib.adyen

import android.app.Activity
import android.content.Context
import android.content.Intent
import com.adyen.checkout.base.model.PaymentMethodsApiResponse
import com.adyen.checkout.base.model.paymentmethods.PaymentMethod
import com.adyen.checkout.base.util.PaymentMethodTypes
import com.adyen.checkout.card.CardConfiguration
import com.adyen.checkout.card.data.CardType
import com.adyen.checkout.core.log.LogUtil
import com.adyen.checkout.core.log.Logger

import com.rnlib.adyen.AdyenComponent.Companion.startPayment
import com.rnlib.adyen.ui.AdyenComponentActivity

/**
 * Drop-in is the easy solution to using components. The Merchant only needs to provide the response of the paymentMethods/ endpoint
 * and some configuration data. Then we will handle the UI flow to get all the needed payment information.
 *
 * Merchant needs to extend [DropInService] and put it in the manifest. That service is where the merchant will make the calls to the
 * server for the payments/ and payments/details/ endpoints/.
 *
 * After setting up the [DropInService], just call [startPayment] and the checkout process will start.
 */
@Suppress("SyntheticAccessor")
class AdyenComponent private constructor() {

    companion object {
        private val TAG = LogUtil.getTag()

        const val RESULT_KEY = "payment_result"

        const val RESULT_CANCEL_KEY = "payment_cancel"


        @JvmStatic
        @Deprecated("You can use `DropIn.startPayment instead`")
        val INSTANCE: AdyenComponent by lazy { AdyenComponent() }

        /**
         * Starts the checkout flow to be handled by the Drop-In solution. Make sure you have [DropInService] set up before calling this.
         * We suggest that you set up the resultHandlerIntent with the appropriate flags to clear the stack of the checkout activities.
         *
         * @param context A context to start the Checkout flow.
         * @param paymentMethodsApiResponse The result from the paymentMethods/ endpoint.
         * @param dropInConfiguration Additional required configuration data.
         *
         */
        @JvmStatic
        fun startPayment(
            context: Context,
            paymentMethodsApiResponse: PaymentMethodsApiResponse,
            adyenComponentConfiguration: AdyenComponentConfiguration
        ) {

            for (each in paymentMethodsApiResponse.paymentMethods!!) {
                if (each.type == PaymentMethodTypes.SCHEME) {
                    this.handleSupportedCards(adyenComponentConfiguration, each, context)
                    break
                }
            }

            val intent = AdyenComponentActivity.createIntent(context, adyenComponentConfiguration, paymentMethodsApiResponse)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        }

        init {
            Logger.d(TAG, "Init")
        }

        /**
         * Try to get supported cards from API response when [CardConfiguration] supported cards are default ones.
         */
        @Suppress("SpreadOperator")
        private fun handleSupportedCards(adyenComponentConfiguration: AdyenComponentConfiguration, schemePaymentMethod: PaymentMethod, context: Context) {

            var cardConfiguration = adyenComponentConfiguration.getConfigurationFor<CardConfiguration>(PaymentMethodTypes.SCHEME, context)

            if (cardConfiguration.supportedCardTypes == CardConfiguration.DEFAULT_SUPPORTED_CARDS_LIST) {
                var supportedCardTypesFromApi = schemePaymentMethod.brands?.mapNotNull { brand -> CardType.getCardTypeByTxVariant(brand) }
                if (!supportedCardTypesFromApi.isNullOrEmpty()) {
                    Logger.d(TAG, "Updating supported cards to - $supportedCardTypesFromApi")
                    val newCardConfiguration = cardConfiguration
                        .newBuilder()
                        .setSupportedCardTypes(*supportedCardTypesFromApi.orEmpty().toTypedArray())
                        .build()

                    adyenComponentConfiguration.availableConfigs[PaymentMethodTypes.SCHEME] = newCardConfiguration
                }
            }
        }
    }
}
