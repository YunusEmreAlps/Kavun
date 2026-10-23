# API Authentication Controller

The API Authentication Controller is used to authenticate users, refresh expired tokens and logout.
The endpoints are not secured and are open to all users.

The base URL for the API Authentication Controller is:

```bash
/api/v1/auth
```

![img.png](images/auth-rest-api.png)

## Configuration

The authentication controller supports optional security features that can be enabled via environment variables:

### Captcha Protection

Captcha verification can be enabled to protect against automated login attempts:

```properties
login.captcha.enabled=${LOGIN_CAPTCHA_ENABLED:false}
```

**Default: `false` (Captcha is disabled)**

When `false` (default):
- Captcha validation is skipped completely
- `/api/v1/auth/captcha` endpoint is still available but not required
- `captchaId` and `captchaText` fields in login request are ignored

When `true`:
- A captcha **must** be requested from `/api/v1/auth/captcha` before login
- Login requests **must** include valid `captchaId` and `captchaText` fields
- Rate limiting applies: 5 captcha requests per minute per IP
- Captchas expire after 5 minutes
- Invalid or missing captcha will reject the login attempt

**Captcha Endpoint:**
```bash
GET: /api/v1/auth/captcha
```

Response:
```json
{
  "imageBase64": "data:image/png;base64,...",
  "captchaId": "uuid-string"
}
```

### OTP (One-Time Password)

Two-factor authentication via OTP can be enabled:

```properties
login.otp.enabled=${LOGIN_OTP_ENABLED:false}
```

**Default: `false` (OTP is disabled)**

When `false` (default):
- OTP verification is skipped completely
- Login returns JWT token directly after successful credential validation
- Two-factor authentication is not required

When `true`:
- Login does **not** return JWT token immediately
- An OTP code is sent to user's email or phone (based on user preference)
- Login returns `{ id, target }`: the challenge id and the masked address the code was sent to
- User **must** call `/api/v1/auth/verify-otp` with the challenge id and the code to complete authentication
- Only after OTP verification, JWT token is returned
- A code can only be obtained by passing the password check; there is no endpoint that sends one
  for a username alone

In the `development` and `test` profiles nothing is sent and the code is always
`app.test-otp-code` (`APP_TEST_OTP_CODE`, default `190303`). The password is still checked first;
other profiles send a random code.

## Authentication Flow

The authentication flow varies based on which security features are enabled:

### Flow 1: Basic Authentication (No Captcha, No OTP) ✅ DEFAULT

**Configuration:**
```properties
login.captcha.enabled=false  # Default
login.otp.enabled=false      # Default
```

**Flow:**
```
POST /api/v1/auth/login
  → username + password
  → Returns: JWT accessToken + refreshToken (cookie)
```

This is the **default behavior** when no environment variables are set.

### Flow 2: With Captcha Only

**Configuration:**
```properties
login.captcha.enabled=true   # Must be set
login.otp.enabled=false      # Default
```

**Flow:**
```
1. GET /api/v1/auth/captcha
   → Returns: { imageBase64, captchaId }

2. POST /api/v1/auth/login
   → username + password + captchaId + captchaText
   → Returns: JWT accessToken + refreshToken (cookie)
```

### Flow 3: With OTP Only

**Configuration:**
```properties
login.captcha.enabled=false  # Default
login.otp.enabled=true       # Must be set
```

**Flow:**
```
1. POST /api/v1/auth/login
   → username + password
   → Returns: { id, target } (challenge id, masked address)

2. POST /api/v1/auth/verify-otp
   → id + code (6-digit OTP)
   → Returns: JWT accessToken + refreshToken (cookie)
```

### Flow 4: With Both Captcha and OTP (Maximum Security)

**Configuration:**
```properties
login.captcha.enabled=true   # Must be set
login.otp.enabled=true       # Must be set
```

**Flow:**
```
1. GET /api/v1/auth/captcha
   → Returns: { imageBase64, captchaId }

2. POST /api/v1/auth/login
   → username + password + captchaId + captchaText
   → Returns: { id, target } (challenge id, masked address)

3. POST /api/v1/auth/verify-otp
   → id + code (6-digit OTP)
   → Returns: JWT accessToken + refreshToken (cookie)
```

