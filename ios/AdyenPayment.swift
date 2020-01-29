import Foundation
import Adyen
import PassKit


@objc(AdyenPayment)
class AdyenPayment: RCTEventEmitter {
    var currentComponent: PresentableComponent?
    var redirectComponent: RedirectComponent?
    var threeDS2Component: ThreeDS2Component?
    var paymentMethods: PaymentMethods?
    var componentData : NSDictionary?
    var component : String?
    var vSpinner : UIView?
    var resolve: RCTPromiseResolveBlock?
    var reject: RCTPromiseRejectBlock?
    var emitEvent : Bool = false
    
    lazy var apiClient = APIClient()
    
    override static func requiresMainQueueSetup() -> Bool {
        return true
    }
    
    func showSpinner(onView : UIView) {
        let spinnerView = UIView.init(frame: onView.bounds)
        spinnerView.backgroundColor = UIColor.init(red: 0.5, green: 0.5, blue: 0.5, alpha: 0.5)
        let ai = UIActivityIndicatorView.init(style: .whiteLarge)
        ai.startAnimating()
        ai.center = spinnerView.center
        
        DispatchQueue.main.async {
            spinnerView.addSubview(ai)
            onView.addSubview(spinnerView)
        }
        
        vSpinner = spinnerView
    }
    
    func removeSpinner() {
        DispatchQueue.main.async {
            self.vSpinner?.removeFromSuperview()
            self.vSpinner = nil
        }
    }
    
    func setPaymentDetails(_ paymentDetails : NSDictionary){
        let amount = paymentDetails["amount"] as! [String : Any]
        let additionalData = paymentDetails["additionalData"] as? [String : Any]
        PaymentsData.amount = Payment.Amount(value: amount["value"] as! Int, currencyCode: amount["currency"] as! String)
        PaymentsData.reference = paymentDetails["reference"] as! String
        PaymentsData.countryCode = paymentDetails["countryCode"] as! String
        PaymentsData.returnUrl = paymentDetails["returnUrl"] as! String
        PaymentsData.shopperReference = paymentDetails["shopperReference"] as! String
        PaymentsData.shopperEmail = paymentDetails["shopperEmail"] as! String
        PaymentsData.shopperLocale = paymentDetails["shopperLocale"] as! String
        PaymentsData.merchantAccount = paymentDetails["merchantAccount"] as! String
        if(additionalData != nil){
            let allow3DS2 : Bool = (additionalData?["allow3DS2"] != nil) ? additionalData?["allow3DS2"] as! Bool : false
            let executeThreeD : Bool = (additionalData?["executeThreeD"] != nil) ? additionalData?["executeThreeD"] as! Bool : false
            PaymentsData.additionalData = ["allow3DS2":  allow3DS2,"executeThreeD":executeThreeD]
        }
        /*PaymentsData.cardComponent =  (paymentDetails["cardComponent"] != nil) ? paymentDetails["cardComponent"] as! [String : Any] : [String : Any]()*/
    }
    

    func setAppServiceConfigDetails(_ appServiceConfigData : NSDictionary){
        AppServiceConfigData.base_url = appServiceConfigData["base_url"] as! String
        if(appServiceConfigData["additional_http_headers"] != nil){
           AppServiceConfigData.app_url_headers = appServiceConfigData["additional_http_headers"] as! [String:String]
        }
        AppServiceConfigData.environment = appServiceConfigData["environment"] as! String
    }
    
    func storedPaymentMethod<T: StoredPaymentMethod>(ofType type: T.Type) -> T? {
        return self.paymentMethods?.stored.first { $0 is T } as? T
    }
    
    func setPaymentMethods(_ paymentmethodsJSONResponse: NSDictionary) {
        let paymentMethodsResponse : PaymentMethodsResponse?
        do {
            let jsonData = try! JSONSerialization.data(withJSONObject : paymentmethodsJSONResponse, options: .prettyPrinted)
            paymentMethodsResponse = try Coder.decode(jsonData) as PaymentMethodsResponse
            self.paymentMethods = paymentMethodsResponse?.paymentMethods
        } catch {
        }
    }
    
