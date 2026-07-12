// src/components/Navbar.tsx
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
      <div style={styles.left}>
        <div style={styles.logo} onClick={() => navigate("/products")}>
          🛒 QuickCart
        </div>
        
        {user?.role === "CUSTOMER" && (
          <div style={styles.location}>
            <span style={styles.locationTitle}>Delivering to</span>
            <span style={styles.locationValue}>IIIT Bangalore ▾</span>
          </div>
        )}
      </div>

      <div style={styles.right}>
        {user?.role === "CUSTOMER" && (
          <>
            <button style={styles.link} onClick={() => navigate("/products")}>Home</button>
            <button style={styles.link} onClick={() => navigate("/orders")}>Orders</button>
            <button style={styles.cartBtn} onClick={() => navigate("/cart")}>
              🛍️ Cart
            </button>
          </>
        )}
        
        {user?.role === "STORE" && (
          <button style={styles.link} onClick={() => navigate("/store")}>Dashboard</button>
        )}
        
        {user?.role === "DELIVERY" && (
          <button style={styles.link} onClick={() => navigate("/delivery")}>Deliveries</button>
        )}
        
        <div style={styles.userSection}>
          <span style={styles.roleBadge}>{user?.role}</span>
          <button style={styles.logoutBtn} onClick={handleLogout}>Logout</button>
        </div>
      </div>
    </nav>
  );
}

const styles: Record<string, CSSProperties> = {
  nav: {
    display: "flex",
    alignItems: "center",
    justifyContent: "space-between",
    padding: "0.8rem 2rem",
    backgroundColor: "var(--color-surface)",
    borderBottom: "1px solid var(--color-border)",
    position: "sticky",
    top: 0,
    zIndex: 1000,
    boxShadow: "var(--shadow-sm)",
  },
  left: {
    display: "flex",
    alignItems: "center",
    gap: "2rem",
  },
  logo: {
    fontSize: "1.4rem",
    fontWeight: 800,
    color: "var(--color-primary)",
    cursor: "pointer",
    letterSpacing: "-0.5px",
  },
  location: {
    display: "flex",
    flexDirection: "column",
    cursor: "pointer",
  },
  locationTitle: {
    fontSize: "0.7rem",
    fontWeight: 700,
    color: "var(--color-text-muted)",
    textTransform: "uppercase",
    letterSpacing: "0.5px",
  },
  locationValue: {
    fontSize: "0.9rem",
    fontWeight: 600,
    color: "var(--color-text-main)",
  },
  right: {
    display: "flex",
    alignItems: "center",
    gap: "1.5rem",
  },
  link: {
    background: "none",
    border: "none",
    color: "var(--color-text-main)",
    cursor: "pointer",
    fontSize: "0.95rem",
    fontWeight: 500,
    padding: "0.5rem",
  },
  cartBtn: {
    display: "flex",
    alignItems: "center",
    gap: "0.4rem",
    background: "var(--color-accent)",
    color: "white",
    border: "none",
    padding: "0.5rem 1rem",
    borderRadius: "var(--radius-md)",
    fontSize: "0.95rem",
    fontWeight: 600,
    cursor: "pointer",
    transition: "background 0.2s",
  },
  userSection: {
    display: "flex",
    alignItems: "center",
    gap: "0.75rem",
    marginLeft: "1rem",
    paddingLeft: "1.5rem",
    borderLeft: "1px solid var(--color-border)",
  },
  roleBadge: {
    fontSize: "0.75rem",
    fontWeight: 600,
    backgroundColor: "var(--color-primary-light)",
    color: "var(--color-primary)",
    padding: "0.2rem 0.6rem",
    borderRadius: "var(--radius-xl)",
  },
  logoutBtn: {
    backgroundColor: "transparent",
    border: "1px solid var(--color-border)",
    color: "var(--color-text-muted)",
    padding: "0.3rem 0.8rem",
    borderRadius: "var(--radius-md)",
    cursor: "pointer",
    fontSize: "0.85rem",
    fontWeight: 500,
    transition: "all 0.2s",
  },
};