// src/pages/OrdersPage.tsx
// Shows all orders placed by the current customer.

import { useState, useEffect } from "react";
import type { CSSProperties } from "react";
import { getOrders } from "../api/endpoints";
import type { Order } from "../api/endpoints";
import Navbar from "../components/Navbar";

// Map order status to a color for visual clarity
const statusColors: Record<string, string> = {
  CREATED: "#6c757d",
  PAYMENT_PENDING: "#fd7e14",
  CONFIRMED: "#28a745",
  ASSIGNED: "#17a2b8",
  OUT_FOR_DELIVERY: "#007bff",
  DELIVERED: "#28a745",
  FAILED: "#dc3545",
  CANCELLED: "#6c757d",
};

export default function OrdersPage() {
  const [orders, setOrders] = useState<Order[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    getOrders()
      .then((res) => setOrders(res.data))
      .catch(() => setError("Failed to load orders"))
      .finally(() => setLoading(false));
  }, []);

  if (loading) return <div><Navbar /><p style={{ padding: "1.5rem" }}>Loading orders...</p></div>;
  if (error) return <div><Navbar /><p style={{ padding: "1.5rem", color: "red" }}>{error}</p></div>;

  return (
    <div>
      <Navbar />
      <div style={styles.container}>
        <h2>My Orders</h2>

        {orders.length === 0 ? (
          <p>No orders yet.</p>
        ) : (
          orders.map((order) => (
            <div key={order.orderId} style={styles.card}>
              <div style={styles.header}>
                <span style={styles.orderId}>Order #{order.orderId}</span>
                <span style={{
                  ...styles.status,
                  backgroundColor: statusColors[order.status] || "#6c757d",
                }}>
                  {order.status}
                </span>
              </div>
              <div style={styles.items}>
                {order.items.map((item, i) => (
                  <div key={i} style={styles.item}>
                    <span>{item.productName} × {item.quantity}</span>
                    <span>₹{(item.price * item.quantity).toFixed(2)}</span>
                  </div>
                ))}
              </div>
              <div style={styles.total}>
                Total: <strong>₹{order.totalAmount.toFixed(2)}</strong>
              </div>
            </div>
          ))
        )}
      </div>
    </div>
  );
}

const styles: Record<string, CSSProperties> = {
  container: { padding: "1.5rem", maxWidth: "700px", margin: "0 auto" },
  card: {
    border: "1px solid #ddd",
    borderRadius: "8px",
    padding: "1rem",
    marginBottom: "1rem",
    backgroundColor: "white",
  },
  header: {
    display: "flex",
    justifyContent: "space-between",
    alignItems: "center",
    marginBottom: "0.75rem",
  },
  orderId: { fontWeight: "bold" },
  status: {
    color: "white",
    padding: "2px 10px",
    borderRadius: "12px",
    fontSize: "0.8rem",
  },
  items: { borderTop: "1px solid #eee", paddingTop: "0.5rem" },
  item: {
    display: "flex",
    justifyContent: "space-between",
    padding: "0.25rem 0",
    fontSize: "0.9rem",
  },
  total: {
    textAlign: "right",
    marginTop: "0.75rem",
    paddingTop: "0.5rem",
    borderTop: "1px solid #eee",
  },
};