**Important Notes:**
- **Both features are disabled by default** - Basic authentication (Flow 1) is used unless explicitly enabled
- Features can be enabled independently or together
- `target` is the user's email or phone number, masked, depending on their OTP delivery preference
- `id` is the challenge id returned from the login endpoint (a random UUID)
- OTP codes expire after 5 minutes and can be used once
- A challenge allows 3 attempts; after that the user has to log in again
- To enable features, set environment variables: `LOGIN_CAPTCHA_ENABLED=true` and/or `LOGIN_OTP_ENABLED=true`

---

## Login Endpoint

Attempts to authenticate with the provided credentials. If successful, a JWT token is returned with
some user details.

A refresh token is generated and returned as a cookie.

The URL for the login endpoint is:

```bash
POST: /api/v1/auth/login
```

![img_1.png](images/auth-rest-login-request.png)

 ```json
{
  "username": "admin",
  "password": "password",
  "captchaText": "Ab3Xy",
  "captchaId": "550e8400-e29b-41d4-a716-446655440000"
}
```

**Note:** The `captchaText` and `captchaId` fields are **optional** by default but become **required** when `login.captcha.enabled=true`. Request a captcha from `/api/v1/auth/captcha` endpoint before login when captcha is enabled.

Takes the username and password of a user as json then generates a JWT response object.
A new refreshToken is generated and returned as a cookie.

When a valid refreshToken is specified, the provided refreshToken will be used instead of generating
a new one.

**Response varies based on OTP configuration:**

1. When `login.otp.enabled=false` (default):

```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "expiresIn": 3600,
  "publicId": "uuid-string"
}
```

2. When `login.otp.enabled=true`:

```json
{
  "id": "1b4e28ba-2fa1-11d2-883f-0016d3cca427",
  "target": "u***@example.com"
}
```

**After receiving this response, proceed to verify-otp endpoint with the `id` and the code the user received.**

![img_2.png](images/auth-rest-login-response.png)

**Response Fields:**

- `accessToken`: JWT token to be passed with requests to access protected endpoints (only when OTP is disabled)
- `publicId`: User identifier for all requests (only when OTP is disabled)
- `id`: OTP challenge id (only when OTP is enabled)
- `target`: Masked email or phone the code was sent to, for display (only when OTP is enabled)

An unknown username and a wrong password get the same `401` response, so the endpoint does not
reveal which usernames exist.

**Note:** Request to retrieve a user object does not include the database id. PublicId must be used instead.

![img_3.png](images/sample-secured-url-access.png)

## Verify OTP Endpoint

Verifies the OTP code and completes the authentication process when OTP is enabled.

The URL for the verify-otp endpoint is:

```bash
POST: /api/v1/auth/verify-otp
```

**Request Body:**
```json
{
  "id": "1b4e28ba-2fa1-11d2-883f-0016d3cca427",
  "code": "123456"
}
```

**Fields:**
- `id`: OTP challenge id returned from the login endpoint
- `code`: 6-digit OTP code received via email or SMS

**Response:**
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "expiresIn": 3600,
  "publicId": "uuid-string"
}
```

**Error Responses:**
- `400 Bad Request`: Unknown challenge, invalid OTP code, expired or used OTP, or maximum attempts exceeded
- `401 Unauthorized`: The account was disabled, locked or deleted after the password check

## Refresh Endpoint

Refreshes the current access token by creating a new access token when given a valid refreshToken.

The URL for the refresh endpoint is:

```bash
GET: /api/v1/auth/refresh
```

![img_4.png](images/auth-rest-refresh-token.png)

## Logout Endpoint

Logs out the current user by invalidating the current refresh token. This is done by returning an
empty refreshToken via cookie.
Logout the user from the system and clear all cookies.

The URL for the logout endpoint is:

```bash
DELETE: /api/v1/auth/logout
```

![img.png](images/auth-rest-logout.png)
