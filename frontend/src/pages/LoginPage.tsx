// The login screen. Handles both login and registration.
//
// After a successful customer login, we resolve the nearest dark store:
//   1. Try GPS (browser geolocation).
//   2. If GPS is unavailable/timed-out (not denied), show a manual area input
//      so the user can type their neighbourhood/city and we forward-geocode it.
//   3. Only block entirely if the user explicitly DENIED location permission —
//      in that case we ask them to re-allow and try again.

import { useEffect, useState } from "react";
import type { CSSProperties, FormEvent } from "react";
import { useNavigate, useSearchParams } from "react-router-dom";
import { getGoogleLoginUrl, login, register, getNearestStore } from "../api/endpoints";
import { saveTokens, saveStoreId, saveUserAddress } from "../auth/token";
import { getCurrentUser } from "../auth/useAuth";
import { reverseGeocode } from "../api/geocoding";

// ── Phase the login card moves through for customers ──────────────────────────
type Phase =
  | "form"          // normal email/password form
  | "locating"      // waiting for GPS
  | "manual"        // GPS unavailable → show area input
  | "finding";      // geocoding the typed area → /stores/nearest

export default function LoginPage() {
  const [isLogin, setIsLogin] = useState(true);
  const [name, setName] = useState("");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);
  const [phase, setPhase] = useState<Phase>("form");
  const [manualArea, setManualArea] = useState("");
  const navigate = useNavigate();
  const [searchParams, setSearchParams] = useSearchParams();

  useEffect(() => {
    const oauthError = searchParams.get("oauthError");
    if (oauthError) {
      setError(oauthError);
      setSearchParams({}, { replace: true });
    }
  }, [searchParams, setSearchParams]);

  // ── Shared: given resolved coords, call backend and navigate ──────────────
  async function resolveStoreAndNavigate(lat: number, lng: number) {
    const storeRes = await getNearestStore(lat, lng);
    if (!storeRes.data.deliverable || !storeRes.data.storeId) {
      setError(
        storeRes.data.message ||
        "QuickCart is not available in your area yet. We're expanding soon!"
      );
      setPhase("form");
      return;
    }

    saveStoreId(storeRes.data.storeId);

    const address = await reverseGeocode(lat, lng);
    if (address) saveUserAddress(address);

    navigate("/products");
  }

  // ── Step 1: Try GPS ───────────────────────────────────────────────────────
  async function tryGps() {
    setPhase("locating");
    setError("");

    if (!navigator.geolocation) {
      setError("Location access is required to use QuickCart. Please use a browser that supports geolocation.");
      setPhase("form");
      return;
    }

    return new Promise<void>((done) => {
      navigator.geolocation.getCurrentPosition(
        async (pos) => {
          try {
            await resolveStoreAndNavigate(pos.coords.latitude, pos.coords.longitude);
          } catch (err: unknown) {
            setError(err instanceof Error ? err.message : "Failed to find your store. Please try again.");
            setPhase("form");
          }
          done();
        },
        (locErr) => {
          if (locErr.code === 1) {
            // User explicitly blocked location — ask them to re-allow
            setError(
              "Location access is required to find your nearest store. " +
              "Please allow location access in your browser settings and try again."
            );
            setPhase("form");
          } else {
            // Code 2 (unavailable) or 3 (timeout) — GPS just can't get a fix.
            // Fall back to manual area entry instead of blocking the user.
            setPhase("manual");
          }
          done();
        },
        { timeout: 10000 }
      );
    });
  }

  // ── Step 2 (manual): forward-geocode typed area → backend ─────────────────
  async function handleManualSubmit(e: FormEvent<HTMLFormElement>) {
    e.preventDefault();
    if (!manualArea.trim()) return;

    setPhase("finding");
    setError("");

    try {
      const res = await fetch(
        `https://nominatim.openstreetmap.org/search?q=${encodeURIComponent(manualArea)}&format=json&limit=1`,
        { headers: { "Accept-Language": "en" } }
      );
      const results = await res.json();

      if (!results.length) {
        setError(`"${manualArea}" could not be found. Try a different area name.`);
        setPhase("manual");
        return;
      }

      const { lat, lon } = results[0];
      await resolveStoreAndNavigate(parseFloat(lat), parseFloat(lon));
    } catch {
      setError("Failed to look up your area. Please check your connection and try again.");
    } finally {
      // If resolveStoreAndNavigate set an error, stay on manual phase
      setPhase((p) => (p === "finding" ? "manual" : p));
    }
  }

  // ── Login / register form submit ───────────────────────────────────────────
  async function handleSubmit(e: FormEvent<HTMLFormElement>) {
    e.preventDefault();
    setError("");
    setLoading(true);

    try {
      const res = isLogin
        ? await login(email, password)
        : await register(name, email, password);

      saveTokens(res.data.accessToken || res.data.token, res.data.refreshToken);

      const user = getCurrentUser();

      if (user?.role === "STORE") { navigate("/store"); return; }
      if (user?.role === "DELIVERY") { navigate("/delivery"); return; }

      // Customer → resolve dark store
      await tryGps();
    } catch (err: unknown) {
      if (err instanceof Error) setError(err.message);
      else setError("Login failed");
      setPhase("form");
    } finally {
      setLoading(false);
    }
  }

  // ── Render ─────────────────────────────────────────────────────────────────

  // While GPS is actively running, show a spinner overlay on the card
  if (phase === "locating") {
    return (
      <div style={styles.container}>
        <div style={styles.card}>
          <h1 style={styles.title}>QuickCart</h1>
          <div style={styles.locatingBox}>
            <div style={styles.spinner} />
            <p style={styles.locatingText}>Detecting your location…</p>
            <p style={styles.locatingHint}>Please allow the location prompt if your browser asks.</p>
          </div>
        </div>
      </div>
    );
  }

  // Manual area entry — GPS failed but user didn't deny permission
  if (phase === "manual" || phase === "finding") {
    return (
      <div style={styles.container}>
        <div style={styles.card}>
          <h1 style={styles.title}>QuickCart</h1>
          <h2 style={styles.subtitle}>Find Your Store</h2>

          <p style={styles.manualInfo}>
            📍 We couldn't detect your location automatically. Enter your neighbourhood,
            area, or city below and we'll find the nearest QuickCart store.
          </p>

          {error && <p style={styles.error}>{error}</p>}

          <form onSubmit={handleManualSubmit} style={styles.form}>
            <input
              style={styles.input}
              type="text"
              placeholder="e.g. Koramangala, Bengaluru"
              value={manualArea}
              onChange={(e) => setManualArea(e.target.value)}
              autoFocus
              required
            />
            <button style={styles.button} type="submit" disabled={phase === "finding"}>
              {phase === "finding" ? "Searching…" : "Find My Store"}
            </button>
          </form>

          <button
            style={styles.retryGpsBtn}
            type="button"
            onClick={tryGps}
            disabled={phase === "finding"}
          >
            🔄 Try GPS again
          </button>
        </div>
      </div>
    );
  }

  // Default: the normal login / register form
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

        {isLogin && (
          <button
            style={styles.googleButton}
            type="button"
            onClick={() => {
              window.location.href = getGoogleLoginUrl();
            }}
          >
            Continue with Google
          </button>
        )}

        {isLogin && (
          <div style={styles.quickLoginContainer}>
            <button
              style={styles.quickLoginButton}
              type="button"
              disabled={loading}
              onClick={() => {
                setEmail("admin@quickcart.com");
                setPassword("admin123");
                // The state won't update immediately for the submit handler if we call it directly,
                // so we use a small timeout to let the state settle, or we can just bypass the state.
                setTimeout(() => {
                  document.querySelector("form")?.dispatchEvent(
                    new Event("submit", { cancelable: true, bubbles: true })
                  );
                }, 100);
              }}
            >
              Login as Admin
            </button>
            <button
              style={styles.quickLoginButton}
              type="button"
              disabled={loading}
              onClick={() => {
                setEmail("delivery@quickcart.com");
                setPassword("delivery123");
                setTimeout(() => {
                  document.querySelector("form")?.dispatchEvent(
                    new Event("submit", { cancelable: true, bubbles: true })
                  );
                }, 100);
              }}
            >
              Login as Delivery
            </button>
          </div>
        )}

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
  googleButton: {
    width: "100%",
    marginTop: "0.75rem",
    padding: "0.75rem",
    backgroundColor: "white",
    color: "#333",
    border: "1px solid #ddd",
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
  quickLoginContainer: {
    display: "flex",
    gap: "0.5rem",
    marginTop: "0.5rem",
  },
  quickLoginButton: {
    flex: 1,
    padding: "0.5rem",
    backgroundColor: "#333",
    color: "white",
    border: "none",
    borderRadius: "4px",
    fontSize: "0.9rem",
    cursor: "pointer",
  },
  // ── Location-specific styles ─────────────────────────────────────────────
  locatingBox: {
    display: "flex",
    flexDirection: "column",
    alignItems: "center",
    padding: "2rem 0",
    gap: "0.75rem",
  },
  spinner: {
    width: "40px",
    height: "40px",
    border: "4px solid #f0f0f0",
    borderTop: "4px solid #e44d26",
    borderRadius: "50%",
    animation: "spin 0.8s linear infinite",
  },
  locatingText: {
    fontSize: "1rem",
    fontWeight: 600,
    color: "#333",
    margin: 0,
  },
  locatingHint: {
    fontSize: "0.85rem",
    color: "#888",
    textAlign: "center",
    margin: 0,
  },
  manualInfo: {
    fontSize: "0.9rem",
    color: "#555",
    lineHeight: 1.5,
    marginBottom: "1rem",
    textAlign: "center",
  },
  retryGpsBtn: {
    width: "100%",
    marginTop: "0.75rem",
    padding: "0.6rem",
    background: "none",
    border: "1px solid #ddd",
    borderRadius: "4px",
    fontSize: "0.9rem",
    color: "#555",
    cursor: "pointer",
  },
};
