const STORAGE_KEY = 'kcff-v1';

const PASS_TYPES = {
  weekly: { label: 'Thẻ tuần', days: 7, immediate: 100, daily: 50 },
  monthly: { label: 'Thẻ tháng', days: 30, immediate: 500, daily: 70 },
};

const emptyState = () => ({
  wallet: 0,
  passes: [],
  expenses: [],
  campaigns: [],
  history: [],
});

let state = loadState();

function loadState() {
  try {
    const parsed = JSON.parse(localStorage.getItem(STORAGE_KEY));
    return parsed && typeof parsed === 'object' ? { ...emptyState(), ...parsed } : emptyState();
  } catch {
    return emptyState();
  }
}

function saveState() {
  localStorage.setItem(STORAGE_KEY, JSON.stringify(state));
  render();
}

function uid(prefix = 'id') {
  return `${prefix}_${Date.now()}_${Math.random().toString(36).slice(2, 8)}`;
}

function localDateKey(date = new Date()) {
  const y = date.getFullYear();
  const m = String(date.getMonth() + 1).padStart(2, '0');
  const d = String(date.getDate()).padStart(2, '0');
  return `${y}-${m}-${d}`;
}

function dateFromKey(key) {
  const [y, m, d] = key.split('-').map(Number);
  return new Date(y, m - 1, d);
}

function addDays(key, days) {
  const d = dateFromKey(key);
  d.setDate(d.getDate() + days);
  return localDateKey(d);
}

function formatNumber(value) {
  return new Intl.NumberFormat('vi-VN').format(Math.max(0, Number(value) || 0));
}

function formatDate(key) {
  if (!key) return '';
  return new Intl.DateTimeFormat('vi-VN', { day: '2-digit', month: '2-digit', year: 'numeric' }).format(dateFromKey(key));
}

function addHistory(kind, title, amount, meta = '') {
  state.history.unshift({ id: uid('h'), kind, title, amount, meta, at: new Date().toISOString() });
  state.history = state.history.slice(0, 120);
}

function getSavedTotal() {
  return state.campaigns.reduce((sum, item) => sum + item.saved, 0);
}

function isPassActive(pass) {
  const today = localDateKey();
  return today >= pass.startDate && today <= addDays(pass.startDate, pass.days - 1);
}

function getPendingFor(type) {
  return state.passes
    .filter(p => p.type === type)
    .reduce((sum, pass) => {
      const cfg = PASS_TYPES[pass.type];
      return sum + Math.max(0, cfg.days - pass.claimedDates.length) * cfg.daily;
    }, 0);
}

function canClaimToday(pass) {
  const today = localDateKey();
  return isPassActive(pass) && !pass.claimedDates.includes(today);
}

function activatePass(type) {
  const cfg = PASS_TYPES[type];
  const pass = {
    id: uid(type),
    type,
    startDate: localDateKey(),
    days: cfg.days,
    claimedDates: [],
  };
  state.passes.unshift(pass);
  state.wallet += cfg.immediate;
  addHistory('in', `Thêm ${cfg.label}`, cfg.immediate, `Nhận ngay • tổng quyền lợi ${formatNumber(cfg.immediate + cfg.daily * cfg.days)} KC`);
  saveState();
  toast(`Đã thêm ${cfg.label}: +${formatNumber(cfg.immediate)} KC ngay.`);
}

function claimPass(passId) {
  const pass = state.passes.find(p => p.id === passId);
  if (!pass) return;
  if (!canClaimToday(pass)) {
    toast('Hôm nay thẻ này đã nhận rồi hoặc đã hết hạn.');
    return;
  }
  const cfg = PASS_TYPES[pass.type];
  const today = localDateKey();
  pass.claimedDates.push(today);
  state.wallet += cfg.daily;
  addHistory('in', `Nhận ngày • ${cfg.label}`, cfg.daily, formatDate(today));
  saveState();
  toast(`+${formatNumber(cfg.daily)} KC từ ${cfg.label}.`);
}

function submitExpense(event) {
  event.preventDefault();
  const name = document.querySelector('#expenseName').value.trim();
  const amount = Math.floor(Number(document.querySelector('#expenseAmount').value));
  const category = document.querySelector('#expenseCategory').value;
  if (!name || !Number.isFinite(amount) || amount <= 0) return;
  if (amount > state.wallet) {
    toast(`KC sẵn chỉ còn ${formatNumber(state.wallet)}. Không thể chi ${formatNumber(amount)} KC.`);
    return;
  }
  state.wallet -= amount;
  state.expenses.unshift({ id: uid('ex'), name, amount, category, date: localDateKey() });
  state.expenses = state.expenses.slice(0, 80);
  addHistory('out', name, -amount, category);
  event.target.reset();
  saveState();
  toast(`Đã ghi chi ${formatNumber(amount)} KC.`);
}

