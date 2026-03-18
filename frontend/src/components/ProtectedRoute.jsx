import { useContext } from "react";
import { Navigate, useLocation } from "react-router-dom";
import { AuthContext } from "@context/AuthContext";
import Spinner from "@components/Spinner";

// auth gate: shows spinner while bootstrapping, redirects to /login if
// no user, saves returnUrl so login can bounce back after auth.
export default function ProtectedRoute({ children }) {
    const { user, bootstrapped } = useContext(AuthContext);
    const location = useLocation();

    if (!bootstrapped) {
        return <Spinner />;
    }

    if (!user) {
        return <Navigate to="/login" state={{ returnUrl: location.pathname + location.search }} replace />;
    }

    return children;
}
