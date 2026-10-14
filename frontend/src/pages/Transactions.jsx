import { useCallback, useEffect, useState } from 'react';
import { categoryApi, errorInfo, transactionApi } from '../api/client.js';
import TransactionForm from '../components/TransactionForm.jsx';
import { Alert, CategoryDot, CategorySelect, Empty, Modal, PageHeader } from '../components/ui.jsx';
import { PAYMENT_ICON, PAYMENT_LABEL, currentMonth, dateLabel, money, monthRange } from '../utils/format.js';

const PAGE_SIZE = 20;

export default function Transactions() {
  const [filters, setFilters] = useState({ ...monthRange(currentMonth()), type: '', categoryId: null, paymentMethod: '', q: '' });
  const [page, setPage] = useState(0);
  const [result, setResult] = useState(null);
  const [categories, setCategories] = useState([]);
  const [editing, setEditing] = useState(null); // {} for new, a transaction for edit
  const [error, setError] = useState('');

  const load = useCallback(async () => {
    try {
      const params = { page, size: PAGE_SIZE };
      Object.entries(filters).forEach(([k, v]) => {
        if (v !== '' && v !== null) params[k] = v;
      });
      setResult(await transactionApi.list(params));
      setError('');
    } catch (err) {
      setError(errorInfo(err).message);
    }
  }, [filters, page]);

  useEffect(() => {
    categoryApi.list({ includeArchived: true }).then(setCategories).catch(() => {});
  }, []);

  useEffect(() => {
    const t = setTimeout(load, filters.q ? 250 : 0);
    return () => clearTimeout(t);
  }, [load, filters.q]);

  const setFilter = (k) => (v) => {
    setPage(0);
    setFilters((f) => ({ ...f, [k]: v }));
  };

  const remove = async (t) => {
    if (!window.confirm(`Delete this ${t.type === 'INCOME' ? 'income' : 'expense'} of ${money(t.amount)}?`)) return;
    try {
      await transactionApi.remove(t.id);
      load();
    } catch (err) {
      setError(errorInfo(err).message);
    }
  };

  return (
    <>
      <PageHeader title="Transactions" subtitle="Everything you've recorded, newest first.">
        <button className="btn btn-primary" onClick={() => setEditing({})}>+ Add transaction</button>
      </PageHeader>

      <div className="card filters">
        <label className="field compact">
          <span className="field-label">From</span>
          <input type="date" value={filters.from} onChange={(e) => setFilter('from')(e.target.value)} />
        </label>
        <label className="field compact">
          <span className="field-label">To</span>
          <input type="date" value={filters.to} onChange={(e) => setFilter('to')(e.target.value)} />
        </label>
        <label className="field compact">
          <span className="field-label">Type</span>
          <select value={filters.type} onChange={(e) => { setFilter('type')(e.target.value); setFilter('categoryId')(null); }}>
            <option value="">All</option>
            <option value="EXPENSE">Expenses</option>
            <option value="INCOME">Income</option>
          </select>
        </label>
        <label className="field compact">
          <span className="field-label">Category</span>
          <CategorySelect categories={categories} type={filters.type || undefined} value={filters.categoryId}
                          onChange={setFilter('categoryId')} allowEmpty />
        </label>
        <label className="field compact">
          <span className="field-label">Payment method</span>
          <select value={filters.paymentMethod} onChange={(e) => setFilter('paymentMethod')(e.target.value)}>
            <option value="">All</option>
            {Object.entries(PAYMENT_LABEL).map(([k, v]) => <option key={k} value={k}>{v}</option>)}
          </select>
        </label>
        <label className="field compact grow">
          <span className="field-label">Search notes</span>
          <input type="search" placeholder="e.g. electricity" value={filters.q} onChange={(e) => setFilter('q')(e.target.value)} />
        </label>
      </div>

      <Alert>{error}</Alert>

      <div className="card table-card">
        {result && result.content.length === 0 ? (
          <Empty title="No transactions match">Try a wider date range, or add a new one.</Empty>
        ) : (
          <table className="table">
            <thead>
              <tr>
                <th>Date</th>
                <th>Category</th>
                <th>Paid by</th>
                <th>Note</th>
                <th className="right">Amount</th>
                <th aria-label="Actions" />
              </tr>
            </thead>
            <tbody>
              {result?.content.map((t) => (
                <tr key={t.id}>
                  <td className="nowrap">{dateLabel(t.date)}</td>
                  <td className="nowrap"><CategoryDot color={t.categoryColor} /> {t.categoryName}</td>
                  <td className="nowrap">
                    {t.paymentMethod ? `${PAYMENT_ICON[t.paymentMethod]} ${PAYMENT_LABEL[t.paymentMethod]}` : <span className="muted">—</span>}
                  </td>
                  <td>
                    {t.note || <span className="muted">—</span>}
                    {t.recurring && <span className="badge">Recurring</span>}
                  </td>
                  <td className={`right num nowrap ${t.type === 'INCOME' ? 'pos' : 'neg'}`}>
                    {t.type === 'INCOME' ? '+' : '−'}{money(t.amount)}
                  </td>
                  <td className="right nowrap">
                    <button className="btn btn-small btn-ghost" onClick={() => setEditing(t)}>Edit</button>
                    <button className="btn btn-small btn-danger-ghost" onClick={() => remove(t)}>Delete</button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
        {result && result.totalPages > 1 && (
          <div className="pager">
            <button className="btn btn-small btn-ghost" disabled={page === 0} onClick={() => setPage(page - 1)}>‹ Newer</button>
            <span className="muted">Page {page + 1} of {result.totalPages} · {result.totalElements} transactions</span>
            <button className="btn btn-small btn-ghost" disabled={page + 1 >= result.totalPages} onClick={() => setPage(page + 1)}>Older ›</button>
          </div>
        )}
      </div>

      {editing && (
        <Modal title={editing.id ? 'Edit transaction' : 'Add transaction'} onClose={() => setEditing(null)}>
          <TransactionForm categories={categories} initial={editing} onCancel={() => setEditing(null)}
                           onSaved={() => { setEditing(null); load(); }} />
        </Modal>
      )}
    </>
  );
}
