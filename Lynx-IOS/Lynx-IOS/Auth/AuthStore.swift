import Auth0
import Foundation
import JWTDecode
import Observation

@Observable
@MainActor
final class AuthStore {
    enum State {
        case loading
        case signedOut
        case signedIn(UserInfo)
    }

    struct UserInfo: Equatable {
        let name: String
        let email: String?
        let pictureURL: URL?
    }

    private(set) var state: State = .loading
    private(set) var lastError: String?

    private let credentialsManager = CredentialsManager(authentication: Auth0.authentication())

    init() {
        Task { await restoreSession() }
    }

    private func restoreSession() async {
        guard credentialsManager.hasValid() else {
            state = .signedOut
            return
        }
        do {
            let credentials = try await credentialsManager.credentials()
            state = .signedIn(Self.userInfo(from: credentials))
        } catch {
            state = .signedOut
        }
    }

    func login() async {
        lastError = nil
        do {
            let credentials = try await Auth0
                .webAuth()
                .audience(AppConfig.auth0Audience)
                .scope(AppConfig.auth0Scope)
                .start()
            _ = credentialsManager.store(credentials: credentials)
            state = .signedIn(Self.userInfo(from: credentials))
        } catch {
            lastError = Self.describe(error)
            state = .signedOut
        }
    }

    /// Surfaces the underlying Auth0 error detail rather than a generic
    /// message, since a failed exchange (bad audience/scope, callback
    /// mismatch, etc.) is otherwise indistinguishable from a user cancel.
    private static func describe(_ error: Error) -> String {
        if let webAuthError = error as? WebAuthError {
            return webAuthError.debugDescription
        }
        return error.localizedDescription
    }

    func logout() async {
        do {
            try await Auth0.webAuth().clearSession()
        } catch {
            // Local sign-out proceeds regardless of remote session clear.
        }
        _ = credentialsManager.clear()
        state = .signedOut
    }

    /// Returns a valid access token, transparently refreshing if needed
    /// (used as the Bearer token against the Cloud Run backend).
    func accessToken() async throws -> String {
        let credentials = try await credentialsManager.credentials()
        return credentials.accessToken
    }

    private static func userInfo(from credentials: Credentials) -> UserInfo {
        let profile = credentials.idTokenPayload
        let name = (profile?["name"] as? String) ?? (profile?["nickname"] as? String) ?? "User"
        let email = profile?["email"] as? String
        let picture = (profile?["picture"] as? String).flatMap(URL.init(string:))
        return UserInfo(name: name, email: email, pictureURL: picture)
    }
}

private extension Credentials {
    /// Decodes the ID token's payload for display fields (name/email/picture).
    var idTokenPayload: [String: Any]? {
        try? JWTDecode.decode(jwt: idToken).body
    }
}
