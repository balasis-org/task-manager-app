import { useNavigate } from "react-router-dom";
import {
    FiArrowLeft, FiFileText, FiInfo, FiUserPlus, FiShield,
    FiUploadCloud, FiAlertCircle, FiEye, FiLock, FiClock,
    FiAlertTriangle, FiRefreshCw, FiMail
} from "react-icons/fi";
import "@styles/pages/Legal.css";
import "@styles/pages/TermsOfService.css";

export default function TermsOfService() {
    const navigate = useNavigate();

    return (
        <div className="legal-page">
            <button onClick={() => navigate(-1)} className="legal-back">
                <FiArrowLeft size={14} /> Back
            </button>

            {/* Hero */}
            <section className="legal-hero">
                <div className="legal-hero-icon"><FiFileText size={28} /></div>
                <h1>Terms of Service</h1>
                <p>Last updated &mdash; February 2026</p>
            </section>

            {/* 1. Introduction */}
            <section className="legal-section">
                <h2><FiInfo size={18} /> Introduction &amp; Acceptance</h2>
                <p>
                    By accessing or using Task Manager (&ldquo;the Service&rdquo;), you
                    agree to be bound by these Terms of Service. If you do not agree with
                    any part of these terms, please do not use the Service.
                </p>
            </section>

            {/* 2. Service Description */}
            <section className="legal-section">
                <h2><FiInfo size={18} /> Service Description</h2>
                <p>
                    Task Manager is a collaborative task-management platform developed as
                    part of a university thesis project. It enables teams to create
                    groups, assign tasks, track progress with review workflows, share
                    files, and communicate through comments.
                </p>
            </section>

            {/* 3. Accounts */}
            <section className="legal-section">
                <h2><FiUserPlus size={18} /> Accounts &amp; Registration</h2>
                <p>
                    When you sign in through Azure Active Directory or any other
                    supported authentication method, an account is <strong>automatically
                    created</strong> for you. You are responsible for maintaining the
                    security of your account credentials and for all activities that occur
                    under your account.
                </p>
                <p>
                    By signing in, you consent to the creation of a user profile using the
                    information provided by your authentication provider (e.g.&nbsp;name
                    and email address).
                </p>
            </section>

            {/* 4. Acceptable Use */}
            <section className="legal-section">
                <h2><FiShield size={18} /> Acceptable Use</h2>
                <p>You agree not to:</p>
                <ul>
                    <li>Upload, share, or transmit content that is illegal, harmful,
                        threatening, abusive, harassing, defamatory, obscene, or
                        otherwise objectionable.</li>
                    <li>Attempt to gain unauthorised access to any part of the Service
                        or its systems.</li>
                    <li>Use the Service for any purpose that violates applicable laws or
                        regulations.</li>
                    <li>Interfere with or disrupt the Service or its infrastructure.</li>
                    <li>Impersonate any person or entity.</li>
                </ul>
            </section>

            {/* 5. User Content & Files */}
            <section className="legal-section">
                <h2><FiUploadCloud size={18} /> User Content &amp; File Uploads</h2>
                <p>
                    You retain ownership of all content (text, files, images) you upload
                    to the Service. By uploading content, you grant Task Manager a
                    limited, non-exclusive, royalty-free licence to <strong>store,
                    process, display, reproduce, and review</strong> that content as
                    necessary to provide, operate, maintain, and moderate the Service.
                    This licence persists for the duration of your content&rsquo;s
                    presence on the platform and terminates upon its deletion.
                </p>
                <div className="legal-highlight">
                    <strong>Content Licence Notice:</strong> By using the Service you
                    acknowledge that user-generated content&mdash;including task
                    descriptions, comments, and uploaded files&mdash;may be reviewed by
                    platform administrators <strong>solely for the purposes of</strong>{" "}
                    content moderation, security enforcement, platform integrity, and
                    compliance with applicable law. Your content is <strong>never
                    published, sold, or shared with third parties</strong>, and
                    administrators access it only when necessary to maintain a safe and
                    functional platform.
                </div>
                <div className="legal-warning">
                    <strong>Disclaimer:</strong> Task Manager is not responsible for any
                    user-uploaded content. We do not pre-screen all uploaded materials.
                    You are solely responsible for ensuring that any content you upload
                    does not violate the rights of others, including intellectual property
                    rights.
                </div>
                <p>
                    The Service employs <strong>automated content-safety
                    filters</strong> (including Azure AI Content Safety) to detect and
                    prevent the upload of prohibited content, including adult, violent, or
                    otherwise harmful material. Content flagged by these filters may be
                    automatically rejected or removed.
                </p>
                <p>
                    <strong>File upload limits apply.</strong> Each task may have a
                    maximum number of files, and individual files are subject to size
                    restrictions as displayed in the application.
                </p>
            </section>

            {/* 6. Copyright / DMCA */}
            <section className="legal-section">
                <h2><FiAlertCircle size={18} /> Copyright &amp; Takedown Process</h2>
                <p>
                    Task Manager respects the intellectual property rights of others. If
                    you believe that content hosted on the Service infringes your
                    copyright, you may submit a takedown notice containing:
                </p>
                <ol>
                    <li><strong>Identification</strong> of the copyrighted work you claim
                        has been infringed.</li>
                    <li><strong>Identification</strong> of the allegedly infringing
                        material, including its location on the Service (e.g.&nbsp;URL or
                        description sufficient for us to locate it).</li>
                    <li><strong>Your contact information</strong>, including name,
                        address, telephone number, and email address.</li>
                    <li><strong>A good-faith statement</strong> that the use of the
                        material is not authorised by the copyright owner, its agent, or
                        the law.</li>
                    <li><strong>An accuracy statement</strong>, made under penalty of
                        perjury, that the information in the notice is accurate and that
                        you are the copyright owner or are authorised to act on behalf of
                        the owner.</li>
                    <li><strong>Proof of copyright ownership</strong>, such as a copy of
                        the copyright registration, a link to the original published
                        work, or other documentation demonstrating your ownership.</li>
                </ol>

                <div className="legal-highlight">
                    <strong>Submission:</strong> Send your takedown notice to rebuildarch5@gmail.com.
                </div>

                <p>
                    Upon receiving a valid takedown notice, we will review the claim and,
                    if warranted, remove or disable access to the allegedly infringing
                    material within a reasonable time-frame.
                </p>

                <h3>Counter-Notification</h3>
                <p>
                    If you believe your content was removed in error, you may submit a
                    counter-notification including:
                </p>
                <ul>
                    <li>Identification of the material that was removed and its former
                        location.</li>
                    <li>A statement under penalty of perjury that you have a good-faith
                        belief the material was removed by mistake or
                        mis-identification.</li>
                    <li>Your name, address, and telephone number.</li>
                    <li>Consent to the jurisdiction of your local court.</li>
                </ul>
            </section>

            {/* 7. Content Moderation */}
            <section className="legal-section">
                <h2><FiEye size={18} /> Content Moderation</h2>
                <p>
                    The Service utilises automated content-safety systems (including
                    Azure AI Content Safety) to analyse uploaded files and text. Content
                    that violates our guidelines may be automatically filtered, flagged,
                    or removed. We reserve the right to remove any content at our
                    discretion.
                </p>
                <p>
                    In addition to automated systems, platform administrators may
                    review user-generated content&mdash;including task data, comments,
                    and attached files&mdash;<strong>only for the purposes of</strong>{" "}
                    moderation, security, and legal-compliance. Such reviews are
                    conducted on a need-to-know basis and your content is{" "}
                    <strong>never published, shared externally, or used for any purpose
                    beyond operating and safeguarding the platform</strong>.
                    Administrators may edit or remove content that violates these Terms
                    or applicable law.
                </p>
            </section>

            {/* 8. Privacy & Cookies */}
            <section className="legal-section">
                <h2><FiLock size={18} /> Privacy &amp; Cookies</h2>
                <p>
                    We use essential cookies and local storage for authentication session
                    management and user preferences (such as theme selection). We do not
                    use third-party tracking or advertising cookies. For full details,
                    please see our{" "}
                    <a href="/cookie-policy" style={{ color: "var(--link-color)" }}>
                        Cookie Policy
                    </a>.
                </p>
            </section>

            {/* 9. Service Availability */}
            <section className="legal-section">
                <h2><FiClock size={18} /> Service Availability &amp; Limited Lifetime</h2>
                <p>
                    Task Manager is developed as part of an <strong>academic thesis
                    project</strong>. As such:
                </p>
                <ul>
                    <li>The Service is provided on a <strong>best-effort
                        basis</strong> with no guaranteed uptime or availability.</li>
                    <li>The Service <strong>may be discontinued, suspended, or
                        significantly modified</strong> at any time without prior
                        notice.</li>
                    <li>Data stored in the Service <strong>may be
                        deleted</strong> when the project concludes.</li>
                    <li>We recommend that you maintain your own backups of any important
                        data.</li>
                </ul>
                <div className="legal-warning">
                    This is a thesis/academic project and may have a limited operational
                    lifetime. Plan accordingly.
                </div>
            </section>

            {/* 10. Limitation of Liability */}
            <section className="legal-section">
                <h2><FiAlertTriangle size={18} /> Limitation of Liability</h2>
                <p>To the maximum extent permitted by applicable law:</p>
                <ul>
                    <li>The Service is provided <strong>&ldquo;AS IS&rdquo;</strong> and{" "}
                        <strong>&ldquo;AS AVAILABLE&rdquo;</strong> without warranties of
                        any kind, whether express or implied.</li>
                    <li>We shall not be liable for any indirect, incidental, special,
                        consequential, or punitive damages.</li>
                    <li>Our total liability shall not exceed the amount you paid to use
                        the Service (which is zero, as the Service is provided free of
                        charge).</li>
                    <li>We are not responsible for any loss of data, interruption of
                        service, or damage resulting from the use of the Service.</li>
                </ul>
            </section>

            {/* 11. Changes */}
            <section className="legal-section">
                <h2><FiRefreshCw size={18} /> Changes to These Terms</h2>
                <p>
                    We reserve the right to modify these Terms at any time. Continued use
                    of the Service after changes are posted constitutes acceptance of the
                    updated Terms. We encourage you to review this page periodically.
                </p>
            </section>

            {/* 12. Contact */}
            <section className="legal-section">
                <h2><FiMail size={18} /> Contact</h2>
                <p>
                    If you have questions about these Terms, or need to submit a
                    copyright takedown notice, please contact us at:
                </p>
                <div className="legal-contact">
                    <FiMail size={18} />
                    <a href="mailto:support@taskmanager.io">support@taskmanager.io</a>
                </div>
            </section>
        </div>
    );
}