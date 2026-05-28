// src/components/Navbar.tsx
// Shown on every page after login.
// Displays the user's role and a logout button.

import { useNavigate } from "react-router-dom";
import { removeToken } from "../auth/token";
import { getCurrentUser } from "../auth/useAuth";
import type { CSSProperties } from "react";

export default function Navbar() {
  const navigate = useNavigate();
  const user = getCurrentUser();

  function handleLogout() {
    removeToken();
    navigate("/login");
  }

  return (
    <nav style={styles.nav}>
      <span style={styles.logo} onClick={() => navigate("/products")}>
        🛒 QuickCart
      </span>
      <div style={styles.right}>
        {user?.role === "CUSTOMER" && (
          <>
            <button style={styles.link} onClick={() => navigate("/products")}>Products</button>
            <button style={styles.link} onClick={() => navigate("/cart")}>Cart</button>
            <button style={styles.link} onClick={() => navigate("/orders")}>Orders</button>
          </>
        )}
        {user?.role === "STORE" && (
          <button style={styles.link} onClick={() => navigate("/store")}>Dashboard</button>
        )}
        {user?.role === "DELIVERY" && (
          <button style={styles.link} onClick={() => navigate("/delivery")}>Deliveries</button>
        )}
        <span style={styles.role}>{user?.role}</span>
        <button style={styles.logout} onClick={handleLogout}>Logout</button>
      </div>
    </nav>
  );
}

const styles: Record<string, CSSProperties> = {
  nav: {
    display: "flex",
    alignItems: "center",
    justifyContent: "space-between",
    padding: "0.75rem 1.5rem",
    backgroundColor: "#e44d26",
    color: "white",
  },
  logo: {
    fontSize: "1.2rem",
    fontWeight: "bold",
    cursor: "pointer",
  },
  right: {
    display: "flex",
    alignItems: "center",
    gap: "0.75rem",
  },
  link: {
    background: "none",
    border: "none",
    color: "white",
    cursor: "pointer",
    fontSize: "0.95rem",
  },
  role: {
    fontSize: "0.8rem",
    backgroundColor: "rgba(255,255,255,0.2)",
    padding: "2px 8px",
    borderRadius: "12px",
  },
  logout: {
    backgroundColor: "rgba(0,0,0,0.2)",
    border: "none",
    color: "white",
    padding: "0.4rem 0.8rem",
    borderRadius: "4px",
    cursor: "pointer",
    fontSize: "0.9rem",
  },
};