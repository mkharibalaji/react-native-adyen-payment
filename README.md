# react-native-adyen-payment -- Work In Progress

## Requirements
  1. React Native > 0.60
  2. XCode 11

## Getting started

`$ npm install react-native-adyen-payment --save`

### Mostly automatic installation

`$ react-native link react-native-adyen-payment`

### Manual installation

#### iOS

 * Goto ProjectFolder/ios and run
 
 ```pod install```
 
 * As per the facebook RN doc, create a empty swift file in the name of your project and create the bridge in your RN project,

1. From Xcode, just go to:

      File → New → File… (or CMD+N)
      Select Swift File
      Name your file <YourProjectName>
      In the Group dropdown, make sure to select the group <YourProjectNameFolder>(Yellow Folder Icon), not the project itself(Blue XCode icon).
    
2. Configure the Objective-C Bridging Header

      After you create the Swift file, you should be prompted to choose if you want to configure an Objective-C Bridging Header. Select “Create Bridging Header”.This file is usually named YourProject-Bridging-Header.h. Don’t change this name manually, because Xcode configures the project with this exact filename.
    
    Note: There is only one Bridging Header per project, so once you have configured it, you won’t be prompted to do it again.

* Within <YourProjectName.swift> empty file add the following,
```
import Foundation
import Adyen

@objc class AdyenObjectiveCBridge: NSObject {
  
  @objc(applicationDidOpenURL:)
  static func applicationDidOpen(_ url: URL) -> Bool {
     let adyenHandled = RedirectComponent.applicationDidOpen(from : url)
     return adyenHandled
  }
}
```
* AppDelegate.m file add the below function
```
.....
#import "<yourProjectName>-Swift.h"
.....
- (BOOL)application:(UIApplication *)app openURL:(NSURL *)url options:(NSDictionary<NSString *,id> *)options {
  
  BOOL handledAdyen =[AdyenObjectiveCBridge applicationDidOpenURL:url];
  
  return handledAdyen;
}
.....
```
  
#### Android

##### Optional Step if autolinking didn't work
 Open up `android/app/src/main/java/[...]/MainApplication.java` - Do this step if the Package is not added properly
  - Add `import com.reactlibrary.AdyenPaymentPackage;` to the imports at the top of the file
  - Add `new AdyenPaymentPackage()` to the list returned by the `getPackages()` method

##### Add to Progaurd for release
  If you use ProGuard or R8, the following rules should be enough to maintain all expected functionality.
```-keep class com.adyen.checkout.base.model.** { *; }
   -keep class com.adyen.threeds2.** { *; }
  -keepclassmembers public class * implements com.adyen.checkout.base.PaymentComponent {
     public <init>(...);
  }
  ```
    
## Usage

```javascript
import AdyenPayment from 'react-native-adyen-payment';

...........
............
...........

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
            "apple_pay_merchant_id" : "Your Apple Merchant ID"
          },
          "bcmc":{
            "card_public_key" : CARD_PUBLIC_KEY
          }
        }
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
      try{
          // The Following Payment Methods are supported
          //DROPIN,IDEAL,MOLPAY_MALAYSIA,MOLPAY_THAILAND,MOLPAY_VIETNAM,DOTPAY,EPS,ENTERCASH,OPEN_BANKING,
          //SCHEME,GOOGLE_PAY,SEPA,BCMC,WECHAT_PAY_SDK,APPLE_PAY,
          AdyenPayment.startPayment(AdyenPayment.DROPIN,componentData,paymentDetails,appServiceConfigData)
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