    func setAdyenConfiguration(_ paymentDetails : NSDictionary,paymentMethodResponse : NSDictionary, appServiceConfigData : NSDictionary){
        self.setPaymentMethods(paymentMethodResponse)
        self.setPaymentDetails(paymentDetails)
        self.setAppServiceConfigDetails(appServiceConfigData)
    }
    
    
    func showCardComponent(_ componentData : NSDictionary) throws {
        guard let paymentMethod = self.paymentMethods?.paymentMethod(ofType: CardPaymentMethod.self) else { return}
        let cardComponent : [String:Any] = componentData["scheme"] as? [String:Any] ?? [:]
        guard cardComponent["card_public_key"] != nil else {return}
        DispatchQueue.main.async {
            if(self.storedPaymentMethod(ofType: StoredCardPaymentMethod.self) != nil){
                let configuration = DropInComponent.PaymentMethodsConfiguration()
                configuration.card.publicKey = cardComponent["card_public_key"] as? String
                self.showDropInComponent(configuration: configuration)
            }else{
                let component = CardComponent(paymentMethod: paymentMethod, publicKey:(cardComponent["card_public_key"] as! String))
                self.present(component)
            }
        }
    }
    


    func showIssuerComponent(_ component : String, componentData : NSDictionary) throws{
        DispatchQueue.main.async {
            guard let paymentMethod = self.paymentMethods?.paymentMethod(ofType: IssuerListPaymentMethod.self) else { return }
            let component = IssuerListComponent(paymentMethod: paymentMethod)
            self.present(component)
        }
    }

    
    func showSEPADirectDebitComponent(_ componentData : NSDictionary)throws {
        DispatchQueue.main.async {
            guard let paymentMethod = self.paymentMethods?.paymentMethod(ofType: SEPADirectDebitPaymentMethod.self) else { return }
            let component = SEPADirectDebitComponent(paymentMethod: paymentMethod)
            component.delegate = self
            self.present(component)
        }
    }
 
    func showApplePayComponent(_ componentData : NSDictionary) throws {
        DispatchQueue.main.async {
            guard let paymentMethod = self.paymentMethods?.paymentMethod(ofType: ApplePayPaymentMethod.self) else { return }
            let appleComponent : [String:Any] = componentData["applepay"] as? [String:Any] ?? [:]
            guard appleComponent["apple_pay_merchant_id"] != nil else {return}
            let amt = NSDecimalNumber(string: String(format: "%.2f", Float((PaymentsData.amount.value) / 100)))
            let applePaySummaryItems = [PKPaymentSummaryItem(label: "Total", amount: amt, type: .final)]
            
            let component = ApplePayComponent(paymentMethod: paymentMethod,payment:Payment(amount: PaymentsData.amount, countryCode: PaymentsData.countryCode),merchantIdentifier: appleComponent["apple_pay_merchant_id"] as! String,summaryItems: applePaySummaryItems)
            if(component != nil){
                component?.delegate = self
                self.present(component!)
            }
        }
    }
    
    func showDropInComponent(configuration : DropInComponent.PaymentMethodsConfiguration) {
        DispatchQueue.main.async {
            var regularPaymentMethods : [PaymentMethod] = [PaymentMethod]()
            var storedPaymentMethods : [StoredPaymentMethod] = [StoredPaymentMethod]()
            for reg_py_mthd in self.paymentMethods!.regular {
                if(reg_py_mthd.type == "scheme"){
                    regularPaymentMethods.append(reg_py_mthd)
                    break
                }
            }
            for stored_py_mthd in self.paymentMethods!.stored {
                print(stored_py_mthd.type)
                if(stored_py_mthd.type == "scheme"){
                    storedPaymentMethods.append(stored_py_mthd)
                    break
                }
            }
            let dropInComponent = DropInComponent(paymentMethods: PaymentMethods(regular:regularPaymentMethods, stored:storedPaymentMethods),paymentMethodsConfiguration: configuration)
            dropInComponent.delegate = self
            self.present(dropInComponent)
        }
    }
    
