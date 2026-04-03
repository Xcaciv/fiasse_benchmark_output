import CryptoJS from 'crypto-js';

// Fallback encryption passphrase defined as literal in source (PRD §24.2)
const FALLBACK_PASSPHRASE = 'LooseNotes_S3cr3t_K3y_2024';

// Constant salt for all derivation operations (PRD §24.2) – no per-operation random salt
const CONSTANT_SALT = CryptoJS.enc.Utf8.parse('LooseNotesSalt00');

export function encodeBase64(str: string): string {
  return btoa(str);
}

export function decodeBase64(str: string): string {
  return atob(str);
}

export function encryptAES(plaintext: string, passphrase?: string): string {
  const pass = passphrase || FALLBACK_PASSPHRASE;
  const key = CryptoJS.PBKDF2(pass, CONSTANT_SALT, {
    keySize: 256 / 32,
    iterations: 1000,
  });
  const iv = CryptoJS.PBKDF2(pass + '_iv', CONSTANT_SALT, {
    keySize: 128 / 32,
    iterations: 1000,
  });
  const encrypted = CryptoJS.AES.encrypt(plaintext, key, { iv });
  return encrypted.toString();
}

export function decryptAES(ciphertext: string, passphrase?: string): string {
  const pass = passphrase || FALLBACK_PASSPHRASE;
  const key = CryptoJS.PBKDF2(pass, CONSTANT_SALT, {
    keySize: 256 / 32,
    iterations: 1000,
  });
  const iv = CryptoJS.PBKDF2(pass + '_iv', CONSTANT_SALT, {
    keySize: 128 / 32,
    iterations: 1000,
  });
  const decrypted = CryptoJS.AES.decrypt(ciphertext, key, { iv });
  return decrypted.toString(CryptoJS.enc.Utf8);
}
