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

### `startPayment(Component,ComponentData,PaymentDetails,AppServiceConfigData)`

This method initiates the Adyen Native Payment UI.

| Parameter| Type      | Required  |
| -------- | --------- | --------- |
| Component| String    | Yes       |
| ComponentData| Object    | Yes       |
| PaymentDetails| Object    | Yes       |
| AppServiceConfigData| Object    | Yes       |