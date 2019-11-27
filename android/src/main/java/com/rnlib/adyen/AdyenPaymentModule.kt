package com.rnlib.adyen

import android.app.Activity
import android.content.Intent
import android.content.Context

import com.adyen.checkout.base.model.PaymentMethodsApiResponse
import com.adyen.checkout.bcmc.BcmcConfiguration
import com.adyen.checkout.card.CardConfiguration
import com.adyen.checkout.googlepay.GooglePayConfiguration
import com.adyen.checkout.core.exception.CheckoutException
import com.adyen.checkout.core.log.LogUtil
import com.adyen.checkout.core.log.Logger
import com.adyen.checkout.dropin.DropIn
import com.adyen.checkout.dropin.DropInConfiguration
import com.adyen.checkout.core.api.Environment
import com.adyen.checkout.base.model.payments.Amount
import android.util.Log
import android.widget.Toast

import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactContextBaseJavaModule
import com.facebook.react.bridge.ActivityEventListener
import com.facebook.react.bridge.BaseActivityEventListener
import com.facebook.react.bridge.ReactMethod
import com.facebook.react.bridge.Callback
import com.facebook.react.modules.core.DeviceEventManagerModule
import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.WritableArray
import com.facebook.react.bridge.WritableMap
import com.facebook.react.bridge.ReadableMap

import com.jakewharton.retrofit2.adapter.kotlin.coroutines.CoroutineCallAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.Call
import retrofit2.Retrofit
import com.rnlib.adyen.CheckoutApiService
import com.rnlib.adyen.ApiService
import com.rnlib.adyen.PaymentData
import com.rnlib.adyen.AppServiceConfigData
import com.rnlib.adyen.PaymentMethodsRequest
import com.rnlib.adyen.AdyenDropInService
import com.rnlib.adyen.ReactNativeUtils
import org.json.JSONObject
import java.util.Locale

import com.rnlib.adyen.AdyenComponent
import com.rnlib.adyen.AdyenComponentConfiguration
import com.adyen.checkout.base.model.paymentmethods.PaymentMethod
import com.adyen.checkout.base.util.PaymentMethodTypes
import com.adyen.checkout.entercash.EntercashConfiguration
import com.adyen.checkout.eps.EPSConfiguration
import com.adyen.checkout.ideal.IdealConfiguration
import com.adyen.checkout.molpay.MolpayConfiguration
import com.adyen.checkout.dotpay.DotpayConfiguration
import com.adyen.checkout.openbanking.OpenBankingConfiguration
import com.adyen.checkout.sepa.SepaConfiguration
import com.adyen.checkout.wechatpay.WeChatPayConfiguration

import com.rnlib.adyen.ui.LoadingDialogFragment
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager

class AdyenPaymentModule(private var reactContext : ReactApplicationContext) : ReactContextBaseJavaModule(reactContext),ActivityEventListener {

    
    private val loadingDialog  = LoadingDialogFragment.newInstance()

    private val mActivityEventListener = object:BaseActivityEventListener() {
        override fun onActivityResult(activity:Activity, requestCode:Int, resultCode:Int, data:Intent) {
            parseActivityResult(requestCode, resultCode, data)
        }
    }

    companion object {
        val REACT_CLASS = "AdyenPayment"
        private val TAG: String = "AdyenPaymentModule"
        private const val LOADING_FRAGMENT_TAG = "LOADING_DIALOG_FRAGMENT"
        private var paymentData : JSONObject = JSONObject()
        private val configData : AppServiceConfigData = AppServiceConfigData()
        private var paymentMethodsApiResponse : PaymentMethodsApiResponse = PaymentMethodsApiResponse()
        fun  getPaymentData(): JSONObject{
           return paymentData
        }
        fun  getAppServiceConfigData(): AppServiceConfigData{
           return configData
        }
    }
    

    init {
        Logger.setLogcatLevel(Log.DEBUG)
        getReactApplicationContext().addActivityEventListener(this)

    }

