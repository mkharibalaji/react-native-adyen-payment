import React, {useState, useEffect} from 'react';
import {Platform, View, Button, Text} from 'react-native';
import AdyenPayment from 'react-native-adyen-payment';
import {
  ADYEN_MERCHANT_ACCOUNT,
  ADYEN_PUBLIC_KEY,
  ADYEN_BASE_URL,
  ADYEN_ENVIRONMENT,
} from '@env';

const MOCK_PAYMENT_DETAILS = {
  amount: {
    value: 100,
    currency: 'USD',
  },
  reference: '123456',
  shopperReference: '123456',
  shopperEmail: 'test@test.test',
  shopperLocale: 'en-US',
  channel: Platform.OS === 'ios' ? 'iOS' : 'Android',
  countryCode: 'US',
  // Remember to replace returnUrl with your app scheme
  returnUrl:
    Platform.OS === 'ios' ? 'your-ios-scheme://' : 'your-android-scheme://',
  merchantAccount: ADYEN_MERCHANT_ACCOUNT,
  additionalData: {
    allow3DS2: true,
    executeThreeD: true,
  },
};

const MOCK_COMPONENT_DATA = {
  scheme: {
    card_public_key: ADYEN_PUBLIC_KEY,
  },
  // Uncomment to add Apple Pay (replace apple_pay_merchant_id):
  // applepay: {
  //   apple_pay_merchant_id: 'merchant.com.adyen.your.merchant',
  //   supportedNetworks: ['visa', 'masterCard', 'amex', 'discover'],
  //   merchantCapabilities: ['supports3DS'],
  // },
};

const APP_SERVICE_CONFIG_DATA = {
  environment: ADYEN_ENVIRONMENT,
  base_url: ADYEN_BASE_URL,
  // Add any additional headers to pass to your backend
  additional_http_headers: {
    'x-channel': Platform.OS, // Example
  },
};

const STATUS = {
  none: 'none',
  initiated: 'initiated',
  success: 'success',
  failure: 'failure',
};

function AdyenExample() {
  const [status, setStatus] = useState(STATUS.none);

  useEffect(() => {
    AdyenPayment.initialize(APP_SERVICE_CONFIG_DATA);

    AdyenPayment.onSuccess((payload) => {
      console.log('success', payload);
      setStatus(STATUS.success);
    });
    AdyenPayment.onError((payload) => {
      console.log('failure', payload);
      setStatus(STATUS.failure);
    });
  }, []);

  function handleDropInButtonPress() {
    setStatus(STATUS.initiated);

    try {
      AdyenPayment.startPayment(
        AdyenPayment.DROPIN,
        MOCK_COMPONENT_DATA,
        MOCK_PAYMENT_DETAILS,
      );
    } catch (err) {
      console.error(err);
    }
  }

  return (
    <View>
      <Text>Status: {status}</Text>
      <Button title="Drop-in" onPress={handleDropInButtonPress} />
    </View>
  );
}

export default React.memo(AdyenExample);
