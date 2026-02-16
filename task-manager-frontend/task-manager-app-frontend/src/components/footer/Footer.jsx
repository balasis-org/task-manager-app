import { Link } from "react-router-dom";
import "@styles/footer/Footer.css";

export default function Footer() {
    return (
        <footer className="footer">
            <div className="footer-links">
                <Link to="/terms-of-service">Terms of service</Link>
                <span>&amp;</span>
                <Link to="/cookie-policy">Cookie policy</Link>
            </div>
            <div className="footer-contact">
                Contact us
                <div className="footer-icons">
                    {/* placeholders – swap for real icons later */}
                    <span className="footer-icon" title="Email">✉</span>
                    <span className="footer-icon" title="GitHub">⌂</span>
                    <span className="footer-icon" title="LinkedIn">in</span>
                    <span className="footer-icon" title="Other">★</span>
                </div>
            </div>
        </footer>
    );
}