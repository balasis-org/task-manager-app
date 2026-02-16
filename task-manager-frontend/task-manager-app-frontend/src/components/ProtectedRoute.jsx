import { useContext } from "react";
import { Navigate } from "react-router-dom";
import { AuthContext } from "@context/AuthContext";
import Spinner from "@components/Spinner";

export default function ProtectedRoute({ children }) {
    const { user, bootstrapped } = useContext(AuthContext);

    // Wait until AuthProvider has finished the initial /me check
    if (!bootstrapped) {
        return <Spinner />;
    }

    // Not logged in → redirect to login
    if (!user) {
        return <Navigate to="/login" replace />;
    }

    return children;
}
