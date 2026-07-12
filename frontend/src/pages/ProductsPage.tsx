// src/pages/ProductsPage.tsx
import { useState, useEffect, useCallback, useMemo } from "react";
import type { CSSProperties } from "react";
import { getProducts, filterProducts, addToCart, getCart, updateCartItem, removeCartItem } from "../api/endpoints";
import type { Product, Cart } from "../api/endpoints";
import Navbar from "../components/Navbar";
import ProductCard from "../components/ProductCard";
import { useToast } from "../context/ToastContext";

type SortKey = "price-asc" | "price-desc" | "name-asc" | "name-desc";

const CATEGORIES = [
  { name: "Fruits & Veggies", icon: "🍎", query: "Apple" },
  { name: "Dairy & Breakfast", icon: "🥛", query: "Milk" },
  { name: "Snacks & Munchies", icon: "🍟", query: "Chips" },
  { name: "Cold Drinks", icon: "🥤", query: "Cola" },
  { name: "Sweet Tooth", icon: "🍫", query: "Chocolate" },
  { name: "Bakery", icon: "🍞", query: "Bread" },
];

function useDebounce<T>(value: T, delay: number): T {
  const [debounced, setDebounced] = useState(value);
  useEffect(() => {
    const t = setTimeout(() => setDebounced(value), delay);
    return () => clearTimeout(t);
  }, [value, delay]);
  return debounced;
}

