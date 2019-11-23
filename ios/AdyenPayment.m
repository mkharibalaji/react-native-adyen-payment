#import <React/RCTBridgeModule.h>
#import <React/RCTEventEmitter.h>

@interface RCT_EXTERN_MODULE(AdyenPayment, NSObject)
RCT_EXTERN_METHOD(startPayment:(NSString *)component componentData:(NSDictionary *)componentData paymentDetails:(NSDictionary *)paymentDetails appServiceConfigData:(NSDictionary *)appServiceConfigData)
@end
