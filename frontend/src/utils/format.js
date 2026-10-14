const inr = new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR' });
const inrShort = new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR', maximumFractionDigits: 0 });

export const money = (n) => inr.format(Number(n || 0));
export const moneyShort = (n) => inrShort.format(Number(n || 0));

/** Compact axis labels: ₹1.2L, ₹45K. */
export function moneyAxis(n) {
  const v = Number(n || 0);
  const abs = Math.abs(v);
  if (abs >= 1e7) return `₹${(v / 1e7).toFixed(1)}Cr`;
  if (abs >= 1e5) return `₹${(v / 1e5).toFixed(1)}L`;
  if (abs >= 1e3) return `₹${(v / 1e3).toFixed(0)}K`;
  return `₹${v}`;
}

const pad = (n) => String(n).padStart(2, '0');

/** yyyy-MM-dd in local time (not UTC), so "today" matches the user's calendar. */
export const isoDate = (d) => `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`;
export const today = () => isoDate(new Date());
export const currentMonth = () => today().slice(0, 7);

export function shiftMonth(month, delta) {
  const [y, m] = month.split('-').map(Number);
  const d = new Date(y, m - 1 + delta, 1);
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}`;
}

export function monthRange(month) {
  const [y, m] = month.split('-').map(Number);
  return { from: `${month}-01`, to: isoDate(new Date(y, m, 0)) };
}

export function monthLabel(month, style = 'long') {
  const [y, m] = month.split('-').map(Number);
  return new Date(y, m - 1, 1).toLocaleDateString('en-IN', { month: style, year: 'numeric' });
}

export function dateLabel(iso) {
  if (!iso) return '';
  const [y, m, d] = iso.split('-').map(Number);
  return new Date(y, m - 1, d).toLocaleDateString('en-IN', { day: 'numeric', month: 'short', year: 'numeric' });
}

export const FREQUENCY_LABEL = { WEEKLY: 'Weekly', MONTHLY: 'Monthly', YEARLY: 'Yearly' };
