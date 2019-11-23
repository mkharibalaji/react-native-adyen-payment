//  Created by react-native-create-bridge

package com.rnlib.adyen

import com.facebook.react.ReactPackage
import com.facebook.react.bridge.JavaScriptModule
import com.facebook.react.bridge.NativeModule
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.uimanager.ViewManager

class AdyenPaymentPackage : ReactPackage {
    
    override fun createNativeModules(reactContext: ReactApplicationContext): MutableList<NativeModule> {
        val modList : MutableList<NativeModule> = mutableListOf()
        modList.add(AdyenPaymentModule(reactContext))
        return modList
    }
    
    override fun createViewManagers(reactContext: ReactApplicationContext): MutableList<ViewManager<*, *>> {
        // https://facebook.github.io/react-native/docs/native-components-android.html#4-register-the-viewmanager
        // Register your native component's view manager
        return mutableListOf()
    }

}
