import { useState } from 'react';
import { Link } from 'react-router-dom';
import { authApi, errorInfo } from '../api/client.js';
import { useAuth } from '../auth/AuthContext.jsx';
import { Alert, Field } from '../components/ui.jsx';
import AuthLayout from './AuthLayout.jsx';

export default function Login() {
  const { signIn } = useAuth();
  const [form, setForm] = useState({ email: '', password: '' });
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);

  const submit = async (e) => {
    e.preventDefault();
    setBusy(true);
    setError('');
    try {
      signIn(await authApi.login(form));
    } catch (err) {
      setError(errorInfo(err).message);
      setBusy(false);
    }
  };

  return (
    <AuthLayout title="Welcome back" subtitle="Sign in to see your own income, expenses and budgets."
                footer={<>New here? <Link to="/register">Create your account</Link></>}>
      <form onSubmit={submit} className="form">
        <Alert>{error}</Alert>
        <Field label="Email">
          <input type="email" autoComplete="email" value={form.email} required autoFocus
                 onChange={(e) => setForm({ ...form, email: e.target.value })} />
        </Field>
        <Field label="Password">
          <input type="password" autoComplete="current-password" value={form.password} required
                 onChange={(e) => setForm({ ...form, password: e.target.value })} />
        </Field>
        <button className="btn btn-primary btn-block" disabled={busy}>{busy ? 'Signing in…' : 'Sign in'}</button>
      </form>
    </AuthLayout>
  );
}
