# Solution Design

## Context
This is the system design solution for the [ontop challenge](ontop-challenge.pdf)

## Identified user use cases
- Add bank Details
- Transfer money from wallet to bank account
- View transaction history with filter and sort capabilities

## Identified System Constraints
- system only supports USD transactions
- user can add bank details only once
  >we want the user to enter their bank details only once


## Design
### Data model
![Data model](data-model.png)

Four models were designed for this solution; account, transfer, payment and wallet_transaction
- The account model is for persisting the user account details which is used for transfers
- The transfer model is the aggregate entity and also the entry point for the transfer process, it holds information about the details of the transfer(status, charge, amount, target account, operations)
- The payment model persist information from the payment api
- The wallet_transaction model persists information about wallet transaction operations(withdrawal, refund)

<br/>

## Flow of use cases
### Add Bank Details Flow
![add bank details flow](user-add-bank-details-flow.png)
```bash
# add bank details api
POST: /ontop/accounts

request body: {
  userId, firstName, lastName, routingNumber, nationalId, accountNumber, bankName, currency
}
response body: {
  userId, accountId, firstName, lastName, routingNumber, nationalId, accountNumber, bankName, currency
}
```
This flow to add user bank details doesn't have any complex logic and is straight forward. I also identified an extra field(currency) as a requirement for adding user bank details. One of the reasons behind adding the currency field is that it is needed for the request to the payment api.

### Money Transfer Flow
![money transfer flow](money-transfer-flow.png)

```bash
# money transfer api
POST: /ontop/transfers

request body: { userId, amount }
response body: { transactionId, userId, amount, created, operation, status }
```
The money transfer request results in two operations, the wallet transaction and the payment.
The wallet transaction could either be a withdrawal by which a transfer is initiated or a refund by which a transfer is reversed if payment fails.

**WITHDRAWAL**\
A transfer is initiated after a successful withdrawal, and is the first operation in the transfer flow.\
After a successful withdrawal, the system continues to process other operations asynchronously while returning a response to the client with a "PROCESSING" status.\
If the withdrawal fails, the client receives an error response and the flow is terminated.\
Before the withdrawal operation, a lock on the user resource is obtained to ensure the withdrawal operation is atomic per user.
The lock is released once the withdrawal operation is complete, regardless of its status.

**PAYMENT** \
Payment processing begins asynchronously once the transfer has been initialized. If payment is successful, the transfer enters the PROCESSING state and no further action is required.\
If payment fails due to timeout, the payment will be retried. The default maximum number of retries is 2, with a retry delay factor of 5 seconds. If payment fails and is not retryable, the transfer enters the FAILED state.\
However, although unlikely, the transfer can enter the UNKNOWN state if there are any unknown errors during payment processing.\
The properties of the ontop account used in the payment processing is configured in the configuration file of the active environment

**REFUND** \
Refund happens if the transfer enters FAILED state. The transfer enters the REVERSED state if refund is successful, also notification should be sent to the user about the refund.

The state of the transfer process is indicated by the status field in the transfer model
- INITIALISED - This is the first state of the transfer, it indicates that the withdrawal operation was successful and the transfer has been initialised.
- PROCESSING - The transfer status is in this state if payment is successful
- FAILED - This is the status of the transfer when payment operation fails or payment confirmation fails
- REVERSED - This indicates that the transfer has been successfully reversed with a refund operation
- SUCCESSFUL - This is the status of transfer once we confirm that the payment to the target account is successful
- UNKNOWN - This indicates that the transfer state needs to be resolved
>ℹ️ The third party payment api always returns PROCESSING for successful payment operations and the solution to confirm payment 
> from third party is outside the scope of this challenge

#### Code navigation
- The `TransferInitialisationService` handles the client transfer request and starts the withdrawal operation, if the withdrawal is successful, it returns a response and publishes an event that indicates a successful withdrawal.
- The `TransferPaymentProcessingService` handles the successful withdrawal event and processes the payment. If the payment processing fails it publishes an event indicating the payment failed.
- The `TransferReversalService` handles the event of payment failure and reverses the withdrawal.

The places in the code with `// alert code owner` comments should be unreachable states but if for any reason the transfer enters those state, then the code owners should be alerted to resolve further cases.
It is also possible to develop a resolution service that first tries to resolve the state of the transfer before alerting code owners


### Transaction History Flow
```bash
# transaction history api
GET: /ontop/transfers

request params: userId, page, size, amount?, startDate?, endDate? # the fields marked with question mark are optional others are required
response body: [{ transactionId, userId, amount, created, operation, status }] # sorted descending by creation date
```
The transaction history is fetched from the wallet transaction table. A transaction has three possible status; PROCESSING, FAILED, SUCCESSFUL.\
A refund transaction if available is always SUCCESSFUL, it has only one possible state, however, the status of the withdrawal transaction is determined by the status of the transfer
```bash
operation=WITHDRAWAL
case INITIALISED, PROCESSING, UNKNOWN -> PROCESSING
case FAILED, REVERSED -> FAILED
case SUCCESSFUL -> SUCCESSFUL
```

## Architectural Diagram
![architectural diagram](architectural-diagram.png)
