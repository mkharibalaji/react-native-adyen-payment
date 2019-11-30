#import <React/RCTBridgeModule.h>
#import <React/RCTEventEmitter.h>

@interface RCT_EXTERN_MODULE(AdyenPayment, NSObject)
RCT_EXTERN_METHOD(startPayment:(NSString *)component componentData:(NSDictionary *)componentData paymentDetails:(NSDictionary *)paymentDetails)
RCT_EXTERN_METHOD(initialize:(NSDictionary *)appServiceConfigData)
RCT_EXTERN_METHOD(startPaymentPromise:(NSString *)component componentData:(NSDictionary *)componentData paymentDetails:(NSDictionary *)paymentDetails resolve:(RCTPromiseResolveBlock)resolve reject:(RCTPromiseRejectBlock)reject)
@end