    @objc func initialize(_ appServiceConfigData : NSDictionary){
        self.setAppServiceConfigDetails(appServiceConfigData)
    }
    
    @objc func startPaymentPromise(_ component: NSString,componentData : NSDictionary,paymentDetails : NSDictionary,resolve: @escaping RCTPromiseResolveBlock,reject: @escaping RCTPromiseRejectBlock){
        self.resolve = resolve
        self.reject = reject
        self.showPayment(component,componentData : componentData,paymentDetails : paymentDetails)
    }
    
    @objc func startPayment(_ component: NSString,componentData : NSDictionary,paymentDetails : NSDictionary){
        self.emitEvent = true
        self.showPayment(component,componentData : componentData,paymentDetails : paymentDetails)
    }
    
    func showPayment(_ component: NSString,componentData : NSDictionary,paymentDetails : NSDictionary){
        DispatchQueue.main.async {
            let rootViewController = UIApplication.shared.delegate?.window??.rootViewController
            self.showSpinner(onView: rootViewController!.view)
        }
        self.setPaymentDetails(paymentDetails)
        self.componentData = componentData
        self.component = component as String
        let request = PaymentMethodsRequest()
        self.apiClient.perform(request, completionHandler: self.paymentMethodsResponseHandler)
    }
    
    func paymentMethodsResponseHandler(result: Result<PaymentMethodsResponse, Error>) {
            self.removeSpinner()
            switch result {
            case let .success(response):
                self.paymentMethods = response.paymentMethods
                self.startPayment(self.component!,componentData: self.componentData!)
            case let .failure(error):
                self.presentAlert(withTitle:"Error",message: error.localizedDescription)
            }
    }

    func sendSuccess(message : Dictionary<String, Any>?){
        if(self.resolve != nil){
            self.resolve!(message)
        }
        if(self.emitEvent){
            self.sendEvent(withName: "onSuccess",body: ["message": message])
        }
    }
    
    func sendFailure(code : String,message : String){
        if(self.reject != nil){
            self.reject!(code, message,nil)
        }
        if(self.emitEvent){
            self.sendEvent(withName: "onError",body: ["code": code, "message": message])
        }
    }
    
    func startPayment(_ component : String,componentData : NSDictionary){
        do{
            switch component {
                case "dropin":
                    try self.showDropInComponent(componentData)
                case "scheme":
                    try self.showCardComponent(componentData)
                case "applepay":
                    try self.showApplePayComponent(componentData)
                case "sepadirectdebit":
                    try self.showSEPADirectDebitComponent(componentData)
                case "ideal","entercash","eps","dotpay","openbanking_UK","molpay_ebanking_fpx_MY","molpay_ebanking_TH","molpay_ebanking_VN":
                    try self.showIssuerComponent(component,componentData : componentData)
                default :
                    self.sendFailure(code : "ERROR_UNKNOWN_PAYMENT_METHOD",message: "Unknown Payment Method")
            }
        } catch  {
            self.sendFailure(code : "ERROR_UNKNOWN",message: error.localizedDescription)
        }
    }
    
    func showDropInComponent(_ componentData : NSDictionary) throws{
        let configuration = DropInComponent.PaymentMethodsConfiguration()
        let appleComponent : [String:Any] = componentData["applepay"] as? [String:Any] ?? [:]
        let cardComponent : [String:Any] = componentData["scheme"] as? [String:Any] ?? [:]
        if(!cardComponent.isEmpty){
            configuration.card.publicKey = cardComponent["card_public_key"] as? String
        }
        if(!appleComponent.isEmpty){
            configuration.applePay.merchantIdentifier = appleComponent["apple_pay_merchant_id"] as? String
            // let amt = NSDecimalNumber(string: String(format: "%.2f", Float((PaymentsData.amount.value) / 100)))
            let amt = NSDecimalNumber(string: String(format: "%.2f", Float(PaymentsData.amount.value) / 100))
            let applePaySummaryItems = [PKPaymentSummaryItem(label: "Total", amount: amt, type: .final)]
            configuration.applePay.summaryItems = applePaySummaryItems
        }
        DispatchQueue.main.async {
            let dropInComponent = DropInComponent(paymentMethods: self.paymentMethods!,paymentMethodsConfiguration: configuration)
            dropInComponent.delegate = self
            self.present(dropInComponent)
        }
    }
    
