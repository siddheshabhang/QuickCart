// src/pages/DeliveryPage.tsx
// Delivery dashboard — view all deliveries, update status, verify OTP.
//
// Flow:
// 1. Delivery partner sees all deliveries (created by payment success via Kafka)
// 2. They update status: ASSIGNED → OUT_FOR_DELIVERY (this generates OTP in backend)
// 3. OTP is logged in delivery-service console (in prod it'd be SMSed to customer)
// 4. Delivery partner enters OTP → status becomes DELIVERED

import { useState, useEffect } from "react";
import type { CSSProperties } from "react";
import { getDeliveries, updateDeliveryStatus, verifyOtp } from "../api/endpoints";
import type { Delivery } from "../api/endpoints";
import Navbar from "../components/Navbar";

const STATUS_ORDER = [
  "PENDING",
  "ASSIGNED",
  "OUT_FOR_DELIVERY",
  "DELIVERED",
  "FAILED",
];

const statusColors: Record<string, string> = {
  PENDING: "#6c757d",
  ASSIGNED: "#fd7e14",
  OUT_FOR_DELIVERY: "#007bff",
  DELIVERED: "#28a745",
  FAILED: "#dc3545",
};

// What status comes next for a given current status
const nextStatus: Record<string, string> = {
  ASSIGNED: "OUT_FOR_DELIVERY",
  OUT_FOR_DELIVERY: "DELIVERED",
};

