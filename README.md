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

1. In XCode, in the project navigator, right click `Libraries` ➜ `Add Files to [your project's name]`
2. Go to `node_modules` ➜ `react-native-adyen-payment` and add `AdyenPayment.xcodeproj`
3. In XCode, in the project navigator, select your project. Add `libAdyenPayment.a` to your project's `Build Phases` ➜ `Link Binary With Libraries`
4. Run your project (`Cmd+R`)<

#### Android

1. Open up `android/app/src/main/java/[...]/MainApplication.java`
  - Add `import com.reactlibrary.AdyenPaymentPackage;` to the imports at the top of the file
  - Add `new AdyenPaymentPackage()` to the list returned by the `getPackages()` method
2. Append the following lines to `android/settings.gradle`:
  	```
  	include ':react-native-adyen-payment'
  	project(':react-native-adyen-payment').projectDir = new File(rootProject.projectDir, 	'../node_modules/react-native-adyen-payment/android')
  	```
3. Insert the following lines inside the dependencies block in `android/app/build.gradle`:
  	```
      compile project(':react-native-adyen-payment')
  	```

### Additional Setup

#### iOS
As per the facebook RN doc, create a empty swift file in the name of your project and create the bridge ni your RN project,

* Create a Swift file

1. From Xcode, just go to:

        File → New → File… (or CMD+N)
        Select Swift File
        Name your file <YourProjectName>
        In the Group dropdown, make sure to select the group <YourProjectNameFolder>(Yellow Folder Icon), not the project itself(Blue XCode icon).
    
2. Configure the Objective-C Bridging Header

        After you create the Swift file, you should be prompted to choose if you want to configure an Objective-C Bridging Header. Select “Create Bridging Header”.This file is usually named YourProject-Bridging-Header.h. Don’t change this name manually, because Xcode configures the project with this exact filename.
    
    Note: there is only one Bridging Header per project, so once you have configured it, you won’t be prompted to do it again.

* Within <YourProjectName.swift> empty file add the following,
```
import Foundation
import Adyen

@objc class AdyenObjectiveCBridge: NSObject {
  
  @objc(applicationDidOpenURL:)
  static func applicationDidOpen(_ url: URL) -> Bool {
     let adyenHandled = RedirectComponent.applicationDidOpen(url)
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

## Usage
```javascript
import AdyenPayment from 'react-native-adyen-payment';

// TODO: What to do with the module?
AdyenPayment;
```
