import { useState } from "react";
import type { CSSProperties } from "react";
import type { Product } from "../api/endpoints";

interface ProductCardProps {
  product: Product;
  cartQuantity: number;
  onAdd: () => void;
  onUpdateQuantity: (newQty: number) => void;
  categoryIcon?: string;
}

export default function ProductCard({
  product,
  cartQuantity,
  onAdd,
  onUpdateQuantity,
  categoryIcon = "🛒",
}: ProductCardProps) {
  const [hovered, setHovered] = useState(false);
  const lowStock = product.available && product.stock > 0 && product.stock <= 5;

  return (
    <div
      style={{
        ...s.card,
        transform: hovered ? "translateY(-4px)" : "translateY(0)",
        boxShadow: hovered ? "var(--shadow-md)" : "var(--shadow-sm)",
      }}
      onMouseEnter={() => setHovered(true)}
      onMouseLeave={() => setHovered(false)}
    >
      <div style={s.imagePlaceholder}>
        {product.imageUrl ? (
          <img src={product.imageUrl} alt={product.name} style={s.productImage as CSSProperties} />
        ) : (
          <span style={{ fontSize: "3rem" }}>{categoryIcon}</span>
        )}
        {lowStock && <span style={s.lowStockBadge}>Only {product.stock} left!</span>}
        {!product.available && <span style={s.outBadge}>Out of stock</span>}
      </div>

      <div style={s.content}>
        <div style={s.deliveryTag}>⚡ 8 mins</div>
        <h3 style={s.cardName}>{product.name}</h3>
        <p style={s.cardDesc}>{product.description}</p>
        
        <div style={s.cardFooter}>
          <span style={s.cardPrice}>₹{product.price.toFixed(2)}</span>
          
          {!product.available ? (
             <button style={s.outBtn} disabled>Out of stock</button>
          ) : cartQuantity > 0 ? (
            <div style={s.stepper}>
              <button style={s.stepBtn} onClick={() => onUpdateQuantity(cartQuantity - 1)}>−</button>
              <span style={s.stepQty}>{cartQuantity}</span>
              <button style={s.stepBtn} onClick={() => onUpdateQuantity(cartQuantity + 1)}>+</button>
            </div>
          ) : (
            <button style={s.addBtn} onClick={onAdd}>
              + Add
            </button>
          )}
        </div>
      </div>
    </div>
  );
}

const s: Record<string, CSSProperties> = {
  card: {
    position: "relative",
    backgroundColor: "var(--color-surface)",
    borderRadius: "var(--radius-lg)",
    transition: "all 0.2s ease",
    display: "flex",
    flexDirection: "column",
    overflow: "hidden",
    border: "1px solid var(--color-border)",
  },
  imagePlaceholder: {
    height: "140px",
    backgroundColor: "var(--color-bg)",
    display: "flex",
    alignItems: "center",
    justifyContent: "center",
    position: "relative",
    borderBottom: "1px solid var(--color-border)",
    overflow: "hidden",
  },
  productImage: {
    width: "100%",
    height: "100%",
    objectFit: "cover",
  },
  content: {
    padding: "0.75rem",
    display: "flex",
    flexDirection: "column",
    flexGrow: 1,
    gap: "0.3rem",
  },
  deliveryTag: {
    display: "inline-flex",
    alignItems: "center",
    backgroundColor: "var(--color-primary-light)",
    color: "var(--color-primary)",
    fontSize: "0.7rem",
    fontWeight: 700,
    padding: "2px 6px",
    borderRadius: "4px",
    alignSelf: "flex-start",
    marginBottom: "0.2rem",
  },
  lowStockBadge: {
    position: "absolute",
    top: "0.5rem",
    right: "0.5rem",
    backgroundColor: "#FEF3C7",
    color: "#D97706",
    fontSize: "0.65rem",
    fontWeight: 700,
    padding: "2px 6px",
    borderRadius: "var(--radius-sm)",
  },
  outBadge: {
    position: "absolute",
    top: "0.5rem",
    right: "0.5rem",
    backgroundColor: "var(--color-error-bg)",
    color: "var(--color-error)",
    fontSize: "0.65rem",
    fontWeight: 700,
    padding: "2px 6px",
    borderRadius: "var(--radius-sm)",
  },
  cardName: {
    margin: 0,
    fontSize: "0.9rem",
    fontWeight: 600,
    color: "var(--color-text-main)",
    lineHeight: 1.3,
  },
  cardDesc: {
    margin: 0,
    fontSize: "0.75rem",
    color: "var(--color-text-muted)",
    flexGrow: 1,
    lineHeight: 1.4,
    display: "-webkit-box",
    WebkitLineClamp: 2,
    WebkitBoxOrient: "vertical",
    overflow: "hidden",
  },
  cardFooter: {
    display: "flex",
    alignItems: "center",
    justifyContent: "space-between",
    marginTop: "0.5rem",
  },
  cardPrice: {
    fontWeight: 700,
    fontSize: "1rem",
    color: "var(--color-text-main)",
  },
  addBtn: {
    backgroundColor: "var(--color-accent-light, #DCFCE7)",
    color: "var(--color-accent)",
    border: "1px solid var(--color-accent)",
    borderRadius: "var(--radius-md)",
    padding: "0.3rem 1rem",
    fontSize: "0.85rem",
    fontWeight: 700,
    cursor: "pointer",
    transition: "all 0.15s",
  },
  outBtn: {
    backgroundColor: "var(--color-bg)",
    color: "var(--color-text-muted)",
    border: "1px solid var(--color-border)",
    borderRadius: "var(--radius-md)",
    padding: "0.3rem 0.6rem",
    fontSize: "0.75rem",
    fontWeight: 600,
  },
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
};
