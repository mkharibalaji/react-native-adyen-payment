require "json"

package = JSON.parse(File.read(File.join(__dir__, "package.json")))

Pod::Spec.new do |s|
  s.name         = "react-native-adyen-payment"
  s.version      = package["version"]
  s.summary      = package["description"]

  s.homepage     = "https://github.com/zhentan/react-native-adyen-payment"
  s.license      = "MIT"
  s.authors      = { "M K Hari Balaji" => "mk_hari_balaji2003@yahoo.co.in" }
  s.platform     = :ios, "11.0"
  s.source       = { :git => "https://github.com/zhentan/react-native-adyen-payment.git", :tag => "#{s.version}" }

  s.source_files = "ios/**/*.{h,m,swift}"
  s.requires_arc = true

  s.dependency "React"
	s.dependency "Adyen","3.6.3"
end

