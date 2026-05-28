// BrowserRouter enables client-side routing (URL changes without page reload)
// Routes = the routing table
// Route = one URL path mapped to one page component

import { BrowserRouter, Routes, Route, Navigate } from "react-router-dom";
import LoginPage from "./pages/LoginPage";

// Placeholder pages — we'll build these in the next milestones
function ProductsPage() { return <div>Products Page - Coming Soon</div>; }
function StorePage() { return <div>Store Dashboard - Coming Soon</div>; }
function DeliveryPage() { return <div>Delivery Dashboard - Coming Soon</div>; }

function App() {
  return (
    <BrowserRouter>
      <Routes>
        {/* Default route → login page */}
        <Route path="/" element={<Navigate to="/login" />} />
        <Route path="/login" element={<LoginPage />} />
        <Route path="/products" element={<ProductsPage />} />
        <Route path="/store" element={<StorePage />} />
        <Route path="/delivery" element={<DeliveryPage />} />
      </Routes>
    </BrowserRouter>
  );
}

export default App;