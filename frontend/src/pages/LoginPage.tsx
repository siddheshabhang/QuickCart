// The login screen. Handles both login and registration.
//
// Concepts used here:
// - useState: stores values that change (email, password, error message)
// - async/await: waits for the API call to finish before moving on
// - useNavigate: redirects to a different page after login

import { useState } from "react";
import type { CSSProperties, FormEvent } from "react";
import { useNavigate } from "react-router-dom";
import { login, register } from "../api/endpoints";
import { saveToken } from "../auth/token";
import { getCurrentUser } from "../auth/useAuth";

export default function LoginPage() {
  const [isLogin, setIsLogin] = useState(true);
  const [name, setName] = useState("");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);
  const navigate = useNavigate();

  async function handleSubmit(e: FormEvent<HTMLFormElement>) {
    e.preventDefault();
    setError("");
    setLoading(true);

    try {
      const res = isLogin
        ? await login(email, password)
        : await register(name, email, password);

      saveToken(res.data.token);

      const user = getCurrentUser();

      if (user?.role === "STORE") navigate("/store");
      else if (user?.role === "DELIVERY") navigate("/delivery");
      else navigate("/products");
    } catch (err: unknown) {
      if (err instanceof Error) setError(err.message);
      else setError("Login failed");
    } finally {
      setLoading(false);
    }
  }

  return (
    <div style={styles.container}>
      <div style={styles.card}>
        <h1 style={styles.title}>QuickCart</h1>
        <h2 style={styles.subtitle}>{isLogin ? "Login" : "Register"}</h2>

        {error && <p style={styles.error}>{error}</p>}

        <form onSubmit={handleSubmit} style={styles.form}>
          {!isLogin && (
            <input
              style={styles.input}
              type="text"
              placeholder="Full Name"
              value={name}
              onChange={(e) => setName(e.target.value)}
              required
            />
          )}

          <input
            style={styles.input}
            type="email"
            placeholder="Email"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            required
          />

          <input
            style={styles.input}
            type="password"
            placeholder="Password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            required
          />

          <button style={styles.button} type="submit" disabled={loading}>
            {loading ? "Please wait..." : isLogin ? "Login" : "Register"}
          </button>
        </form>

        <p style={styles.toggle}>
          {isLogin ? "Don't have an account? " : "Already have an account? "}
          <span
            style={styles.link}
            onClick={() => {
              setIsLogin(!isLogin);
              setError("");
            }}
          >
            {isLogin ? "Register" : "Login"}
          </span>
        </p>

        <div style={styles.demoBox}>
          <p style={styles.demoTitle}>Demo Users</p>
          <p>customer@quickcart.dev / Customer@123</p>
          <p>store@quickcart.dev / Store@123</p>
          <p>delivery@quickcart.dev / Delivery@123</p>
        </div>
      </div>
    </div>
  );
}

const styles: Record<string, CSSProperties> = {
  container: {
    minHeight: "100vh",
    display: "flex",
    alignItems: "center",
    justifyContent: "center",
    backgroundColor: "#f5f5f5",
  },
  card: {
    backgroundColor: "white",
    padding: "2rem",
    borderRadius: "8px",
    boxShadow: "0 2px 12px rgba(0,0,0,0.1)",
    width: "100%",
    maxWidth: "400px",
  },
  title: {
    textAlign: "center",
    color: "#e44d26",
    marginBottom: "0.25rem",
  },
  subtitle: {
    textAlign: "center",
    marginBottom: "1.5rem",
    color: "#333",
  },
  form: {
    display: "flex",
    flexDirection: "column",
    gap: "1rem",
  },
  input: {
    padding: "0.75rem",
    borderRadius: "4px",
    border: "1px solid #ddd",
    fontSize: "1rem",
  },
  button: {
    padding: "0.75rem",
    backgroundColor: "#e44d26",
    color: "white",
    border: "none",
    borderRadius: "4px",
    fontSize: "1rem",
    cursor: "pointer",
  },
  error: {
    color: "red",
    textAlign: "center",
    marginBottom: "0.5rem",
  },
  toggle: {
    textAlign: "center",
    marginTop: "1rem",
  },
  link: {
    color: "#e44d26",
    cursor: "pointer",
    fontWeight: "bold",
  },
  demoBox: {
    marginTop: "1.5rem",
    padding: "0.75rem",
    backgroundColor: "#f9f9f9",
    borderRadius: "4px",
    fontSize: "0.8rem",
    color: "#666",
    lineHeight: "1.6",
  },
  demoTitle: {
    fontWeight: "bold",
    marginBottom: "0.25rem",
    color: "#333",
  },
};