    fun emitDeviceEvent(eventName: String, eventData: WritableMap?) {
        getReactApplicationContext().getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter::class.java).emit(eventName, eventData)
    }

    override fun getName() = REACT_CLASS

    fun getAmt(amtJson : JSONObject?) : Amount {
        val amount = Amount()
        amount.currency = amtJson?.getString("currency") as String
        amount.value = amtJson?.getInt("value") as Int
        return amount;
    }

    @ReactMethod
    fun startPayment(component : String,componentData : ReadableMap,paymentDetails : ReadableMap,appServiceConfigData : ReadableMap) {
        paymentData = ReactNativeUtils.convertMapToJson(paymentDetails)
        val compData = ReactNativeUtils.convertMapToJson(componentData)
        val appServiceConfigJSON : JSONObject = ReactNativeUtils.convertMapToJson(appServiceConfigData)
        val headersMap: MutableMap<String, String> = linkedMapOf()
        headersMap.put("Accept", "application/json")
        headersMap.put("Accept-Charset", "utf-8")
        headersMap.put("Content-Type","application/json")
        val additional_http_headers : JSONObject =  appServiceConfigJSON.getJSONObject("additional_http_headers")
        for(key in additional_http_headers.keys()){
            headersMap.put(key as String,additional_http_headers.getString(key))
        }
        configData.environment = appServiceConfigJSON.getString("environment")
        configData.base_url = appServiceConfigJSON.getString("base_url")
        configData.app_url_headers = headersMap
        val additionalData: MutableMap<String, String> = linkedMapOf()
        val paymentMethodReq : PaymentMethodsRequest = PaymentMethodsRequest(paymentData.getString("merchantAccount"),
            paymentData.getString("shopperReference"),additionalData,ArrayList<String>(),getAmt(paymentData.getJSONObject("amount")),
                 ArrayList<String>(),paymentData.getString("countryCode"),paymentData.getString("shopperLocale"),"Android")

        val paymentMethods : Call<ResponseBody> = ApiService.checkoutApi(configData.base_url).paymentMethods(configData.app_url_headers,paymentMethodReq)
        /*
        public static final String  = "sepadirectdebit";
        public static final String WECHAT_PAY_SDK = "wechatpaySDK";*/
        setLoading(true)
        paymentMethods.enqueue(object : retrofit2.Callback<ResponseBody> {
            override fun onResponse(call : Call<ResponseBody>,response : Response<ResponseBody>) {
                setLoading(false)
                if (response.isSuccessful()) {
                    // tasks available
                    val pmApiResponse : PaymentMethodsApiResponse = PaymentMethodsApiResponse.SERIALIZER.deserialize(JSONObject(response.body()?.string()))
                    val paymentMethods : MutableList<PaymentMethod> = mutableListOf<PaymentMethod>()
                    if(component != "dropin"){
                        for (each in pmApiResponse.paymentMethods!!) {
                            Log.i(TAG,each.toString())
                            if (each.type == component) {
                                paymentMethods.add(each)
                                break
                            }
                        }
                        pmApiResponse.setPaymentMethods(paymentMethods)
                        paymentMethodsApiResponse = pmApiResponse
                        when(component){
                            PaymentMethodTypes.GOOGLE_PAY -> showGooglePayComponent(compData)
                            PaymentMethodTypes.SCHEME -> showCardComponent(compData)
                            PaymentMethodTypes.IDEAL -> showIdealComponent(compData)
                            PaymentMethodTypes.MOLPAY_MALAYSIA -> showMOLPayComponent(component,compData)
                            PaymentMethodTypes.MOLPAY_THAILAND -> showMOLPayComponent(component,compData)
                            PaymentMethodTypes.MOLPAY_VIETNAM -> showMOLPayComponent(component,compData)
                            PaymentMethodTypes.DOTPAY -> showDotPayComponent(compData)
                            PaymentMethodTypes.EPS -> showEPSComponent(compData)
                            PaymentMethodTypes.ENTERCASH -> showEnterCashComponent(compData)
                            PaymentMethodTypes.OPEN_BANKING -> showOpenBankingComponent(compData)
                            PaymentMethodTypes.SEPA -> showSEPAComponent(compData)
                            PaymentMethodTypes.BCMC -> showBCMCComponent(compData)
                            PaymentMethodTypes.WECHAT_PAY_SDK -> showWeChatPayComponent(component,compData)
                            else -> {
                                val evtObj : JSONObject = JSONObject()
                                evtObj.put("code","ERROR_UNKNOWN_PAYMENT_METHOD")
                                evtObj.put("message","Unknown Payment Method")
                                emitDeviceEvent("onError",ReactNativeUtils.convertJsonToMap(evtObj))
                            }
                        }
                    }else{
                        paymentMethodsApiResponse = pmApiResponse
                        showDropInComponent(compData)
                    }
                } else {
                   val byteArray = response.errorBody()?.bytes()
                    if (byteArray != null) {
                        Log.e(TAG, "errorBody - ${String(byteArray)}")
                    }
                }
            }
            override fun onFailure(call: Call<ResponseBody> ?, t: Throwable ?) {
                // something went completely south (like no internet connection)
                setLoading(false)
                Log.d("Error", t!!.message)
            }
        })
    }

    private fun createConfigurationBuilder(context : Context) : AdyenComponentConfiguration.Builder {
        val resultIntent : Intent = (context.getPackageManager().getLaunchIntentForPackage(context.getApplicationContext().getPackageName())) as Intent
        resultIntent.flags = (Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        val adyenConfigurationBuilder = AdyenComponentConfiguration.Builder(
            context,
            resultIntent,
            AdyenComponentService::class.java
        )
        adyenConfigurationBuilder.setEnvironment(Environment.TEST)
        val shoppersLocale = Locale(paymentData.getString("shopperLocale").toLowerCase().split("_")[0])
        adyenConfigurationBuilder.setShopperLocale(shoppersLocale)
        try {
            adyenConfigurationBuilder.setAmount(getAmt(paymentData.getJSONObject("amount")))
        } catch (e: CheckoutException) {
            Log.e(TAG, "Amount not valid", e)
        }
        return adyenConfigurationBuilder
    }

    private fun showWeChatPayComponent(component : String,componentData : JSONObject){
        val context = getReactApplicationContext()
        val wechatPayConfiguration = WeChatPayConfiguration.Builder(context).build()
        val configBuilder : AdyenComponentConfiguration.Builder = createConfigurationBuilder(context)
        when (component){
            PaymentMethodTypes.WECHAT_PAY_SDK -> configBuilder.addWeChatPaySDKConfiguration(wechatPayConfiguration)
        }
        AdyenComponent.startPayment(context, paymentMethodsApiResponse, configBuilder.build())
    }

    private fun showIdealComponent(componentData : JSONObject){
        val context = getReactApplicationContext()
        val idealConfiguration = IdealConfiguration.Builder(context).build()
        val configBuilder : AdyenComponentConfiguration.Builder = createConfigurationBuilder(context)
        configBuilder.addIdealConfiguration(idealConfiguration)
        AdyenComponent.startPayment(context, paymentMethodsApiResponse, configBuilder.build())
    }

    private fun showMOLPayComponent(component : String,componentData : JSONObject){
        val context = getReactApplicationContext()
        val molPayConfiguration = MolpayConfiguration.Builder(context).build()
        val configBuilder : AdyenComponentConfiguration.Builder = createConfigurationBuilder(context)
        when (component){
            PaymentMethodTypes.MOLPAY_MALAYSIA -> configBuilder.addMolpayMalasyaConfiguration(molPayConfiguration)
            PaymentMethodTypes.MOLPAY_THAILAND -> configBuilder.addMolpayThailandConfiguration(molPayConfiguration)
            PaymentMethodTypes.MOLPAY_VIETNAM -> configBuilder.addMolpayVietnamConfiguration(molPayConfiguration)
        }
        AdyenComponent.startPayment(context, paymentMethodsApiResponse, configBuilder.build())
    }

    private fun showDotPayComponent(componentData : JSONObject){
        val context = getReactApplicationContext()
        val dotPayConfiguration = DotpayConfiguration.Builder(context).build()
        val configBuilder : AdyenComponentConfiguration.Builder = createConfigurationBuilder(context)
        configBuilder.addDotpayConfiguration(dotPayConfiguration)
        AdyenComponent.startPayment(context, paymentMethodsApiResponse, configBuilder.build())
    }

    private fun showEPSComponent(componentData : JSONObject){
        val context = getReactApplicationContext()
        val epsConfiguration = EPSConfiguration.Builder(context).build()
        val configBuilder : AdyenComponentConfiguration.Builder = createConfigurationBuilder(context)
        configBuilder.addEpsConfiguration(epsConfiguration)
        AdyenComponent.startPayment(context, paymentMethodsApiResponse, configBuilder.build())
    }

    private fun showEnterCashComponent(componentData : JSONObject){
        val context = getReactApplicationContext()
        val enterCashConfiguration = EntercashConfiguration.Builder(context).build()
        val configBuilder : AdyenComponentConfiguration.Builder = createConfigurationBuilder(context)
        configBuilder.addEntercashConfiguration(enterCashConfiguration)
        AdyenComponent.startPayment(context, paymentMethodsApiResponse, configBuilder.build())
    }

    private fun showOpenBankingComponent(componentData : JSONObject){
        val context = getReactApplicationContext()
        val openBankingConfiguration = OpenBankingConfiguration.Builder(context).build()
        val configBuilder : AdyenComponentConfiguration.Builder = createConfigurationBuilder(context)
        configBuilder.addOpenBankingConfiguration(openBankingConfiguration)
        AdyenComponent.startPayment(context, paymentMethodsApiResponse, configBuilder.build())
    }

    private fun showSEPAComponent(componentData : JSONObject){
        val context = getReactApplicationContext()
        val sepaConfiguration = SepaConfiguration.Builder(context).build()
        val configBuilder : AdyenComponentConfiguration.Builder = createConfigurationBuilder(context)
        configBuilder.addSepaConfiguration(sepaConfiguration)
        AdyenComponent.startPayment(context, paymentMethodsApiResponse, configBuilder.build())
    }

    private fun showBCMCComponent(componentData : JSONObject){
        val context = getReactApplicationContext()
        val bcmcComponent : JSONObject = componentData.getJSONObject(PaymentMethodTypes.BCMC)
        val bcmcConfiguration = BcmcConfiguration.Builder(context, bcmcComponent.getString("card_public_key")).build()
        val configBuilder : AdyenComponentConfiguration.Builder = createConfigurationBuilder(context)
        configBuilder.addBcmcConfiguration(bcmcConfiguration)
        AdyenComponent.startPayment(context, paymentMethodsApiResponse, configBuilder.build())
    }

    private fun showCardComponent(componentData : JSONObject){
        val context = getReactApplicationContext()
        val cardComponent : JSONObject = componentData.getJSONObject(PaymentMethodTypes.SCHEME)
        val cardConfiguration = CardConfiguration.Builder(context, cardComponent.getString("card_public_key"))
                            .setShopperReference(paymentData.getString("shopperReference"))
                            .build()
        val configBuilder : AdyenComponentConfiguration.Builder = createConfigurationBuilder(context)
        configBuilder.addCardConfiguration(cardConfiguration)
        AdyenComponent.startPayment(context, paymentMethodsApiResponse, configBuilder.build())
    }

    private fun showGooglePayComponent(componentData : JSONObject){
        val context = getReactApplicationContext()
        val googlePayConfig = GooglePayConfiguration.Builder(context,paymentData.getString("merchantAccount")).build()
        val configBuilder : AdyenComponentConfiguration.Builder = createConfigurationBuilder(context)
        configBuilder.addGooglePayConfiguration(googlePayConfig)
        AdyenComponent.startPayment(context, paymentMethodsApiResponse, configBuilder.build())
    }
 
    private fun showDropInComponent(componentData : JSONObject) {

        Log.d(TAG, "startDropIn")
        val context = getReactApplicationContext()

        val googlePayConfig = GooglePayConfiguration.Builder(context,paymentData.getString("merchantAccount"))
                                .build()
        val cardComponent : JSONObject = componentData.getJSONObject(PaymentMethodTypes.SCHEME)
        val cardConfiguration = CardConfiguration.Builder(context, cardComponent.getString("card_public_key"))
                            .setShopperReference(paymentData.getString("shopperReference"))
                            .build()
        val bcmcComponent : JSONObject = componentData.getJSONObject(PaymentMethodTypes.BCMC)
        val bcmcConfiguration = BcmcConfiguration.Builder(context, bcmcComponent.getString("card_public_key")).build()

        
        val resultIntent : Intent = (context.getPackageManager().getLaunchIntentForPackage(context.getApplicationContext().getPackageName())) as Intent
        resultIntent.flags = (Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        
        val dropInConfigurationBuilder = DropInConfiguration.Builder(
            context,
            resultIntent,
            AdyenDropInService::class.java
        ).addCardConfiguration(cardConfiguration)
            .addBcmcConfiguration(bcmcConfiguration)
            .addGooglePayConfiguration(googlePayConfig)

        dropInConfigurationBuilder.setEnvironment(Environment.TEST)
        val shoppersLocale = Locale(paymentData.getString("shopperLocale").toLowerCase().split("_")[0])
        dropInConfigurationBuilder.setShopperLocale(shoppersLocale)

        val amount = Amount()
        val amtJson : JSONObject  = paymentData.getJSONObject("amount")
        amount.currency = amtJson.getString("currency")
        amount.value = amtJson.getInt("value")

        try {
            dropInConfigurationBuilder.setAmount(amount)
        } catch (e: CheckoutException) {
            Log.e(TAG, "Amount $amount not valid", e)
        }
        
        DropIn.startPayment(context, paymentMethodsApiResponse, dropInConfigurationBuilder.build())
    }

    override fun onNewIntent(intent: Intent?) {
        Log.d(TAG, "onNewIntent")
        if (intent?.hasExtra(DropIn.RESULT_KEY) == true) {
            Log.d(TAG,intent.getStringExtra(DropIn.RESULT_KEY))
            //Toast.makeText(getReactApplicationContext(), intent.getStringExtra(DropIn.RESULT_KEY), Toast.LENGTH_SHORT).show()
            val response : JSONObject = JSONObject(intent.getStringExtra(DropIn.RESULT_KEY))
            sendResponse(response)
        }
    }

    override fun onActivityResult(activity:Activity, requestCode:Int, resultCode:Int, data: Intent?){
       Log.d(TAG, "Calling activity result")
        parseActivityResult(requestCode, resultCode, data)
    }

    private fun sendResponse(response : JSONObject){
        val resultType : String = response.get("resultType").toString()
        if(resultType == "SUCCESS"){
            val detailsResponse : JSONObject = response.getJSONObject("message")
            val rsCode : String = detailsResponse.getString("resultCode")
            if(rsCode == "Authorised" || rsCode == "Received" || rsCode == "Pending"){
                val message : JSONObject = JSONObject()
                message.put("resultCode", detailsResponse.getString("resultCode"))
                message.put("merchantReference", detailsResponse.getString("merchantReference"))
                message.put("pspReference", detailsResponse.getString("pspReference"))
                message.put("additionalData", detailsResponse.getJSONObject("additionalData"))
                val evtObj : JSONObject = JSONObject()
                evtObj.put("message",message)
                emitDeviceEvent("onSuccess",ReactNativeUtils.convertJsonToMap(evtObj))
            }else if(rsCode == "Refused" || rsCode == "Error"){
                val evtObj : JSONObject = JSONObject()
                val err_refusal_code = detailsResponse.getString("refusalReasonCode")
                val err_code = when(err_refusal_code) {
                    "0" -> "ERROR_GENERAL"
                    "2" -> "ERROR_TRANSACTION_REFUSED"
                    "3" -> "ERROR_REFERRAL"
                    "4" -> "ERROR_ACQUIRER"
                    "5" -> "ERROR_BLOCKED_CARD"
                    "6" -> "ERROR_EXPIRED_CARD"
                    "7" -> "ERROR_INVALID_AMOUNT"
                    "8" -> "ERROR_INVALID_CARDNUMBER"
                    "9" -> "ERROR_ISSUER_UNAVAILABLE"
                    "10" -> "ERROR_BANK_NOT_SUPPORTED"
                    "11" -> "ERROR_3DSECURE_AUTH_FAILED"
                    "12" -> "ERROR_NO_ENOUGH_BALANCE"
                    "14" -> "ERROR_FRAUD_DETECTED"
                    "15" -> "ERROR_CANCELLED"
                    "16" -> "ERROR_CANCELLED"
                    "17" -> "ERROR_INVALID_PIN"
                    "18" -> "ERROR_PIN_RETRY_EXCEEDED"
                    "19" -> "ERROR_UNABLE_VALIDATE_PIN"
                    "20" -> "ERROR_FRAUD_DETECTED"
                    "21" -> "ERROR_SUBMMISSION_ADYEN"
                    "23" -> "ERROR_TRANSACTION_REFUSED"
                    "24" -> "ERROR_CVC_DECLINED"
                    "25" -> "ERROR_RESTRICTED_CARD"
                    "27" -> "ERROR_DO_NOT_HONOR"
                    "28" -> "ERROR_WDRW_AMOUNT_EXCEEDED"
                    "29" -> "ERROR_WDRW_COUNT_EXCEEDED"
                    "31" -> "ERROR_FRAUD_DETECTED"
                    "32" -> "ERROR_AVS_DECLINED"
                    "33" -> "ERROR_CARD_ONLINE_PIN"
                    "34" -> "ERROR_NO_ACCT_ATCHD_CARD"
                    "35" -> "ERROR_NO_ACCT_ATCHD_CARD"
                    "36" -> "ERROR_MOBILE_PIN"
                    "37" -> "ERROR_CONTACTLESS_FALLBACK"
                    "38" -> "ERROR_AUTH_REQUIRED"
                    else -> "ERROR_UNKNOWN"
                }
                evtObj.put("code",err_code)
                evtObj.put("message",detailsResponse.getString("refusalReason"))
                emitDeviceEvent("onError",ReactNativeUtils.convertJsonToMap(evtObj))
            }else if(rsCode == "Cancelled"){
                val evtObj : JSONObject = JSONObject()
                evtObj.put("code","ERROR_CANCELLED")
                evtObj.put("message","Transaction Cancelled")
                emitDeviceEvent("onError",ReactNativeUtils.convertJsonToMap(evtObj))
            }else{
                val evtObj : JSONObject = JSONObject()
                evtObj.put("code","ERROR_UNKNOWN")
                evtObj.put("message","Unknown Error")
                emitDeviceEvent("onError",ReactNativeUtils.convertJsonToMap(evtObj))
            }
        }else if (resultType=="ERROR"){
            val evtObj : JSONObject = JSONObject()
            evtObj.put("code",response.get("code").toString())
            evtObj.put("message",response.get("message").toString())
            emitDeviceEvent("onError",ReactNativeUtils.convertJsonToMap(evtObj))
        }else{
            val evtObj : JSONObject = JSONObject()
            evtObj.put("code","ERROR_UNKNOWN")
            evtObj.put("message","Unknown Error")
            emitDeviceEvent("onError",ReactNativeUtils.convertJsonToMap(evtObj))
        }
    }

    private fun parseActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        Log.d(TAG, "parseActivityResult")
        if (requestCode == DropIn.DROP_IN_REQUEST_CODE && resultCode == Activity.RESULT_CANCELED) {
            Log.d(TAG, "DropIn CANCELED")
            val evtObj : JSONObject = JSONObject()
            evtObj.put("code","ERROR_CANCELLED")
            evtObj.put("message","Dropin Cancelled")
            emitDeviceEvent("onError",ReactNativeUtils.convertJsonToMap(evtObj))
        }
    }

    private fun setLoading(showLoading: Boolean) {
        val curr_activity : FragmentActivity? = getCurrentActivity() as FragmentActivity
        val curr_fragment_mgr : FragmentManager = curr_activity?.getSupportFragmentManager() as FragmentManager
        if (showLoading) {
            if (!loadingDialog.isAdded) {
                loadingDialog.show(curr_fragment_mgr, LOADING_FRAGMENT_TAG)
            }
        } else {
            val df : DialogFragment = curr_fragment_mgr.findFragmentByTag(LOADING_FRAGMENT_TAG) as DialogFragment
            df.dismiss()
        }
    }

}
