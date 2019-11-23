import {NativeEventEmitter, NativeModules} from 'react-native';

const { AdyenPayment } = NativeModules;
const events = new NativeEventEmitter(AdyenPayment);
let onAdyenPaymentSuccessListener;
let onAdyenPaymentErrorListener;
export default {
    
    startPayment(component,componentData,paymentDetails,appServiceConfigData){
        return AdyenPayment.startPayment(component,componentData,paymentDetails,appServiceConfigData)
    },
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
