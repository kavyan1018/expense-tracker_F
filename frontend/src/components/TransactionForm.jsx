import { useState } from 'react';
import { errorInfo, transactionApi } from '../api/client.js';
import { today } from '../utils/format.js';
import CategoryPicker from './CategoryPicker.jsx';
import { Alert, Field, PaymentMethodPicker } from './ui.jsx';

/** Add or edit a transaction. Type follows the chosen category. */
export default function TransactionForm({ categories, initial, onSaved, onCancel }) {
  const editing = Boolean(initial?.id);
  const [type, setType] = useState(initial?.type || 'EXPENSE');
  const [form, setForm] = useState({
    categoryId: initial?.categoryId ?? null,
    amount: initial?.amount ?? '',
    date: initial?.date ?? today(),
    note: initial?.note ?? '',
    paymentMethod: editing ? initial.paymentMethod : 'CASH',
  });
  const [errors, setErrors] = useState({});
  const [message, setMessage] = useState('');
  const [saving, setSaving] = useState(false);

  const set = (k) => (v) => setForm((f) => ({ ...f, [k]: v }));
  const usable = categories.filter((c) => !c.archived || c.id === initial?.categoryId);

  const switchType = (t) => {
    setType(t);
    setForm((f) => ({ ...f, categoryId: null }));
  };

  const submit = async (e) => {
    e.preventDefault();
    setSaving(true);
    setMessage('');
    setErrors({});
    try {
      const body = { ...form, amount: form.amount === '' ? null : Number(form.amount) };
      const saved = editing ? await transactionApi.update(initial.id, body) : await transactionApi.create(body);
      onSaved(saved);
    } catch (err) {
      const info = errorInfo(err);
      setErrors(info.fieldErrors);
      setMessage(info.message);
    } finally {
      setSaving(false);
    }
  };

  return (
    <form onSubmit={submit} className="form">
      <div className="segmented" role="tablist">
        {['EXPENSE', 'INCOME'].map((t) => (
          <button type="button" key={t} className={type === t ? `active ${t.toLowerCase()}` : ''} onClick={() => switchType(t)}>
            {t === 'EXPENSE' ? 'Expense' : 'Income'}
          </button>
        ))}
      </div>
      <Alert>{message && Object.keys(errors).length === 0 ? message : ''}</Alert>
      <div className="form-row">
        <Field label="Amount (₹)" error={errors.amount}>
          <input type="number" inputMode="decimal" step="0.01" min="0.01" value={form.amount}
                 onChange={(e) => set('amount')(e.target.value)} placeholder="0.00" autoFocus required />
        </Field>
        <Field label="Date" error={errors.date}>
          <input type="date" value={form.date} onChange={(e) => set('date')(e.target.value)} required />
        </Field>
      </div>
      <Field label="Category" error={errors.categoryId}>
        <CategoryPicker categories={usable} type={type} value={form.categoryId} onChange={set('categoryId')} required />
      </Field>
      {/* a div, not Field's <label>: clicking the label text would press the first button */}
      <div className="field">
        <span className="field-label">Payment method</span>
        <PaymentMethodPicker value={form.paymentMethod} onChange={set('paymentMethod')} />
        {errors.paymentMethod && <span className="field-error">{errors.paymentMethod}</span>}
      </div>
      <Field label="Note" error={errors.note} hint="Optional, e.g. shop name or what it was for">
        <input type="text" maxLength={255} value={form.note || ''} onChange={(e) => set('note')(e.target.value)} />
      </Field>
      <div className="form-actions">
        <button type="button" className="btn btn-ghost" onClick={onCancel}>Cancel</button>
        <button type="submit" className="btn btn-primary" disabled={saving}>
          {saving ? 'Saving…' : editing ? 'Save changes' : 'Add transaction'}
        </button>
      </div>
    </form>
  );
}
