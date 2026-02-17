import { useNavigate } from "react-router-dom";
import {
    FiArrowLeft, FiShield, FiInfo, FiLock, FiMoon,
    FiSlash, FiSettings, FiRefreshCw, FiMail
} from "react-icons/fi";
import "@styles/pages/Legal.css";
import "@styles/pages/CookiePolicy.css";

export default function CookiePolicy() {
    const navigate = useNavigate();

    return (
        <div className="legal-page">
            <button onClick={() => navigate(-1)} className="legal-back">
                <FiArrowLeft size={14} /> Back
            </button>

            {/* Hero */}
            <section className="legal-hero">
                <div className="legal-hero-icon"><FiShield size={28} /></div>
                <h1>Cookie Policy</h1>
                <p>Last updated &mdash; February 2026</p>
            </section>

            {/* 1. What are cookies */}
            <section className="legal-section">
                <h2><FiInfo size={18} /> What Are Cookies</h2>
                <p>
                    Cookies are small text files stored on your device when you visit a
                    website. They help the website remember your preferences and improve
                    your experience. This policy also covers similar technologies such as
                    local storage (Web Storage API).
                </p>
            </section>

            {/* 2. How we use cookies */}
            <section className="legal-section">
                <h2><FiLock size={18} /> How We Use Cookies &amp; Local Storage</h2>
                <p>
                    Task Manager uses only <strong>essential cookies and local
                    storage</strong>. We do not use any third-party tracking, advertising,
                    or analytics cookies.
                </p>

                <h3>Authentication Cookies</h3>
                <p>
                    When you sign in, session cookies are used to maintain your
                    authenticated state. These cookies are essential for the Service to
                    function and cannot be disabled while using the application.
                </p>
                <ul>
                    <li><strong>Purpose:</strong> Keep you signed in and verify your
                        identity on each request.</li>
                    <li><strong>Duration:</strong> Session-based (cleared when you close
                        your browser) or as defined by the authentication provider.</li>
                    <li><strong>Type:</strong> Essential / Strictly Necessary.</li>
                </ul>

                <h3>Theme Preference</h3>
                <p>
                    Your display theme preference (light or dark mode) is stored using
                    your browser&rsquo;s local storage.
                </p>
                <ul>
                    <li><strong>Purpose:</strong> Remember your chosen colour scheme
                        across sessions.</li>
                    <li><strong>Storage key:</strong> <code>theme</code></li>
                    <li><strong>Duration:</strong> Persistent until you clear browser
                        data or change the setting.</li>
                    <li><strong>Type:</strong> Functional.</li>
                </ul>
            </section>

            {/* 3. Third-party cookies */}
            <section className="legal-section">
                <h2><FiSlash size={18} /> Third-Party Cookies</h2>
                <p>
                    We do <strong>not</strong> use any third-party cookies. No data is
                    shared with advertisers, analytics providers, or social-media
                    platforms through cookie-based tracking.
                </p>
            </section>

            {/* 4. Managing cookies */}
            <section className="legal-section">
                <h2><FiSettings size={18} /> Managing Cookies</h2>
                <p>
                    You can manage or delete cookies through your browser settings:
                </p>
                <ul>
                    <li><strong>Chrome:</strong> Settings → Privacy and Security →
                        Cookies</li>
                    <li><strong>Firefox:</strong> Settings → Privacy &amp; Security →
                        Cookies</li>
                    <li><strong>Edge:</strong> Settings → Cookies and Site
                        Permissions</li>
                    <li><strong>Safari:</strong> Preferences → Privacy → Cookies</li>
                </ul>
                <div className="legal-highlight">
                    Disabling essential cookies may prevent you from using the Service.
                    Clearing your browser&rsquo;s local storage will reset your theme
                    preference to the default (light mode).
                </div>
            </section>

            {/* 5. Changes */}
            <section className="legal-section">
                <h2><FiRefreshCw size={18} /> Changes to This Policy</h2>
                <p>
                    We may update this Cookie Policy from time to time. Any changes will
                    be reflected on this page with an updated effective date. We encourage
                    you to review this page periodically.
                </p>
            </section>

            {/* 6. Contact */}
            <section className="legal-section">
                <h2><FiMail size={18} /> Contact</h2>
                <p>
                    If you have questions about our use of cookies, please contact us at:
                </p>
                <div className="legal-contact">
                    <FiMail size={18} />
                    <a href="mailto:support@taskmanager.io">support@taskmanager.io</a>
                </div>
            </section>
        </div>
    );
}