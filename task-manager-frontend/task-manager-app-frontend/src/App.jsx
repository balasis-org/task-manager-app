import { BrowserRouter as Router, Routes, Route } from "react-router-dom";

import Layout from "./Layout/Layout.jsx";
import ProtectedRoute from "@components/ProtectedRoute.jsx";
import AboutUs from "./pages/AboutUs.jsx";
import Comments from "./pages/Comments.jsx";
import CookiePolicy from "./pages/CookiePolicy.jsx";
import Dashboard from "./pages/Dashboard.jsx";
import Invitations from "./pages/Invitations.jsx";
import Login from "./pages/Login.jsx";
import AuthCallback from "./pages/AuthCallback.jsx";
import Settings from "./pages/Settings.jsx";
import Task from "./pages/Task.jsx";
import TermsOfService from "./pages/TermsOfService.jsx";
import AuthProvider from "@context/AuthProvider.jsx";
import GroupProvider from "@context/GroupProvider.jsx";
import { ToastProvider } from "@context/ToastContext.jsx";

export default function App() {
    return (
        <Router>
            <AuthProvider>
                <ToastProvider>
                <GroupProvider>
                <Routes>
                    <Route path="/about-us" element={<ProtectedRoute><Layout><AboutUs /></Layout></ProtectedRoute>} />
                    <Route path="/comments" element={<ProtectedRoute><Layout><Comments /></Layout></ProtectedRoute>} />
                    <Route path="/cookie-policy" element={<CookiePolicy />} />
                    <Route path="/dashboard" element={<ProtectedRoute><Layout><Dashboard /></Layout></ProtectedRoute>} />
                    <Route path="/" element={<ProtectedRoute><Layout><Dashboard /></Layout></ProtectedRoute>} />
                    <Route path="/invitations" element={<ProtectedRoute><Layout><Invitations /></Layout></ProtectedRoute>} />
                    <Route path="/login" element={<Login />} />
                    <Route path="/auth/callback" element={<AuthCallback />} />
                    <Route path="/settings" element={<ProtectedRoute><Layout><Settings /></Layout></ProtectedRoute>} />
                    <Route path="/task" element={<ProtectedRoute><Layout><Task /></Layout></ProtectedRoute>} />
                    <Route path="/group/:groupId/task/:taskId" element={<ProtectedRoute><Layout><Task /></Layout></ProtectedRoute>} />
                    <Route path="/group/:groupId/task/:taskId/comments" element={<ProtectedRoute><Layout><Comments /></Layout></ProtectedRoute>} />
                    <Route path="/terms-of-service" element={<TermsOfService />} />

                </Routes>
                </GroupProvider>
                </ToastProvider>
            </AuthProvider>
        </Router>
    );
}
