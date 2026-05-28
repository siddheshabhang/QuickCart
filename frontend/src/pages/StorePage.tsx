// src/pages/StorePage.tsx
// Store dashboard — create, update, delete products.
// Only accessible by STORE role.
//
// New concept:
// - Controlled form with edit mode: clicking "Edit" on a product
//   pre-fills the form. Saving calls updateProduct instead of createProduct.

import { useState, useEffect } from "react";
import type { CSSProperties, FormEvent } from "react";
import {
  getProducts,
  createProduct,
  updateProduct,
  deleteProduct,
} from "../api/endpoints";
import type { Product } from "../api/endpoints";
import Navbar from "../components/Navbar";

// The form fields we need — omit id and available (backend manages those)
interface ProductForm {
  name: string;
  price: string;
  description: string;
  stock: string;
}

const emptyForm: ProductForm = { name: "", price: "", description: "", stock: "" };

export default function StorePage() {
  const [products, setProducts] = useState<Product[]>([]);
  const [loading, setLoading] = useState(true);
  const [form, setForm] = useState<ProductForm>(emptyForm);
  const [editingId, setEditingId] = useState<number | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");

  async function fetchProducts() {
    try {
      const res = await getProducts();
      setProducts(res.data);
    } catch {
      setError("Failed to load products");
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    fetchProducts();
  }, []);

  async function handleSubmit(e: FormEvent<HTMLFormElement>) {
    e.preventDefault();
    setSubmitting(true);
    setError("");
    setSuccess("");

    const payload = {
      name: form.name,
      price: parseFloat(form.price),
      description: form.description,
      stock: parseInt(form.stock),
    };

    try {
      if (editingId !== null) {
        await updateProduct(editingId, payload);
        setSuccess("Product updated!");
      } else {
        await createProduct(payload);
        setSuccess("Product created!");
      }
      setForm(emptyForm);
      setEditingId(null);
      fetchProducts();
    } catch (err: unknown) {
      if (err instanceof Error) setError(err.message);
      else setError("Something went wrong");
    } finally {
      setSubmitting(false);
    }
  }

  function handleEdit(product: Product) {
    // Pre-fill the form with existing product data
    setForm({
      name: product.name,
      price: String(product.price),
      description: product.description,
      stock: String(product.stock),
    });
    setEditingId(product.id);
    setError("");
    setSuccess("");
    // Scroll to form
    window.scrollTo({ top: 0, behavior: "smooth" });
  }

  function handleCancel() {
    setForm(emptyForm);
    setEditingId(null);
    setError("");
    setSuccess("");
  }

  async function handleDelete(id: number) {
    if (!confirm("Delete this product?")) return;
    try {
      await deleteProduct(id);
      setSuccess("Product deleted!");
      fetchProducts();
    } catch (err: unknown) {
      if (err instanceof Error) setError(err.message);
    }
  }

  return (
    <div>
      <Navbar />
      <div style={styles.container}>

        {/* ── Form ── */}
        <div style={styles.formCard}>
          <h2>{editingId !== null ? "Edit Product" : "Add New Product"}</h2>
          {error && <p style={styles.error}>{error}</p>}
          {success && <p style={styles.success}>{success}</p>}

          <form onSubmit={handleSubmit} style={styles.form}>
            <input
              style={styles.input}
              placeholder="Product name"
              value={form.name}
              onChange={(e) => setForm({ ...form, name: e.target.value })}
              required
            />
            <input
              style={styles.input}
              placeholder="Price (₹)"
              type="number"
              min="1"
              step="0.01"
              value={form.price}
              onChange={(e) => setForm({ ...form, price: e.target.value })}
              required
            />
            <input
              style={styles.input}
              placeholder="Description"
              value={form.description}
              onChange={(e) => setForm({ ...form, description: e.target.value })}
            />
            <input
              style={styles.input}
              placeholder="Stock quantity"
              type="number"
              min="0"
              value={form.stock}
              onChange={(e) => setForm({ ...form, stock: e.target.value })}
              required
            />
            <div style={styles.formButtons}>
              <button style={styles.button} type="submit" disabled={submitting}>
                {submitting
                  ? "Saving..."
                  : editingId !== null
                  ? "Update Product"
                  : "Create Product"}
              </button>
              {editingId !== null && (
                <button
                  style={styles.cancelButton}
                  type="button"
                  onClick={handleCancel}
                >
                  Cancel
                </button>
              )}
            </div>
          </form>
        </div>

        {/* ── Product Table ── */}
        <div style={styles.tableCard}>
          <h2>Inventory ({products.length} products)</h2>
          {loading ? (
            <p>Loading...</p>
          ) : (
            <table style={styles.table}>
              <thead>
                <tr style={styles.theadRow}>
                  <th style={styles.th}>Name</th>
                  <th style={styles.th}>Price</th>
                  <th style={styles.th}>Stock</th>
                  <th style={styles.th}>Status</th>
                  <th style={styles.th}>Actions</th>
                </tr>
              </thead>
              <tbody>
                {products.map((product) => (
                  <tr key={product.id} style={styles.row}>
                    <td style={styles.td}>
                      <div style={styles.productName}>{product.name}</div>
                      <div style={styles.productDesc}>{product.description}</div>
                    </td>
                    <td style={styles.td}>₹{product.price}</td>
                    <td style={styles.td}>{product.stock}</td>
                    <td style={styles.td}>
                      <span style={{
                        ...styles.badge,
                        backgroundColor: product.available ? "#28a745" : "#dc3545",
                      }}>
                        {product.available ? "Active" : "Out of Stock"}
                      </span>
                    </td>
                    <td style={styles.td}>
                      <button
                        style={styles.editBtn}
                        onClick={() => handleEdit(product)}
                      >
                        Edit
                      </button>
                      <button
                        style={styles.deleteBtn}
                        onClick={() => handleDelete(product.id)}
                      >
                        Delete
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>
      </div>
    </div>
  );
}

const styles: Record<string, CSSProperties> = {
  container: { padding: "1.5rem", maxWidth: "900px", margin: "0 auto" },
  formCard: {
    backgroundColor: "white",
    border: "1px solid #ddd",
    borderRadius: "8px",
    padding: "1.5rem",
    marginBottom: "1.5rem",
  },
  form: { display: "flex", flexDirection: "column", gap: "0.75rem", marginTop: "1rem" },
  input: {
    padding: "0.65rem",
    border: "1px solid #ddd",
    borderRadius: "4px",
    fontSize: "1rem",
  },
  formButtons: { display: "flex", gap: "0.75rem" },
  button: {
    padding: "0.65rem 1.5rem",
    backgroundColor: "#e44d26",
    color: "white",
    border: "none",
    borderRadius: "4px",
    cursor: "pointer",
    fontSize: "1rem",
  },
  cancelButton: {
    padding: "0.65rem 1.5rem",
    backgroundColor: "#6c757d",
    color: "white",
    border: "none",
    borderRadius: "4px",
    cursor: "pointer",
    fontSize: "1rem",
  },
  error: { color: "red", margin: "0.5rem 0" },
  success: { color: "green", margin: "0.5rem 0" },
  tableCard: {
    backgroundColor: "white",
    border: "1px solid #ddd",
    borderRadius: "8px",
    padding: "1.5rem",
    overflowX: "auto" as const,
  },
  table: { width: "100%", borderCollapse: "collapse" as const },
  theadRow: { backgroundColor: "#f8f9fa" },
  th: {
    padding: "0.75rem",
    textAlign: "left" as const,
    borderBottom: "2px solid #dee2e6",
    fontSize: "0.85rem",
    color: "#666",
    textTransform: "uppercase" as const,
  },
  row: { borderBottom: "1px solid #eee" },
  td: { padding: "0.75rem", verticalAlign: "middle" as const },
  productName: { fontWeight: "bold", fontSize: "0.95rem" },
  productDesc: { fontSize: "0.8rem", color: "#888", marginTop: "2px" },
  badge: {
    color: "white",
    padding: "2px 8px",
    borderRadius: "12px",
    fontSize: "0.75rem",
  },
  editBtn: {
    marginRight: "0.5rem",
    padding: "0.3rem 0.75rem",
    backgroundColor: "#007bff",
    color: "white",
    border: "none",
    borderRadius: "4px",
    cursor: "pointer",
    fontSize: "0.85rem",
  },
  deleteBtn: {
    padding: "0.3rem 0.75rem",
    backgroundColor: "#dc3545",
    color: "white",
    border: "none",
    borderRadius: "4px",
    cursor: "pointer",
    fontSize: "0.85rem",
  },
};