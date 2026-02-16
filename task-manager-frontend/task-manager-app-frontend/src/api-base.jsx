// Use same-origin `/api` during frontend dev so HttpOnly cookies are accepted.
// The demo serves static files and proxies `/api` to the backend, which keeps cookie
// scope same-origin. Pointing directly at the backend IP makes cookies cross-site
// and they may be rejected by browsers (SameSite/Secure rules).
const DUMMY_SERVER_API = "";
export default DUMMY_SERVER_API;