export default function ProductsPage() {
  const { showToast } = useToast();
  
  const [allProducts, setAllProducts] = useState<Product[]>([]);
  const [products, setProducts]       = useState<Product[]>([]);
  const [cart, setCart]               = useState<Cart | null>(null);
  const [loading, setLoading]         = useState(true);
  const [filtering, setFiltering]     = useState(false);
  const [error, setError]             = useState("");

  const [search, setSearch]     = useState("");
  const [minPrice, setMinPrice] = useState("");
  const [maxPrice, setMaxPrice] = useState("");
  const [sort, setSort]         = useState<SortKey>("price-asc");

  const debouncedSearch   = useDebounce(search, 350);
  const debouncedMinPrice = useDebounce(minPrice, 350);
  const debouncedMaxPrice = useDebounce(maxPrice, 350);

  async function fetchCart() {
    try {
      const res = await getCart();
      setCart(res.data);
    } catch {
      // ignore
    }
  }

  useEffect(() => {
    Promise.all([getProducts(), getCart().catch(() => null)])
      .then(([prodRes, cartRes]) => {
        setAllProducts(prodRes.data);
        setProducts(sortProducts(prodRes.data, "price-asc"));
        if (cartRes) setCart(cartRes.data);
      })
      .catch(() => setError("Failed to load products"))
      .finally(() => setLoading(false));
  }, []);

  function sortProducts(list: Product[], key: SortKey): Product[] {
    return [...list].sort((a, b) => {
      if (key === "price-asc")  return a.price - b.price;
      if (key === "price-desc") return b.price - a.price;
      if (key === "name-asc")   return a.name.localeCompare(b.name);
      if (key === "name-desc")  return b.name.localeCompare(a.name);
      return 0;
    });
  }

  const applyFilters = useCallback(async () => {
    const hasName = debouncedSearch.trim().length > 0;
    const hasMin  = debouncedMinPrice !== "" && !isNaN(Number(debouncedMinPrice));
    const hasMax  = debouncedMaxPrice !== "" && !isNaN(Number(debouncedMaxPrice));

    if (!hasName && !hasMin && !hasMax) {
      setProducts(sortProducts(allProducts, sort));
      return;
    }

    setFiltering(true);
    try {
      const res = await filterProducts(
        hasName ? debouncedSearch.trim() : undefined,
        hasMin  ? Number(debouncedMinPrice) : undefined,
        hasMax  ? Number(debouncedMaxPrice) : undefined,
      );
      setProducts(sortProducts(res.data, sort));
    } catch {
      showToast("Filter failed. Please try again.", "error");
    } finally {
      setFiltering(false);
    }
  }, [debouncedSearch, debouncedMinPrice, debouncedMaxPrice, sort, allProducts, showToast]);

  useEffect(() => { applyFilters(); }, [applyFilters]);

  useEffect(() => {
    setProducts((prev) => sortProducts(prev, sort));
  }, [sort]);

  const activeFilters: { label: string; clear: () => void }[] = [];
  if (search)   activeFilters.push({ label: `Name: "${search}"`,  clear: () => setSearch("") });
  if (minPrice) activeFilters.push({ label: `Min ₹${minPrice}`,   clear: () => setMinPrice("") });
  if (maxPrice) activeFilters.push({ label: `Max ₹${maxPrice}`,   clear: () => setMaxPrice("") });

  function clearAllFilters() {
    setSearch("");
    setMinPrice("");
    setMaxPrice("");
  }

  // --- Cart operations ---
  const cartMap = useMemo(() => {
    const map = new Map<number, { cartItemId: number; quantity: number }>();
    cart?.items.forEach((item) => map.set(item.productId, { cartItemId: item.cartItemId, quantity: item.quantity }));
    return map;
  }, [cart]);

  async function handleAdd(productId: number, productName: string) {
    try {
      await addToCart(productId, 1);
      showToast(`Added ${productName} to cart`);
      fetchCart();
    } catch (err: any) {
      showToast(err.message || "Failed to add to cart", "error");
    }
  }

  async function handleUpdateQuantity(productId: number, newQty: number) {
    const cartInfo = cartMap.get(productId);
    if (!cartInfo) return;
    try {
      if (newQty <= 0) {
        await removeCartItem(cartInfo.cartItemId);
      } else {
        await updateCartItem(cartInfo.cartItemId, newQty);
      }
      fetchCart();
    } catch (err: any) {
      showToast(err.message || "Failed to update quantity", "error");
    }
  }

  return (
    <div style={s.page}>
      <Navbar />

      {/* ── Hero Section ── */}
      <div style={s.hero}>
        <div style={s.heroContent}>
          <h1 style={s.heroTitle}>Groceries delivered in minutes</h1>
          <p style={s.heroSub}>Fresh products, live inventory, straight from the nearest dark store.</p>
        </div>
      </div>

      {/* ── Categories ── */}
      <div style={s.categoryScroll}>
        <div style={s.categoryContainer}>
          {CATEGORIES.map((cat) => (
            <div key={cat.name} style={s.categoryCard} onClick={() => setSearch(cat.query)}>
              <div style={s.categoryIcon}>{cat.icon}</div>
              <span style={s.categoryName}>{cat.name}</span>
            </div>
          ))}
        </div>
      </div>

      {/* ── Sticky search + filter bar ── */}
      <div style={s.filterBar}>
        <div style={s.searchWrapper}>
          <span style={s.searchIcon}>🔍</span>
          <input
            id="product-search"
            style={s.searchInput}
            type="text"
            placeholder='Search for "Amul Butter"…'
            value={search}
            onChange={(e) => setSearch(e.target.value)}
          />
          {search && (
            <button style={s.clearBtn} onClick={() => setSearch("")} title="Clear">✕</button>
          )}
        </div>

        <div style={s.priceGroup}>
          <input
            id="min-price"
            style={s.priceInput}
            type="number"
            placeholder="Min ₹"
            min={0}
            value={minPrice}
            onChange={(e) => setMinPrice(e.target.value)}
          />
          <span style={s.priceSep}>–</span>
          <input
            id="max-price"
            style={s.priceInput}
            type="number"
            placeholder="Max ₹"
            min={0}
            value={maxPrice}
            onChange={(e) => setMaxPrice(e.target.value)}
          />
        </div>

        <select
          id="sort-select"
          style={s.sortSelect}
          value={sort}
          onChange={(e) => setSort(e.target.value as SortKey)}
        >
          <option value="price-asc">Price: Low → High</option>
          <option value="price-desc">Price: High → Low</option>
          <option value="name-asc">Name: A → Z</option>
          <option value="name-desc">Name: Z → A</option>
        </select>
      </div>

      {activeFilters.length > 0 && (
        <div style={s.chipsRow}>
          {activeFilters.map((f) => (
            <span key={f.label} style={s.chip}>
              {f.label}
              <button style={s.chipX} onClick={f.clear}>✕</button>
            </span>
          ))}
          <button style={s.clearAll} onClick={clearAllFilters}>Clear all</button>
        </div>
      )}

      <div style={s.resultsHeader}>
        <span style={s.resultsCount}>
          {filtering ? "Searching…" : `${products.length} product${products.length !== 1 ? "s" : ""} found`}
        </span>
      </div>

      {error && <p style={s.error}>{error}</p>}

      {loading && (
        <div style={s.grid}>
          {Array.from({ length: 8 }).map((_, i) => (
            <div key={i} style={s.skeleton} />
          ))}
        </div>
      )}

      {!loading && products.length === 0 && (
        <div style={s.empty}>
          <div style={s.emptyIcon}>🛒</div>
          <p style={s.emptyTitle}>No products found</p>
          <p style={s.emptySubtitle}>Try adjusting your search or filters</p>
          {activeFilters.length > 0 && (
            <button style={s.emptyBtn} onClick={clearAllFilters}>Clear filters</button>
          )}
        </div>
      )}

      {!loading && products.length > 0 && (
        <div style={s.gridContainer}>
          <div style={s.grid}>
            {products.map((product) => {
              const cartInfo = cartMap.get(product.id);
              // Pick an icon based on name
              const icon = CATEGORIES.find(c => product.name.toLowerCase().includes(c.query.toLowerCase()))?.icon || "🛒";
              
              return (
                <ProductCard
                  key={product.id}
                  product={product}
                  categoryIcon={icon}
                  cartQuantity={cartInfo?.quantity || 0}
                  onAdd={() => handleAdd(product.id, product.name)}
                  onUpdateQuantity={(q) => handleUpdateQuantity(product.id, q)}
                />
              );
            })}
          </div>
        </div>
      )}
    </div>
  );
}

