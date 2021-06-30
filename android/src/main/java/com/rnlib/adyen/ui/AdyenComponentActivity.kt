package com.rnlib.adyen.ui

import android.app.Activity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.appcompat.app.AppCompatActivity
import android.widget.Toast
import android.text.TextUtils
import com.adyen.checkout.base.ActionComponentData
import com.adyen.checkout.base.ComponentError
import com.adyen.checkout.base.model.PaymentMethodsApiResponse
import com.adyen.checkout.base.model.paymentmethods.PaymentMethod
import com.adyen.checkout.base.model.payments.request.PaymentComponentData
import com.adyen.checkout.base.model.payments.request.PaymentMethodDetails
import com.adyen.checkout.base.model.payments.request.GenericPaymentMethod
import com.adyen.checkout.base.model.payments.response.Action
import com.adyen.checkout.base.util.PaymentMethodTypes
import com.adyen.checkout.core.code.Lint
import com.adyen.checkout.core.exception.CheckoutException
import com.adyen.checkout.core.log.LogUtil
import com.adyen.checkout.core.log.Logger

import com.rnlib.adyen.ActionHandler
import com.rnlib.adyen.AdyenComponent
import com.rnlib.adyen.AdyenComponentConfiguration
import com.rnlib.adyen.R
import com.rnlib.adyen.service.CallResult
import com.rnlib.adyen.service.ComponentService
import com.rnlib.adyen.ui.base.DropInBottomSheetDialogFragment
import com.rnlib.adyen.ui.component.GenericComponentDialogFragment
import com.rnlib.adyen.ui.paymentmethods.PaymentMethodListDialogFragment

import com.adyen.checkout.googlepay.GooglePayComponent
import com.adyen.checkout.googlepay.GooglePayComponentState
import com.adyen.checkout.googlepay.GooglePayConfiguration
import com.adyen.checkout.redirect.RedirectUtil
import com.adyen.checkout.wechatpay.WeChatPayUtils
import com.rnlib.adyen.ui.component.CardComponentDialogFragment
import org.json.JSONObject
import java.util.Locale

/**
 * Activity that presents the available PaymentMethods to the Shopper.
 */
@Suppress("TooManyFunctions", "SyntheticAccessor")
class AdyenComponentActivity : AppCompatActivity(), DropInBottomSheetDialogFragment.Protocol, ActionHandler.DetailsRequestedInterface {

    companion object {
        private val TAG = LogUtil.getTag()

        private const val PAYMENT_METHOD_FRAGMENT_TAG = "PAYMENT_METHODS_DIALOG_FRAGMENT"
        private const val COMPONENT_FRAGMENT_TAG = "COMPONENT_DIALOG_FRAGMENT"
        private const val LOADING_FRAGMENT_TAG = "LOADING_DIALOG_FRAGMENT"

        private const val PAYMENT_METHODS_RESPONSE_KEY = "PAYMENT_METHODS_RESPONSE_KEY"
        private const val ADYEN_COMPONENT_CONFIGURATION_KEY = "ADYEN_COMPONENT_CONFIGURATION_KEY"
        private const val IS_WAITING_FOR_RESULT = "IS_WAITING_FOR_RESULT"
        private const val ADYEN_COMPONENT_INTENT = "ADYEN_COMPONENT_INTENT"

        private const val GOOGLE_PAY_REQUEST_CODE = 1

        fun createIntent(context: Context, adyenComponentConfiguration: AdyenComponentConfiguration, paymentMethodsApiResponse: PaymentMethodsApiResponse): Intent {
            val intent = Intent(context, AdyenComponentActivity::class.java)
            intent.putExtra(PAYMENT_METHODS_RESPONSE_KEY, paymentMethodsApiResponse)
            intent.putExtra(ADYEN_COMPONENT_CONFIGURATION_KEY, adyenComponentConfiguration)
            intent.putExtra(ADYEN_COMPONENT_INTENT, adyenComponentConfiguration.resultHandlerIntent)
            return intent
        }
    }

    private lateinit var adyenComponentConfiguration: AdyenComponentConfiguration
    private lateinit var adyenComponentViewModel: AdyenComponentViewModel
    private lateinit var resultIntent: Intent
    private lateinit var googlePayComponent: GooglePayComponent

