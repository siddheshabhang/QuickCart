// Handles storing and reading the JWT token from localStorage.
// localStorage persists across page refreshes.

const TOKEN_KEY = "token";
const REFRESH_TOKEN_KEY = "refreshToken";

// Save the token after a successful login
export function saveToken(token: string): void {
  localStorage.setItem(TOKEN_KEY, token);
}

export function saveTokens(accessToken: string, refreshToken?: string | null): void {
  localStorage.setItem(TOKEN_KEY, accessToken);
  if (refreshToken) {
    localStorage.setItem(REFRESH_TOKEN_KEY, refreshToken);
  }
}

// Read the token (used by apiClient.ts to attach to every request)
export function getToken(): string | null {
  return localStorage.getItem(TOKEN_KEY);
}

export function getRefreshToken(): string | null {
  return localStorage.getItem(REFRESH_TOKEN_KEY);
}

// Delete the token on logout
export function removeToken(): void {
  localStorage.removeItem(TOKEN_KEY);
  localStorage.removeItem(REFRESH_TOKEN_KEY);
}

export function removeTokens(): void {
  removeToken();
}

// Quick check — is anyone logged in?
export function isLoggedIn(): boolean {
  return !!localStorage.getItem(TOKEN_KEY);
}
