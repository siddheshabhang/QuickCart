import { BrowserRouter, Routes, Route, Navigate } from "react-router-dom";
import LoginPage from "./pages/LoginPage";
import ProductsPage from "./pages/ProductsPage";
import CartPage from "./pages/CartPage";
import CheckoutPage from "./pages/CheckoutPage";
import PaymentPage from "./pages/PaymentPage";
import OrdersPage from "./pages/OrdersPage";

function StorePage() { return <div>Store Dashboard - Coming Soon</div>; }
function DeliveryPage() { return <div>Delivery Dashboard - Coming Soon</div>; }

function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/" element={<Navigate to="/login" />} />
        <Route path="/login" element={<LoginPage />} />
        <Route path="/products" element={<ProductsPage />} />
        <Route path="/cart" element={<CartPage />} />
        <Route path="/checkout" element={<CheckoutPage />} />
        <Route path="/payment/:orderId" element={<PaymentPage />} />
        <Route path="/orders" element={<OrdersPage />} />
        <Route path="/store" element={<StorePage />} />
        <Route path="/delivery" element={<DeliveryPage />} />
      </Routes>
    </BrowserRouter>
  );
}

export default App;