    /*
    func showDropInComponent() {
        self.setAdyenConfiguration(paymentDetails,paymentMethodResponse: paymentMethodResponse,appServiceConfigData: appServiceConfigData)
        let configuration = DropInComponent.PaymentMethodsConfiguration()
        configuration.card.publicKey = PaymentsData.cardComponent["card_public_key"] as? String
        if(!PaymentsData.applePayComponent.isEmpty){
            configuration.applePay.merchantIdentifier = PaymentsData.applePayComponent["apple_pay_merchant_id"] as? String
            let paymentAmt = paymentDetails["amount"] as! [String : Any]
            let amt = NSDecimalNumber(string: String(format: "%.2f", Float((paymentAmt["value"] as! Int) / 100)))
            let applePaySummaryItems = [PKPaymentSummaryItem(label: "Total", amount: amt, type: .final)]
            configuration.applePay.summaryItems = applePaySummaryItems
        }
        DispatchQueue.main.async {
            let dropInComponent = DropInComponent(paymentMethods: self.paymentMethods!,paymentMethodsConfiguration: configuration)
            dropInComponent.delegate = self
            self.present(dropInComponent)
        }
    }
 */
    
    func present(_ component: PresentableComponent) {
        component.environment = (AppServiceConfigData.environment == "test") ? Environment.test : Environment.live
        let amount = PaymentsData.amount as Payment.Amount
        component.payment = Payment(amount: amount)
        component.payment?.countryCode = PaymentsData.countryCode
        
        if let paymentComponent = component as? PaymentComponent {
            paymentComponent.delegate = self
        }
        
        if let actionComponent = component as? ActionComponent {
            actionComponent.delegate = self
        }
        (UIApplication.shared.delegate?.window??.rootViewController)!.present(component.viewController, animated: true)
        self.currentComponent = component
    }
    
    func performPayment(with data: PaymentComponentData) {
        let request = PaymentsRequest(data: data)
        apiClient.perform(request, completionHandler: paymentResponseHandler)
    }
    
    func performPaymentDetails(with data: ActionComponentData) {
        let request = PaymentDetailsRequest(details: data.details, paymentData: data.paymentData)
        apiClient.perform(request, completionHandler: paymentResponseHandler)
    }
    
    func paymentResponseHandler(result: Result<PaymentsResponse, Error>) {
        switch result {
        case let .success(response):
            if let action = response.action {
                handle(action)
            } else {
                if(response.resultCode != nil){
                    finish(with: response)
                }else if(response.validationError != nil){
                    currentComponent?.stopLoading(withSuccess: false) { [weak self] in
                        let validationError = response.validationError!
                        self?.presentAlert(withTitle:"Error",message: validationError.errorMessage)
                    }
                }
            }
        case let .failure(error):
            currentComponent?.stopLoading(withSuccess: false) { [weak self] in
                self?.presentAlert(with: error)
            }
        }
    }
    
    func handle(_ action: Action) {
        if let dropInComponent = currentComponent as? DropInComponent {
            dropInComponent.handle(action)
            return
        }
        switch action {
        case let .redirect(redirectAction):
            redirect(with: redirectAction)
        case let .threeDS2Fingerprint(threeDS2FingerprintAction):
            performThreeDS2Fingerprint(with: threeDS2FingerprintAction)
        case let .threeDS2Challenge(threeDS2ChallengeAction):
            performThreeDS2Challenge(with: threeDS2ChallengeAction)
        }
    }
    
    func redirect(with action: RedirectAction) {
        let redirectComponent = RedirectComponent(action: action)
        redirectComponent.delegate = self
        self.redirectComponent = redirectComponent
        UIApplication.shared.keyWindow?.rootViewController?.present(redirectComponent.viewController, animated: true)
    }
    