    private lateinit var callResultIntentFilter: IntentFilter

    private lateinit var localBroadcastManager: LocalBroadcastManager

    @Suppress(Lint.PROTECTED_IN_FINAL)
    protected lateinit var actionHandler: ActionHandler

    // If a new intent is received we can continue processing, otherwise we might need to time out
    @Suppress(Lint.PROTECTED_IN_FINAL)
    private var isWaitingResult = false

    private val loadingDialog = LoadingDialogFragment.newInstance()
    //private var isLoadingVisible = false

    private val callResultReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            Logger.d(TAG, "callResultReceiver onReceive")
            if (context != null && intent != null) {
                isWaitingResult = false
                if (intent.hasExtra(ComponentService.API_CALL_RESULT_KEY)) {
                    // TODO: Define what to do if [callResult] is null
                    val callResult = intent.getParcelableExtra<CallResult>(ComponentService.API_CALL_RESULT_KEY)!!
                    handleCallResult(callResult)
                } else {
                    throw CheckoutException("No extra on callResultReceiver")
                }
            }
        }
    }

    private val googlePayObserver: Observer<GooglePayComponentState> = Observer {
        if (it!!.isValid) {
            requestPaymentsCall(it.data)
        }
    }

    private val googlePayErrorObserver: Observer<ComponentError> = Observer {
        Logger.d(TAG, "GooglePay error - ${it?.errorMessage}")
        val errObj : JSONObject = JSONObject()
        errObj.put("resultType","ERROR")
        val code = if (it != null) {"ERROR_GENERAL"} else {"ERROR_CANCELLED"}
        errObj.put("code",code)
        errObj.put("message",it?.errorMessage.toString())
        this.sendResult(errObj.toString())
    }

    override fun attachBaseContext(newBase: Context?) {
        Logger.d(TAG, "attachBaseContext")
        super.attachBaseContext(createLocalizedContext(newBase))
    }

    // False positive from countryStartPosition
    @Suppress("MagicNumber")
    private fun createLocalizedContext(baseContext: Context?): Context? {
        return if (baseContext == null) {
            baseContext
        } else {
            val localeString = baseContext.getSharedPreferences(AdyenComponent.DROP_IN_PREFS, Context.MODE_PRIVATE).getString(AdyenComponent.LOCALE_PREF, "")
            Logger.d(TAG, "localeString - $localeString")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1 && !TextUtils.isEmpty(localeString)) {
                val config = Configuration(baseContext.resources.configuration)
                val languageEndPosition = 2
                val countryStartPosition = 3
                config.setLocale(Locale(localeString!!.substring(0, languageEndPosition), localeString.substring(countryStartPosition)))
                baseContext.createConfigurationContext(config)
            } else {
                Logger.e(TAG, "Failed to create localized context.")
                baseContext
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Logger.d(TAG, "onCreate - $savedInstanceState")
        setContentView(R.layout.activity_drop_in)
        overridePendingTransition(0, 0)

        savedInstanceState?.let {
            isWaitingResult = it.getBoolean(IS_WAITING_FOR_RESULT, false)
        }

        adyenComponentViewModel = ViewModelProviders.of(this).get(AdyenComponentViewModel::class.java)

        // TODO: Define what to do if [adyenComponentConfiguration] is null
        adyenComponentConfiguration = intent.getParcelableExtra(ADYEN_COMPONENT_CONFIGURATION_KEY)!!

        adyenComponentViewModel.adyenComponentConfiguration = adyenComponentConfiguration

        // TODO: Define what to do if [adyenComponentViewModel] is null
        adyenComponentViewModel.paymentMethodsApiResponse = intent.getParcelableExtra(PAYMENT_METHODS_RESPONSE_KEY)!!
        val paymentMethod : PaymentMethod = adyenComponentViewModel.paymentMethodsApiResponse.paymentMethods!![0]
        when (paymentMethod.type) {
            PaymentMethodTypes.SCHEME -> {
                if(adyenComponentViewModel.paymentMethodsApiResponse.getStoredPaymentMethods() != null){
                   PaymentMethodListDialogFragment.newInstance(false).show(supportFragmentManager, PAYMENT_METHOD_FRAGMENT_TAG) 
                }else{
                    this.showComponentDialog(paymentMethod, true)
                }
            }
                PaymentMethodTypes.GOOGLE_PAY -> {
                var googlepayConfiguration = adyenComponentConfiguration.getConfigurationFor<GooglePayConfiguration>(PaymentMethodTypes.GOOGLE_PAY, this)
                this.startGooglePay(paymentMethod, googlepayConfiguration)
            }
            PaymentMethodTypes.WECHAT_PAY_SDK -> {
                this.startWeChatPay()
            }
            else -> {
                this.showComponentDialog(paymentMethod, true)
            }
        }

        // TODO: Define what to do if [resultIntent] is null
        resultIntent = if (savedInstanceState != null && savedInstanceState.containsKey(ADYEN_COMPONENT_INTENT)) {
            savedInstanceState.getParcelable(ADYEN_COMPONENT_INTENT)!!
        } else {
            intent.getParcelableExtra(ADYEN_COMPONENT_INTENT)!!
        }

        callResultIntentFilter = IntentFilter(ComponentService.getCallResultAction(this))

        // registerBroadcastReceivers
        localBroadcastManager = LocalBroadcastManager.getInstance(this)
        localBroadcastManager.registerReceiver(callResultReceiver, callResultIntentFilter)

        actionHandler = ActionHandler(this, this)
        actionHandler.restoreState(savedInstanceState)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            GOOGLE_PAY_REQUEST_CODE -> googlePayComponent.handleActivityResult(resultCode, data)
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        Logger.d(TAG, "onNewIntent")
        if (intent != null) {
            handleIntent(intent)
        } else {
            Logger.e(TAG, "Null intent")
        }
    }

    override fun requestPaymentsCall(paymentComponentData: PaymentComponentData<*>) {
        isWaitingResult = true
        setLoading(true)
        ComponentService.requestPaymentsCall(this, paymentComponentData, adyenComponentConfiguration.serviceComponentName)
    }

    override fun requestDetailsCall(actionComponentData: ActionComponentData) {
        isWaitingResult = true
        setLoading(true)
        ComponentService.requestDetailsCall(this,
            ActionComponentData.SERIALIZER.serialize(actionComponentData),
            adyenComponentConfiguration.serviceComponentName)
    }

    override fun onError(errorMessage: String) {
        Toast.makeText(this, R.string.action_failed, Toast.LENGTH_LONG).show()
        finish()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        Logger.d(TAG, "onSaveInstanceState")

        outState?.let {
            it.putParcelable(PAYMENT_METHODS_RESPONSE_KEY, adyenComponentViewModel.paymentMethodsApiResponse)
            it.putParcelable(ADYEN_COMPONENT_CONFIGURATION_KEY, adyenComponentConfiguration)
            it.putBoolean(IS_WAITING_FOR_RESULT, isWaitingResult)

            actionHandler.saveState(it)
        }
    }

    override fun onResume() {
        super.onResume()
        setLoading(isWaitingResult)
    }

    override fun onDestroy() {
        super.onDestroy()
        Logger.d(TAG, "onDestroy")
        //savedInstanceState
        localBroadcastManager.unregisterReceiver(callResultReceiver)
    }

    override fun showComponentDialog(paymentMethod: PaymentMethod, wasInExpandMode: Boolean) {
        Logger.d(TAG, "showComponentDialog")
        hideFragmentDialog(PAYMENT_METHOD_FRAGMENT_TAG)
        val dialogFragment = when (paymentMethod.type) {
            PaymentMethodTypes.SCHEME -> CardComponentDialogFragment
            else -> GenericComponentDialogFragment
        }.newInstance(paymentMethod, adyenComponentConfiguration, wasInExpandMode)

        dialogFragment.show(supportFragmentManager, COMPONENT_FRAGMENT_TAG)
    }

    override fun showPaymentMethodsDialog(showInExpandStatus: Boolean) {
        Logger.d(TAG, "showPaymentMethodsDialog")
        hideFragmentDialog(COMPONENT_FRAGMENT_TAG)
        PaymentMethodListDialogFragment.newInstance(showInExpandStatus).show(supportFragmentManager, PAYMENT_METHOD_FRAGMENT_TAG)
    }

    override fun terminateDropIn() {
        Logger.d(TAG, "terminateDropIn")
        adyenComponentConfiguration.resultHandlerIntent.putExtra(AdyenComponent.RESULT_CANCEL_KEY, "Cancelled").let { intent ->
            startActivity(intent)
        }
        setResult(Activity.RESULT_CANCELED)
        finish()
        overridePendingTransition(0, R.anim.fade_out)
    }

    override fun startGooglePay(paymentMethod: PaymentMethod, googlePayConfiguration: GooglePayConfiguration) {
        Logger.d(TAG, "startGooglePay")
        googlePayComponent = GooglePayComponent.PROVIDER.get(this, paymentMethod, googlePayConfiguration)
        googlePayComponent.observe(this@AdyenComponentActivity, googlePayObserver)
        googlePayComponent.observeErrors(this@AdyenComponentActivity, googlePayErrorObserver)

        hideFragmentDialog(PAYMENT_METHOD_FRAGMENT_TAG)
        googlePayComponent.startGooglePayScreen(this, GOOGLE_PAY_REQUEST_CODE)
    }

    fun startWeChatPay() {
        val paymentComponentData = PaymentComponentData<PaymentMethodDetails>()
        paymentComponentData.paymentMethod = GenericPaymentMethod(PaymentMethodTypes.WECHAT_PAY_SDK)
        this.requestPaymentsCall(paymentComponentData)
    }

    @Suppress(Lint.PROTECTED_IN_FINAL)
    protected fun handleCallResult(callResult: CallResult) {
        Logger.d(TAG, "handleCallResult - ${callResult.type.name}")
        when (callResult.type) {
            CallResult.ResultType.FINISHED -> {
                this.sendResult(callResult.content)
            }
            CallResult.ResultType.ACTION -> {
                val action = Action.SERIALIZER.deserialize(JSONObject(callResult.content))
                actionHandler.handleAction(this, action, this::sendResult)
            }
            CallResult.ResultType.ERROR -> {
                Logger.d(TAG, "ERROR - ${callResult.content}")
                Toast.makeText(this, R.string.payment_failed, Toast.LENGTH_LONG).show()
                finish()
            }
            CallResult.ResultType.WAIT -> {
                throw CheckoutException("WAIT CallResult is not expected to be propagated.")
            }
        }
    }

    private fun closeComponent(){
        setResult(Activity.RESULT_CANCELED)
        finish()
        overridePendingTransition(0, R.anim.fade_out)
    }

    private fun sendResult(content: String) {
        adyenComponentConfiguration.resultHandlerIntent.putExtra(AdyenComponent.RESULT_KEY, content).let { intent ->
            startActivity(intent)
            closeComponent()
        }
    }

    private fun handleIntent(intent: Intent) {
        Logger.d(TAG, "handleIntent: action - ${intent.action}")
        isWaitingResult = false

        if (WeChatPayUtils.isResultIntent(intent)) {
            Logger.d(TAG, "isResultIntent")
            actionHandler.handleWeChatPayResponse(intent)
        }

        when (intent.action) {
            // Redirect response
            Intent.ACTION_VIEW -> {
                val data = intent.data
                if (data != null && data.toString().startsWith(RedirectUtil.REDIRECT_RESULT_SCHEME)) {
                    actionHandler.handleRedirectResponse(data)
                } else {
                    Logger.e(TAG, "Unexpected response from ACTION_VIEW - ${intent.data}")
                }
            }
            else -> {
                Logger.e(TAG, "Unable to find action")
            }
        }
    }

    private fun hideFragmentDialog(tag: String) {
        getFragmentByTag(tag)?.dismiss()
    }

    private fun getFragmentByTag(tag: String): DialogFragment? {
        val fragment = supportFragmentManager.findFragmentByTag(tag)
        return fragment as DialogFragment?
    }

    private fun setLoading(showLoading: Boolean) {
        if (showLoading) {
            if (!loadingDialog.isAdded) {
                loadingDialog.show(supportFragmentManager, LOADING_FRAGMENT_TAG)
            }
        } else {
            getFragmentByTag(LOADING_FRAGMENT_TAG)?.dismiss()
        }
    }
}
