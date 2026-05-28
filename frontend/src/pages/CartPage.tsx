// src/pages/CartPage.tsx
// Shows current cart items with quantity controls.
// "Checkout" navigates to checkout page.

import { useState, useEffect } from "react";
import type { CSSProperties } from "react";
import { useNavigate } from "react-router-dom";
import { getCart, removeCartItem, updateCartItem } from "../api/endpoints";
import type { Cart } from "../api/endpoints";
import Navbar from "../components/Navbar";

export default function CartPage() {
  const [cart, setCart] = useState<Cart | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const navigate = useNavigate();

  // fetchCart is defined outside useEffect so we can call it again after updates
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

  async function handleRemove(cartItemId: number) {
    await removeCartItem(cartItemId);
    fetchCart(); // re-fetch to update totals
  }

  async function handleQuantityChange(cartItemId: number, newQty: number) {
    if (newQty < 1) return;
    await updateCartItem(cartItemId, newQty);
    fetchCart();
  }

  if (loading) return <div><Navbar /><p style={{ padding: "1.5rem" }}>Loading cart...</p></div>;
  if (error) return <div><Navbar /><p style={{ padding: "1.5rem", color: "red" }}>{error}</p></div>;

  const isEmpty = !cart || cart.items.length === 0;

  return (
    <div>
      <Navbar />
      <div style={styles.container}>
        <h2>Your Cart</h2>

        {isEmpty ? (
          <div style={styles.empty}>
            <p>Your cart is empty.</p>
            <button style={styles.button} onClick={() => navigate("/products")}>
              Browse Products
            </button>
          </div>
        ) : (
          <>
            {cart.items.map((item) => (
              <div key={item.cartItemId} style={styles.item}>
                <div style={styles.itemLeft}>
                  <p style={styles.itemName}>{item.productName}</p>
                  <p style={styles.itemPrice}>₹{item.price} each</p>
                </div>
                <div style={styles.itemRight}>
                  {/* Quantity controls */}
                  <button
                    style={styles.qtyBtn}
                    onClick={() => handleQuantityChange(item.cartItemId, item.quantity - 1)}
                  >−</button>
                  <span style={styles.qty}>{item.quantity}</span>
                  <button
                    style={styles.qtyBtn}
                    onClick={() => handleQuantityChange(item.cartItemId, item.quantity + 1)}
                  >+</button>
                  <span style={styles.itemTotal}>₹{item.itemTotal.toFixed(2)}</span>
                  <button style={styles.remove} onClick={() => handleRemove(item.cartItemId)}>
                    Remove
                  </button>
                </div>
              </div>
            ))}

            <div style={styles.footer}>
              <strong>Total: ₹{cart.totalAmount.toFixed(2)}</strong>
              <button style={styles.button} onClick={() => navigate("/checkout")}>
                Proceed to Checkout
              </button>
            </div>
          </>
        )}
      </div>
    </div>
  );
}

const styles: Record<string, CSSProperties> = {
  container: { padding: "1.5rem", maxWidth: "700px", margin: "0 auto" },
  empty: { textAlign: "center", marginTop: "2rem" },
  item: {
    display: "flex",
    justifyContent: "space-between",
    alignItems: "center",
    padding: "0.75rem",
    borderBottom: "1px solid #eee",
  },
  itemLeft: {},
  itemName: { margin: 0, fontWeight: "bold" },
  itemPrice: { margin: 0, color: "#888", fontSize: "0.85rem" },
  itemRight: { display: "flex", alignItems: "center", gap: "0.5rem" },
  qtyBtn: {
    width: "28px",
    height: "28px",
    border: "1px solid #ddd",
    background: "white",
    cursor: "pointer",
    borderRadius: "4px",
    fontSize: "1rem",
  },
  qty: { minWidth: "24px", textAlign: "center" },
  itemTotal: { minWidth: "60px", textAlign: "right", fontWeight: "bold" },
  remove: {
    background: "none",
    border: "none",
    color: "#e44d26",
    cursor: "pointer",
    fontSize: "0.85rem",
  },
  footer: {
    display: "flex",
    justifyContent: "space-between",
    alignItems: "center",
    marginTop: "1.5rem",
    paddingTop: "1rem",
    borderTop: "2px solid #eee",
  },
  button: {
    padding: "0.6rem 1.2rem",
    backgroundColor: "#e44d26",
    color: "white",
    border: "none",
    borderRadius: "4px",
    cursor: "pointer",
    fontSize: "1rem",
  },
};