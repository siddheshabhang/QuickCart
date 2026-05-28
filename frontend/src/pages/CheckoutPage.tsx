// src/pages/CheckoutPage.tsx
// Customer enters address + phone, then places the order.
// On success → navigate to payment page with the orderId.

import { useState } from "react";
import type { CSSProperties, FormEvent } from "react";
import { useNavigate } from "react-router-dom";
import { placeOrder } from "../api/endpoints";
import Navbar from "../components/Navbar";

export default function CheckoutPage() {
  const [address, setAddress] = useState("");
  const [phone, setPhone] = useState("");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const navigate = useNavigate();

  async function handleSubmit(e: FormEvent<HTMLFormElement>) {
    e.preventDefault();
    setLoading(true);
    setError("");
    try {
      const res = await placeOrder(address, phone);
      const orderId = res.data.orderId;
      // Pass orderId to payment page via URL
      navigate(`/payment/${orderId}`);
    } catch (err: unknown) {
      if (err instanceof Error) setError(err.message);
      else setError("Failed to place order");
    } finally {
      setLoading(false);
    }
  }

  return (
    <div>
      <Navbar />
      <div style={styles.container}>
        <h2>Checkout</h2>
        {error && <p style={styles.error}>{error}</p>}
        <form onSubmit={handleSubmit} style={styles.form}>
          <label style={styles.label}>Delivery Address</label>
          <textarea
            style={styles.textarea}
            value={address}
            onChange={(e) => setAddress(e.target.value)}
            placeholder="Enter your full address"
            required
            rows={3}
          />
          <label style={styles.label}>Phone Number</label>
          <input
            style={styles.input}
            type="tel"
            value={phone}
            onChange={(e) => setPhone(e.target.value)}
            placeholder="10-digit phone number"
            required
          />
          <button style={styles.button} type="submit" disabled={loading}>
            {loading ? "Placing order..." : "Place Order"}
          </button>
        </form>
      </div>
    </div>
  );
}

const styles: Record<string, CSSProperties> = {
  container: { padding: "1.5rem", maxWidth: "500px", margin: "0 auto" },
  error: { color: "red" },
  form: { display: "flex", flexDirection: "column", gap: "0.75rem", marginTop: "1rem" },
  label: { fontWeight: "bold", fontSize: "0.9rem" },
  input: {
    padding: "0.75rem",
    border: "1px solid #ddd",
    borderRadius: "4px",
    fontSize: "1rem",
  },
  textarea: {
    padding: "0.75rem",
    border: "1px solid #ddd",
    borderRadius: "4px",
    fontSize: "1rem",
    resize: "vertical" as const,
  },
  button: {
    padding: "0.75rem",
    backgroundColor: "#e44d26",
    color: "white",
    border: "none",
    borderRadius: "4px",
    cursor: "pointer",
    fontSize: "1rem",
    marginTop: "0.5rem",
  },
};