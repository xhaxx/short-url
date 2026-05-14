/**
 * API service layer — all backend communication lives here.
 */
const API_BASE = window.SHORTURL_API_BASE || 'http://localhost:8080';

const api = {
  /**
   * Create a short URL.
   * @param {string} longUrl
   * @param {number} [expireDays]
   * @returns {Promise<{shortCode:string, shortUrl:string, longUrl:string, expireTime:string|null}>}
   */
  async shorten(longUrl, expireDays) {
    const body = { longUrl };
    if (expireDays != null && expireDays > 0) body.expireDays = expireDays;

    const res = await fetch(`${API_BASE}/api/shorten`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(body),
    });

    if (!res.ok) {
      const err = await res.json().catch(() => ({}));
      throw new Error(err.error || `请求失败 (${res.status})`);
    }

    return res.json();
  },

  /**
   * Query short URL info by code.
   * @param {string} shortCode
   * @returns {Promise<{shortCode:string, shortUrl:string, longUrl:string, expireTime:string|null}>}
   */
  async getShortUrl(shortCode) {
    const res = await fetch(`${API_BASE}/api/shorten/${encodeURIComponent(shortCode)}`);

    if (!res.ok) {
      if (res.status === 404) throw new Error('短链接不存在或已过期');
      const err = await res.json().catch(() => ({}));
      throw new Error(err.error || `请求失败 (${res.status})`);
    }

    return res.json();
  },

  /**
   * Get visit statistics for a short URL.
   * @param {string} shortCode
   * @param {number} [days=30]
   * @returns {Promise<{totalPv:number, totalUv:number, dailyStats:Array, regionStats:Array}>}
   */
  async getStats(shortCode, days = 30) {
    const res = await fetch(
      `${API_BASE}/api/stats/${encodeURIComponent(shortCode)}?days=${days}`
    );

    if (!res.ok) {
      const err = await res.json().catch(() => ({}));
      throw new Error(err.error || `请求失败 (${res.status})`);
    }

    return res.json();
  },
};
