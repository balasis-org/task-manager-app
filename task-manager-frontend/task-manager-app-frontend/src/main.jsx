import { createRoot } from 'react-dom/client'
import '@styles/index.css'
import App from './App.jsx'

const savedTheme = localStorage.getItem("theme");
if (savedTheme === "dark") {
    document.documentElement.setAttribute("data-theme", "dark");
}

createRoot(document.getElementById('root')).render(<App />);
