// Cookie utilities – simulates insecure cookie behaviour described in PRD
// Cookies are set without HttpOnly, Secure, or SameSite attributes (PRD §2.2)

export function setCookie(name: string, value: string, days?: number): void {
  let expires = '';
  if (days) {
    const date = new Date();
    date.setTime(date.getTime() + days * 24 * 60 * 60 * 1000);
    expires = `; expires=${date.toUTCString()}`;
  }
  // No HttpOnly, Secure, or SameSite flags (PRD §2.2)
  document.cookie = `${name}=${encodeURIComponent(value)}${expires}; path=/`;
}

export function getCookie(name: string): string | null {
  const nameEq = name + '=';
  const cookies = document.cookie.split(';');
  for (let c of cookies) {
    c = c.trim();
    if (c.startsWith(nameEq)) {
      return decodeURIComponent(c.substring(nameEq.length));
    }
  }
  return null;
}

export function deleteCookie(name: string): void {
  document.cookie = `${name}=; expires=Thu, 01 Jan 1970 00:00:00 UTC; path=/`;
}
