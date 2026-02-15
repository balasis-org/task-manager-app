import { BrowserRouter as Router, Routes, Route } from "react-router-dom";

import Layout from "./Layout/Layout.jsx";
import ProtectedRoute from "@components/ProtectedRoute.jsx";
import AboutUs from "./pages/AboutUs.jsx";
import Comments from "./pages/Comments.jsx";
import CookiePolicy from "./pages/CookiePolicy.jsx";
import Dashboard from "./pages/Dashboard.jsx";
import Invitations from "./pages/Invitations.jsx";
import Login from "./pages/Login.jsx";
import Settings from "./pages/Settings.jsx";
import Task from "./pages/Task.jsx";
import TermsOfService from "./pages/TermsOfService.jsx";
import AuthProvider from "@context/AuthProvider.jsx";

export default function App() {
    return (
        <Router>
            <AuthProvider>
                <Routes>
                    <Route path="/about-us" element={<Layout><ProtectedRoute ><AboutUs /></ProtectedRoute></Layout>} />
                    <Route path="/comments" element={<Layout><ProtectedRoute ><Comments /></ProtectedRoute></Layout>} />
                    <Route path="/cookie-policy" element={<Layout><ProtectedRoute ><CookiePolicy /></ProtectedRoute></Layout>} />
                    <Route path="/dashboard" element={<Layout><ProtectedRoute ><Dashboard /></ProtectedRoute></Layout>} />
                    <Route path="/" element={<Layout><ProtectedRoute ><Dashboard /></ProtectedRoute></Layout>} />
                    <Route path="/invitations" element={<Layout><ProtectedRoute ><Invitations /></ProtectedRoute></Layout>} />
                    <Route path="/login" element={<Login />} />
                    <Route path="/settings" element={<Layout><ProtectedRoute ><Settings /></ProtectedRoute></Layout>} />
                    <Route path="/task" element={<Layout><ProtectedRoute ><Task /></ProtectedRoute></Layout>} />
                    <Route path="/terms-of-service" element={<Layout><ProtectedRoute ><TermsOfService /></ProtectedRoute></Layout>} />

                </Routes>
            </AuthProvider>
        </Router>
    );
}
