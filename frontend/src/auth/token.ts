// Handles storing and reading the JWT token from localStorage.
// localStorage persists across page refreshes.

const TOKEN_KEY = "token";

// Save the token after a successful login
export function saveToken(token: string): void {
  localStorage.setItem(TOKEN_KEY, token);
}

// Read the token (used by apiClient.ts to attach to every request)
export function getToken(): string | null {
  return localStorage.getItem(TOKEN_KEY);
}

// Delete the token on logout
export function removeToken(): void {
  localStorage.removeItem(TOKEN_KEY);
}

// Quick check — is anyone logged in?
export function isLoggedIn(): boolean {
  return !!localStorage.getItem(TOKEN_KEY);
}
