package com.rnlib.adyen.ui.paymentmethods

import com.adyen.checkout.base.model.paymentmethods.PaymentMethod

class PaymentMethodsModel {
    var storedPaymentMethods: MutableList<PaymentMethod> = mutableListOf()
    var paymentMethods: MutableList<PaymentMethod> = mutableListOf()
}
