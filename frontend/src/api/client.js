import axios from 'axios';

const TOKEN_KEY = 'fet.token';

export const tokenStore = {
  get: () => localStorage.getItem(TOKEN_KEY),
  set: (t) => localStorage.setItem(TOKEN_KEY, t),
  clear: () => localStorage.removeItem(TOKEN_KEY),
};

// Base URL is /api; Vite forwards it to Spring Boot in development.
// Set VITE_API_URL to call a backend on another address.
const api = axios.create({ baseURL: import.meta.env.VITE_API_URL || '/api' });

api.interceptors.request.use((config) => {
  const token = tokenStore.get();
  if (token) config.headers.Authorization = `Bearer ${token}`;
  return config;
});

let onUnauthorized = () => {};
export const setUnauthorizedHandler = (fn) => {
  onUnauthorized = fn;
};

api.interceptors.response.use(
  (res) => res,
  (err) => {
    const url = err.config?.url || '';
    if (err.response?.status === 401 && !url.startsWith('/auth/')) onUnauthorized();
    return Promise.reject(err);
  },
);

/** Turns an API error into { message, fieldErrors } for forms. */
export function errorInfo(err) {
  const data = err?.response?.data;
  if (data && typeof data === 'object' && data.message) {
    return { message: data.message, fieldErrors: data.fieldErrors || {} };
  }
  if (!err?.response) return { message: 'Cannot reach the server. Is the backend running?', fieldErrors: {} };
  return { message: 'Something went wrong. Please try again.', fieldErrors: {} };
}

const data = (p) => p.then((r) => r.data);

export const authApi = {
  login: (body) => data(api.post('/auth/login', body)),
  register: (body) => data(api.post('/auth/register', body)),
  me: () => data(api.get('/auth/me')),
};

export const categoryApi = {
  list: (params) => data(api.get('/categories', { params })),
  create: (body) => data(api.post('/categories', body)),
  update: (id, body) => data(api.put(`/categories/${id}`, body)),
  remove: (id) => data(api.delete(`/categories/${id}`)),
};

export const transactionApi = {
  list: (params) => data(api.get('/transactions', { params })),
  create: (body) => data(api.post('/transactions', body)),
  update: (id, body) => data(api.put(`/transactions/${id}`, body)),
  remove: (id) => api.delete(`/transactions/${id}`),
};

export const budgetApi = {
  forMonth: (month) => data(api.get('/budgets', { params: { month } })),
  save: (body) => data(api.put('/budgets', body)),
  copy: (from, to) => data(api.post('/budgets/copy', null, { params: { from, to } })),
  remove: (id) => api.delete(`/budgets/${id}`),
};

export const recurringApi = {
  list: () => data(api.get('/recurring')),
  create: (body) => data(api.post('/recurring', body)),
  update: (id, body) => data(api.put(`/recurring/${id}`, body)),
  pause: (id) => data(api.post(`/recurring/${id}/pause`)),
  resume: (id) => data(api.post(`/recurring/${id}/resume`)),
  remove: (id) => api.delete(`/recurring/${id}`),
};

export const reportApi = {
  summary: (from, to) => data(api.get('/reports/summary', { params: { from, to } })),
  byCategory: (from, to, type = 'EXPENSE') => data(api.get('/reports/by-category', { params: { from, to, type } })),
  trend: (months = 12) => data(api.get('/reports/monthly-trend', { params: { months } })),
  /** Downloads the Excel or PDF report through the browser. */
  async download(from, to, format) {
    const res = await api.get('/reports/export', { params: { from, to, format }, responseType: 'blob' });
    const match = /filename="?([^"]+)"?/.exec(res.headers['content-disposition'] || '');
    const url = URL.createObjectURL(res.data);
    const a = document.createElement('a');
    a.href = url;
    a.download = match ? match[1] : `expenses.${format}`;
    document.body.appendChild(a);
    a.click();
    a.remove();
    URL.revokeObjectURL(url);
  },
};

export default api;
