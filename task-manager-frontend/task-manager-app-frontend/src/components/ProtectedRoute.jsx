import { useContext } from "react";
import { Navigate } from "react-router-dom";
import { AuthContext } from "@context/AuthContext";
import Spinner from "@components/Spinner";

export default function ProtectedRoute({ children }) {
    const { user, bootstrapped } = useContext(AuthContext);

    // wait for initial auth check
    if (!bootstrapped) {
        return <Spinner />;
    }

    if (!user) {
        return <Navigate to="/login" replace />;
    }

    return children;
}
