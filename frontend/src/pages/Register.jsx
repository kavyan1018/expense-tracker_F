import { useState } from 'react';
import { Link } from 'react-router-dom';
import { authApi, errorInfo } from '../api/client.js';
import { useAuth } from '../auth/AuthContext.jsx';
import { Alert, Field } from '../components/ui.jsx';
import AuthLayout from './AuthLayout.jsx';

export default function Register() {
  const { signIn } = useAuth();
  const [form, setForm] = useState({ displayName: '', email: '', password: '', confirm: '' });
  const [errors, setErrors] = useState({});
  const [message, setMessage] = useState('');
  const [busy, setBusy] = useState(false);
  const set = (k) => (e) => setForm({ ...form, [k]: e.target.value });

  const submit = async (e) => {
    e.preventDefault();
    if (form.password !== form.confirm) {
      setErrors({ confirm: 'Passwords do not match' });
      return;
    }
    setBusy(true);
    setErrors({});
    setMessage('');
    try {
      const { confirm, ...body } = form;
      signIn(await authApi.register(body));
    } catch (err) {
      const info = errorInfo(err);
      setErrors(info.fieldErrors);
      setMessage(Object.keys(info.fieldErrors).length ? '' : info.message);
      setBusy(false);
    }
  };

  return (
    <AuthLayout title="Create your account"
                subtitle="Each family member gets their own private account. Nobody else can see your data."
                footer={<>Already have an account? <Link to="/login">Sign in</Link></>}>
      <form onSubmit={submit} className="form">
        <Alert>{message}</Alert>
        <Field label="Your name" error={errors.displayName}>
          <input autoComplete="name" value={form.displayName} onChange={set('displayName')} required autoFocus />
        </Field>
        <Field label="Email" error={errors.email}>
          <input type="email" autoComplete="email" value={form.email} onChange={set('email')} required />
        </Field>
        <div className="form-row">
          <Field label="Password" error={errors.password} hint="At least 8 characters">
            <input type="password" autoComplete="new-password" minLength={8} value={form.password}
                   onChange={set('password')} required />
          </Field>
          <Field label="Confirm password" error={errors.confirm}>
            <input type="password" autoComplete="new-password" value={form.confirm} onChange={set('confirm')} required />
          </Field>
        </div>
        <button className="btn btn-primary btn-block" disabled={busy}>{busy ? 'Creating…' : 'Create account'}</button>
      </form>
    </AuthLayout>
  );
}
