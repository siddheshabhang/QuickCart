// This is the single fetch wrapper for the entire app.
// Every API call goes through this function.

const BASE_URL = import.meta.env.VITE_API_BASE_URL;

// This is the shape of every response your Spring Boot backend sends back.
// ApiResponse<T> in Java maps to this in TypeScript.
// { success: true, message: "...", data: <actual payload> }
export interface ApiResponse<T> {
  success: boolean;
  message: string;
  data: T;
}

// The core fetch wrapper.
// - method: GET, POST, PUT, DELETE
// - path: e.g. "/auth/login", "/products", "/cart"
// - body: whatever JSON you want to send (optional)
export async function apiClient<T>(
  method: string,
  path: string,
  body?: unknown
): Promise<ApiResponse<T>> {
  // Grab the JWT token from localStorage (we'll store it there during login)
  const token = localStorage.getItem("token");

  const response = await fetch(`${BASE_URL}${path}`, {
    method,
    headers: {
      "Content-Type": "application/json",
      // If a token exists, attach it as a Bearer token on every request
      // Your API Gateway's JwtAuthenticationFilter reads this header
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
    },
    // Only attach a body for POST/PUT requests
    body: body ? JSON.stringify(body) : undefined,
  });

  // Parse the JSON response
  const data: ApiResponse<T> = await response.json();

  // If the server returned a non-2xx status, throw so callers can catch it
  if (!response.ok) {
    throw new Error(data.message || "Something went wrong");
  }
  return data;
}