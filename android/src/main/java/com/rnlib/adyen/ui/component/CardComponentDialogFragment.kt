package com.rnlib.adyen.ui.component

import android.os.Bundle
import com.google.android.material.bottomsheet.BottomSheetBehavior
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.adyen.checkout.base.PaymentComponentState
import com.adyen.checkout.base.model.payments.request.PaymentMethodDetails
import com.adyen.checkout.base.util.CurrencyUtils
import com.adyen.checkout.card.CardComponent
import com.adyen.checkout.core.exception.CheckoutException
import com.adyen.checkout.core.log.LogUtil
import com.adyen.checkout.core.log.Logger

import com.rnlib.adyen.R
import com.rnlib.adyen.ui.base.BaseComponentDialogFragment
import kotlinx.android.synthetic.main.fragment_card_component.adyenCardView
import kotlinx.android.synthetic.main.view_card_component.view.header
import kotlinx.android.synthetic.main.view_card_component.view.payButton

class CardComponentDialogFragment : BaseComponentDialogFragment() {

    companion object : BaseCompanion<CardComponentDialogFragment>(CardComponentDialogFragment::class.java) {
        private val TAG = LogUtil.getTag()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_card_component, container, false)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            component as CardComponent
        } catch (e: ClassCastException) {
            throw CheckoutException("Component is not CardComponent")
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        Logger.d(TAG, "onViewCreated")

        if (!adyenComponentConfiguration.amount.isEmpty) {
            val value = CurrencyUtils.formatAmount(adyenComponentConfiguration.amount, adyenComponentConfiguration.shopperLocale)
            adyenCardView.payButton.text = String.format(resources.getString(R.string.pay_button_with_value), value)
        }

        component.observe(this, this)
        component.observeErrors(this, createErrorHandlerObserver())

        adyenCardView.attach(component as CardComponent, this)

        adyenCardView.header.setText(R.string.credit_card)

        if (adyenCardView.isConfirmationRequired) {
            adyenCardView.payButton.setOnClickListener {
                startPayment()
            }

            setInitViewState(BottomSheetBehavior.STATE_EXPANDED)
            adyenCardView.requestFocus()
        } else {
            adyenCardView.payButton.visibility = View.GONE
        }
    }

    override fun onChanged(paymentComponentState: PaymentComponentState<in PaymentMethodDetails>?) {
        adyenCardView.payButton.isEnabled = paymentComponentState != null && paymentComponentState.isValid()
    }
}
