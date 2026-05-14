/**
 * Application entry point — SPA router, page renderers, and UI helpers.
 */

/* ── History (localStorage) ── */
const HISTORY_KEY = 'shorturl_history';
const MAX_HISTORY = 20;

function loadHistory() {
  try {
    return JSON.parse(localStorage.getItem(HISTORY_KEY)) || [];
  } catch { return []; }
}

function saveToHistory(item) {
  let list = loadHistory();
  // Remove duplicate by shortCode
  list = list.filter(e => e.shortCode !== item.shortCode);
  list.unshift(item);
  if (list.length > MAX_HISTORY) list = list.slice(0, MAX_HISTORY);
  try { localStorage.setItem(HISTORY_KEY, JSON.stringify(list)); } catch { /* quota exceeded */ }
}

/* ── Toast ── */
function showToast(message, type = 'success') {
  const container = document.getElementById('toast-container');
  const toast = document.createElement('div');
  toast.className = `toast toast-${type}`;
  toast.textContent = message;
  container.appendChild(toast);
  // Trigger animation
  requestAnimationFrame(() => toast.classList.add('toast-visible'));
  setTimeout(() => {
    toast.classList.remove('toast-visible');
    setTimeout(() => toast.remove(), 300);
  }, 2500);
}

/* ── Formatting helpers ── */
function formatNumber(n) {
  if (n == null) return '--';
  if (n >= 10000) return (n / 10000).toFixed(1) + '万';
  return n.toLocaleString();
}

function formatDate(dateStr) {
  if (!dateStr) return '--';
  const d = new Date(dateStr);
  return `${d.getMonth() + 1}/${d.getDate()}`;
}

function truncateUrl(url, max = 50) {
  if (!url) return '--';
  return url.length > max ? url.slice(0, max) + '...' : url;
}

/* ── Router ── */
const router = {
  routes: {
    '/': () => renderPage('home', renderHome),
    '/list': () => renderPage('list', renderList),
    '/stats': () => {
      // /stats?code=xxx or /stats/xxx
      const params = new URLSearchParams(window.location.search);
      const code = params.get('code') || window.location.pathname.split('/stats/')[1];
      if (code) renderPage('stats', () => renderStats(code));
      else router.replace('/');
    },
  },

  navigate(path) {
    history.pushState(null, '', path);
    this.dispatch();
  },

  replace(path) {
    history.replaceState(null, '', path);
    this.dispatch();
  },

  dispatch() {
    const path = window.location.pathname;
    // Serve /stats with query or sub-path
    if (path.startsWith('/stats')) {
      this.routes['/stats']();
      return;
    }
    (this.routes[path] || this.routes['/'])();
    // Update active nav
    document.querySelectorAll('[data-active]').forEach(el => {
      const active = el.dataset.active;
      el.classList.toggle('active', path === '/' + active || (path === '/' && active === 'home'));
    });
  },
};

window.addEventListener('popstate', () => router.dispatch());

// Delegate link clicks for SPA navigation
document.addEventListener('click', e => {
  const link = e.target.closest('a[data-link]');
  if (!link) return;
  e.preventDefault();
  router.navigate(link.getAttribute('href'));
});

/* ── Page renderer ── */
function renderPage(name, renderFn) {
  const app = document.getElementById('app');
  const tpl = document.getElementById(`tpl-${name}`);
  if (!tpl) {
    app.innerHTML = '<p style="text-align:center;padding:4rem;">页面未找到</p>';
    document.title = '404 - ShortURL';
    return;
  }
  app.innerHTML = '';
  app.appendChild(tpl.content.cloneNode(true));
  document.title = name === 'home' ? 'ShortURL - 短链接服务' : name === 'list' ? '我的链接 - ShortURL' : '访问统计 - ShortURL';
  window.scrollTo({ top: 0, behavior: 'smooth' });
  renderFn();
}

