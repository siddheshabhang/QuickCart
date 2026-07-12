// src/pages/CheckoutPage.tsx
import { useState } from "react";
import type { CSSProperties, FormEvent } from "react";
import { useNavigate } from "react-router-dom";
import { placeOrder } from "../api/endpoints";
import Navbar from "../components/Navbar";
import { getUserAddress, saveUserAddress } from "../auth/token";
import { useToast } from "../context/ToastContext";

export default function CheckoutPage() {
  const [address, setAddress] = useState(getUserAddress() || "");
  const [phone, setPhone] = useState("");
  const [loading, setLoading] = useState(false);
  const navigate = useNavigate();
  const { showToast } = useToast();

  async function handleSubmit(e: FormEvent<HTMLFormElement>) {
    e.preventDefault();
    if (phone.length < 10) {
      showToast("Please enter a valid 10-digit phone number", "error");
      return;
    }

    setLoading(true);
    try {
      saveUserAddress(address); // Save for next time
      const res = await placeOrder(address, phone);
      const orderId = res.data.orderId;
      showToast("Order placed! Proceeding to payment.");
      navigate(`/payment/${orderId}`);
    } catch (err: any) {
      showToast(err.message || "Failed to place order", "error");
    } finally {
      setLoading(false);
    }
  }

  return (
    <div style={s.page}>
      <Navbar />
      <div style={s.container}>
        
        <div style={s.card}>
          <div style={s.header}>
            <div style={s.headerIcon}>📍</div>
            <div>
              <h2 style={s.title}>Delivery Details</h2>
              <p style={s.subtitle}>Where should we deliver your order?</p>
            </div>
          </div>

          <form onSubmit={handleSubmit} style={s.form}>
            <div style={s.inputGroup}>
              <label style={s.label}>Delivery Address</label>
              <textarea
                style={s.textarea}
                value={address}
                onChange={(e) => setAddress(e.target.value)}
                placeholder="E.g., Flat 402, Block B, Green Valley Apts..."
                required
                rows={3}
              />
            </div>

            <div style={s.inputGroup}>
              <label style={s.label}>Phone Number</label>
              <div style={s.phoneWrapper}>
                <span style={s.phonePrefix}>+91</span>
                <input
                  style={s.phoneInput}
                  type="tel"
                  value={phone}
                  onChange={(e) => setPhone(e.target.value)}
                  placeholder="9876543210"
                  required
                />
              </div>
            </div>

            <button style={s.submitBtn} type="submit" disabled={loading}>
              {loading ? "Processing..." : "Continue to Payment"}
            </button>
          </form>
        </div>

      </div>
    </div>
  );
}

const s: Record<string, CSSProperties> = {
  page: { minHeight: "100vh", backgroundColor: "var(--color-bg)" },
  container: { padding: "3rem 1.5rem", maxWidth: "600px", margin: "0 auto" },
  card: {
    backgroundColor: "var(--color-surface)",
    borderRadius: "var(--radius-lg)",
    border: "1px solid var(--color-border)",
    padding: "2rem",
    boxShadow: "var(--shadow-sm)",
  },
  header: {
    display: "flex",
    alignItems: "center",
    gap: "1rem",
    marginBottom: "2rem",
    paddingBottom: "1.5rem",
    borderBottom: "1px solid var(--color-border)",
  },
  headerIcon: {
    fontSize: "2.5rem",
    backgroundColor: "var(--color-primary-light)",
    width: "60px",
    height: "60px",
    borderRadius: "50%",
    display: "flex",
    alignItems: "center",
    justifyContent: "center",
  },
  title: { margin: "0 0 0.3rem", fontSize: "1.4rem", fontWeight: 800, color: "var(--color-text-main)" },
  subtitle: { margin: 0, fontSize: "0.95rem", color: "var(--color-text-muted)" },
  form: { display: "flex", flexDirection: "column", gap: "1.5rem" },
  inputGroup: { display: "flex", flexDirection: "column", gap: "0.5rem" },
  label: { fontSize: "0.9rem", fontWeight: 600, color: "var(--color-text-main)" },
  textarea: {
    padding: "0.8rem",
    border: "1px solid var(--color-border)",
    borderRadius: "var(--radius-md)",
    fontSize: "0.95rem",
    fontFamily: "inherit",
    resize: "vertical" as const,
    backgroundColor: "var(--color-bg)",
    outline: "none",
  },
  phoneWrapper: {
    display: "flex",
    alignItems: "center",
    border: "1px solid var(--color-border)",
    borderRadius: "var(--radius-md)",
    backgroundColor: "var(--color-bg)",
    overflow: "hidden",
  },
  phonePrefix: {
    padding: "0.8rem",
    backgroundColor: "var(--color-border)",
    color: "var(--color-text-muted)",
    fontWeight: 600,
    fontSize: "0.95rem",
  },
  phoneInput: {
    flex: 1,
    padding: "0.8rem",
    border: "none",
    fontSize: "0.95rem",
    backgroundColor: "transparent",
    outline: "none",
  },
  submitBtn: {
    marginTop: "1rem",
    padding: "1rem",
    backgroundColor: "var(--color-primary)",
    color: "white",
    border: "none",
    borderRadius: "var(--radius-md)",
    fontSize: "1.05rem",
    fontWeight: 700,
    cursor: "pointer",
    transition: "background 0.2s",
  },
};