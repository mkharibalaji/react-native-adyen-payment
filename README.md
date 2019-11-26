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

1. Open up `android/app/src/main/java/[...]/MainApplication.java` - Do this step if the Package is not added properly
  - Add `import com.reactlibrary.AdyenPaymentPackage;` to the imports at the top of the file
  - Add `new AdyenPaymentPackage()` to the list returned by the `getPackages()` method


## Usage
```javascript
import AdyenPayment from 'react-native-adyen-payment';

// TODO: What to do with the module?
AdyenPayment;
```