function createCampaign(event) {
  event.preventDefault();
  const name = document.querySelector('#campaignName').value.trim();
  const target = Math.floor(Number(document.querySelector('#campaignTarget').value));
  if (!name || !Number.isFinite(target) || target <= 0) return;
  state.campaigns.unshift({ id: uid('cp'), name, target, saved: 0, created: localDateKey() });
  addHistory('neutral', `Tạo chiến dịch: ${name}`, 0, `Mục tiêu ${formatNumber(target)} KC`);
  event.target.reset();
  saveState();
}

function saveToCampaign(campaignId) {
  const input = document.querySelector(`[data-save-input="${campaignId}"]`);
  const amount = Math.floor(Number(input?.value));
  const campaign = state.campaigns.find(c => c.id === campaignId);
  if (!campaign || !Number.isFinite(amount) || amount <= 0) return;
  if (amount > state.wallet) {
    toast(`Chỉ có ${formatNumber(state.wallet)} KC sẵn để chuyển.`);
    return;
  }
  state.wallet -= amount;
  campaign.saved += amount;
  addHistory('save', `Tiết kiệm • ${campaign.name}`, amount, 'Chuyển từ KC sẵn');
  input.value = '';
  saveState();
  toast(`Đã khóa ${formatNumber(amount)} KC cho “${campaign.name}”.`);
}

function releaseCampaign(campaignId) {
  const campaign = state.campaigns.find(c => c.id === campaignId);
  if (!campaign || campaign.saved <= 0) return;
  const amount = campaign.saved;
  campaign.saved = 0;
  state.wallet += amount;
  addHistory('in', `Hoàn tiết kiệm • ${campaign.name}`, amount, 'Chuyển về KC sẵn');
  saveState();
  toast(`Đã hoàn ${formatNumber(amount)} KC về ví sẵn.`);
}

function deleteCampaign(campaignId) {
  const campaign = state.campaigns.find(c => c.id === campaignId);
  if (!campaign) return;
  if (campaign.saved > 0) {
    state.wallet += campaign.saved;
    addHistory('in', `Đóng chiến dịch • ${campaign.name}`, campaign.saved, 'KC tiết kiệm được hoàn về ví');
  }
  state.campaigns = state.campaigns.filter(c => c.id !== campaignId);
  saveState();
}

function exportData() {
  const blob = new Blob([JSON.stringify(state, null, 2)], { type: 'application/json' });
  const url = URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = `kcff-backup-${localDateKey()}.json`;
  a.click();
  URL.revokeObjectURL(url);
  toast('Đã xuất file sao lưu JSON.');
}

function resetData() {
  if (!confirm('Xóa toàn bộ dữ liệu KCFF trên thiết bị này?')) return;
  state = emptyState();
  saveState();
  toast('Đã đặt lại dữ liệu. Sạch như ví KC sau một vòng quay xui.');
}

function renderPasses() {
  const root = document.querySelector('#activePasses');
  if (!state.passes.length) {
    root.innerHTML = '<div class="empty">Chưa có thẻ nào. Thêm thẻ tuần hoặc thẻ tháng ở phía trên.</div>';
    return;
  }
  root.innerHTML = state.passes.map(pass => {
    const cfg = PASS_TYPES[pass.type];
    const claimed = pass.claimedDates.length;
    const pending = Math.max(0, cfg.days - claimed) * cfg.daily;
    const active = isPassActive(pass);
    const claimable = canClaimToday(pass);
    const status = claimed >= cfg.days ? 'Đã nhận đủ' : active ? 'Đang hoạt động' : 'Đã hết hạn';
    return `
      <div class="pass-row">
        <div>
          <div class="pass-title">${cfg.label} <span class="badge">${status}</span></div>
          <div class="pass-meta">${formatDate(pass.startDate)} → ${formatDate(addDays(pass.startDate, cfg.days - 1))} • Đã nhận ${claimed}/${cfg.days} ngày • Còn ${formatNumber(pending)} KC</div>
        </div>
        <div class="pass-actions">
          <span class="amount in">+${cfg.daily} KC</span>
          <button class="cta" data-claim="${pass.id}" ${claimable ? '' : 'disabled'}>${claimable ? 'Nhận hôm nay' : 'Đã nhận / hết hạn'}</button>
        </div>
      </div>`;
  }).join('');
}

function renderExpenses() {
  const root = document.querySelector('#expenseList');
  const items = state.expenses.slice(0, 8);
  root.innerHTML = items.length ? items.map(item => `
    <div class="list-item">
      <div class="list-main"><strong>${escapeHtml(item.name)}</strong><small>${escapeHtml(item.category)} • ${formatDate(item.date)}</small></div>
      <div class="amount out">-${formatNumber(item.amount)} KC</div>
    </div>`).join('') : '<div class="empty">Chưa có khoản chi nào.</div>';
}

