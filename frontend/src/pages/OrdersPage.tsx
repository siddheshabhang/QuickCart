// src/pages/OrdersPage.tsx
import { useState, useEffect } from "react";
import type { CSSProperties } from "react";
import { getOrders } from "../api/endpoints";
import type { Order } from "../api/endpoints";
import Navbar from "../components/Navbar";
import StatusBadge from "../components/StatusBadge";
import { useToast } from "../context/ToastContext";
import { useNavigate } from "react-router-dom";

export default function OrdersPage() {
  const [orders, setOrders] = useState<Order[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const { showToast } = useToast();
  const navigate = useNavigate();

  useEffect(() => {
    getOrders()
      .then((res) => {
        // Sort orders descending by orderId so newest is first
        const sorted = res.data.sort((a, b) => b.orderId - a.orderId);
        setOrders(sorted);
      })
      .catch(() => {
        setError("Failed to load orders");
        showToast("Could not load your orders.", "error");
      })
      .finally(() => setLoading(false));
  }, [showToast]);

  if (loading) {
    return (
      <div style={s.page}>
        <Navbar />
        <div style={s.loadingWrap}>Loading orders...</div>
      </div>
    );
  }

  if (error) {
    return (
      <div style={s.page}>
        <Navbar />
        <div style={s.errorWrap}>{error}</div>
      </div>
    );
  }

  return (
    <div style={s.page}>
      <Navbar />
      <div style={s.container}>
        <h2 style={s.title}>My Orders</h2>

        {orders.length === 0 ? (
          <div style={s.empty}>
            <div style={s.emptyIcon}>📦</div>
            <h3 style={s.emptyTitle}>No orders found</h3>
            <p style={s.emptySubtitle}>You haven't placed any orders yet.</p>
            <button style={s.primaryBtn} onClick={() => navigate("/products")}>
              Start Shopping
            </button>
          </div>
        ) : (
          <div style={s.orderList}>
            {orders.map((order) => (
              <div key={order.orderId} style={s.card}>
                <div style={s.cardHeader}>
                  <div style={s.orderHeaderLeft}>
                    <span style={s.orderId}>Order #{order.orderId}</span>
                  </div>
                  <StatusBadge status={order.status} />
                </div>
                
                <div style={s.items}>
                  {order.items.map((item, i) => (
                    <div key={i} style={s.itemRow}>
                      <div style={s.itemInfo}>
                        <span style={s.itemQty}>{item.quantity} ×</span>
                        <span style={s.itemName}>{item.productName}</span>
                      </div>
                      <span style={s.itemPrice}>₹{(item.price * item.quantity).toFixed(2)}</span>
                    </div>
                  ))}
                </div>
                
                <div style={s.cardFooter}>
                  <span style={s.totalLabel}>Total Amount Paid</span>
                  <strong style={s.totalAmount}>₹{order.totalAmount.toFixed(2)}</strong>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
}

const s: Record<string, CSSProperties> = {
  page: { minHeight: "100vh", backgroundColor: "var(--color-bg)" },
  loadingWrap: { padding: "2rem", textAlign: "center" },
  errorWrap: { padding: "2rem", textAlign: "center", color: "var(--color-error)" },
  container: { padding: "2rem", maxWidth: "800px", margin: "0 auto" },
  title: { fontSize: "1.75rem", fontWeight: 800, marginBottom: "1.5rem" },
  
  // Empty State
  empty: {
    display: "flex",
    flexDirection: "column",
    alignItems: "center",
    justifyContent: "center",
    padding: "4rem 2rem",
    backgroundColor: "var(--color-surface)",
    borderRadius: "var(--radius-lg)",
    border: "1px solid var(--color-border)",
  },
  emptyIcon: { fontSize: "4rem", marginBottom: "1rem" },
  emptyTitle: { fontSize: "1.25rem", fontWeight: 700, margin: "0 0 0.5rem" },
  emptySubtitle: { fontSize: "0.95rem", color: "var(--color-text-muted)", margin: "0 0 1.5rem" },
  primaryBtn: {
    padding: "0.75rem 1.5rem",
    backgroundColor: "var(--color-primary)",
    color: "white",
    border: "none",
    borderRadius: "var(--radius-md)",
    fontSize: "1rem",
    fontWeight: 600,
    cursor: "pointer",
  },
  
  orderList: {
    display: "flex",
    flexDirection: "column",
    gap: "1.5rem",
  },
  card: {
    backgroundColor: "var(--color-surface)",
    borderRadius: "var(--radius-lg)",
    border: "1px solid var(--color-border)",
    padding: "1.5rem",
    boxShadow: "var(--shadow-sm)",
  },
  cardHeader: {
    display: "flex",
    justifyContent: "space-between",
    alignItems: "flex-start",
    marginBottom: "1rem",
    paddingBottom: "1rem",
    borderBottom: "1px solid var(--color-border)",
  },
  orderHeaderLeft: {
    display: "flex",
    flexDirection: "column",
  },
  orderId: { fontSize: "1.1rem", fontWeight: 800, color: "var(--color-text-main)" },
  
  items: { display: "flex", flexDirection: "column", gap: "0.75rem", marginBottom: "1.5rem" },
  itemRow: {
    display: "flex",
    justifyContent: "space-between",
    alignItems: "flex-start",
    fontSize: "0.95rem",
  },
  itemInfo: { display: "flex", gap: "0.5rem", alignItems: "baseline" },
  itemQty: { fontWeight: 700, color: "var(--color-text-muted)", minWidth: "30px" },
  itemName: { color: "var(--color-text-main)", fontWeight: 500 },
  itemPrice: { fontWeight: 600, color: "var(--color-text-main)" },
  
  cardFooter: {
    display: "flex",
    justifyContent: "space-between",
    alignItems: "center",
    paddingTop: "1rem",
    borderTop: "1px dashed var(--color-border)",
    backgroundColor: "var(--color-bg)",
    margin: "0 -1.5rem -1.5rem", // extend to edges
    padding: "1rem 1.5rem",
    borderBottomLeftRadius: "var(--radius-lg)",
    borderBottomRightRadius: "var(--radius-lg)",
  },
  totalLabel: { fontSize: "0.95rem", color: "var(--color-text-muted)", fontWeight: 500 },
  totalAmount: { fontSize: "1.2rem", color: "var(--color-text-main)", fontWeight: 800 },
};