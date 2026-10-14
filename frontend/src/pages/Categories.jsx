import { useCallback, useEffect, useState } from 'react';
import { categoryApi, errorInfo } from '../api/client.js';
import { Alert, CategoryDot, Field, Modal, PageHeader } from '../components/ui.jsx';

const SWATCHES = ['#16a34a', '#0891b2', '#2563eb', '#7c3aed', '#db2777', '#dc2626', '#ea580c', '#ca8a04', '#0d9488', '#64748b'];

function CategoryForm({ initial, onSaved, onCancel }) {
  const editing = Boolean(initial?.id);
  const [form, setForm] = useState({ name: initial?.name ?? '', type: initial?.type ?? 'EXPENSE', color: initial?.color ?? SWATCHES[0] });
  const [errors, setErrors] = useState({});
  const [message, setMessage] = useState('');

  const submit = async (e) => {
    e.preventDefault();
    try {
      onSaved(editing ? await categoryApi.update(initial.id, form) : await categoryApi.create(form));
    } catch (err) {
      const info = errorInfo(err);
      setErrors(info.fieldErrors);
      setMessage(Object.keys(info.fieldErrors).length ? '' : info.message);
    }
  };

  return (
    <form className="form" onSubmit={submit}>
      <Alert>{message}</Alert>
      {!editing && (
        <div className="segmented">
          {['EXPENSE', 'INCOME'].map((t) => (
            <button type="button" key={t} className={form.type === t ? `active ${t.toLowerCase()}` : ''}
                    onClick={() => setForm({ ...form, type: t })}>
              {t === 'EXPENSE' ? 'Expense' : 'Income'}
            </button>
          ))}
        </div>
      )}
      <Field label="Name" error={errors.name}>
        <input value={form.name} maxLength={60} onChange={(e) => setForm({ ...form, name: e.target.value })} required autoFocus />
      </Field>
      <Field label="Color" error={errors.color}>
        <div className="swatches">
          {SWATCHES.map((c) => (
            <button type="button" key={c} className={`swatch ${form.color === c ? 'selected' : ''}`} style={{ background: c }}
                    onClick={() => setForm({ ...form, color: c })} aria-label={`Color ${c}`} />
          ))}
          <input type="color" value={form.color} onChange={(e) => setForm({ ...form, color: e.target.value })} aria-label="Custom color" />
        </div>
      </Field>
      <div className="form-actions">
        <button type="button" className="btn btn-ghost" onClick={onCancel}>Cancel</button>
        <button className="btn btn-primary">{editing ? 'Save' : 'Add category'}</button>
      </div>
    </form>
  );
}

export default function Categories() {
  const [categories, setCategories] = useState([]);
  const [showArchived, setShowArchived] = useState(false);
  const [editing, setEditing] = useState(null);
  const [notice, setNotice] = useState('');
  const [error, setError] = useState('');

  const load = useCallback(async () => {
    try {
      setCategories(await categoryApi.list({ includeArchived: showArchived }));
    } catch (err) {
      setError(errorInfo(err).message);
    }
  }, [showArchived]);

  useEffect(() => {
    load();
  }, [load]);

  const remove = async (c) => {
    if (!window.confirm(`Delete the "${c.name}" category?`)) return;
    try {
      const res = await categoryApi.remove(c.id);
      setNotice(res.archived
        ? `"${c.name}" is used by existing records, so it was archived instead. Old records keep it, but it won't show up for new ones.`
        : `"${c.name}" was deleted.`);
      load();
    } catch (err) {
      setError(errorInfo(err).message);
    }
  };

  const column = (type, title) => (
    <section className="card">
      <h2 className="card-title">{title}</h2>
      <ul className="category-list">
        {categories.filter((c) => c.type === type).map((c) => (
          <li key={c.id} className={c.archived ? 'inactive' : ''}>
            <CategoryDot color={c.color} />
            <span className="grow">{c.name}{c.archived && <span className="badge badge-muted">Archived</span>}</span>
            <button className="btn btn-small btn-ghost" onClick={() => setEditing(c)}>{c.archived ? 'Restore' : 'Edit'}</button>
            {!c.archived && <button className="btn btn-small btn-danger-ghost" onClick={() => remove(c)}>Delete</button>}
          </li>
        ))}
      </ul>
    </section>
  );

  return (
    <>
      <PageHeader title="Categories" subtitle="Your own categories. Other family members have their own list.">
        <label className="check">
          <input type="checkbox" checked={showArchived} onChange={(e) => setShowArchived(e.target.checked)} /> Show archived
        </label>
        <button className="btn btn-primary" onClick={() => setEditing({})}>+ Add category</button>
      </PageHeader>
      <Alert>{error}</Alert>
      <Alert tone="info">{notice}</Alert>
      <div className="grid-2">
        {column('EXPENSE', 'Expense categories')}
        {column('INCOME', 'Income categories')}
      </div>
      {editing && (
        <Modal title={editing.id ? (editing.archived ? 'Restore category' : 'Edit category') : 'Add category'} onClose={() => setEditing(null)}>
          <CategoryForm initial={editing} onCancel={() => setEditing(null)} onSaved={() => { setEditing(null); setNotice(''); load(); }} />
        </Modal>
      )}
    </>
  );
}
