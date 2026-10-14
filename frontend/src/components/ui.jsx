import { useEffect } from 'react';
import { PAYMENT_ICON, PAYMENT_LABEL, monthLabel, shiftMonth } from '../utils/format.js';

export function PageHeader({ title, subtitle, children }) {
  return (
    <div className="page-header">
      <div>
        <h1>{title}</h1>
        {subtitle && <p className="muted">{subtitle}</p>}
      </div>
      {children && <div className="page-actions">{children}</div>}
    </div>
  );
}

export function StatCard({ label, value, tone, hint }) {
  return (
    <div className={`card stat ${tone ? `stat-${tone}` : ''}`}>
      <div className="stat-label">{label}</div>
      <div className="stat-value">{value}</div>
      {hint && <div className="stat-hint">{hint}</div>}
    </div>
  );
}

export function ProgressBar({ percent }) {
  const tone = percent >= 100 ? 'over' : percent >= 80 ? 'warn' : 'ok';
  return (
    <div className="progress" role="progressbar" aria-valuenow={percent} aria-valuemin={0} aria-valuemax={100}>
      <div className={`progress-fill ${tone}`} style={{ width: `${Math.min(percent, 100)}%` }} />
    </div>
  );
}

export function Empty({ title, children }) {
  return (
    <div className="empty">
      <div className="empty-title">{title}</div>
      {children && <div className="muted">{children}</div>}
    </div>
  );
}

export function Alert({ children, tone = 'error' }) {
  if (!children) return null;
  return <div className={`alert alert-${tone}`}>{children}</div>;
}

export function Field({ label, error, children, hint }) {
  return (
    <label className={`field ${error ? 'has-error' : ''}`}>
      <span className="field-label">{label}</span>
      {children}
      {error ? <span className="field-error">{error}</span> : hint && <span className="field-hint">{hint}</span>}
    </label>
  );
}

export function Modal({ title, onClose, children }) {
  useEffect(() => {
    const onKey = (e) => e.key === 'Escape' && onClose();
    window.addEventListener('keydown', onKey);
    return () => window.removeEventListener('keydown', onKey);
  }, [onClose]);
  return (
    <div className="modal-backdrop" onMouseDown={(e) => e.target === e.currentTarget && onClose()}>
      <div className="modal" role="dialog" aria-modal="true" aria-label={title}>
        <div className="modal-head">
          <h2>{title}</h2>
          <button className="icon-btn" onClick={onClose} aria-label="Close">✕</button>
        </div>
        {children}
      </div>
    </div>
  );
}

export function MonthPicker({ month, onChange }) {
  return (
    <div className="month-picker">
      <button className="icon-btn" onClick={() => onChange(shiftMonth(month, -1))} aria-label="Previous month">‹</button>
      <span className="month-label">{monthLabel(month)}</span>
      <button className="icon-btn" onClick={() => onChange(shiftMonth(month, 1))} aria-label="Next month">›</button>
    </div>
  );
}

export function CategoryDot({ color }) {
  return <span className="dot" style={{ background: color || '#94a3b8' }} />;
}

const ADD_NEW = 'new';

/** Category dropdown grouped by type. With onAddNew it ends with an "Other" choice that calls it. */
export function CategorySelect({ categories, value, onChange, onAddNew, type, allowEmpty, emptyLabel = 'All categories', ...rest }) {
  const list = type ? categories.filter((c) => c.type === type) : categories;
  const groups = type ? [[type, list]] : [['EXPENSE', list.filter((c) => c.type === 'EXPENSE')], ['INCOME', list.filter((c) => c.type === 'INCOME')]];
  const change = (v) => (v === ADD_NEW ? onAddNew() : onChange(v ? Number(v) : null));
  return (
    <select value={value ?? ''} onChange={(e) => change(e.target.value)} {...rest}>
      {allowEmpty && <option value="">{emptyLabel}</option>}
      {!allowEmpty && !value && <option value="">Choose a category</option>}
      {groups.map(([t, cats]) => (
        <optgroup key={t} label={t === 'EXPENSE' ? 'Expenses' : 'Income'}>
          {cats.map((c) => (
            <option key={c.id} value={c.id}>{c.name}</option>
          ))}
        </optgroup>
      ))}
      {onAddNew && <option value={ADD_NEW}>➕ Other (add a new category)…</option>}
    </select>
  );
}

/** Cash, UPI, Card or Bank Transfer, as a row of buttons. */
export function PaymentMethodPicker({ value, onChange }) {
  return (
    <div className="segmented four" role="radiogroup" aria-label="Payment method">
      {Object.entries(PAYMENT_LABEL).map(([k, label]) => (
        <button type="button" key={k} role="radio" aria-checked={value === k} className={value === k ? 'active' : ''}
                onClick={() => onChange(k)}>
          {PAYMENT_ICON[k]} {label}
        </button>
      ))}
    </div>
  );
}
