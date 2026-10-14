import { useState } from 'react';
import { categoryApi, errorInfo } from '../api/client.js';
import { CategorySelect } from './ui.jsx';

const COLORS = ['#0d9488', '#7c3aed', '#db2777', '#ea580c', '#2563eb', '#ca8a04', '#16a34a', '#dc2626'];

/**
 * Category dropdown whose last choice, "Other", asks for a new category's name,
 * adds it to the user's list and selects it. Without a fixed type it also asks expense or income.
 */
export default function CategoryPicker({ categories, type, value, onChange, required }) {
  const [added, setAdded] = useState([]);
  const [adding, setAdding] = useState(false);
  const [name, setName] = useState('');
  const [newType, setNewType] = useState('EXPENSE');
  const [error, setError] = useState('');
  const [saving, setSaving] = useState(false);

  const all = [...categories, ...added.filter((a) => !categories.some((c) => c.id === a.id))];

  const close = () => {
    setAdding(false);
    setName('');
    setError('');
  };

  const add = async () => {
    if (!name.trim()) {
      setError('Type a name for the new category');
      return;
    }
    setSaving(true);
    try {
      const created = await categoryApi.create({
        name: name.trim(),
        type: type || newType,
        color: COLORS[all.length % COLORS.length],
      });
      setAdded((a) => [...a, created]);
      onChange(created.id);
      close();
    } catch (err) {
      const info = errorInfo(err);
      setError(info.fieldErrors.name || info.message);
    } finally {
      setSaving(false);
    }
  };

  if (!adding) {
    return <CategorySelect categories={all} type={type} value={value} onChange={onChange}
                           onAddNew={() => setAdding(true)} required={required} />;
  }

  return (
    <div className="new-category">
      {!type && (
        <div className="segmented">
          {['EXPENSE', 'INCOME'].map((t) => (
            <button type="button" key={t} className={newType === t ? `active ${t.toLowerCase()}` : ''} onClick={() => setNewType(t)}>
              {t === 'EXPENSE' ? 'Expense' : 'Income'}
            </button>
          ))}
        </div>
      )}
      <input value={name} maxLength={60} autoFocus placeholder="New category name, e.g. Pet care or Gym"
             onChange={(e) => setName(e.target.value)}
             onKeyDown={(e) => {
               if (e.key === 'Enter') {
                 e.preventDefault(); // don't submit the surrounding form
                 add();
               }
             }} />
      {error && <span className="field-error">{error}</span>}
      <div className="new-category-actions">
        <button type="button" className="btn btn-ghost" onClick={close}>Cancel</button>
        <button type="button" className="btn btn-primary" onClick={add} disabled={saving}>
          {saving ? 'Adding…' : 'Add to my list'}
        </button>
      </div>
    </div>
  );
}
