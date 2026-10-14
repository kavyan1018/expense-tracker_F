import { useCallback, useEffect, useState } from 'react';
import { Bar, BarChart, CartesianGrid, Cell, Legend, Line, LineChart, Pie, PieChart, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts';
import { errorInfo, reportApi } from '../api/client.js';
import { Alert, CategoryDot, Empty, PageHeader, StatCard } from '../components/ui.jsx';
import { currentMonth, money, moneyAxis, monthLabel, monthRange, shiftMonth } from '../utils/format.js';

function presetRange(key) {
  const now = new Date();
  const m = currentMonth();
  switch (key) {
    case 'this-month': return monthRange(m);
    case 'last-month': return monthRange(shiftMonth(m, -1));
    case 'last-3': return { from: monthRange(shiftMonth(m, -2)).from, to: monthRange(m).to };
    case 'this-year': return { from: `${now.getFullYear()}-01-01`, to: `${now.getFullYear()}-12-31` };
    case 'last-year': return { from: `${now.getFullYear() - 1}-01-01`, to: `${now.getFullYear() - 1}-12-31` };
    default: return monthRange(m);
  }
}

const PRESETS = [
  ['this-month', 'This month'],
  ['last-month', 'Last month'],
  ['last-3', 'Last 3 months'],
  ['this-year', 'This year'],
  ['last-year', 'Last year'],
];

export default function Reports() {
  const [preset, setPreset] = useState('this-month');
  const [range, setRange] = useState(presetRange('this-month'));
  const [data, setData] = useState(null);
  const [trend, setTrend] = useState([]);
  const [error, setError] = useState('');
  const [downloading, setDownloading] = useState('');

  const load = useCallback(async () => {
    if (!range.from || !range.to || range.to < range.from) return;
    try {
      const [summary, expenses, income] = await Promise.all([
        reportApi.summary(range.from, range.to),
        reportApi.byCategory(range.from, range.to, 'EXPENSE'),
        reportApi.byCategory(range.from, range.to, 'INCOME'),
      ]);
      setData({ summary, expenses, income });
      setError('');
    } catch (err) {
      setError(errorInfo(err).message);
    }
  }, [range]);

  useEffect(() => {
    load();
  }, [load]);
  useEffect(() => {
    reportApi.trend(12).then(setTrend).catch(() => {});
  }, []);

  const choose = (key) => {
    setPreset(key);
    setRange(presetRange(key));
  };

  const download = async (format) => {
    setDownloading(format);
    try {
      await reportApi.download(range.from, range.to, format);
    } catch (err) {
      setError('Could not create the file. Please try again.');
    } finally {
      setDownloading('');
    }
  };

  const savings = trend.map((t) => ({ ...t, label: monthLabel(t.month, 'short'), saved: Number(t.income) - Number(t.expense) }));

  return (
    <>
      <PageHeader title="Reports" subtitle="See the bigger picture and download your records.">
        <button className="btn btn-ghost" onClick={() => download('xlsx')} disabled={!!downloading}>
          {downloading === 'xlsx' ? 'Preparing…' : '⬇ Excel'}
        </button>
        <button className="btn btn-ghost" onClick={() => download('pdf')} disabled={!!downloading}>
          {downloading === 'pdf' ? 'Preparing…' : '⬇ PDF'}
        </button>
      </PageHeader>

      <div className="card filters">
        <div className="chips">
          {PRESETS.map(([k, label]) => (
            <button key={k} className={`chip ${preset === k ? 'active' : ''}`} onClick={() => choose(k)}>{label}</button>
          ))}
        </div>
        <label className="field compact">
          <span className="field-label">From</span>
          <input type="date" value={range.from} onChange={(e) => { setPreset('custom'); setRange({ ...range, from: e.target.value }); }} />
        </label>
        <label className="field compact">
          <span className="field-label">To</span>
          <input type="date" value={range.to} min={range.from} onChange={(e) => { setPreset('custom'); setRange({ ...range, to: e.target.value }); }} />
        </label>
      </div>
      <Alert>{error}</Alert>

      {data && (
        <>
          <div className="stats stats-3">
            <StatCard label="Income" value={money(data.summary.income)} tone="income" />
            <StatCard label="Expenses" value={money(data.summary.expense)} tone="expense" />
            <StatCard label="Saved" value={money(data.summary.balance)} tone={data.summary.balance < 0 ? 'expense' : undefined}
                      hint={Number(data.summary.income) > 0
                        ? `${Math.round((data.summary.balance / data.summary.income) * 100)}% of income` : undefined} />
          </div>

          <div className="grid-2">
            <section className="card">
              <h2 className="card-title">Expenses by category</h2>
              {data.expenses.length === 0 ? <Empty title="No expenses in this period" /> : (
                <>
                  <ResponsiveContainer width="100%" height={240}>
                    <PieChart>
                      <Pie data={data.expenses} dataKey="total" nameKey="name" innerRadius={55} outerRadius={95} paddingAngle={2}>
                        {data.expenses.map((c) => <Cell key={c.categoryId} fill={c.color || '#94a3b8'} />)}
                      </Pie>
                      <Tooltip formatter={(v) => money(v)} />
                    </PieChart>
                  </ResponsiveContainer>
                  <table className="table compact-table">
                    <tbody>
                      {data.expenses.map((c) => (
                        <tr key={c.categoryId}>
                          <td><CategoryDot color={c.color} /> {c.name}</td>
                          <td className="right muted">{c.percent}%</td>
                          <td className="right num">{money(c.total)}</td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </>
              )}
            </section>

            <section className="card">
              <h2 className="card-title">Income by category</h2>
              {data.income.length === 0 ? <Empty title="No income in this period" /> : (
                <ResponsiveContainer width="100%" height={Math.max(160, data.income.length * 48)}>
                  <BarChart data={data.income} layout="vertical" margin={{ left: 8, right: 16 }}>
                    <XAxis type="number" tickFormatter={moneyAxis} fontSize={12} />
                    <YAxis type="category" dataKey="name" width={90} fontSize={12} />
                    <Tooltip formatter={(v) => money(v)} />
                    <Bar dataKey="total" name="Income" radius={[0, 4, 4, 0]}>
                      {data.income.map((c) => <Cell key={c.categoryId} fill={c.color || 'var(--income)'} />)}
                    </Bar>
                  </BarChart>
                </ResponsiveContainer>
              )}
            </section>
          </div>
        </>
      )}

      <section className="card">
        <h2 className="card-title">Last 12 months</h2>
        <ResponsiveContainer width="100%" height={300}>
          <LineChart data={savings}>
            <CartesianGrid vertical={false} stroke="var(--border)" />
            <XAxis dataKey="label" tickLine={false} axisLine={false} fontSize={12} />
            <YAxis tickFormatter={moneyAxis} tickLine={false} axisLine={false} fontSize={12} width={56} />
            <Tooltip formatter={(v) => money(v)} />
            <Legend />
            <Line type="monotone" dataKey="income" name="Income" stroke="var(--income)" strokeWidth={2} dot={false} />
            <Line type="monotone" dataKey="expense" name="Expenses" stroke="var(--expense)" strokeWidth={2} dot={false} />
            <Line type="monotone" dataKey="saved" name="Saved" stroke="var(--primary)" strokeWidth={2} strokeDasharray="4 4" dot={false} />
          </LineChart>
        </ResponsiveContainer>
      </section>
    </>
  );
}
