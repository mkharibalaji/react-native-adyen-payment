## Props

 There are no props at this moment

## Listeners

### `onSuccess()`

This listens on to the success of the Adyen Payment and returns a message Object.

| Type     | Required  |
| -------- | --------- |
| function | Yes       |

### `onError()`

This listens on to the error of the Adyen Payment and returns a error code and a message.

| Type     | Required  |
| -------- | --------- |
| function | Yes       |

## Methods

### `initialize(AppServiceConfigData)`

This method initiates the Adyen Native Payment UI.

| Parameter| Type      | Required  |
| -------- | --------- | --------- |
| AppServiceConfigData| Object    | Yes       |

### `startPayment(Component,ComponentData,PaymentDetails)`

This method starts the Adyen Native Payment UI and adds the listener

| Parameter| Type      | Required  |
| -------- | --------- | --------- |
| Component| String    | Yes       |
| ComponentData| Object    | Yes       |
| PaymentDetails| Object    | Yes       |

### `startPaymentPromise(Component,ComponentData,PaymentDetails)`

This method starts the Adyen Native Payment UI via Promise

| Parameter| Type      | Required  |
| -------- | --------- | --------- |
| Component| String    | Yes       |
| ComponentData| Object    | Yes       |
| PaymentDetails| Object    | Yes       |
