import { Link } from "react-router-dom";
import { FiMail, FiGithub } from "react-icons/fi";
import "@styles/footer/Footer.css";

export default function Footer() {
    return (
        <footer className="footer">
            <div className="footer-links">
                <Link to="/terms-of-service">Terms of service</Link>
                <span className="footer-dot">•</span>
                <Link to="/cookie-policy">Cookie policy</Link>
            </div>
            <div className="footer-contact">
                <span className="footer-contact-label">Contact us</span>
                <div className="footer-icons">
                    <a href="mailto:rebuildarch5@gmail.com" title="Email" className="footer-icon-link">
                        <FiMail size={14} />
                    </a>
                    <a href="https://github.com/Balasis" target="_blank" rel="noopener noreferrer" title="GitHub" className="footer-icon-link">
                        <FiGithub size={14} />
                    </a>
                </div>
            </div>
            <div className="footer-copyright">
                &copy; {new Date().getFullYear()} Task Manager. All rights reserved.
            </div>
        </footer>
    );
}