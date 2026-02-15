
import Header from "@components/header/Header";
import Footer from "@components/footer/Footer";
import { useState } from "react";


export default function Layout({ children }) {
    return (
        <div className="layout">
            <Header onOpenSidebar={() => setSidebarOpen(true)} />
            <main className="layout-main">
                {children}
            </main>
            <Footer />
        </div>
    );
}
