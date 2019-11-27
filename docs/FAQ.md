# FAQ

**Can I call a Individual Adyen Component?**

Yes, of course you can.

```jsx
import AdyenPayment from 'react-native-adyen-payment';

Instead of DropIn use on of the below stated variables,

//DROPIN,IDEAL,MOLPAY_MALAYSIA,MOLPAY_THAILAND,MOLPAY_VIETNAM,DOTPAY,EPS,ENTERCASH,OPEN_BANKING,
//SCHEME,GOOGLE_PAY,SEPA,BCMC,WECHAT_PAY_SDK,APPLE_PAY,

AdyenPayment.startPayment(AdyenPayment.DROPIN,componentData,paymentDetails,appServiceConfigData)

```

**Do you have an Expo Example ?**

Not at this time.
