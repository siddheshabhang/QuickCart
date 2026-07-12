import { useEffect } from "react";
import { useNavigate, useSearchParams } from "react-router-dom";
import { saveTokens, saveStoreId, saveUserAddress } from "../auth/token";
import { getCurrentUser } from "../auth/useAuth";
import { getNearestStore } from "../api/endpoints";
import { reverseGeocode } from "../api/geocoding";

export default function OAuthRedirectPage() {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();

  useEffect(() => {
    // 1. Try React Router's search params
    // 2. Fallback to hash params (if backend appended them as #token=...)
    const getParam = (name: string) => {
      let val = searchParams.get(name);
      if (val) return val;

      const hash = window.location.hash.startsWith("#")
        ? window.location.hash.slice(1)
        : window.location.hash;
      const hashParams = new URLSearchParams(hash);
      return hashParams.get(name);
    };

    const callbackError = getParam("error");
    if (callbackError) {
      navigate(`/login?oauthError=${encodeURIComponent(callbackError)}`, { replace: true });
      return;
    }

    const accessToken = getParam("accessToken") || getParam("token");
    const refreshToken = getParam("refreshToken");

    if (!accessToken) {
      // Debug logging to help identify why token is missing
      console.error("OAuth Redirect Error: Token missing in URL.", window.location.href);
      navigate("/login?oauthError=Google%20login%20did%20not%20return%20a%20token", { replace: true });
      return;
    }

    saveTokens(accessToken, refreshToken);
    const user = getCurrentUser();

    if (user?.role === "STORE") { navigate("/store", { replace: true }); return; }
    if (user?.role === "DELIVERY") { navigate("/delivery", { replace: true }); return; }

    // For customers, resolve the nearest dark store before continuing.
    if (!navigator.geolocation) {
      navigate(
        "/login?oauthError=" +
          encodeURIComponent("Location access is required to use QuickCart. Please use a browser that supports geolocation."),
        { replace: true }
      );
      return;
    }

    navigator.geolocation.getCurrentPosition(
      async (position) => {
        try {
          const lat = position.coords.latitude;
          const lng = position.coords.longitude;

          const storeRes = await getNearestStore(lat, lng);
          if (!storeRes.data.deliverable || !storeRes.data.storeId) {
            navigate(
              "/login?oauthError=" +
                encodeURIComponent(
                  storeRes.data.message || "QuickCart is not available in your area yet. We're expanding soon!"
                ),
              { replace: true }
            );
            return;
          }

          saveStoreId(storeRes.data.storeId);

          const address = await reverseGeocode(lat, lng);
          if (address) saveUserAddress(address);

          navigate("/products", { replace: true });
        } catch {
          navigate(
            "/login?oauthError=" +
              encodeURIComponent("Failed to find your nearest store. Please try again."),
            { replace: true }
          );
        }
      },
      (locErr) => {
        if (locErr.code === 1) {
          // User explicitly denied — send back to login with clear message
          const msg = "Location access is required to find your nearest store. Please allow location access in your browser and try again.";
          navigate(`/login?oauthError=${encodeURIComponent(msg)}`, { replace: true });
        } else {
          // Code 2 (unavailable) or 3 (timeout) — GPS can't get a fix even with permission.
          // Redirect back to login; the LoginPage "manual" phase will take over
          // once they try again (the GPS error flow will show the area input).
          const msg = "We couldn't detect your GPS location. Please log in again and enter your area manually when prompted.";
          navigate(`/login?oauthError=${encodeURIComponent(msg)}`, { replace: true });
        }
      },
      { timeout: 10000 }
    );
  }, [navigate, searchParams]);

  return null;
}
