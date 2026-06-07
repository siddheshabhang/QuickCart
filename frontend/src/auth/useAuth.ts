// Decodes the JWT token to extract user info.
// Your Spring Boot JwtService creates tokens with:
//   subject  → email
//   role     → "CUSTOMER" | "STORE" | "DELIVERY"
//   userId   → the user's database ID

import { getToken } from "./token";

export type Role = "CUSTOMER" | "STORE" | "DELIVERY";

export interface AuthUser {
  email: string;
  role: Role;
  userId: number;
}

// A JWT has 3 parts separated by dots: header.payload.signature.
// The payload is base64-encoded JSON.
function decodeBase64Url(value: string): string {
  const base64 = value.replace(/-/g, "+").replace(/_/g, "/");
  const padded = base64.padEnd(base64.length + ((4 - base64.length % 4) % 4), "=");
  return atob(padded);
}

function decodeToken(token: string): AuthUser | null {
  try {
    const payload = token.split(".")[1];
    const decoded = JSON.parse(decodeBase64Url(payload));

    return {
      email: decoded.sub,
      role: decoded.role,
      userId: decoded.userId,
    };
  } catch {
    return null;
  }
}

// Call this anywhere in the app to get the current logged-in user.
// Returns null if not logged in or token is invalid.
export function getCurrentUser(): AuthUser | null {
  const token = getToken();

  if (!token) {
    return null;
  }

  return decodeToken(token);
}
