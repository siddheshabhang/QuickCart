// src/pages/PaymentPage.tsx
import { useState } from "react";
import type { CSSProperties } from "react";
import { useParams, useNavigate } from "react-router-dom";
import { processPayment } from "../api/endpoints";
import Navbar from "../components/Navbar";
import { useToast } from "../context/ToastContext";

function createIdempotencyKey() {
  if (typeof crypto !== "undefined" && crypto.randomUUID) {
    return crypto.randomUUID();
  }
  return `${Date.now()}-${Math.random().toString(36).slice(2)}`;
}

export default function PaymentPage() {
  const { orderId } = useParams<{ orderId: string }>();
  const navigate = useNavigate();
  const { showToast } = useToast();

  const [loading, setLoading] = useState(false);
  const [result, setResult] = useState<"success" | "failed" | null>(null);
  const [idempotencyKey, setIdempotencyKey] = useState(createIdempotencyKey);

  async function handlePayment(simulateSuccess: boolean) {
    if (!orderId) return;
    setLoading(true);
    try {
      const res = await processPayment(Number(orderId), simulateSuccess, idempotencyKey);
      const isSuccess = res.data.status === "SUCCESS";
      setResult(isSuccess ? "success" : "failed");
      if (isSuccess) {
        showToast("Payment Successful!");
      } else {
        showToast("Payment Failed", "error");
      }
    } catch (err: any) {
      showToast(err.message || "Payment request failed", "error");
    } finally {
      setLoading(false);
    }
  }

  return (
    <div style={s.page}>
      <Navbar />
      <div style={s.container}>
        
        <div style={s.card}>
          <h2 style={s.title}>Payment Gateway</h2>
          <p style={s.orderInfo}>Complete payment for Order #{orderId}</p>

          {result === null && (
            <div style={s.gatewayBox}>
              <div style={s.demoLabel}>Demo Mode</div>
              <p style={s.hint}>Please choose a simulated outcome for this transaction:</p>
              
              <div style={s.buttons}>
                <button
                  style={s.successBtn}
                  onClick={() => handlePayment(true)}
                  disabled={loading}
                >
                  {loading ? "Processing..." : "✅ Simulate Success"}
                </button>
                <button
                  style={s.failBtn}
                  onClick={() => handlePayment(false)}
                  disabled={loading}
                >
                  {loading ? "Processing..." : "❌ Simulate Failure"}
                </button>
              </div>
            </div>
          )}

          {result === "success" && (
            <div style={s.resultBoxSuccess}>
              <div style={s.resultIcon}>✅</div>
              <h3 style={s.resultTitle}>Payment Successful!</h3>
              <p style={s.resultDesc}>Your order has been confirmed. A delivery rider will be assigned shortly.</p>
              <button style={s.actionBtn} onClick={() => navigate("/orders")}>
                View My Orders
              </button>
            </div>
          )}

          {result === "failed" && (
            <div style={s.resultBoxFail}>
              <div style={s.resultIcon}>❌</div>
              <h3 style={s.resultTitleFail}>Payment Failed</h3>
              <p style={s.resultDesc}>Your transaction could not be completed. Please try again.</p>
              <button
                style={s.actionBtnFail}
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
    </div>
  );
}

const s: Record<string, CSSProperties> = {
  page: { minHeight: "100vh", backgroundColor: "var(--color-bg)" },
  container: { padding: "4rem 1.5rem", display: "flex", justifyContent: "center" },
  card: {
    backgroundColor: "var(--color-surface)",
    borderRadius: "var(--radius-lg)",
    border: "1px solid var(--color-border)",
    padding: "2.5rem 2rem",
    boxShadow: "var(--shadow-md)",
    width: "100%",
    maxWidth: "500px",
    textAlign: "center",
  },
  title: { margin: "0 0 0.5rem", fontSize: "1.5rem", fontWeight: 800, color: "var(--color-text-main)" },
  orderInfo: { margin: "0 0 2rem", fontSize: "1rem", color: "var(--color-text-muted)", fontWeight: 500 },
  
  gatewayBox: {
    backgroundColor: "var(--color-bg)",
    border: "1px dashed var(--color-border)",
    borderRadius: "var(--radius-md)",
    padding: "2rem 1.5rem",
    position: "relative",
  },
  demoLabel: {
    position: "absolute",
    top: "-12px",
    left: "50%",
    transform: "translateX(-50%)",
    backgroundColor: "var(--color-warning)",
    color: "white",
    fontSize: "0.75rem",
    fontWeight: 700,
    padding: "4px 12px",
    borderRadius: "var(--radius-xl)",
    letterSpacing: "0.5px",
    textTransform: "uppercase",
  },
  hint: { margin: "0 0 1.5rem", fontSize: "0.95rem", color: "var(--color-text-muted)" },
  buttons: { display: "flex", flexDirection: "column", gap: "1rem" },
  
  successBtn: {
    padding: "1rem",
    backgroundColor: "var(--color-success)",
    color: "white",
    border: "none",
    borderRadius: "var(--radius-md)",
    fontSize: "1rem",
    fontWeight: 600,
    cursor: "pointer",
    transition: "transform 0.1s",
  },
  failBtn: {
    padding: "1rem",
    backgroundColor: "white",
    color: "var(--color-error)",
    border: "1px solid var(--color-error)",
    borderRadius: "var(--radius-md)",
    fontSize: "1rem",
    fontWeight: 600,
    cursor: "pointer",
  },
  
  resultBoxSuccess: {
    padding: "2rem 1rem",
    backgroundColor: "var(--color-success-bg)",
    borderRadius: "var(--radius-md)",
    border: "1px solid #A7F3D0",
  },
  resultBoxFail: {
    padding: "2rem 1rem",
    backgroundColor: "var(--color-error-bg)",
    borderRadius: "var(--radius-md)",
    border: "1px solid #FECACA",
  },
  resultIcon: { fontSize: "3rem", marginBottom: "1rem" },
  resultTitle: { margin: "0 0 0.5rem", fontSize: "1.25rem", color: "var(--color-success)" },
  resultTitleFail: { margin: "0 0 0.5rem", fontSize: "1.25rem", color: "var(--color-error)" },
  resultDesc: { margin: "0 0 1.5rem", fontSize: "0.95rem", color: "var(--color-text-main)", lineHeight: 1.5 },
  
  actionBtn: {
    padding: "0.8rem 1.5rem",
    backgroundColor: "var(--color-primary)",
    color: "white",
    border: "none",
    borderRadius: "var(--radius-md)",
    fontSize: "1rem",
    fontWeight: 600,
    cursor: "pointer",
  },
  actionBtnFail: {
    padding: "0.8rem 1.5rem",
    backgroundColor: "var(--color-error)",
    color: "white",
    border: "none",
    borderRadius: "var(--radius-md)",
    fontSize: "1rem",
    fontWeight: 600,
    cursor: "pointer",
  },
};