const s: Record<string, CSSProperties> = {
  page: {
    minHeight: "100vh",
    backgroundColor: "var(--color-bg)",
  },
  hero: {
    backgroundColor: "var(--color-primary)",
    color: "white",
    padding: "3rem 2rem",
    textAlign: "center",
    backgroundImage: "linear-gradient(to right, var(--color-primary), var(--color-primary-hover))",
  },
  heroContent: {
    maxWidth: "600px",
    margin: "0 auto",
  },
  heroTitle: {
    fontSize: "2.5rem",
    fontWeight: 800,
    margin: "0 0 1rem",
    letterSpacing: "-1px",
  },
  heroSub: {
    fontSize: "1.1rem",
    margin: 0,
    color: "var(--color-primary-light)",
    lineHeight: 1.5,
  },
  categoryScroll: {
    width: "100%",
    overflowX: "auto",
    padding: "1.5rem 2rem",
    backgroundColor: "var(--color-surface)",
    borderBottom: "1px solid var(--color-border)",
    scrollbarWidth: "none",
  },
  categoryContainer: {
    display: "flex",
    gap: "1rem",
    minWidth: "max-content",
  },
  categoryCard: {
    display: "flex",
    flexDirection: "column",
    alignItems: "center",
    justifyContent: "center",
    width: "100px",
    height: "100px",
    backgroundColor: "var(--color-bg)",
    borderRadius: "var(--radius-lg)",
    cursor: "pointer",
    transition: "transform 0.2s, box-shadow 0.2s",
  },
  categoryIcon: {
    fontSize: "2rem",
    marginBottom: "0.5rem",
  },
  categoryName: {
    fontSize: "0.75rem",
    fontWeight: 600,
    color: "var(--color-text-main)",
    textAlign: "center",
    padding: "0 0.5rem",
  },
  filterBar: {
    display: "flex",
    alignItems: "center",
    gap: "0.75rem",
    flexWrap: "wrap",
    padding: "1rem 2rem",
    backgroundColor: "var(--color-surface)",
    borderBottom: "1px solid var(--color-border)",
    position: "sticky",
    top: 60, // below navbar
    zIndex: 100,
  },
  searchWrapper: {
    position: "relative",
    flex: 1,
    minWidth: "250px",
    display: "flex",
    alignItems: "center",
  },
  searchIcon: {
    position: "absolute",
    left: "1rem",
    color: "var(--color-text-muted)",
  },
  searchInput: {
    width: "100%",
    padding: "0.6rem 2.5rem",
    border: "1px solid var(--color-border)",
    borderRadius: "var(--radius-md)",
    fontSize: "0.95rem",
    backgroundColor: "var(--color-bg)",
    outline: "none",
    transition: "border-color 0.2s",
  },
  clearBtn: {
    position: "absolute",
    right: "0.8rem",
    background: "none",
    border: "none",
    color: "var(--color-text-muted)",
    cursor: "pointer",
    padding: "2px",
  },
  priceGroup: {
    display: "flex",
    alignItems: "center",
    gap: "0.5rem",
  },
  priceInput: {
    width: "90px",
    padding: "0.6rem",
    border: "1px solid var(--color-border)",
    borderRadius: "var(--radius-md)",
    fontSize: "0.9rem",
    backgroundColor: "var(--color-bg)",
    outline: "none",
  },
  priceSep: { color: "var(--color-text-muted)" },
  sortSelect: {
    padding: "0.6rem 1rem",
    border: "1px solid var(--color-border)",
    borderRadius: "var(--radius-md)",
    fontSize: "0.9rem",
    backgroundColor: "var(--color-bg)",
    color: "var(--color-text-main)",
    outline: "none",
    cursor: "pointer",
  },
  chipsRow: {
    display: "flex",
    flexWrap: "wrap",
    gap: "0.5rem",
    padding: "0.75rem 2rem",
    backgroundColor: "var(--color-primary-light)",
    borderBottom: "1px solid #E0E7FF",
  },
  chip: {
    display: "flex",
    alignItems: "center",
    gap: "0.4rem",
    backgroundColor: "var(--color-surface)",
    border: "1px solid var(--color-primary)",
    color: "var(--color-primary)",
    padding: "4px 12px",
    borderRadius: "var(--radius-xl)",
    fontSize: "0.8rem",
    fontWeight: 600,
  },
  chipX: {
    background: "none",
    border: "none",
    color: "var(--color-primary)",
    cursor: "pointer",
    fontSize: "0.8rem",
    padding: 0,
  },
  clearAll: {
    background: "none",
    border: "none",
    color: "var(--color-text-muted)",
    cursor: "pointer",
    fontSize: "0.8rem",
    textDecoration: "underline",
  },
  resultsHeader: { padding: "1rem 2rem 0" },
  resultsCount: { fontSize: "0.9rem", color: "var(--color-text-muted)", fontWeight: 500 },
  error: { color: "var(--color-error)", padding: "1rem 2rem" },
  gridContainer: {
    padding: "1rem 2rem 3rem",
    maxWidth: "1400px",
    margin: "0 auto",
  },
  grid: {
    display: "grid",
    gridTemplateColumns: "repeat(auto-fill, minmax(220px, 1fr))",
    gap: "1.5rem",
  },
  skeleton: {
    height: "260px",
    backgroundColor: "var(--color-border)",
    borderRadius: "var(--radius-lg)",
    animation: "pulse 1.5s infinite",
  },
  empty: {
    display: "flex",
    flexDirection: "column",
    alignItems: "center",
    justifyContent: "center",
    padding: "5rem 2rem",
    gap: "1rem",
  },
  emptyIcon: { fontSize: "4rem" },
  emptyTitle: { fontSize: "1.25rem", fontWeight: 700, margin: 0 },
  emptySubtitle: { fontSize: "0.95rem", color: "var(--color-text-muted)", margin: 0 },
  emptyBtn: {
    backgroundColor: "var(--color-primary)",
    color: "white",
    border: "none",
    padding: "0.75rem 1.5rem",
    borderRadius: "var(--radius-md)",
    fontSize: "0.95rem",
    fontWeight: 600,
    cursor: "pointer",
  },
};