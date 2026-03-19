
// AES-256-GCM encryption for localStorage group cache.
// key is user.cacheKey (server-generated, rotated on password change).
// PBKDF2 100k iterations derives the AES key, cached in-memory.
// stored format: "<8-char tag>:<base64(iv + ciphertext)>" — the tag
// lets cacheMatchesKey quickly check if the cache belongs to the current user.
const SALT = new TextEncoder().encode("tm-cache-salt-v1");
const IV_LEN = 12;

// rawKey -> CryptoKey  (avoid re-deriving PBKDF2 on every call)
const derivedKeyCache = new Map();

async function deriveKey(rawKey) {
   const cached = derivedKeyCache.get(rawKey);
   if (cached) return cached;

   const keyMaterial = await crypto.subtle.importKey(
      "raw",
      new TextEncoder().encode(rawKey),
      "PBKDF2",
      false,
      ["deriveKey"]
   );
   const key = await crypto.subtle.deriveKey(
      { name: "PBKDF2", salt: SALT, iterations: 100_000, hash: "SHA-256" },
      keyMaterial,
      { name: "AES-GCM", length: 256 },
      false,
      ["encrypt", "decrypt"]
   );
   derivedKeyCache.set(rawKey, key);
   return key;
}

export async function encryptForCache(rawKey, data) {
   const key = await deriveKey(rawKey);
   const iv = crypto.getRandomValues(new Uint8Array(IV_LEN));
   const plaintext = new TextEncoder().encode(JSON.stringify(data));

   const cipherBuf = await crypto.subtle.encrypt(
      { name: "AES-GCM", iv },
      key,
      plaintext
   );

   const combined = new Uint8Array(IV_LEN + cipherBuf.byteLength);
   combined.set(iv, 0);
   combined.set(new Uint8Array(cipherBuf), IV_LEN);

   const tag = rawKey.substring(0, 8);
   const b64 = btoa(String.fromCharCode(...combined));
   return tag + ":" + b64;
}

export async function decryptFromCache(rawKey, stored) {
   if (!stored || typeof stored !== "string") return null;

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

      return null;
   }
}

export function cacheMatchesKey(rawKey, stored) {
   if (!stored || typeof stored !== "string" || !rawKey) return false;
    const tag = rawKey.substring(0, 8);
   return stored.startsWith(tag + ":");
}
