import Adyen
import Foundation

internal struct AppServiceConfigData {
    static var base_url : String = ""
    static var app_url_headers : [String:String] = [:]
    static var card_public_key : String = ""
    static var applePayMerchantIdentifier : String = ""
    static var environment : String = "test"
}

internal final class APIClient {
    
    internal typealias CompletionHandler<T> = (Result<T, Error>) -> Void
    
    internal func perform<R: Request>(_ request: R, completionHandler: @escaping CompletionHandler<R.ResponseType>) {
        let url : URL = URL(string:AppServiceConfigData.base_url)!.appendingPathComponent(request.path)
        
        let body: Data
        do {
            body = try Coder.encode(request)
        } catch {
            completionHandler(.failure(error))
            return
        }
        
        print(" ---- Request (/\(request.path)) ----")
        printAsJSON(body)
        
        var urlRequest = URLRequest(url: url)
        urlRequest.httpMethod = "POST"
        urlRequest.httpBody = body
        
        var url_headers = ["Content-Type": "application/json"]
        if(!AppServiceConfigData.app_url_headers.isEmpty){
            for (key, value) in AppServiceConfigData.app_url_headers {
                url_headers[key] = value as String
            }
        }
        urlRequest.allHTTPHeaderFields = url_headers
        
        requestCounter += 1
        
        urlSession.adyen.dataTask(with: urlRequest) { result in
            switch result {
            case let .success(data):
                print(" ---- Response (/\(request.path)) ----")
                printAsJSON(data)
                
                do {
                    let response = try Coder.decode(data) as R.ResponseType
                    print(" ---- SuccessFully DEcoded ) ----")
                    completionHandler(.success(response))
                } catch {
                    print("APIClient Decoder error: \(error).")
                    completionHandler(.failure(error))
                }
            case let .failure(error):
                print("API Failure error: \(error).")
                completionHandler(.failure(error))
            }
            
            self.requestCounter -= 1
        }.resume()
    }
    
    private lazy var urlSession: URLSession = {
        URLSession(configuration: .default, delegate: nil, delegateQueue: .main)
    }()
    
    private var requestCounter = 0 {
        didSet {
            let application = UIApplication.shared
            application.isNetworkActivityIndicatorVisible = self.requestCounter > 0
        }
    }
    
}

private func printAsJSON(_ data: Data) {
    do {
        let jsonObject = try JSONSerialization.jsonObject(with: data, options: [])
        let jsonData = try JSONSerialization.data(withJSONObject: jsonObject, options: [.prettyPrinted])
        guard let jsonString = String(data: jsonData, encoding: .utf8) else { return }
        
        print(jsonString)
    } catch {
        if let string = String(data: data, encoding: .utf8) {
            print(string)
        }
    }
}
