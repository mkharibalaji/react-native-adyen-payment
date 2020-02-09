# Installation

## Requirements
  1. React Native > 0.60
  2. XCode 11

## Getting started

`$ npm install react-native-adyen-payment --save`

## Mostly automatic installation (Skip this step)

`$ react-native link react-native-adyen-payment`

## Additional Setup

### iOS

 * Goto ProjectFolder/ios and run
 
 ```bash
 pod install
 ```
 
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

```swift
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

```objectivec
#import "<yourProjectName>-Swift.h"

- (BOOL)application:(UIApplication *)app openURL:(NSURL *)url options:(NSDictionary<NSString *,id> *)options {
  
  BOOL handledAdyen =[AdyenObjectiveCBridge applicationDidOpenURL:url];
  
  return handledAdyen;
}
```
3 . Configure info.plist with CFBundleURL for redirect URL in the PaymentData
```xml
<key>CFBundleURLTypes</key>
  <array>
    <dict>
      <key>CFBundleTypeRole</key>
      <string>Editor</string>
      <key>CFBundleURLName</key>
      <string>YOUR_APP_BUNDLE_NAME_UNIQUE(com.xxxx.xxxx)</string>
      <key>CFBundleURLSchemes</key>
      <array>
        <string>YOUR_APP_URL_FOR_REDIRECT(Ex : CFBundleDisplayName-app)</string>
      </array>
    </dict>
  </array>
```
### Android

#### Optional Step if autolinking didn't work
 Open up `android/app/src/main/java/[...]/MainApplication.java` - Do this step if the Package is not added properly
  - Add `import com.reactlibrary.AdyenPaymentPackage;` to the imports at the top of the file
  - Add `new AdyenPaymentPackage()` to the list returned by the `getPackages()` method

#### Change the Theme to Material Theme
  Add the below styles in `android/app/src/main/res/values/styles.xml` - Adyen depends on ThemedComponents or else you will end up with `Caused by: java.lang.IllegalArgumentException: The style on this component requires your app theme to be Theme.MaterialComponents (or a descendant)`
  
  ```xml
	<resources>
	    <!-- Base application theme. -->
	    <style name="AppTheme" parent="Theme.MaterialComponents.Light.NoActionBar">
	        <item name="android:textColor">#000000</item>
	        <item name="colorPrimary">@color/colorPrimary</item>
	        <item name="colorPrimaryDark">@color/colorPrimaryDark</item>
	        <item name="colorAccent">@color/colorAccent</item>
	    </style>
	    <style name="AdyenCheckout.TextInputLayout" parent="Widget.MaterialComponents.TextInputLayout.OutlinedBox">
	        <item name="boxStrokeColor">@color/primaryColor</item>
	        <item name="hintTextColor">@color/primaryColor</item>
	        <item name="android:minHeight">@dimen/input_layout_height</item>
	    </style>
	    <style name="ThreeDS2Theme" parent="Theme.MaterialComponents.Light.DarkActionBar">
		    <item name="colorPrimary">@color/colorPrimary</item>
		    <item name="colorPrimaryDark">@color/colorPrimaryDark</item>
		    <item name="colorAccent">@color/colorAccent</item>
		</style>
    </resources>
  ```

#### Add to Progaurd for release
  If you use ProGuard or R8, the following rules should be enough to maintain all expected functionality.
```gradle
  -keep class com.adyen.checkout.base.model.** { *; }
  -keep class com.adyen.threeds2.** { *; }
  -keepclassmembers public class * implements com.adyen.checkout.base.PaymentComponent {
     public <init>(...);
  }
  ```
### API Server

  Create a API Server which calls the below methods with the same EndPoint URI's.

  Test Env :

    https://checkout-test.adyen.com/v51/paymentMethods
    https://checkout-test.adyen.com/v51/payments
    https://checkout-test.adyen.com/v51/payments/details

  Live Env : (https://docs.adyen.com/development-resources/live-endpoints)

    https://[random]-[company name]-checkout-live.adyenpayments.com/checkout/[version]/[method]

  - [version] The service version number, always starting with "v" (for example, v49).
  - [method] The endpoint name.
  - [random] A random string of hex-encoded bytes to make the hostname unpredictable.
  - [company name] The company name to be included in the URL endpoint. If the name is too long, it is shortened. If the name includes underscores or hyphens, any underscores and/or hyphens are stripped.

  The AdyenPayment system internally calls your API base URL with the following POST endpoints which internally calls the above mentioned URL's

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

         var appServiceConfigData = {
          "environment" : "test",
          "base_url": "https://checkoutshopper-test.adyen.com/checkoutshopper/demoserver/",
          "additional_http_headers" : {
            "x-demo-server-api-key": DEMO_SERVER_API_KEY
          }