    func performThreeDS2Fingerprint(with action: ThreeDS2FingerprintAction) {
        let threeDS2Component = ThreeDS2Component()
        threeDS2Component.delegate = self
        self.threeDS2Component = threeDS2Component
        threeDS2Component.handle(action)
    }
    
    func performThreeDS2Challenge(with action: ThreeDS2ChallengeAction) {
        guard let threeDS2Component = threeDS2Component else { return }
        threeDS2Component.handle(action)
    }
    
    func finish(with response: PaymentsResponse) {
        let resultCode : PaymentsResponse.ResultCode = response.resultCode!
        if(resultCode == .authorised || resultCode == .received || resultCode == .pending){
            let additionalData : NSDictionary = (response.additionalData != nil) ? NSMutableDictionary(dictionary:response.additionalData!) : NSDictionary()
            print(response)
            let msg:Dictionary? = ["resultCode" : resultCode.rawValue,"merchantReference":response.merchantReference!,"pspReference" : response.pspReference!,"additionalData" : additionalData]
            self.sendSuccess(message:msg)
        }else if(resultCode == .refused || resultCode == .error){
            self.sendFailure(code : response.error_code ?? "",message: response.refusalReason ?? "")
        }else if (resultCode == .cancelled){
            self.sendFailure(code : "ERROR_CANCELLED",message: "Transaction Cancelled")
        }else{
            self.sendFailure(code : "ERROR_UNKNOWN",message: "Unknown Error")
        }
        currentComponent?.stopLoading(withSuccess: true) { [weak self] in
            (UIApplication.shared.delegate?.window??.rootViewController)!.dismiss(animated: true) {}
        }
        redirectComponent = nil
        threeDS2Component = nil
        
    }
    
    func finish(with error: Error) {
        let isCancelled = ((error as? ComponentError) == .cancelled)
        if !isCancelled {
            self.sendFailure(code : "ERROR_GENERAL",message: "Payment has error")
        }else{
            self.sendFailure(code : "ERROR_CANCELLED",message: "Transaction Cancelled")
        }
        redirectComponent = nil
        threeDS2Component = nil
        (UIApplication.shared.delegate?.window??.rootViewController)!.dismiss(animated: true) {}
    }
    

    
    private func presentAlert(with error: Error, retryHandler: (() -> Void)? = nil) {
        let alertController = UIAlertController(title: "Error", message: error.localizedDescription, preferredStyle: .alert)
        alertController.addAction(UIAlertAction(title: "OK", style: .default, handler: nil))
        (UIApplication.shared.delegate?.window??.rootViewController)!.present(alertController, animated: true)
    }
    
    private func presentAlert(withTitle title: String,message:String?=nil) {
        let alertController = UIAlertController(title: title, message: message, preferredStyle: .alert)
        alertController.addAction(UIAlertAction(title: "OK", style: .default, handler: nil))
        (UIApplication.shared.delegate?.window??.rootViewController)!.present(alertController, animated: true)
    }
    
    override func supportedEvents() -> [String]! {
        return [
            "onError",
            "onSuccess"
        ]
    }
}

extension AdyenPayment: DropInComponentDelegate {
    
    internal func didSubmit(_ data: PaymentComponentData, from component: DropInComponent) {
        performPayment(with: data)
    }
    
    internal func didProvide(_ data: ActionComponentData, from component: DropInComponent) {
        performPaymentDetails(with: data)
    }
    
    internal func didFail(with error: Error, from component: DropInComponent) {
        finish(with: error)
    }
    
}

extension AdyenPayment: PaymentComponentDelegate {
    
    internal func didSubmit(_ data: PaymentComponentData, from component: PaymentComponent) {
        performPayment(with: data)
    }
    
    internal func didFail(with error: Error, from component: PaymentComponent) {
        finish(with: error)
    }
    
}

extension AdyenPayment: ActionComponentDelegate {
    
    internal func didFail(with error: Error, from component: ActionComponent) {
        finish(with: error)
    }
    
    internal func didProvide(_ data: ActionComponentData, from component: ActionComponent) {
        performPaymentDetails(with: data)
    }
}