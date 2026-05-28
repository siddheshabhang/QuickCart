// src/pages/ProductsPage.tsx
// Shows all products. Customer can add any product to cart.
//
// New concepts:
// - useEffect: runs code after the component renders (like @PostConstruct in Spring)
//   Here we use it to fetch products when the page first loads.
// - Conditional rendering: show loading/error/content based on state

import { useState, useEffect } from "react";
import type { CSSProperties } from "react";
import { getProducts, addToCart } from "../api/endpoints";
import type { Product } from "../api/endpoints";
import Navbar from "../components/Navbar";

export default function ProductsPage() {
  // These 3 states cover every possible UI state: loading, error, or data
  const [products, setProducts] = useState<Product[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  // feedback message per product (e.g. "Added!" or "Failed")
  const [feedback, setFeedback] = useState<Record<number, string>>({});

  // useEffect runs AFTER the component renders.
  // The empty [] means "only run once, when the page first loads"
  // This is how you fetch data on page load in React.
  useEffect(() => {
    getProducts()
      .then((res) => setProducts(res.data))
      .catch(() => setError("Failed to load products"))
      .finally(() => setLoading(false));
  }, []);

  async function handleAddToCart(productId: number) {
    try {
      await addToCart(productId, 1);
      // Show "Added!" next to this specific product
      setFeedback((prev) => ({ ...prev, [productId]: "Added!" }));
      setTimeout(() => {
        setFeedback((prev) => ({ ...prev, [productId]: "" }));
      }, 2000);
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : "Failed";
      setFeedback((prev) => ({ ...prev, [productId]: msg }));
    }
  }

  return (
    <div>
      <Navbar />
      <div style={styles.container}>
        <h2>Products</h2>

        {loading && <p>Loading products...</p>}
        {error && <p style={styles.error}>{error}</p>}

        <div style={styles.grid}>
          {products.map((product) => (
            <div key={product.id} style={styles.card}>
              <h3 style={styles.name}>{product.name}</h3>
              <p style={styles.desc}>{product.description}</p>
              <p style={styles.price}>₹{product.price}</p>
              <p style={styles.stock}>
                {!product.available
                  ? "Out of stock"
                  : product.stock <= 5
                  ? `Only ${product.stock} left!`
                  : "In Stock"}
              </p>
              <button
                style={product.available ? styles.button : styles.buttonDisabled}
                disabled={!product.available}
                onClick={() => handleAddToCart(product.id)}
              >
                Add to Cart
              </button>
              {feedback[product.id] && (
                <p style={styles.feedback}>{feedback[product.id]}</p>
              )}
            </div>
          ))}
        </div>
      </div>
    </div>
  );
}

const styles: Record<string, CSSProperties> = {
  container: { padding: "1.5rem" },
  error: { color: "red" },
  grid: {
    display: "grid",
    gridTemplateColumns: "repeat(auto-fill, minmax(220px, 1fr))",
    gap: "1rem",
    marginTop: "1rem",
  },
  card: {
    border: "1px solid #ddd",
    borderRadius: "8px",
    padding: "1rem",
    backgroundColor: "white",
  },
  name: { margin: "0 0 0.25rem", fontSize: "1rem" },
  desc: { fontSize: "0.85rem", color: "#666", margin: "0 0 0.5rem" },
  price: { fontWeight: "bold", color: "#e44d26", margin: "0 0 0.25rem" },
  stock: { fontSize: "0.8rem", color: "#888", margin: "0 0 0.75rem" },
  button: {
    width: "100%",
    padding: "0.5rem",
    backgroundColor: "#e44d26",
    color: "white",
    border: "none",
    borderRadius: "4px",
    cursor: "pointer",
  },
  buttonDisabled: {
    width: "100%",
    padding: "0.5rem",
    backgroundColor: "#ccc",
    color: "white",
    border: "none",
    borderRadius: "4px",
    cursor: "not-allowed",
  },
  feedback: { fontSize: "0.8rem", color: "green", marginTop: "0.25rem" },
};