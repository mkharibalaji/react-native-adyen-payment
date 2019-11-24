package com.rnlib.adyen.ui.base

import androidx.lifecycle.Observer
import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.adyen.checkout.base.ComponentError
import com.adyen.checkout.base.PaymentComponent
import com.adyen.checkout.base.PaymentComponentState
import com.adyen.checkout.base.model.paymentmethods.PaymentMethod
import com.adyen.checkout.base.model.payments.request.PaymentMethodDetails
import com.adyen.checkout.core.exception.CheckoutException
import com.adyen.checkout.core.log.LogUtil
import com.adyen.checkout.core.log.Logger

import com.rnlib.adyen.AdyenComponentConfiguration
import com.rnlib.adyen.R
import com.rnlib.adyen.getComponentFor

open abstract class BaseComponentDialogFragment : DropInBottomSheetDialogFragment(), Observer<PaymentComponentState<in PaymentMethodDetails>> {

    companion object {
        private val TAG = LogUtil.getTag()
    }

    lateinit var paymentMethod: PaymentMethod
    lateinit var component: PaymentComponent<PaymentComponentState<in PaymentMethodDetails>>
    lateinit var adyenComponentConfiguration: AdyenComponentConfiguration

    open class BaseCompanion<T : BaseComponentDialogFragment>(var classes: Class<T>) {

        companion object {
            const val PAYMENT_METHOD = "PAYMENT_METHOD"
            const val WAS_IN_EXPAND_STATUS = "WAS_IN_EXPAND_STATUS"
            const val DROP_IN_CONFIGURATION = "DROP_IN_CONFIGURATION"
        }

        fun newInstance(
            paymentMethod: PaymentMethod,
            adyenComponentConfiguration: AdyenComponentConfiguration,
            wasInExpandStatus: Boolean
        ): T {
            var args = Bundle()
            args.putParcelable(PAYMENT_METHOD, paymentMethod)
            args.putBoolean(WAS_IN_EXPAND_STATUS, wasInExpandStatus)
            args.putParcelable(DROP_IN_CONFIGURATION, adyenComponentConfiguration)

            var dialogFragment = classes.newInstance()
            dialogFragment.arguments = args
            return dialogFragment
        }
    }

    abstract override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View?

    abstract override fun onViewCreated(view: View, savedInstanceState: Bundle?)

    abstract override fun onChanged(paymentComponentState: PaymentComponentState<in PaymentMethodDetails>?)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        paymentMethod = arguments?.getParcelable(BaseCompanion.PAYMENT_METHOD) ?: throw IllegalArgumentException("Payment method is null")
        adyenComponentConfiguration = arguments?.getParcelable(BaseCompanion.DROP_IN_CONFIGURATION)
            ?: throw IllegalArgumentException("DropIn Configuration is null")

        try {
            component = getComponentFor(this, paymentMethod, adyenComponentConfiguration)
        } catch (e: CheckoutException) {
            handleError(ComponentError(e))
            return
        }
    }

    override fun onBackPressed(): Boolean {
        Logger.d(TAG, "onBackPressed")
        protocol.showPaymentMethodsDialog(arguments?.getBoolean(BaseCompanion.WAS_IN_EXPAND_STATUS, false)!!)
        return true
    }

    override fun onCancel(dialog: DialogInterface) {
        super.onCancel(dialog)
        Logger.d(TAG, "onCancel")
        protocol.terminateDropIn()
    }

    fun startPayment() {
        val componentState = component.getState()
        try {
            if (componentState != null) {
                if (componentState.isValid) {
                    protocol.requestPaymentsCall(componentState.data)
                } else {
                    throw CheckoutException("PaymentComponentState are not valid.")
                }
            } else {
                throw CheckoutException("PaymentComponentState are null.")
            }
        } catch (e: CheckoutException) {
            handleError(ComponentError(e))
        }
    }

    fun createErrorHandlerObserver(): Observer<ComponentError> {
        return Observer {
            if (it != null) {
                handleError(it)
            }
        }
    }

    fun handleError(componentError: ComponentError) {
        Logger.e(TAG, componentError.errorMessage)
        Toast.makeText(context, R.string.component_error, Toast.LENGTH_LONG).show()

        protocol.terminateDropIn()
    }
}
