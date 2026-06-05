// src/pages/PaymentPage.tsx
// Simulate payment success or failure for the order.
// orderId comes from the URL: /payment/:orderId
//
// New concept:
// - useParams: reads URL parameters. /payment/42 → orderId = "42"

import { useState } from "react";
import type { CSSProperties } from "react";
import { useParams, useNavigate } from "react-router-dom";
import { processPayment } from "../api/endpoints";
import Navbar from "../components/Navbar";

function createIdempotencyKey() {
  if (typeof crypto !== "undefined" && crypto.randomUUID) {
    return crypto.randomUUID();
  }
  return `${Date.now()}-${Math.random().toString(36).slice(2)}`;
}

export default function PaymentPage() {
  // useParams reads the :orderId from the URL
  const { orderId } = useParams<{ orderId: string }>();
  const navigate = useNavigate();
  const [loading, setLoading] = useState(false);
  const [result, setResult] = useState<"success" | "failed" | null>(null);
  const [error, setError] = useState("");
  const [idempotencyKey, setIdempotencyKey] = useState(createIdempotencyKey);

  async function handlePayment(simulateSuccess: boolean) {
    if (!orderId) return;
    setLoading(true);
    setError("");
    try {
      const res = await processPayment(Number(orderId), simulateSuccess, idempotencyKey);
      setResult(res.data.status === "SUCCESS" ? "success" : "failed");
    } catch (err: unknown) {
      if (err instanceof Error) setError(err.message);
      else setError("Payment failed");
    } finally {
      setLoading(false);
    }
  }

  return (
    <div>
      <Navbar />
      <div style={styles.container}>
        <h2>Payment</h2>
        <p style={styles.orderInfo}>Order #{orderId}</p>

        {error && <p style={styles.error}>{error}</p>}

        {result === null && (
          <div style={styles.buttons}>
            <p style={styles.hint}>This is a demo — choose an outcome:</p>
            <button
              style={styles.successBtn}
              onClick={() => handlePayment(true)}
              disabled={loading}
            >
              {loading ? "Processing..." : "✓ Simulate Success"}
            </button>
            <button
              style={styles.failBtn}
              onClick={() => handlePayment(false)}
              disabled={loading}
            >
              {loading ? "Processing..." : "✗ Simulate Failure"}
            </button>
          </div>
        )}

        {result === "success" && (
          <div style={styles.successBox}>
            <h3>Payment Successful!</h3>
            <p>Your order has been confirmed. Delivery will be assigned shortly.</p>
            <button style={styles.button} onClick={() => navigate("/orders")}>
              View My Orders
            </button>
          </div>
        )}

        {result === "failed" && (
          <div style={styles.failBox}>
            <h3>Payment Failed</h3>
            <p>Your order was not confirmed. You can retry the payment.</p>
            <button
              style={styles.button}
              onClick={() => {
                setIdempotencyKey(createIdempotencyKey());
                setResult(null);
              }}
            >
              Retry Payment
            </button>
          </div>
        )}
      </div>
    </div>
  );
}

const styles: Record<string, CSSProperties> = {
  container: { padding: "1.5rem", maxWidth: "500px", margin: "0 auto", textAlign: "center" },
  orderInfo: { color: "#888", marginBottom: "1.5rem" },
  error: { color: "red" },
  hint: { color: "#666", marginBottom: "1rem" },
  buttons: { display: "flex", flexDirection: "column", gap: "1rem", alignItems: "center" },
  successBtn: {
    padding: "0.75rem 2rem",
    backgroundColor: "#28a745",
    color: "white",
    border: "none",
    borderRadius: "4px",
    cursor: "pointer",
    fontSize: "1rem",
    width: "250px",
  },
  failBtn: {
    padding: "0.75rem 2rem",
    backgroundColor: "#dc3545",
    color: "white",
    border: "none",
    borderRadius: "4px",
    cursor: "pointer",
    fontSize: "1rem",
    width: "250px",
  },
  successBox: {
    padding: "1.5rem",
    backgroundColor: "#d4edda",
    borderRadius: "8px",
    color: "#155724",
  },
  failBox: {
    padding: "1.5rem",
    backgroundColor: "#f8d7da",
    borderRadius: "8px",
    color: "#721c24",
  },
  button: {
    marginTop: "1rem",
    padding: "0.6rem 1.5rem",
    backgroundColor: "#e44d26",
    color: "white",
    border: "none",
    borderRadius: "4px",
    cursor: "pointer",
    fontSize: "1rem",
  },
};
