import Foundation

enum AppConfig {
    /// Mirrors NEXT_PUBLIC_API_URL in the web client (chat-app.tsx).
    /// Pointed at the deployed Cloud Run backend so no local server is needed.
    /// Swap to "http://localhost:8080" here (Simulator only — see
    /// README-AUTH0-SETUP.md) if you want to hit a local backend instead.
    static let apiBaseURL = URL(string: "https://lynx-backend-waw7tiae6q-uc.a.run.app")!

    static let auth0Audience = "https://api.lynx.app"
    static let auth0Scope = "openid profile email offline_access"
}
