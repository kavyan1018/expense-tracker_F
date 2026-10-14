import { useCallback, useEffect, useState } from 'react';
import { budgetApi, categoryApi, errorInfo } from '../api/client.js';
import { Alert, CategoryDot, Empty, Field, Modal, MonthPicker, PageHeader, ProgressBar } from '../components/ui.jsx';
import { currentMonth, money, monthLabel, shiftMonth } from '../utils/format.js';

function BudgetForm({ month, categories, initial, taken, onSaved, onCancel }) {
  const editing = Boolean(initial);
  const options = categories.filter((c) => c.type === 'EXPENSE' && !c.archived && (editing || !taken.has(c.id)));
  // a new budget defaults to "overall" unless this month already has one
  const [categoryId, setCategoryId] = useState(
    editing ? initial.categoryId : taken.has(null) ? options[0]?.id ?? null : null);
  const [amount, setAmount] = useState(initial?.amount ?? '');
  const [errors, setErrors] = useState({});
  const [message, setMessage] = useState('');

  const submit = async (e) => {
    e.preventDefault();
    try {
      onSaved(await budgetApi.save({ month, categoryId, amount: amount === '' ? null : Number(amount) }));
    } catch (err) {
      const info = errorInfo(err);
      setErrors(info.fieldErrors);
      setMessage(Object.keys(info.fieldErrors).length ? '' : info.message);
    }
  };

  return (
    <form className="form" onSubmit={submit}>
      <Alert>{message}</Alert>
      <Field label="Budget for">
        <select value={categoryId ?? ''} disabled={editing}
                onChange={(e) => setCategoryId(e.target.value ? Number(e.target.value) : null)}>
          {(editing || !taken.has(null)) && <option value="">All spending (overall)</option>}
          {options.map((c) => <option key={c.id} value={c.id}>{c.name}</option>)}
        </select>
      </Field>
      <Field label={`Limit for ${monthLabel(month)} (₹)`} error={errors.amount}>
        <input type="number" step="0.01" min="0.01" value={amount} onChange={(e) => setAmount(e.target.value)} required autoFocus />
      </Field>
      <div className="form-actions">
        <button type="button" className="btn btn-ghost" onClick={onCancel}>Cancel</button>
        <button className="btn btn-primary">{editing ? 'Save' : 'Add budget'}</button>
      </div>
    </form>
  );
}

export default function Budgets() {
  const [month, setMonth] = useState(currentMonth());
  const [budgets, setBudgets] = useState(null);
  const [categories, setCategories] = useState([]);
  const [editing, setEditing] = useState(undefined); // undefined closed, null new, budget for edit
  const [error, setError] = useState('');

  const load = useCallback(async () => {
    try {
      setBudgets(await budgetApi.forMonth(month));
      setError('');
    } catch (err) {
      setError(errorInfo(err).message);
    }
  }, [month]);

  useEffect(() => {
    load();
  }, [load]);
  useEffect(() => {
    categoryApi.list({ type: 'EXPENSE' }).then(setCategories).catch(() => {});
  }, []);

  const copyPrevious = async () => {
    try {
      const copied = await budgetApi.copy(shiftMonth(month, -1), month);
      setBudgets(copied);
      if (copied.length === 0) setError(`There were no budgets in ${monthLabel(shiftMonth(month, -1))} to copy.`);
    } catch (err) {
      setError(errorInfo(err).message);
    }
  };

  const remove = async (b) => {
    if (!window.confirm(`Remove the ${b.categoryName || 'overall'} budget for ${monthLabel(month)}?`)) return;
    await budgetApi.remove(b.id).catch((err) => setError(errorInfo(err).message));
    load();
  };

  const overall = budgets?.find((b) => b.categoryId === null);
  const perCategory = budgets?.filter((b) => b.categoryId !== null) ?? [];
  const taken = new Set((budgets ?? []).map((b) => b.categoryId));

  return (
    <>
      <PageHeader title="Budgets" subtitle="Set monthly limits and see how much is left.">
        <MonthPicker month={month} onChange={setMonth} />
        <button className="btn btn-primary" onClick={() => setEditing(null)}>+ Add budget</button>
      </PageHeader>
      <Alert>{error}</Alert>

      {budgets && budgets.length === 0 && (
        <div className="card">
          <Empty title={`No budgets for ${monthLabel(month)}`}>
            <p>Add an overall limit or one per category.</p>
            <button className="btn btn-ghost" onClick={copyPrevious}>Copy last month's budgets</button>
          </Empty>
        </div>
      )}

      {overall && (
        <section className="card overall-budget">
          <div className="card-head">
            <h2 className="card-title">Overall monthly budget</h2>
            <div>
              <button className="btn btn-small btn-ghost" onClick={() => setEditing(overall)}>Edit</button>
              <button className="btn btn-small btn-danger-ghost" onClick={() => remove(overall)}>Remove</button>
            </div>
          </div>
          <div className="overall-numbers">
            <div><div className="stat-label">Spent</div><div className="stat-value">{money(overall.spent)}</div></div>
            <div><div className="stat-label">Limit</div><div className="stat-value">{money(overall.amount)}</div></div>
            <div>
              <div className="stat-label">{overall.remaining < 0 ? 'Over by' : 'Left'}</div>
              <div className={`stat-value ${overall.remaining < 0 ? 'neg' : 'pos'}`}>{money(Math.abs(overall.remaining))}</div>
            </div>
          </div>
          <ProgressBar percent={overall.percentUsed} />
        </section>
      )}

      {perCategory.length > 0 && (
        <section className="card">
          <div className="card-head">
            <h2 className="card-title">By category</h2>
          </div>
          <ul className="budget-list">
            {perCategory.map((b) => (
              <li key={b.id}>
                <div className="budget-row">
                  <span className="grow"><CategoryDot color={b.categoryColor} /> {b.categoryName}</span>
                  <span className="muted">{money(b.spent)} of {money(b.amount)}</span>
                  <span className={`budget-left ${b.remaining < 0 ? 'neg' : ''}`}>
                    {b.remaining < 0 ? `${money(-b.remaining)} over` : `${money(b.remaining)} left`}
                  </span>
                  <span className="row-actions">
                    <button className="btn btn-small btn-ghost" onClick={() => setEditing(b)}>Edit</button>
                    <button className="btn btn-small btn-danger-ghost" onClick={() => remove(b)}>Remove</button>
                  </span>
                </div>
                <ProgressBar percent={b.percentUsed} />
              </li>
            ))}
          </ul>
        </section>
      )}

      {editing !== undefined && (
        <Modal title={editing ? `Edit ${editing.categoryName || 'overall'} budget` : 'Add budget'} onClose={() => setEditing(undefined)}>
          <BudgetForm month={month} categories={categories} initial={editing} taken={taken}
                      onCancel={() => setEditing(undefined)} onSaved={() => { setEditing(undefined); load(); }} />
        </Modal>
      )}
    </>
  );
}