function renderCampaigns() {
  const root = document.querySelector('#campaignList');
  if (!state.campaigns.length) {
    root.innerHTML = '<div class="empty">Chưa có chiến dịch tiết kiệm.</div>';
    return;
  }
  root.innerHTML = state.campaigns.map(c => {
    const percent = Math.min(100, Math.round((c.saved / c.target) * 100));
    return `
      <div class="campaign">
        <div class="campaign-head">
          <div><strong>${escapeHtml(c.name)}</strong><br><small>${formatNumber(c.saved)} / ${formatNumber(c.target)} KC</small></div>
          <b>${percent}%</b>
        </div>
        <div class="progress"><div style="width:${percent}%"></div></div>
        <div class="campaign-actions">
          <input data-save-input="${c.id}" type="number" min="1" step="1" inputmode="numeric" placeholder="KC muốn tiết kiệm" />
          <button class="cta" data-save-campaign="${c.id}">Cất KC</button>
          <button class="ghost" data-release-campaign="${c.id}" title="Hoàn KC về ví">Hoàn</button>
        </div>
        <button class="ghost danger" data-delete-campaign="${c.id}" style="margin-top:8px;width:100%">Đóng chiến dịch</button>
      </div>`;
  }).join('');
}

function renderHistory() {
  const root = document.querySelector('#historyList');
  const items = state.history.slice(0, 12);
  root.innerHTML = items.length ? items.map(item => {
    const amountClass = item.amount > 0 ? 'in' : item.amount < 0 ? 'out' : '';
    const prefix = item.amount > 0 ? '+' : '';
    const when = new Intl.DateTimeFormat('vi-VN', { day:'2-digit', month:'2-digit', hour:'2-digit', minute:'2-digit' }).format(new Date(item.at));
    return `<div class="list-item">
      <div class="list-main"><strong>${escapeHtml(item.title)}</strong><small>${escapeHtml(item.meta || '')}${item.meta ? ' • ' : ''}${when}</small></div>
      <div class="amount ${amountClass}">${item.amount ? `${prefix}${formatNumber(item.amount)} KC` : '—'}</div>
    </div>`;
  }).join('') : '<div class="empty">Lịch sử sẽ xuất hiện khi bạn thêm thẻ, chi KC hoặc tiết kiệm.</div>';
}

function render() {
  const saved = getSavedTotal();
  document.querySelector('#availableDiamonds').textContent = formatNumber(state.wallet);
  document.querySelector('#totalDiamonds').textContent = formatNumber(state.wallet + saved);
  document.querySelector('#weeklyPending').textContent = formatNumber(getPendingFor('weekly'));
  document.querySelector('#monthlyPending').textContent = formatNumber(getPendingFor('monthly'));
  document.querySelector('#savedTotal').textContent = `${formatNumber(saved)} KC`;
  renderPasses();
  renderExpenses();
  renderCampaigns();
  renderHistory();
}

function escapeHtml(value) {
  return String(value).replace(/[&<>'"]/g, char => ({ '&':'&amp;', '<':'&lt;', '>':'&gt;', "'":'&#039;', '"':'&quot;' }[char]));
}

let toastTimer;
function toast(message) {
  const node = document.querySelector('#toast');
  node.textContent = message;
  node.classList.add('show');
  clearTimeout(toastTimer);
  toastTimer = setTimeout(() => node.classList.remove('show'), 2600);
}

document.querySelector('#addWeeklyBtn').addEventListener('click', () => activatePass('weekly'));
document.querySelector('#addMonthlyBtn').addEventListener('click', () => activatePass('monthly'));
document.querySelector('#expenseForm').addEventListener('submit', submitExpense);
document.querySelector('#campaignForm').addEventListener('submit', createCampaign);
document.querySelector('#exportBtn').addEventListener('click', exportData);
document.querySelector('#resetBtn').addEventListener('click', resetData);

document.addEventListener('click', event => {
  const claim = event.target.closest('[data-claim]');
  if (claim) claimPass(claim.dataset.claim);
  const save = event.target.closest('[data-save-campaign]');
  if (save) saveToCampaign(save.dataset.saveCampaign);
  const release = event.target.closest('[data-release-campaign]');
  if (release) releaseCampaign(release.dataset.releaseCampaign);
  const remove = event.target.closest('[data-delete-campaign]');
  if (remove) deleteCampaign(remove.dataset.deleteCampaign);
});

if ('serviceWorker' in navigator) {
  window.addEventListener('load', () => navigator.serviceWorker.register('./sw.js').catch(() => {}));
}

render();