/* ── Home page ── */
function renderHome() {
  const form = document.getElementById('shorten-form');
  const input = document.getElementById('long-url-input');
  const btn = document.getElementById('shorten-btn');
  const resultSection = document.getElementById('result-section');

  form.addEventListener('submit', async e => {
    e.preventDefault();
    const longUrl = input.value.trim();
    if (!longUrl) return showToast('请输入链接地址', 'error');

    // Basic validation
    let urlToShorten = longUrl;
    if (!/^https?:\/\//i.test(urlToShorten)) urlToShorten = 'https://' + urlToShorten;

    // Loading state
    btn.classList.add('loading');
    btn.disabled = true;

    try {
      const expireDays = parseInt(document.getElementById('expire-days-input').value) || 90;
      const data = await api.shorten(urlToShorten, expireDays);

      // Show result
      document.getElementById('result-url-input').value = data.shortUrl;
      document.getElementById('result-long-url').href = data.longUrl;
      document.getElementById('result-long-url').textContent = truncateUrl(data.longUrl, 60);
      document.getElementById('result-stats-link').href = `/stats?code=${data.shortCode}`;
      resultSection.style.display = '';

      // Save to history
      saveToHistory({
        shortCode: data.shortCode,
        shortUrl: data.shortUrl,
        longUrl: data.longUrl,
        expireTime: data.expireTime,
        createdAt: new Date().toISOString(),
      });

      showToast('短链接生成成功');
      resultSection.scrollIntoView({ behavior: 'smooth', block: 'center' });
    } catch (err) {
      showToast(err.message || '生成失败，请稍后重试', 'error');
    } finally {
      btn.classList.remove('loading');
      btn.disabled = false;
    }
  });

  // Copy button
  document.getElementById('copy-btn').addEventListener('click', () => {
    const input = document.getElementById('result-url-input');
    input.select();
    document.execCommand('copy'); // fallback
    navigator.clipboard?.writeText(input.value);
    const btn = document.getElementById('copy-btn');
    btn.classList.add('copied');
    btn.querySelector('span').textContent = '已复制';
    showToast('已复制到剪贴板');
    setTimeout(() => {
      btn.classList.remove('copied');
      btn.querySelector('span').textContent = '复制';
    }, 2000);
  });

  // Open button
  document.getElementById('open-btn').addEventListener('click', () => {
    window.open(document.getElementById('result-url-input').value, '_blank');
  });
}

/* ── List page ── */
function renderList() {
  const list = loadHistory();
  const empty = document.getElementById('list-empty');
  const cards = document.getElementById('link-cards');

  if (list.length === 0) {
    empty.style.display = '';
    return;
  }

  cards.innerHTML = list.map(item => `
    <div class="link-card">
      <div class="link-card-body">
        <div class="link-card-code">${escapeHtml(item.shortCode)}</div>
        <a href="${escapeHtml(item.shortUrl)}" target="_blank" class="link-card-short" title="${escapeHtml(item.shortUrl)}">${escapeHtml(item.shortUrl)}</a>
        <a href="${escapeHtml(item.longUrl)}" target="_blank" class="link-card-long" title="${escapeHtml(item.longUrl)}">${escapeHtml(truncateUrl(item.longUrl, 55))}</a>
      </div>
      <div class="link-card-actions">
        <button class="btn btn-ghost btn-sm copy-link-btn" data-url="${escapeHtml(item.shortUrl)}" title="复制短链接">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="9" y="9" width="13" height="13" rx="2"/><path d="M5 15H4a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h9a2 2 0 0 1 2 2v1"/></svg>
        </button>
        <a href="/stats?code=${escapeHtml(item.shortCode)}" class="btn btn-ghost btn-sm" data-link title="查看统计">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="18" y1="20" x2="18" y2="10"/><line x1="12" y1="20" x2="12" y2="4"/><line x1="6" y1="20" x2="6" y2="14"/></svg>
        </a>
        <span class="link-card-date" title="创建时间">${formatDate(item.createdAt)}</span>
      </div>
    </div>
  `).join('');

  // Delegate copy clicks
  cards.addEventListener('click', e => {
    const btn = e.target.closest('.copy-link-btn');
    if (!btn) return;
    const url = btn.dataset.url;
    navigator.clipboard?.writeText(url);
    showToast('已复制到剪贴板');
  });
}

/* ── Stats page ── */
let dailyChart = null;
let regionChart = null;

async function renderStats(shortCode) {
  const codeEl = document.getElementById('stats-short-code');
  codeEl.textContent = `短码：${shortCode}`;

  // Load short URL info for metadata
  let urlInfo = null;
  try {
    urlInfo = await api.getShortUrl(shortCode);
    document.getElementById('stat-code').textContent = shortCode;
    document.getElementById('stat-long-url').textContent = truncateUrl(urlInfo.longUrl, 40);
    document.getElementById('stat-long-url').title = urlInfo.longUrl;
  } catch {
    document.getElementById('stat-code').textContent = shortCode;
    document.getElementById('stat-long-url').textContent = '--';
  }

  // Load stats
  await loadStatsData(shortCode, 30);

  // Days selector
  document.getElementById('stats-days-select').addEventListener('change', async e => {
    const days = parseInt(e.target.value);
    await loadStatsData(shortCode, days);
  });
}

async function loadStatsData(shortCode, days) {
  const loadingEl = document.getElementById('stats-loading');
  const errorEl = document.getElementById('stats-error');
  const emptyEl = document.getElementById('stats-empty');
  const ovCards = document.getElementById('overview-cards');
  const chartsRow = document.querySelector('.charts-row');

  // Reset
  loadingEl.style.display = '';
  errorEl.style.display = 'none';
  emptyEl.style.display = 'none';
  ovCards.style.opacity = '0.4';
  chartsRow.style.opacity = '0.4';

  try {
    const data = await api.getStats(shortCode, days);

    loadingEl.style.display = 'none';
    ovCards.style.opacity = '1';
    chartsRow.style.opacity = '1';

    // Check if there's any data
    if (data.totalPv === 0 && data.totalUv === 0) {
      emptyEl.style.display = '';
      ovCards.style.opacity = '1';
      return;
    }

    // Update overview cards
    document.getElementById('stat-pv').textContent = formatNumber(data.totalPv);
    document.getElementById('stat-uv').textContent = formatNumber(data.totalUv);

    // Daily chart
    renderDailyChart(data.dailyStats || []);

    // Region chart
    renderRegionChart(data.regionStats || []);

  } catch (err) {
    loadingEl.style.display = 'none';
    errorEl.style.display = '';
    document.getElementById('stats-error-msg').textContent = err.message;
    ovCards.style.opacity = '1';
  }
}

function renderDailyChart(dailyStats) {
  const ctx = document.getElementById('chart-daily');

  if (dailyChart) dailyChart.destroy();

  const labels = dailyStats.map(d => formatDate(d.statDate));
  const pvData = dailyStats.map(d => d.pv || 0);
  const uvData = dailyStats.map(d => d.uv || 0);

  dailyChart = new Chart(ctx, {
    type: 'line',
    data: {
      labels,
      datasets: [
        {
          label: 'PV',
          data: pvData,
          borderColor: '#6366f1',
          backgroundColor: 'rgba(99, 102, 241, 0.08)',
          fill: true,
          tension: 0.35,
          pointRadius: 2,
          pointHoverRadius: 5,
          borderWidth: 2,
        },
        {
          label: 'UV',
          data: uvData,
          borderColor: '#f59e0b',
          backgroundColor: 'rgba(245, 158, 11, 0.06)',
          fill: true,
          tension: 0.35,
          pointRadius: 2,
          pointHoverRadius: 5,
          borderWidth: 2,
        },
      ],
    },
    options: {
      responsive: true,
      maintainAspectRatio: false,
      interaction: { intersect: false, mode: 'index' },
      plugins: {
        legend: { position: 'bottom', labels: { usePointStyle: true, boxWidth: 8 } },
        tooltip: {
          backgroundColor: '#1e293b',
          titleColor: '#e2e8f0',
          bodyColor: '#cbd5e1',
          padding: 12,
          cornerRadius: 8,
        },
      },
      scales: {
        x: {
          grid: { display: false },
          ticks: { color: '#94a3b8', font: { size: 11 } },
        },
        y: {
          beginAtZero: true,
          grid: { color: '#f1f5f9' },
          ticks: { color: '#94a3b8', font: { size: 11 }, callback: v => formatNumber(v) },
        },
      },
    },
  });
}

function renderRegionChart(regionStats) {
  const ctx = document.getElementById('chart-region');

  if (regionChart) regionChart.destroy();

  // Aggregate by province/city
  const top10 = regionStats.slice(0, 10);
  const labels = top10.map(r => {
    const parts = [r.province, r.city].filter(Boolean);
    return parts.join(' ') || '未知';
  });
  const values = top10.map(r => r.pv || 0);

  const colors = [
    '#6366f1', '#8b5cf6', '#a855f7', '#d946ef', '#ec4899',
    '#f43f5e', '#f97316', '#eab308', '#22c55e', '#14b8a6',
  ];

  regionChart = new Chart(ctx, {
    type: 'bar',
    data: {
      labels,
      datasets: [{
        label: 'PV',
        data: values,
        backgroundColor: labels.map((_, i) => colors[i % colors.length] + 'cc'),
        borderColor: labels.map((_, i) => colors[i % colors.length]),
        borderWidth: 1,
        borderRadius: 6,
      }],
    },
    options: {
      responsive: true,
      maintainAspectRatio: false,
      indexAxis: 'y',
      plugins: {
        legend: { display: false },
        tooltip: {
          backgroundColor: '#1e293b',
          bodyColor: '#cbd5e1',
          padding: 12,
          cornerRadius: 8,
          callbacks: {
            label: ctx => ` ${ctx.raw} 次访问`,
          },
        },
      },
      scales: {
        x: {
          beginAtZero: true,
          grid: { color: '#f1f5f9' },
          ticks: { color: '#94a3b8', font: { size: 11 }, callback: v => formatNumber(v) },
        },
        y: {
          grid: { display: false },
          ticks: { color: '#64748b', font: { size: 11 } },
        },
      },
    },
  });
}

/* ── Util ── */
function escapeHtml(str) {
  const div = document.createElement('div');
  div.textContent = str;
  return div.innerHTML;
}

/* ── Bootstrap ── */
router.dispatch();
