// simple encrypt / decrypt for localStorage cache
// uses AES-GCM via the built-in Web Crypto API — no extra deps needed.
//
// the "key" we get from the backend is a plain hex-ish string (UUID without dashes).
// we derive an actual CryptoKey from it with PBKDF2 so we can use AES-GCM properly.
// the salt is fixed per-app — thats fine, the key itself is already random.

const SALT = new TextEncoder().encode("tm-cache-salt-v1");
const IV_LEN = 12; // bytes, standard for AES-GCM

// turn the backend key string into a CryptoKey we can actually use
async function deriveKey(rawKey) {
   const keyMaterial = await crypto.subtle.importKey(
      "raw",
      new TextEncoder().encode(rawKey),
      "PBKDF2",
      false,
      ["deriveKey"]
   );
   return crypto.subtle.deriveKey(
      { name: "PBKDF2", salt: SALT, iterations: 100_000, hash: "SHA-256" },
      keyMaterial,
      { name: "AES-GCM", length: 256 },
      false,
      ["encrypt", "decrypt"]
   );
}

// encrypt a JS object into a base64 string for localStorage
// we prepend a small "tag" (first 8 chars of the raw key) so we can
// quickly check whether the stored blob matches the current key
// without attempting a full decrypt.
export async function encryptForCache(rawKey, data) {
   const key = await deriveKey(rawKey);
   const iv = crypto.getRandomValues(new Uint8Array(IV_LEN));
   const plaintext = new TextEncoder().encode(JSON.stringify(data));

   const cipherBuf = await crypto.subtle.encrypt(
      { name: "AES-GCM", iv },
      key,
      plaintext
   );

   // combine iv + ciphertext into one array
   const combined = new Uint8Array(IV_LEN + cipherBuf.byteLength);
   combined.set(iv, 0);
   combined.set(new Uint8Array(cipherBuf), IV_LEN);

   // store as:  keyTag:base64payload
   const tag = rawKey.substring(0, 8);
   const b64 = btoa(String.fromCharCode(...combined));
   return tag + ":" + b64;
}

// decrypt a localStorage string back into a JS object.
// returns null if the key doesn't match (tag mismatch) or decryption fails.
export async function decryptFromCache(rawKey, stored) {
   if (!stored || typeof stored !== "string") return null;

   // check the tag prefix first — quick bail if key changed
   const tag = rawKey.substring(0, 8);
   if (!stored.startsWith(tag + ":")) return null;

   try {
      const b64 = stored.slice(tag.length + 1);
      const raw = Uint8Array.from(atob(b64), c => c.charCodeAt(0));

      const iv = raw.slice(0, IV_LEN);
      const ciphertext = raw.slice(IV_LEN);

      const key = await deriveKey(rawKey);
      const plainBuf = await crypto.subtle.decrypt(
          { name: "AES-GCM", iv },
         key,
         ciphertext
      );
      return JSON.parse(new TextDecoder().decode(plainBuf));
   } catch {
      // wrong key, corrupted data, whatever — treat as cache miss
      return null;
   }
}

// quick check: does this stored blob belong to the current key?
// avoids a full decrypt just to detect a rotation.
export function cacheMatchesKey(rawKey, stored) {
   if (!stored || typeof stored !== "string" || !rawKey) return false;
    const tag = rawKey.substring(0, 8);
   return stored.startsWith(tag + ":");
}
