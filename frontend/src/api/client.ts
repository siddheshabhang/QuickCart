// This is the single fetch wrapper for the entire app.
// Every API call goes through this function.

import { getRefreshToken, getToken, removeTokens, saveTokens } from "../auth/token";

export const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || "http://localhost:8080";

// This is the shape of every response your Spring Boot backend sends back.
// ApiResponse<T> in Java maps to this in TypeScript.
// { success: true, message: "...", data: <actual payload> }
export interface ApiResponse<T> {
  success: boolean;
  message: string;
  data: T;
}

interface AuthResponse {
  token?: string;
  accessToken?: string;
  refreshToken?: string;
}

let refreshPromise: Promise<string | null> | null = null;

async function refreshAccessToken(): Promise<string | null> {
  const refreshToken = getRefreshToken();
  if (!refreshToken) return null;

  const response = await fetch(`${API_BASE_URL}/auth/refresh`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    body: JSON.stringify({ refreshToken }),
  });

  if (!response.ok) {
    removeTokens();
    return null;
  }

  const data: ApiResponse<AuthResponse> = await response.json();
  const accessToken = data.data.accessToken || data.data.token;
  if (!accessToken) {
    removeTokens();
    return null;
  }

  saveTokens(accessToken, data.data.refreshToken || refreshToken);
  return accessToken;
}

async function getRefreshedAccessToken(): Promise<string | null> {
  refreshPromise ??= refreshAccessToken().finally(() => {
    refreshPromise = null;
  });
  return refreshPromise;
}

// The core fetch wrapper.
// - method: GET, POST, PUT, DELETE
// - path: e.g. "/auth/login", "/products", "/cart"
// - body: whatever JSON you want to send (optional)
export async function apiClient<T>(
  method: string,
  path: string,
  body?: unknown,
  headers: Record<string, string> = {},
  retryOnUnauthorized = true
): Promise<ApiResponse<T>> {
  // Grab the JWT token from localStorage (we'll store it there during login)
  const token = getToken();

  const response = await fetch(`${API_BASE_URL}${path}`, {
    method,
    headers: {
      "Content-Type": "application/json",
      // If a token exists, attach it as a Bearer token on every request
      // Your API Gateway's JwtAuthenticationFilter reads this header
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
      ...headers,
    },
    // Only attach a body for POST/PUT requests
    body: body ? JSON.stringify(body) : undefined,
  });

  if (response.status === 401 && retryOnUnauthorized && path !== "/auth/refresh") {
    const refreshedAccessToken = await getRefreshedAccessToken();
    if (refreshedAccessToken) {
      return apiClient<T>(method, path, body, headers, false);
    }
  }

  // Parse the JSON response
  const data: ApiResponse<T> = await response.json();

  // If the server returned a non-2xx status, throw so callers can catch it
  if (!response.ok) {
    throw new Error(data.message || "Something went wrong");
  }
  return data;
}
