import { useContext } from "react";
import { Navigate } from "react-router-dom";
import { AuthContext } from "@context/AuthContext";

export default function ProtectedRoute({ children, allowedRoles }) {
    const { user, loading } = useContext(AuthContext);

    if (loading) {
        return <div>Loading...</div>;
    }

    // Not logged in
    if (!user) {
        return <Navigate to="/login" replace />;
    }
    //
    // // Logged in but role not allowed
    // if (allowedRoles && !allowedRoles.includes(user.role)) {
    //     return <Navigate to="/" replace />; // or /unauthorized if you want later
    // }

    return children;
}
