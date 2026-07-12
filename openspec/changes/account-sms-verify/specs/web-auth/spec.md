## ADDED Requirements

### Requirement: Registration SMS verification UI

When SMS verification is enabled, the registration page SHALL collect a verification code and support resend with cooldown.

#### Scenario: Show verification field when enabled

- **WHEN** the frontend detects SMS verification is required for registration
- **THEN** `/app/register` shows a verification code input and a "获取验证码" button

#### Scenario: Resend cooldown

- **WHEN** a user clicks send verification code
- **THEN** the button enters a countdown state (e.g. 60 seconds) before allowing resend

#### Scenario: Hide when SMS disabled

- **WHEN** SMS verification is not required
- **THEN** the registration form does not show the verification code fields
