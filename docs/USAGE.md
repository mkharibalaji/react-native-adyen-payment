# Usage

**1. Import Adyen Payment**

```jsx
import AdyenPayment from 'react-native-adyen-payment';
```

**2. Two Ways to use the AdyenPayment**

***a. Add the Listeners and StartPayment in your Component, and use the `onPress` method to start the Payment***

```jsx
import AdyenPayment from 'react-native-adyen-payment';

const DEMO_SERVER_API_KEY ="Your Demo Server Key";
const CARD_PUBLIC_KEY = "Your Public Card Key";
const MERCHANT_ACCOUNT = 'Your Merchant Account';

class App extends Component<Props> {
  constructor(props) {
    super(props);
    this.state = {
      isLoading : false,
      adyen_payment_status : 'initiated'
    };
        /*
    The base URL should be your API server running with the following POST endpoints
     - /paymentMethods
     - /payments
     - /payments/details
     Ex :
     Base URL : https://XXXXXX.com/payments/adyen
     https://XXXXXX.com/payments/adyen/paymentMethods
     https://XXXXXX.com/payments/adyen/payments
     https://XXXXXX.com/payments/adyen/payments/details
     Any Extra Header Parameters to be passed can be given in the "additional_http_headers"
     As an example we are using Adyens Demo Server Base URL
    */
     var appServiceConfigData = {
      "environment" : "test",
      "base_url": "https://checkoutshopper-test.adyen.com/checkoutshopper/demoserver/",
      "additional_http_headers" : {
        "x-demo-server-api-key": DEMO_SERVER_API_KEY
      }
    };
    AdyenPayment.initialize(appServiceConfigData);

    AdyenPayment.onSuccess((payload) => {
      this.setState({adyen_payment_status: 'success'});
      console.log(payload);
    });

    AdyenPayment.onError((code, error) => {
      console.log("Adyen Error : ",error);
      this.setState({adyen_payment_status: 'failure'});
    });
  }
  
   onClickPayment = () =>
   {
        var paymentDetails = {
          amount: {
              value: 200,//In Multiples of hundred
              currency: 'EUR'
          },
          reference: "XXXXXXXX",
          shopperReference : "XXXXXX",
          shopperEmail : "XXXXXXX@XXXX.com",
          channel: (Platform.OS === 'ios') ? "iOS" : "Android",
          countryCode: "FR",
          shopperLocale: "fr_FR",
          returnUrl: (Platform.OS === 'ios') ? '<YourAppDisplayName>-app://' : "adyencheckout://<packageName>",
          merchantAccount: MERCHANT_ACCOUNT,
          additionalData : {
                  allow3DS2 : true,
                  executeThreeD : true
          }
        };
        // Data for various Components
        var componentData = {
          "scheme" : {
            "card_public_key" : CARD_PUBLIC_KEY
          },
          "applepay" : {
            "apple_pay_merchant_id" : "Your Apple Merchant ID",
            "apple_pay_label": "Your company name"
          },
          "bcmc":{
            "card_public_key" : CARD_PUBLIC_KEY
          }
        }
      try{
          // The Following Payment Methods are supported
          //DROPIN,IDEAL,MOLPAY_MALAYSIA,MOLPAY_THAILAND,MOLPAY_VIETNAM,DOTPAY,EPS,ENTERCASH,OPEN_BANKING,
          //SCHEME,GOOGLE_PAY,SEPA,BCMC,WECHAT_PAY_SDK,APPLE_PAY,
          AdyenPayment.startPayment(AdyenPayment.DROPIN,componentData,paymentDetails)
      }catch(err){
        console.log(err.message);
      }
   }
   
   render() {
    return (
      <View style={styles.container}>
          <Button label={"Pay"} onPress={() => {this.onClickPayment();}}/>
      </View>
   )
  }
```

***b. Add a Promise using startPaymentPromise in your Component, and use the `onPress` method to start the Payment***

```jsx
import AdyenPayment from 'react-native-adyen-payment';

const DEMO_SERVER_API_KEY ="Your Demo Server Key";
const CARD_PUBLIC_KEY = "Your Public Card Key";
const MERCHANT_ACCOUNT = 'Your Merchant Account';

class App extends Component<Props> {
  constructor(props) {
    super(props);
    this.state = {
      isLoading : false,
      adyen_payment_status : 'initiated'
    };
        /*
    The base URL should be your API server running with the following POST endpoints
     - /paymentMethods
     - /payments
     - /payments/details
     Ex :
     Base URL : https://XXXXXX.com/payments/adyen
     https://XXXXXX.com/payments/adyen/paymentMethods
     https://XXXXXX.com/payments/adyen/payments
     https://XXXXXX.com/payments/adyen/payments/details
     Any Extra Header Parameters to be passed can be given in the "additional_http_headers"
     As an example we are using Adyens Demo Server Base URL
    */
     var appServiceConfigData = {
      "environment" : "test",
      "base_url": "https://checkoutshopper-test.adyen.com/checkoutshopper/demoserver/",
      "additional_http_headers" : {
        "x-demo-server-api-key": DEMO_SERVER_API_KEY
      }
    };
    AdyenPayment.initialize(appServiceConfigData);

  }
  
   onClickPayment = async () =>
   {
        var paymentDetails = {
          amount: {
              value: 200,//In Multiples of hundred
              currency: 'EUR'
          },
          reference: "XXXXXXXX",
          shopperReference : "XXXXXX",
          shopperEmail : "XXXXXXX@XXXX.com",
          channel: (Platform.OS === 'ios') ? "iOS" : "Android",
          countryCode: "FR",
          shopperLocale: "fr_FR",
          returnUrl: (Platform.OS === 'ios') ? '<YourAppDisplayName>-app://' : "adyencheckout://<packageName>",
          merchantAccount: MERCHANT_ACCOUNT,
          additionalData : {
                  allow3DS2 : true,
                  executeThreeD : true
          }
        };
        // Data for various Components
        var componentData = {
          "scheme" : {
            "card_public_key" : CARD_PUBLIC_KEY
          },
          "applepay" : {
            "apple_pay_merchant_id" : "Your Apple Merchant ID",
            "apple_pay_label": "Your company name"
          },
          "bcmc":{
            "card_public_key" : CARD_PUBLIC_KEY
          }
        }
      try{
        let response = await AdyenPayment.startPaymentPromise(AdyenPayment.GOOGLE_PAY,componentData,paymentDetails)
        console.log(response);
      }catch(err){
        console.log(err.code);
        console.log(err.message);
      }
   }
   
   render() {
    return (
      <View style={styles.container}>
          <Button label={"Pay"} onPress={() => {this.onClickPayment();}}/>
      </View>
   )
  }
```

?> This is a really straight-forward example.check the [examples' documentation](/EXAMPLES.md).
