package com.rnlib.adyen.ui.component

import android.os.Bundle
import androidx.lifecycle.ViewModelProviders
import com.google.android.material.bottomsheet.BottomSheetBehavior
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.SwitchCompat
import com.adyen.checkout.base.PaymentComponentState
import com.adyen.checkout.base.model.payments.request.PaymentMethodDetails
import com.adyen.checkout.base.util.CurrencyUtils
import com.adyen.checkout.base.util.PaymentMethodTypes
import com.adyen.checkout.card.CardComponent
import com.adyen.checkout.core.exception.CheckoutException
import com.adyen.checkout.core.log.LogUtil
import com.adyen.checkout.core.log.Logger

import com.rnlib.adyen.R
import com.rnlib.adyen.ui.AdyenComponentViewModel
import com.rnlib.adyen.ui.base.BaseComponentDialogFragment
import kotlinx.android.synthetic.main.frag_card_component.adyenCardView
import kotlinx.android.synthetic.main.view_card_component.view.*

class CardComponentDialogFragment : BaseComponentDialogFragment() {

    companion object : BaseCompanion<CardComponentDialogFragment>(CardComponentDialogFragment::class.java) {
        private val TAG = LogUtil.getTag()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.frag_card_component, container, false)
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

        val cardComponent = component as CardComponent

        if (!adyenComponentConfiguration.amount.isEmpty) {
            val value = CurrencyUtils.formatAmount(adyenComponentConfiguration.amount, adyenComponentConfiguration.shopperLocale)
            adyenCardView.payButton.text = resources.getString(R.string.add_card)
        }

        component.observe(this, this)
        cardComponent.observeErrors(this, createErrorHandlerObserver())

        // try to get the name from the payment methods response
        activity?.let { activity ->
            val dropInViewModel = ViewModelProviders.of(activity).get(AdyenComponentViewModel::class.java)
            adyenCardView.header.text = dropInViewModel.paymentMethodsApiResponse.paymentMethods?.find { it.type == PaymentMethodTypes.SCHEME }?.name
        }

        adyenCardView.attach(component as CardComponent, this)

        //adyenCardView.header.setText(R.string.credit_card)

        if (adyenCardView.isConfirmationRequired) {
            adyenCardView.payButton.setOnClickListener {
                if (cardComponent.state?.isValid == true) {
                    startPayment()
                } else {
                    adyenCardView.highlightValidationErrors()
                }
            }

            setInitViewState(BottomSheetBehavior.STATE_EXPANDED)
            adyenCardView.requestFocus()
        } else {
            adyenCardView.payButton.visibility = View.GONE
        }

        bindAddCardButtonWithAuthorizeSwitch()
    }

    override fun onChanged(paymentComponentState: PaymentComponentState<in PaymentMethodDetails>?) {
        //adyenCardView.payButton.isEnabled = paymentComponentState != null && paymentComponentState.isValid()
    }

    private fun bindAddCardButtonWithAuthorizeSwitch() {
        val switchCompat = adyenCardView
                .cardView
                .findViewById<SwitchCompat>(R.id.switch_storePaymentMethod)

        switchCompat.isChecked = true

        adyenCardView.payButton.isEnabled = switchCompat.isChecked
        switchCompat?.setOnCheckedChangeListener { _, isChecked ->
            adyenCardView.payButton.isEnabled = isChecked
        }
    }

}
