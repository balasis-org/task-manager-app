import { useNavigate } from "react-router-dom";
import {
    FiArrowLeft, FiShield, FiInfo, FiLock,
    FiSlash, FiSettings, FiRefreshCw, FiMail
} from "react-icons/fi";
import "@styles/pages/Legal.css";
import "@styles/pages/CookiePolicy.css";
import usePageTitle from "@hooks/usePageTitle";

export default function CookiePolicy() {
    const navigate = useNavigate();

    usePageTitle("Cookie policy");

    return (
        <div className="legal-page">
            <button onClick={() => navigate(-1)} className="legal-back">
                <FiArrowLeft size={14} /> Back
            </button>

            { }
            <section className="legal-hero">
                <div className="legal-hero-icon"><FiShield size={28} /></div>
                <h1>Cookie Policy</h1>
                <p>Last updated &mdash; March 2026</p>
            </section>

            { }
            <section className="legal-section">
                <h2><FiInfo size={18} /> What Are Cookies</h2>
                <p>
                    Cookies are small text files stored on your device when you visit a
                    website. They help the website remember your preferences and improve
                    your experience. This policy also covers similar technologies such as
                    local storage (Web Storage API).
                </p>
            </section>

            { }
            <section className="legal-section">
                <h2><FiLock size={18} /> How We Use Cookies &amp; Local Storage</h2>
                <p>
                    MyTeamTasks uses only <strong>essential cookies and local
                    storage</strong>. We do not use any third-party tracking, advertising,
                    or analytics cookies.
                </p>

                <h3>Authentication Cookies</h3>
                <p>
                    When you sign in, the following HTTP-only cookies are set to
                    maintain your authenticated state. These cookies are essential for
                    the Service to function and cannot be disabled while using the
                    application.
                </p>
                <ul>
                    <li><strong><code>jwt</code></strong> &mdash; A short-lived JSON Web
                        Token used to authenticate each request. Expires after
                        10&nbsp;minutes and is refreshed automatically.</li>
                    <li><strong><code>RefreshKey</code></strong> &mdash; A long-lived
                        refresh credential used to obtain a new JWT without requiring
                        you to sign in again. Expires after 24&nbsp;hours.</li>
                    <li><strong><code>oauth_state</code></strong> &mdash; A one-time
                        token created during the sign-in flow to prevent cross-site
                        request forgery. Expires after 5&nbsp;minutes and is removed
                        immediately after authentication completes.</li>
                </ul>
                <p>
                    All authentication cookies are <strong>HttpOnly</strong>,{" "}
                    <strong>Secure</strong>, and use the <strong>Strict</strong>{" "}
                    (or Lax for oauth_state) SameSite policy. They are not accessible
                    to client-side JavaScript.
                </p>
                <ul>
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
                    <li><strong>Type:</strong> Functional (essential for user
                        preference).</li>
                </ul>

                <h3>Application Cache</h3>
                <p>
                    To provide a faster experience, the application stores encrypted
                    copies of your group data (e.g.&nbsp;group list, group detail,
                    active selection) in your browser&rsquo;s local storage. This data
                    is encrypted with a server-provided key that rotates
                    periodically and is scoped to your user account.
                </p>
                <ul>
                    <li><strong>Purpose:</strong> Show cached group data instantly
                        while fresh data is fetched in the background.</li>
                    <li><strong>Storage keys:</strong> Prefixed
                        with <code>tm_u&lt;userId&gt;_</code></li>
                    <li><strong>Duration:</strong> Persistent until you sign out,
                        the encryption key rotates, or you clear browser data.</li>
                    <li><strong>Type:</strong> Essential / Performance (no personal
                        data is stored in plain text).</li>
                </ul>
            </section>

            { }
            <section className="legal-section">
                <h2><FiSlash size={18} /> Third-Party Cookies</h2>
                <p>
                    We do <strong>not</strong> use any third-party cookies. No data is
                    shared with advertisers, analytics providers, or social-media
                    platforms through cookie-based tracking.
                </p>
            </section>

            { }
            <section className="legal-section">
                <h2><FiSettings size={18} /> Managing Cookies</h2>
                <p>
                    You can manage or delete cookies through your browser settings:
                </p>
                <ul>
                    <li><strong>Chrome:</strong> Settings &rarr; Privacy and Security &rarr;
                        Cookies</li>
                    <li><strong>Firefox:</strong> Settings &rarr; Privacy &amp; Security &rarr;
                        Cookies</li>
                    <li><strong>Edge:</strong> Settings &rarr; Cookies and Site
                        Permissions</li>
                    <li><strong>Safari:</strong> Preferences &rarr; Privacy &rarr; Cookies</li>
                </ul>
                <div className="legal-highlight">
                    Disabling essential cookies will prevent you from using the Service.
                    Clearing your browser&rsquo;s local storage will reset your theme
                    preference and remove the cached group data (it will be re-fetched
                    on your next visit).
                </div>
                <div className="legal-highlight">
                    <strong>No consent banner is required</strong> because every cookie
                    and local-storage entry listed above is strictly necessary for the
                    Service to operate or to remember a preference you explicitly set.
                    We do not use any optional, analytics, or advertising cookies.
                </div>
            </section>

            { }
            <section className="legal-section">
                <h2><FiRefreshCw size={18} /> Changes to This Policy</h2>
                <p>
                    We may update this Cookie Policy from time to time. Any changes will
                    be reflected on this page with an updated effective date. We encourage
                    you to review this page periodically.
                </p>
            </section>

            { }
            <section className="legal-section">
                <h2><FiMail size={18} /> Contact</h2>
                <p>
                    If you have questions about our use of cookies, please contact us at:
                </p>
                <div className="legal-contact">
                    <FiMail size={18} />
                    <a href="mailto:rebuildarch5@gmail.com">rebuildarch5@gmail.com</a>
                </div>
            </section>
        </div>
    );
}
