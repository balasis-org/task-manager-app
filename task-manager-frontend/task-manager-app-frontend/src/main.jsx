import { createRoot } from 'react-dom/client'
import '@styles/index.css'
import App from './App.jsx'

// apply saved theme before first paint to avoid flash
const savedTheme = localStorage.getItem("theme");
if (savedTheme === "dark") {
    document.documentElement.setAttribute("data-theme", "dark");
}

createRoot(document.getElementById('root')).render(<App />);
