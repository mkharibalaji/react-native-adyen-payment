package com.rnlib.adyen.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.adyen.checkout.base.ComponentAvailableCallback
import com.adyen.checkout.base.component.Configuration
import com.adyen.checkout.base.model.PaymentMethodsApiResponse
import com.adyen.checkout.base.model.paymentmethods.PaymentMethod
import com.adyen.checkout.base.model.paymentmethods.StoredPaymentMethod
import com.adyen.checkout.base.util.PaymentMethodTypes
import com.adyen.checkout.core.log.LogUtil
import com.adyen.checkout.core.log.Logger

import com.rnlib.adyen.AdyenComponentConfiguration
import com.rnlib.adyen.checkComponentAvailability
import com.rnlib.adyen.ui.paymentmethods.PaymentMethodsModel

class AdyenComponentViewModel(application: Application) : AndroidViewModel(application), ComponentAvailableCallback<Configuration> {

    companion object {
        val TAG = LogUtil.getTag()
    }

    val paymentMethodsModelLiveData: MutableLiveData<PaymentMethodsModel> = MutableLiveData()

    var paymentMethodsApiResponse: PaymentMethodsApiResponse = PaymentMethodsApiResponse()
        set(value) {
            if (value != paymentMethodsApiResponse) {
                field = value
                if (value.paymentMethods != null) {
                    onPaymentMethodsResponseChanged(value.paymentMethods.orEmpty() + value.storedPaymentMethods.orEmpty())
                }
            }
        }

    lateinit var adyenComponentConfiguration: AdyenComponentConfiguration

    private val paymentMethodsModel = PaymentMethodsModel()

    override fun onAvailabilityResult(isAvailable: Boolean, paymentMethod: PaymentMethod, config: Configuration?) {
        Logger.d(TAG, "onAvailabilityResult - ${paymentMethod.type} $isAvailable")

        if (isAvailable) {
            addPaymentMethod(paymentMethod)
        }

        // TODO handle unavailable and only notify when all list is checked
    }

    private fun onPaymentMethodsResponseChanged(paymentMethods: List<PaymentMethod>) {
        Logger.d(TAG, "onPaymentMethodsResponseChanged")

        for (paymentMethod in paymentMethods) {
            val type = paymentMethod.type

            if (type == null) {
                Logger.e(TAG, "PaymentMethod type is null")
            } else if (isSupported(type)) {
                checkComponentAvailability(getApplication(), paymentMethod, adyenComponentConfiguration, this)
            } else {
                if (!requiresDetails(paymentMethod)) {
                    Logger.d(TAG, "No details required - $type")
                    addPaymentMethod(paymentMethod)
                } else {
                    Logger.e(TAG, "PaymentMethod not yet supported - $type")
                }
            }
        }
    }

    private fun isSupported(paymentMethodType: String): Boolean {

        if (PaymentMethodTypes.UNSUPPORTED_PAYMENT_METHODS.contains(paymentMethodType)) {
            Logger.e(TAG, "Unsupported PaymentMethod - $paymentMethodType")
            return false
        }

        return PaymentMethodTypes.SUPPORTED_PAYMENT_METHODS.contains(paymentMethodType)
    }

    private fun requiresDetails(paymentMethod: PaymentMethod): Boolean {
        // If details is empty or all optional, we can call payments directly.
        paymentMethod.details?.let {
            for (inputDetail in it) {
                if (!inputDetail.isOptional) {
                    return true
                }
            }
        }
        return false
    }
    

    private fun addPaymentMethod(paymentMethod: PaymentMethod) {
        if (paymentMethod is StoredPaymentMethod) {
            if (paymentMethod.isEcommerce) {
                paymentMethodsModel.storedPaymentMethods.add(paymentMethod)
            } else {
                Logger.d(TAG, "Stored method ${paymentMethod.type} is not Ecommerce")
            }
        } else {
            paymentMethodsModel.paymentMethods.add(paymentMethod)
        }
        paymentMethodsModelLiveData.value = paymentMethodsModel
    }
}