export default function DeliveryPage() {
  const [deliveries, setDeliveries] = useState<Delivery[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  // OTP input state per delivery
  const [otpInputs, setOtpInputs] = useState<Record<number, string>>({});
  const [otpErrors, setOtpErrors] = useState<Record<number, string>>({});
  const [feedback, setFeedback] = useState<Record<number, string>>({});

  async function fetchDeliveries() {
    try {
      const res = await getDeliveries();
      setDeliveries(res.data);
    } catch {
      setError("Failed to load deliveries");
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    fetchDeliveries();
  }, []);

  async function handleStatusUpdate(orderId: number, status: string) {
    try {
      await updateDeliveryStatus(orderId, status);
      setFeedback((prev) => ({ ...prev, [orderId]: `Status updated to ${status}` }));
      fetchDeliveries();
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : "Failed to update status";
      setFeedback((prev) => ({ ...prev, [orderId]: msg }));
    }
  }

  async function handleVerifyOtp(orderId: number) {
    const otp = otpInputs[orderId];
    if (!otp) {
      setOtpErrors((prev) => ({ ...prev, [orderId]: "Enter OTP" }));
      return;
    }
    try {
      await verifyOtp(orderId, otp);
      setFeedback((prev) => ({ ...prev, [orderId]: "Delivered successfully!" }));
      setOtpInputs((prev) => ({ ...prev, [orderId]: "" }));
      setOtpErrors((prev) => ({ ...prev, [orderId]: "" }));
      fetchDeliveries();
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : "Invalid OTP";
      setOtpErrors((prev) => ({ ...prev, [orderId]: msg }));
    }
  }

  async function handleResendOtp(orderId: number) {
    try {
      // Need to import resendOtp from endpoints
      const { resendOtp } = await import("../api/endpoints");
      await resendOtp(orderId);
      setFeedback((prev) => ({ ...prev, [orderId]: "OTP resent successfully!" }));
      setOtpErrors((prev) => ({ ...prev, [orderId]: "" }));
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : "Failed to resend OTP";
      setOtpErrors((prev) => ({ ...prev, [orderId]: msg }));
    }
  }

  if (loading) return <div><Navbar /><p style={{ padding: "1.5rem" }}>Loading deliveries...</p></div>;
  if (error) return <div><Navbar /><p style={{ padding: "1.5rem", color: "red" }}>{error}</p></div>;

  const active = deliveries.filter((d) => d.status !== "DELIVERED" && d.status !== "FAILED");
  const completed = deliveries.filter((d) => d.status === "DELIVERED" || d.status === "FAILED");

  return (
    <div>
      <Navbar />
      <div style={styles.container}>
        <h2>Delivery Dashboard</h2>
        <p style={styles.hint}>
          OTP is logged in the delivery-service console when status moves to OUT_FOR_DELIVERY.
        </p>

        {/* ── Active Deliveries ── */}
        <h3>Active ({active.length})</h3>
        {active.length === 0 && <p style={styles.empty}>No active deliveries.</p>}

        {active.map((delivery) => (
          <div key={delivery.id} style={styles.card}>
            <div style={styles.cardHeader}>
              <span style={styles.orderId}>Order #{delivery.orderId}</span>
              <span style={{
                ...styles.badge,
                backgroundColor: statusColors[delivery.status] || "#6c757d",
              }}>
                {delivery.status}
              </span>
            </div>

            <p style={styles.meta}>
              Delivery ID: {delivery.id} · Created: {new Date(delivery.createdAt).toLocaleString()}
            </p>

            {/* Status progress bar */}
            <div style={styles.progressBar}>
              {STATUS_ORDER.slice(0, 4).map((s) => (
                <div
                  key={s}
                  style={{
                    ...styles.progressStep,
                    backgroundColor:
                      STATUS_ORDER.indexOf(s) <= STATUS_ORDER.indexOf(delivery.status)
                        ? statusColors[delivery.status]
                        : "#dee2e6",
                  }}
                />
              ))}
            </div>

            {feedback[delivery.orderId] && (
              <p style={styles.feedback}>{feedback[delivery.orderId]}</p>
            )}

            {/* Move to next status button */}
            {nextStatus[delivery.status] && delivery.status !== "OUT_FOR_DELIVERY" && (
              <button
                style={styles.actionBtn}
                onClick={() => handleStatusUpdate(delivery.orderId, nextStatus[delivery.status])}
              >
                Mark as {nextStatus[delivery.status].replace(/_/g, " ")}
              </button>
            )}

            {/* OTP verify section — only shown when OUT_FOR_DELIVERY */}
            {delivery.status === "OUT_FOR_DELIVERY" && (
              <div style={styles.otpSection}>
                <p style={styles.otpHint}>Enter OTP from delivery-service logs to complete delivery:</p>
                <div style={styles.otpRow}>
                  <input
                    style={styles.otpInput}
                    type="text"
                    placeholder="6-digit OTP"
                    maxLength={6}
                    value={otpInputs[delivery.orderId] || ""}
                    onChange={(e) =>
                      setOtpInputs((prev) => ({ ...prev, [delivery.orderId]: e.target.value }))
                    }
                  />
                  <button
                    style={styles.otpBtn}
                    onClick={() => handleVerifyOtp(delivery.orderId)}
                  >
                    Verify OTP
                  </button>
                  <button
                    style={styles.resendBtn}
                    onClick={() => handleResendOtp(delivery.orderId)}
                  >
                    Resend OTP
                  </button>
                </div>
                {otpErrors[delivery.orderId] && (
                  <p style={styles.otpError}>{otpErrors[delivery.orderId]}</p>
                )}
              </div>
            )}
          </div>
        ))}

        {/* ── Completed Deliveries ── */}
        {completed.length > 0 && (
          <>
            <h3 style={{ marginTop: "2rem" }}>Completed ({completed.length})</h3>
            {completed.map((delivery) => (
              <div key={delivery.id} style={{ ...styles.card, opacity: 0.7 }}>
                <div style={styles.cardHeader}>
                  <span style={styles.orderId}>Order #{delivery.orderId}</span>
                  <span style={{
                    ...styles.badge,
                    backgroundColor: statusColors[delivery.status],
                  }}>
                    {delivery.status}
                  </span>
                </div>
                <p style={styles.meta}>
                  Delivery ID: {delivery.id} · {new Date(delivery.createdAt).toLocaleString()}
                </p>
              </div>
            ))}
          </>
        )}
      </div>
    </div>
  );
}

const styles: Record<string, CSSProperties> = {
  container: { padding: "1.5rem", maxWidth: "700px", margin: "0 auto" },
  hint: { color: "#888", fontSize: "0.85rem", marginBottom: "1rem" },
  empty: { color: "#888", fontStyle: "italic" },
  card: {
    backgroundColor: "white",
    border: "1px solid #ddd",
    borderRadius: "8px",
    padding: "1.25rem",
    marginBottom: "1rem",
  },
  cardHeader: {
    display: "flex",
    justifyContent: "space-between",
    alignItems: "center",
    marginBottom: "0.5rem",
  },
  orderId: { fontWeight: "bold", fontSize: "1rem" },
  badge: {
    color: "white",
    padding: "3px 10px",
    borderRadius: "12px",
    fontSize: "0.8rem",
  },
  meta: { fontSize: "0.8rem", color: "#888", margin: "0 0 0.75rem" },
  progressBar: {
    display: "flex",
    gap: "4px",
    marginBottom: "0.75rem",
  },
  progressStep: {
    flex: 1,
    height: "4px",
    borderRadius: "2px",
  },
  feedback: { color: "green", fontSize: "0.85rem", margin: "0.5rem 0" },
  actionBtn: {
    padding: "0.5rem 1rem",
    backgroundColor: "#007bff",
    color: "white",
    border: "none",
    borderRadius: "4px",
    cursor: "pointer",
    fontSize: "0.9rem",
  },
  otpSection: {
    marginTop: "0.75rem",
    padding: "0.75rem",
    backgroundColor: "#fff3cd",
    borderRadius: "6px",
    border: "1px solid #ffc107",
  },
  otpHint: { fontSize: "0.85rem", color: "#856404", margin: "0 0 0.5rem" },
  otpRow: { display: "flex", gap: "0.5rem" },
  otpInput: {
    padding: "0.5rem",
    border: "1px solid #ddd",
    borderRadius: "4px",
    fontSize: "1rem",
    width: "130px",
    letterSpacing: "0.2em",
  },
  otpBtn: {
    padding: "0.5rem 1rem",
    backgroundColor: "#28a745",
    color: "white",
    border: "none",
    borderRadius: "4px",
    cursor: "pointer",
    fontSize: "0.9rem",
  },
  resendBtn: {
    padding: "0.5rem 1rem",
    backgroundColor: "#6c757d",
    color: "white",
    border: "none",
    borderRadius: "4px",
    cursor: "pointer",
    fontSize: "0.9rem",
  },
  otpError: { color: "red", fontSize: "0.85rem", marginTop: "0.25rem" },
};