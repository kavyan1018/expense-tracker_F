export default function AuthLayout({ title, subtitle, children, footer }) {
  return (
    <div className="auth-page">
      <div className="auth-card">
        <div className="brand brand-center">
          <span className="brand-mark">₹</span>
          <span>Daily Expenses Track</span>
        </div>
        <h1>{title}</h1>
        <p className="muted">{subtitle}</p>
        {children}
        <div className="auth-foot">{footer}</div>
      </div>
    </div>
  );
}
