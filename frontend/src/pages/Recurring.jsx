import { useCallback, useEffect, useState } from 'react';
import { categoryApi, errorInfo, recurringApi } from '../api/client.js';
import CategoryPicker from '../components/CategoryPicker.jsx';
import { Alert, Empty, Field, Modal, PageHeader, PaymentMethodPicker } from '../components/ui.jsx';
import { FREQUENCY_LABEL, dateLabel, money, today } from '../utils/format.js';

function RecurringForm({ categories, initial, onSaved, onCancel }) {
  const editing = Boolean(initial?.id);
  const [form, setForm] = useState({
    description: initial?.description ?? '',
    categoryId: initial?.categoryId ?? null,
    amount: initial?.amount ?? '',
    frequency: initial?.frequency ?? 'MONTHLY',
    startDate: initial?.startDate ?? today(),
    endDate: initial?.endDate ?? '',
    paymentMethod: editing ? initial.paymentMethod : 'BANK_TRANSFER',
  });
  const [errors, setErrors] = useState({});
  const [message, setMessage] = useState('');
  const set = (k) => (v) => setForm((f) => ({ ...f, [k]: v }));

  const submit = async (e) => {
    e.preventDefault();
    const body = { ...form, amount: form.amount === '' ? null : Number(form.amount), endDate: form.endDate || null };
    try {
      onSaved(editing ? await recurringApi.update(initial.id, body) : await recurringApi.create(body));
    } catch (err) {
      const info = errorInfo(err);
      setErrors(info.fieldErrors);
      setMessage(Object.keys(info.fieldErrors).length ? '' : info.message);
    }
  };

  const past = !editing && form.startDate && form.startDate < today();

  return (
    <form className="form" onSubmit={submit}>
      <Alert>{message}</Alert>
      <Field label="Description" error={errors.description} hint="Shown as the note on each transaction">
        <input value={form.description} onChange={(e) => set('description')(e.target.value)} maxLength={255}
               placeholder="e.g. House rent, Netflix, School fees" required autoFocus />
      </Field>
      <div className="form-row">
        <Field label="Amount (₹)" error={errors.amount}>
          <input type="number" step="0.01" min="0.01" value={form.amount} onChange={(e) => set('amount')(e.target.value)} required />
        </Field>
        <Field label="Repeats" error={errors.frequency}>
          <select value={form.frequency} onChange={(e) => set('frequency')(e.target.value)}>
            {Object.entries(FREQUENCY_LABEL).map(([k, v]) => <option key={k} value={k}>{v}</option>)}
          </select>
        </Field>
      </div>
      <Field label="Category" error={errors.categoryId}>
        <CategoryPicker categories={categories.filter((c) => !c.archived || c.id === initial?.categoryId)}
                        value={form.categoryId} onChange={set('categoryId')} required />
      </Field>
      {/* a div, not Field's <label>: clicking the label text would press the first button */}
      <div className="field">
        <span className="field-label">Payment method</span>
        <PaymentMethodPicker value={form.paymentMethod} onChange={set('paymentMethod')} />
        {errors.paymentMethod && <span className="field-error">{errors.paymentMethod}</span>}
      </div>
      <div className="form-row">
        <Field label="First date" error={errors.startDate}>
          <input type="date" value={form.startDate} onChange={(e) => set('startDate')(e.target.value)} required />
        </Field>
        <Field label="End date" error={errors.endDate} hint="Optional">
          <input type="date" value={form.endDate || ''} min={form.startDate} onChange={(e) => set('endDate')(e.target.value)} />
        </Field>
      </div>
      {past && <Alert tone="info">Occurrences from {dateLabel(form.startDate)} up to today will be added right away.</Alert>}
      {editing && <Alert tone="info">Changes apply from today. Transactions already added stay as they are.</Alert>}
      <div className="form-actions">
        <button type="button" className="btn btn-ghost" onClick={onCancel}>Cancel</button>
        <button className="btn btn-primary">{editing ? 'Save changes' : 'Add recurring'}</button>
      </div>
    </form>
  );
}

export default function Recurring() {
  const [rules, setRules] = useState(null);
  const [categories, setCategories] = useState([]);
  const [editing, setEditing] = useState(null);
  const [error, setError] = useState('');

  const load = useCallback(async () => {
    try {
      setRules(await recurringApi.list());
      setError('');
    } catch (err) {
      setError(errorInfo(err).message);
    }
  }, []);

  useEffect(() => {
    load();
    categoryApi.list({ includeArchived: true }).then(setCategories).catch(() => {});
  }, [load]);

  const act = (fn) => async (r) => {
    try {
      await fn(r);
      load();
    } catch (err) {
      setError(errorInfo(err).message);
    }
  };
  const toggle = act((r) => (r.active ? recurringApi.pause(r.id) : recurringApi.resume(r.id)));
  const remove = act(async (r) => {
    if (window.confirm(`Delete "${r.description}"? Transactions it already added will be kept.`)) await recurringApi.remove(r.id);
  });

  const monthlyTotal = (rules ?? [])
    .filter((r) => r.active && r.type === 'EXPENSE')
    .reduce((sum, r) => sum + Number(r.amount) * (r.frequency === 'WEEKLY' ? 52 / 12 : r.frequency === 'YEARLY' ? 1 / 12 : 1), 0);

  return (
    <>
      <PageHeader title="Recurring" subtitle="Bills and income that repeat are added for you automatically on their date.">
        <button className="btn btn-primary" onClick={() => setEditing({})}>+ Add recurring</button>
      </PageHeader>
      <Alert>{error}</Alert>

      {rules && rules.length > 0 && (
        <p className="muted">Active recurring expenses add up to about <strong>{money(monthlyTotal)}</strong> a month.</p>
      )}

      <div className="card table-card">
        {rules && rules.length === 0 ? (
          <Empty title="No recurring items yet">Add rent, subscriptions, school fees or your salary.</Empty>
        ) : (
          <table className="table">
            <thead>
              <tr>
                <th>Description</th>
                <th>Category</th>
                <th>Repeats</th>
                <th>Next date</th>
                <th className="right">Amount</th>
                <th aria-label="Actions" />
              </tr>
            </thead>
            <tbody>
              {rules?.map((r) => (
                <tr key={r.id} className={r.active ? '' : 'inactive'}>
                  <td>
                    {r.description}
                    {!r.active && <span className="badge badge-muted">{r.endDate && r.nextRunDate > r.endDate ? 'Ended' : 'Paused'}</span>}
                  </td>
                  <td>{r.categoryName}</td>
                  <td>{FREQUENCY_LABEL[r.frequency]}</td>
                  <td className="nowrap">{r.active ? dateLabel(r.nextRunDate) : '—'}</td>
                  <td className={`right num nowrap ${r.type === 'INCOME' ? 'pos' : 'neg'}`}>
                    {r.type === 'INCOME' ? '+' : '−'}{money(r.amount)}
                  </td>
                  <td className="right nowrap">
                    <button className="btn btn-small btn-ghost" onClick={() => setEditing(r)}>Edit</button>
                    <button className="btn btn-small btn-ghost" onClick={() => toggle(r)}>{r.active ? 'Pause' : 'Resume'}</button>
                    <button className="btn btn-small btn-danger-ghost" onClick={() => remove(r)}>Delete</button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>

      {editing && (
        <Modal title={editing.id ? 'Edit recurring' : 'Add recurring'} onClose={() => setEditing(null)}>
          <RecurringForm categories={categories} initial={editing} onCancel={() => setEditing(null)}
                         onSaved={() => { setEditing(null); load(); }} />
        </Modal>
      )}
    </>
  );
}
