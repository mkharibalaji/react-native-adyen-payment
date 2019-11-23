import {NativeEventEmitter, NativeModules} from 'react-native';

const { AdyenPayment } = NativeModules;
const events = new NativeEventEmitter(AdyenPayment);
let onAdyenPaymentSuccessListener;
let onAdyenPaymentErrorListener;
export default {
    /**
     * @callback mOnRequestPaymentSession
     * @param {String} token
     * @param {String} returnUrl
     */
    showDropInComponent(paymentDetails,paymentMethods,appServiceConfigData) {
        return AdyenPayment.showDropInComponent(paymentDetails,paymentMethods,appServiceConfigData);
    },
    showCardComponent(paymentDetails,paymentMethodResponse,appServiceConfigData){
        let paymentMethodsResp = paymentMethodResponse;
        paymentMethodsResp["paymentMethods"] = paymentMethodsResp.paymentMethods.filter(pm => {return (pm.type == "card" || pm.type == "scheme")});
        paymentMethodsResp["storedPaymentMethods"] = paymentMethodsResp.storedPaymentMethods.filter(pm => {return (pm.type == "card" || pm.type == "scheme")});
        return AdyenPayment.showCardComponent(paymentDetails,paymentMethodsResp,appServiceConfigData);
    },
    showIdealComponent(paymentDetails,paymentMethodResponse,appServiceConfigData){
        let paymentMethodsResp = paymentMethodResponse;
        paymentMethodsResp["paymentMethods"] = paymentMethodsResp.paymentMethods.filter(pm => {return (pm.type == "ideal")});
        return AdyenPayment.showIdealComponent(paymentDetails,paymentMethodsResp,appServiceConfigData);
    },
    showMOLPayComponent(paymentDetails,paymentMethodResponse,appServiceConfigData){
        let paymentMethodsResp = paymentMethodResponse;
        paymentMethodsResp["paymentMethods"] = paymentMethodsResp.paymentMethods.filter(pm => {return (pm.type == "molpay_ebanking_fpx_MY" || pm.type == "molpay_ebanking_TH" || pm.type == "molpay_ebanking_VN")});
        return AdyenPayment.showMOLPayComponent(paymentDetails,paymentMethodsResp,appServiceConfigData);
    }, 
    showDotpayComponent(paymentDetails,paymentMethodResponse,appServiceConfigData){
        let paymentMethodsResp = paymentMethodResponse;
        paymentMethodsResp["paymentMethods"] = paymentMethodsResp.paymentMethods.filter(pm => {return (pm.type == "dotpay")});
        return AdyenPayment.showDotpayComponent(paymentDetails,paymentMethodsResp,appServiceConfigData);
    },
    showEPSComponent(paymentDetails,paymentMethodResponse,appServiceConfigData){
        let paymentMethodsResp = paymentMethodResponse;
        paymentMethodsResp["paymentMethods"] = paymentMethodsResp.paymentMethods.filter(pm => {return (pm.type == "eps")});
        return AdyenPayment.showEPSComponent(paymentDetails,paymentMethodsResp,appServiceConfigData);
    }, 
    showEntercashComponent(paymentDetails,paymentMethodResponse,appServiceConfigData){
        let paymentMethodsResp = paymentMethodResponse;
        paymentMethodsResp["paymentMethods"] = paymentMethodsResp.paymentMethods.filter(pm => {return (pm.type == "entercash")});
        return AdyenPayment.showEntercashComponent(paymentDetails,paymentMethodsResp,appServiceConfigData);
    }, 
    showOpenBankingComponent(paymentDetails,paymentMethodResponse,appServiceConfigData){
        let paymentMethodsResp = paymentMethodResponse;
        paymentMethodsResp["paymentMethods"] = paymentMethodsResp.paymentMethods.filter(pm => {return (pm.type == "openbanking_UK")});
        return AdyenPayment.showOpenBankingComponent(paymentDetails,paymentMethodsResp,appServiceConfigData);
    },
    showSEPADirectDebitComponent(paymentDetails,paymentMethodResponse,appServiceConfigData){
        let paymentMethodsResp = paymentMethodResponse;
        paymentMethodsResp["paymentMethods"] = paymentMethodsResp.paymentMethods.filter(pm => {return (pm.type == "sepadirectdebit")});
        return AdyenPayment.showSEPADirectDebitComponent(paymentDetails,paymentMethodsResp,appServiceConfigData);
    },  
    showApplePayComponent(paymentDetails,paymentMethodResponse,appServiceConfigData){
        let paymentMethodsResp = paymentMethodResponse;
        paymentMethodsResp["paymentMethods"] = paymentMethodsResp.paymentMethods.filter(pm => {return (pm.type == "applepay")})
        return AdyenPayment.showApplePayComponent(paymentDetails,paymentMethodsResp,appServiceConfigData);
    },                                   
    /**
     * @callback mOnSuccess
     * @param {Object} message
     */
    /**
     * After successfully payment, added payload data for confirmation payments
     * @param {mOnSuccess} mOnSuccess
     */
    onSuccess(mOnSuccess) {
        this._validateParam(mOnSuccess, 'onSuccess', 'function');
        onAdyenPaymentSuccessListener = events.addListener('onSuccess', (response) => {
            mOnSuccess(response['message']);
        });
    },
    /**
     * @callback mOnError
     * @param {String} error_code
     * @param {String} message
     */
    /**
     * If payment was cancelled or something else. Calling instead of onPaymentResult event.
     * @param {mOnError} mOnError
     */
    onError(mOnError) {
        this._validateParam(mOnError, 'onError', 'function');
        onAdyenPaymentErrorListener = events.addListener('onError', (response) => {
            mOnError(response['code'], response['message']);
        });
    },
    /**
     * @param {*} param
     * @param {String} methodName
     * @param {String} requiredType
     * @private
     */
    _validateParam(param, methodName, requiredType) {
        if (typeof param !== requiredType) {
            throw new Error(`Error: Adyen.${methodName}() requires a ${requiredType === 'function' ? 'callback function' : requiredType} but got a ${typeof param}`);
        }
    },
    events,
    removeListeners(){
        if(null != onAdyenPaymentSuccessListener)
            onAdyenPaymentSuccessListener.remove();
        if(null != onAdyenPaymentErrorListener)
            onAdyenPaymentErrorListener.remove();
    }
};
