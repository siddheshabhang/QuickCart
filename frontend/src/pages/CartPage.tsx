// src/pages/CartPage.tsx
import { useState, useEffect } from "react";
import type { CSSProperties } from "react";
import { useNavigate } from "react-router-dom";
import { getCart, removeCartItem, updateCartItem } from "../api/endpoints";
import type { Cart } from "../api/endpoints";
import Navbar from "../components/Navbar";
import { useToast } from "../context/ToastContext";

export default function CartPage() {
  const [cart, setCart] = useState<Cart | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const navigate = useNavigate();
  const { showToast } = useToast();

  async function fetchCart() {
    try {
      const res = await getCart();
      setCart(res.data);
    } catch {
      setError("Failed to load cart");
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    fetchCart();
  }, []);

  async function handleRemove(cartItemId: number, name: string) {
    try {
      await removeCartItem(cartItemId);
      showToast(`Removed ${name}`);
      fetchCart();
    } catch {
      showToast("Failed to remove item", "error");
    }
  }

  async function handleQuantityChange(cartItemId: number, newQty: number, name: string) {
    if (newQty < 1) {
      await handleRemove(cartItemId, name);
      return;
    }
    try {
      await updateCartItem(cartItemId, newQty);
      fetchCart();
    } catch {
      showToast("Failed to update quantity", "error");
    }
  }

  if (loading) {
    return (
      <div style={s.page}>
        <Navbar />
        <div style={s.loadingWrap}>Loading cart...</div>
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

  const isEmpty = !cart || cart.items.length === 0;

  return (
    <div style={s.page}>
      <Navbar />
      <div style={s.container}>
        <h2 style={s.title}>Your Cart</h2>

        {isEmpty ? (
          <div style={s.empty}>
            <div style={s.emptyIcon}>🛍️</div>
            <h3 style={s.emptyTitle}>Your cart is empty</h3>
            <p style={s.emptySubtitle}>Looks like you haven't added anything yet.</p>
            <button style={s.primaryBtn} onClick={() => navigate("/products")}>
              Browse Products
            </button>
          </div>
        ) : (
          <div style={s.grid}>
            {/* Left Column: Items */}
            <div style={s.itemsList}>
              <div style={s.itemsHeader}>
                <span>{cart.items.length} item(s)</span>
              </div>
              
              {cart.items.map((item) => (
                <div key={item.cartItemId} style={s.itemCard}>
                  <div style={s.itemImage}>🛒</div>
                  <div style={s.itemDetails}>
                    <p style={s.itemName}>{item.productName}</p>
                    <p style={s.itemPrice}>₹{item.price.toFixed(2)}</p>
                  </div>
                  
                  <div style={s.itemActions}>
                    <div style={s.stepper}>
                      <button style={s.stepBtn} onClick={() => handleQuantityChange(item.cartItemId, item.quantity - 1, item.productName)}>−</button>
                      <span style={s.stepQty}>{item.quantity}</span>
                      <button style={s.stepBtn} onClick={() => handleQuantityChange(item.cartItemId, item.quantity + 1, item.productName)}>+</button>
                    </div>
                    <span style={s.itemTotal}>₹{item.itemTotal.toFixed(2)}</span>
                  </div>
                </div>
              ))}
            </div>

            {/* Right Column: Summary */}
            <div style={s.summaryColumn}>
              <div style={s.summaryCard}>
                <h3 style={s.summaryTitle}>Bill Details</h3>
                
                <div style={s.summaryRow}>
                  <span>Item Total</span>
                  <span>₹{cart.totalAmount.toFixed(2)}</span>
                </div>
                <div style={s.summaryRow}>
                  <span>Delivery Fee</span>
                  <span style={s.freeTag}>FREE</span>
                </div>
                
                <div style={s.summaryTotal}>
                  <span>To Pay</span>
                  <span>₹{cart.totalAmount.toFixed(2)}</span>
                </div>
                
                <button style={s.checkoutBtn} onClick={() => navigate("/checkout")}>
                  Proceed to Checkout
                </button>
              </div>
            </div>
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
  container: { padding: "2rem", maxWidth: "1200px", margin: "0 auto" },
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
  
  // Grid Layout
  grid: {
    display: "flex",
    gap: "2rem",
    flexWrap: "wrap",
    alignItems: "flex-start",
  },
  
  // Items List
  itemsList: {
    flex: "1 1 600px",
    backgroundColor: "var(--color-surface)",
    borderRadius: "var(--radius-lg)",
    border: "1px solid var(--color-border)",
    padding: "1.5rem",
  },
  itemsHeader: {
    fontSize: "1.1rem",
    fontWeight: 700,
    marginBottom: "1rem",
    paddingBottom: "1rem",
    borderBottom: "1px solid var(--color-border)",
  },
  itemCard: {
    display: "flex",
    alignItems: "center",
    padding: "1rem 0",
    borderBottom: "1px solid var(--color-border)",
    gap: "1rem",
  },
  itemImage: {
    width: "60px",
    height: "60px",
    backgroundColor: "var(--color-bg)",
    borderRadius: "var(--radius-md)",
    display: "flex",
    alignItems: "center",
    justifyContent: "center",
    fontSize: "1.5rem",
  },
  itemDetails: { flex: 1 },
  itemName: { margin: 0, fontWeight: 600, fontSize: "1rem", color: "var(--color-text-main)" },
  itemPrice: { margin: "0.2rem 0 0", color: "var(--color-text-muted)", fontSize: "0.9rem" },
  itemActions: {
    display: "flex",
    flexDirection: "column",
    alignItems: "flex-end",
    gap: "0.5rem",
  },
  itemTotal: { fontWeight: 700, fontSize: "1rem", color: "var(--color-text-main)" },
  
  stepper: {
    display: "flex",
    alignItems: "center",
    backgroundColor: "var(--color-accent)",
    borderRadius: "var(--radius-md)",
    overflow: "hidden",
  },
  stepBtn: {
    background: "transparent",
    border: "none",
    color: "white",
    fontSize: "1rem",
    fontWeight: "bold",
    cursor: "pointer",
    padding: "0.3rem 0.6rem",
  },
  stepQty: {
    color: "white",
    fontSize: "0.85rem",
    fontWeight: 700,
    padding: "0 0.2rem",
    minWidth: "16px",
    textAlign: "center",
  },
  
  // Summary Column
  summaryColumn: {
    flex: "0 1 350px",
    position: "sticky",
    top: "90px",
  },
  summaryCard: {
    backgroundColor: "var(--color-surface)",
    borderRadius: "var(--radius-lg)",
    border: "1px solid var(--color-border)",
    padding: "1.5rem",
  },
  summaryTitle: { margin: "0 0 1rem", fontSize: "1.1rem", fontWeight: 700 },
  summaryRow: {
    display: "flex",
    justifyContent: "space-between",
    padding: "0.5rem 0",
    color: "var(--color-text-muted)",
    fontSize: "0.95rem",
  },
  freeTag: { color: "var(--color-success)", fontWeight: 700 },
  summaryTotal: {
    display: "flex",
    justifyContent: "space-between",
    marginTop: "1rem",
    paddingTop: "1rem",
    borderTop: "1px dashed var(--color-border)",
    fontWeight: 800,
    fontSize: "1.1rem",
    color: "var(--color-text-main)",
  },
  checkoutBtn: {
    width: "100%",
    marginTop: "1.5rem",
    padding: "0.8rem",
    backgroundColor: "var(--color-primary)",
    color: "white",
    border: "none",
    borderRadius: "var(--radius-md)",
    fontSize: "1rem",
    fontWeight: 700,
    cursor: "pointer",
    transition: "background 0.2s",
  },
};