import { useCallback, useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { Bar, BarChart, CartesianGrid, Cell, Legend, Pie, PieChart, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts';
import { budgetApi, categoryApi, errorInfo, reportApi, transactionApi } from '../api/client.js';
import { useAuth } from '../auth/AuthContext.jsx';
import TransactionForm from '../components/TransactionForm.jsx';
import { Alert, CategoryDot, Empty, Modal, MonthPicker, PageHeader, ProgressBar, StatCard } from '../components/ui.jsx';
import { currentMonth, dateLabel, money, moneyAxis, monthLabel, monthRange } from '../utils/format.js';

export default function Dashboard() {
  const { user } = useAuth();
  const [month, setMonth] = useState(currentMonth());
  const [data, setData] = useState(null);
  const [categories, setCategories] = useState([]);
  const [error, setError] = useState('');
  const [adding, setAdding] = useState(false);

  const load = useCallback(async () => {
    const { from, to } = monthRange(month);
    try {
      const [summary, byCategory, trend, budgets, recent, cats] = await Promise.all([
        reportApi.summary(from, to),
        reportApi.byCategory(from, to),
        reportApi.trend(6),
        budgetApi.forMonth(month),
        transactionApi.list({ from, to, size: 6 }),
        categoryApi.list(),
      ]);
      setData({ summary, byCategory, trend, budgets, recent: recent.content });
      setCategories(cats);
      setError('');
    } catch (err) {
      setError(errorInfo(err).message);
    }
  }, [month]);

  useEffect(() => {
    load();
  }, [load]);

  const overall = data?.budgets.find((b) => b.categoryId === null);
  const categoryBudgets = data?.budgets.filter((b) => b.categoryId !== null) ?? [];

  return (
    <>
      <PageHeader title={`Hello, ${user.displayName}`} subtitle="Here is where your money went this month.">
        <MonthPicker month={month} onChange={setMonth} />
        <button className="btn btn-primary" onClick={() => setAdding(true)}>+ Add transaction</button>
      </PageHeader>
      <Alert>{error}</Alert>

      {data && (
        <>
          <div className="stats">
            <StatCard label="Income" value={money(data.summary.income)} tone="income" />
            <StatCard label="Expenses" value={money(data.summary.expense)} tone="expense" />
            <StatCard label="Balance" value={money(data.summary.balance)}
                      tone={data.summary.balance < 0 ? 'expense' : undefined} />
            <StatCard
              label="Budget left"
              value={overall ? money(overall.remaining) : '—'}
              tone={overall && overall.remaining < 0 ? 'expense' : undefined}
              hint={overall ? `${overall.percentUsed}% of ${money(overall.amount)} used` : <Link to="/budgets">Set a monthly budget</Link>}
            />
          </div>

          <div className="grid-2">
            <section className="card">
              <h2 className="card-title">Spending by category</h2>
              {data.byCategory.length === 0 ? (
                <Empty title="No expenses yet">Add an expense to see where your money goes.</Empty>
              ) : (
                <div className="chart-with-legend">
                  <ResponsiveContainer width="100%" height={240}>
                    <PieChart>
                      <Pie data={data.byCategory} dataKey="total" nameKey="name" innerRadius={60} outerRadius={95} paddingAngle={2}>
                        {data.byCategory.map((c) => <Cell key={c.categoryId} fill={c.color || '#94a3b8'} />)}
                      </Pie>
                      <Tooltip formatter={(v) => money(v)} />
                    </PieChart>
                  </ResponsiveContainer>
                  <ul className="legend-list">
                    {data.byCategory.slice(0, 6).map((c) => (
                      <li key={c.categoryId}>
                        <CategoryDot color={c.color} />
                        <span className="grow">{c.name}</span>
                        <span className="muted">{c.percent}%</span>
                        <span className="num">{money(c.total)}</span>
                      </li>
                    ))}
                  </ul>
                </div>
              )}
            </section>

            <section className="card">
              <h2 className="card-title">Last 6 months</h2>
              <ResponsiveContainer width="100%" height={260}>
                <BarChart data={data.trend.map((t) => ({ ...t, label: monthLabel(t.month, 'short') }))}>
                  <CartesianGrid vertical={false} stroke="var(--border)" />
                  <XAxis dataKey="label" tickLine={false} axisLine={false} fontSize={12} />
                  <YAxis tickFormatter={moneyAxis} tickLine={false} axisLine={false} fontSize={12} width={56} />
                  <Tooltip formatter={(v) => money(v)} />
                  <Legend />
                  <Bar dataKey="income" name="Income" fill="var(--income)" radius={[4, 4, 0, 0]} />
                  <Bar dataKey="expense" name="Expenses" fill="var(--expense)" radius={[4, 4, 0, 0]} />
                </BarChart>
              </ResponsiveContainer>
            </section>
          </div>

          <div className="grid-2">
            <section className="card">
              <div className="card-head">
                <h2 className="card-title">Budgets</h2>
                <Link to="/budgets" className="link">Manage</Link>
              </div>
              {categoryBudgets.length === 0 ? (
                <Empty title="No category budgets for this month"><Link to="/budgets">Set one up</Link></Empty>
              ) : (
                <ul className="budget-list">
                  {categoryBudgets.map((b) => (
                    <li key={b.id}>
                      <div className="budget-row">
                        <span><CategoryDot color={b.categoryColor} /> {b.categoryName}</span>
                        <span className="muted">{money(b.spent)} of {money(b.amount)}</span>
                      </div>
                      <ProgressBar percent={b.percentUsed} />
                    </li>
                  ))}
                </ul>
              )}
            </section>

            <section className="card">
              <div className="card-head">
                <h2 className="card-title">Recent transactions</h2>
                <Link to="/transactions" className="link">See all</Link>
              </div>
              {data.recent.length === 0 ? (
                <Empty title="Nothing recorded this month" />
              ) : (
                <ul className="txn-list">
                  {data.recent.map((t) => (
                    <li key={t.id}>
                      <CategoryDot color={t.categoryColor} />
                      <div className="grow">
                        <div>{t.note || t.categoryName}</div>
                        <div className="muted small">{t.categoryName} · {dateLabel(t.date)}</div>
                      </div>
                      <span className={`num ${t.type === 'INCOME' ? 'pos' : 'neg'}`}>
                        {t.type === 'INCOME' ? '+' : '−'}{money(t.amount)}
                      </span>
                    </li>
                  ))}
                </ul>
              )}
            </section>
          </div>
        </>
      )}

      {adding && (
        <Modal title="Add transaction" onClose={() => setAdding(false)}>
          <TransactionForm categories={categories} onCancel={() => setAdding(false)}
                           onSaved={() => { setAdding(false); load(); }} />
        </Modal>
      )}
    </>
  );
}
