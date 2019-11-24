package com.rnlib.adyen.ui.base

import android.app.Dialog
import android.content.Context
import android.os.Bundle

import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

import android.view.KeyEvent
import android.widget.FrameLayout
import com.adyen.checkout.base.model.paymentmethods.PaymentMethod
import com.adyen.checkout.base.model.payments.request.PaymentComponentData
import com.adyen.checkout.base.model.payments.request.PaymentMethodDetails

import com.rnlib.adyen.R
import com.adyen.checkout.googlepay.GooglePayConfiguration

abstract class DropInBottomSheetDialogFragment : BottomSheetDialogFragment() {

    lateinit var protocol: Protocol

    private var dialogInitViewState: Int = BottomSheetBehavior.STATE_COLLAPSED

    fun setInitViewState(firstViewState: Int) {
        this.dialogInitViewState = firstViewState
    }

    override fun getTheme(): Int = R.style.AdyenCheckout_BottomSheetDialogTheme

    override fun onAttach(context: Context) {
        super.onAttach(context)

        if (activity is Protocol) {
            protocol = activity as Protocol
        } else {
            throw IllegalArgumentException("Host activity need to inheritance the IDropIn")
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        var dialog = super.onCreateDialog(savedInstanceState)

        dialog.setOnKeyListener { _, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_BACK && event.action == KeyEvent.ACTION_UP) {
                onBackPressed()
            }
            false
        }

        dialog.setOnShowListener { dialog ->
            val bottomSheet = (dialog as BottomSheetDialog)
                    .findViewById<FrameLayout>(com.google.android.material.R.id.design_bottom_sheet)

            var behavior = BottomSheetBehavior.from(bottomSheet)

            if (this.dialogInitViewState == BottomSheetBehavior.STATE_EXPANDED)
                behavior.skipCollapsed = true

            behavior.state = this.dialogInitViewState
        }

        return dialog
    }

    open fun onBackPressed(): Boolean {
        return false
    }

    interface Protocol {
        fun showComponentDialog(paymentMethod: PaymentMethod, wasInExpandMode: Boolean)
        fun showPaymentMethodsDialog(showInExpandStatus: Boolean)
        fun requestPaymentsCall(paymentComponentData: PaymentComponentData<*>)
        //fun sendPaymentRequest(paymentComponentData: PaymentComponentData<in PaymentMethodDetails>)
        fun terminateDropIn()
        fun startGooglePay(paymentMethod: PaymentMethod, googlePayConfiguration: GooglePayConfiguration)
    }
}
