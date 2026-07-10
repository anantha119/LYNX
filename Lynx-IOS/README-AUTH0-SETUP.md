# Auth0 setup for Lynx-IOS (manual, one-time)

The web app's Auth0 client (`AUTH0_CLIENT_ID` in `lynx/.env.local`) is a confidential
**Regular Web Application** with a client secret. Do not reuse it here — native apps must be
a public **Native** application using PKCE, with no secret embedded in the binary.

## 1. Create the Native application

In the Auth0 dashboard (tenant `dev-z6spty18qf8usxoo.us.auth0.com`):

1. Applications → Create Application → **Native**.
2. Under **Settings**, set:
   - **Allowed Callback URLs**: `BU.Lynx-IOS://dev-z6spty18qf8usxoo.us.auth0.com/ios/BU.Lynx-IOS/callback`
   - **Allowed Logout URLs**: same URL as above.
3. Under **APIs** (or the API's settings for `https://api.lynx.app`), make sure:
   - The API's **Allow Offline Access** is enabled (required for refresh tokens / `offline_access` scope).
4. Copy the new application's **Client ID**.

## 2. Wire the Client ID into the app

Open `Lynx-IOS/Lynx-IOS/Auth0.plist` and replace:

```xml
<key>ClientId</key>
<string>REPLACE_WITH_NATIVE_APP_CLIENT_ID</string>
```

with the Client ID from step 1. The `Domain` value is already set.

## 3. Bundle identifier / URL scheme

The app's URL scheme (`Info.plist` → `CFBundleURLTypes`) is set to the bundle identifier
`BU.Lynx-IOS`, matching Auth0.swift's convention. If you ever change the bundle ID in Xcode,
update both the callback URL in the dashboard and the scheme in `Info.plist` to match.

## 4. Local backend during development

`AppConfig.apiBaseURL` points at `http://localhost:8080` for Debug builds. On the iOS
**Simulator** this reaches your Mac's backend directly. On a **physical device** it will not —
either run the backend on the same network and use your Mac's LAN IP, or point at the deployed
Cloud Run URL.
