// One typed function per backend endpoint.
// Pages import these — never call fetch directly.

import { apiClient } from "./client";

// ─── Types ────────────────────────────────────────────────────────────────────
// These mirror your Java DTOs exactly.

export interface AuthResponse {
  token: string;
}

export interface Product {
  id: number;
  name: string;
  price: number;
  description: string;
  stock: number;
  available: boolean;
}

export interface CartItem {
  cartItemId: number;
  productId: number;
  productName: string;
  price: number;
  quantity: number;
  itemTotal: number;
}

export interface Cart {
  items: CartItem[];
  totalAmount: number;
}

export interface OrderItem {
  productId: number;
  productName: string;
  price: number;
  quantity: number;
}

export interface Order {
  orderId: number;
  userId: number;
  totalAmount: number;
  items: OrderItem[];
  status: string;
}

export interface Payment {
  paymentId: number;
  orderId: number;
  amount: number;
  status: string;
  transactionId: string;
}

export interface Delivery {
  id: number;
  orderId: number;
  userId: number;
  status: string;
  createdAt: string;
}

// ─── Auth ─────────────────────────────────────────────────────────────────────

export const login = (email: string, password: string) =>
  apiClient<AuthResponse>("POST", "/auth/login", { email, password });

export const register = (name: string, email: string, password: string) =>
  apiClient<AuthResponse>("POST", "/auth/register", { name, email, password });

// ─── Products ─────────────────────────────────────────────────────────────────

export const getProducts = () =>
  apiClient<Product[]>("GET", "/products");

export const filterProducts = (name?: string, minPrice?: number, maxPrice?: number) => {
  const params = new URLSearchParams();

  if (name) params.append("name", name);
  if (minPrice !== undefined) params.append("minPrice", String(minPrice));
  if (maxPrice !== undefined) params.append("maxPrice", String(maxPrice));

  return apiClient<Product[]>("GET", `/products/filter?${params}`);
};

export const createProduct = (data: Omit<Product, "id" | "available">) =>
  apiClient<Product>("POST", "/products", data);

export const updateProduct = (id: number, data: Omit<Product, "id" | "available">) =>
  apiClient<Product>("PUT", `/products/${id}`, data);

export const deleteProduct = (id: number) =>
  apiClient<null>("DELETE", `/products/${id}`);

// ─── Cart ─────────────────────────────────────────────────────────────────────

export const getCart = () =>
  apiClient<Cart>("GET", "/cart");

export const addToCart = (productId: number, quantity: number) =>
  apiClient<null>("POST", "/cart/add", { productId, quantity });

export const updateCartItem = (cartItemId: number, quantity: number) =>
  apiClient<null>("PUT", `/cart/update/${cartItemId}?quantity=${quantity}`);

export const removeCartItem = (cartItemId: number) =>
  apiClient<null>("DELETE", `/cart/remove/${cartItemId}`);

// ─── Orders ───────────────────────────────────────────────────────────────────

export const placeOrder = (address: string, phoneNumber: string) =>
  apiClient<Order>("POST", "/order/place", { address, phoneNumber });

export const getOrders = () =>
  apiClient<Order[]>("GET", "/order");

// ─── Payment ──────────────────────────────────────────────────────────────────

export const processPayment = (
  orderId: number,
  simulateSuccess: boolean,
  idempotencyKey?: string
) =>
  apiClient<Payment>(
    "POST",
    "/payment/process",
    { orderId, simulateSuccess },
    idempotencyKey ? { "X-Idempotency-Key": idempotencyKey } : {}
  );

// ─── Delivery ─────────────────────────────────────────────────────────────────

export const getDeliveries = () =>
  apiClient<Delivery[]>("GET", "/delivery");

export const updateDeliveryStatus = (orderId: number, status: string) =>
  apiClient<null>("PUT", `/delivery/${orderId}/status?status=${status}`);

export const verifyOtp = (orderId: number, otp: string) =>
  apiClient<null>("POST", `/delivery/${orderId}/verify-otp?otp=${otp}`);
