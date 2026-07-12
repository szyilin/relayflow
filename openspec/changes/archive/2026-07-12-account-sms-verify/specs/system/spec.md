## ADDED Requirements

### Requirement: SMS verification code send

When `relayflow.sms.enabled=true`, the system SHALL expose a public API to send a time-limited numeric verification code for a given mobile and scene.

#### Scenario: Send register code

- **WHEN** a client calls `POST /app-api/system/auth/sms/send` with `{ mobile, scene: "register" }` and SMS is enabled
- **THEN** the system stores a 6-digit code in Redis with configured TTL
- **AND** dispatches the code through the configured `SmsSender`
- **AND** returns success without revealing whether the mobile is already registered

#### Scenario: Resend too frequent

- **WHEN** a client requests another code for the same mobile and scene within the resend interval
- **THEN** the system rejects with `SMS_SEND_TOO_FREQUENT`

#### Scenario: SMS disabled

- **WHEN** `relayflow.sms.enabled=false`
- **THEN** the send endpoint is not available or returns `SMS_DISABLED`
- **AND** registration does not require `smsCode`

### Requirement: SMS verification on registration

When SMS is enabled, registration SHALL require a valid verification code for the same mobile and `register` scene.

#### Scenario: Register with valid sms code

- **WHEN** a client calls `POST /app-api/system/auth/register` with matching `mobile` and valid `smsCode` while SMS is enabled
- **THEN** registration proceeds as defined by open registration rules
- **AND** the used code is consumed and cannot be reused

#### Scenario: Register with invalid sms code

- **WHEN** `smsCode` is missing, expired, or incorrect while SMS is enabled
- **THEN** the system rejects with `SMS_CODE_INVALID` or `SMS_CODE_EXPIRED`

### Requirement: Mock SMS sender for development

The system SHALL provide a mock SMS sender for non-production profiles that logs verification codes instead of calling an external provider.

#### Scenario: Dev mock logs code

- **WHEN** `relayflow.sms.mock=true` and a send request succeeds
- **THEN** the verification code is written to application logs
- **AND** no external SMS API is called
