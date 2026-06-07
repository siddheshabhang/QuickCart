import { useEffect } from "react";
import { useNavigate } from "react-router-dom";
import { saveTokens } from "../auth/token";
import { getCurrentUser } from "../auth/useAuth";

function getCallbackParam(name: string): string | null {
  const searchParams = new URLSearchParams(window.location.search);
  const searchValue = searchParams.get(name);
  if (searchValue) return searchValue;

  const hash = window.location.hash.startsWith("#")
    ? window.location.hash.slice(1)
    : window.location.hash;
  const hashParams = new URLSearchParams(hash);
  return hashParams.get(name);
}

export default function OAuthRedirectPage() {
  const navigate = useNavigate();

  useEffect(() => {
    const callbackError = getCallbackParam("error");
    if (callbackError) {
      navigate(`/login?oauthError=${encodeURIComponent(callbackError)}`, { replace: true });
      return;
    }

    const accessToken = getCallbackParam("accessToken") || getCallbackParam("token");
    const refreshToken = getCallbackParam("refreshToken");

    if (!accessToken) {
      navigate("/login?oauthError=Google%20login%20did%20not%20return%20a%20token", { replace: true });
      return;
    }

    saveTokens(accessToken, refreshToken);
    const user = getCurrentUser();

    if (user?.role === "STORE") navigate("/store", { replace: true });
    else if (user?.role === "DELIVERY") navigate("/delivery", { replace: true });
    else navigate("/products", { replace: true });
  }, [navigate]);

  return null;
}
