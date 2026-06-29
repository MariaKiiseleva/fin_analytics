export function AuthScreen({ mode, setMode, form, setForm, status, message, onSubmit }) {
  const isRegister = mode === 'register';

  function updateField(field, value) {
    setForm((current) => ({ ...current, [field]: value }));
  }

  return (
    <main className="auth-page">
      <section className="auth-card">
        <h1>{isRegister ? 'Регистрация' : 'Вход'}</h1>
        <p>{isRegister ? 'Создайте аккаунт для финансовой аналитики.' : 'Войдите, чтобы открыть свои данные.'}</p>

        <div className="auth-form">
          {isRegister && (
            <label>
              Имя
              <input type="text" value={form.displayName} onChange={(event) => updateField('displayName', event.target.value)} />
            </label>
          )}
          <label>
            Email
            <input type="email" value={form.email} onChange={(event) => updateField('email', event.target.value)} />
          </label>
          <label>
            Пароль
            <input type="password" value={form.password} onChange={(event) => updateField('password', event.target.value)} />
          </label>
          <button type="button" className="primary-button" disabled={status === 'loading'} onClick={onSubmit}>
            {isRegister ? 'Зарегистрироваться' : 'Войти'}
          </button>
        </div>

        {message && <div className={`notice ${status === 'error' ? 'error' : 'success'}`}>{message}</div>}

        <button
          type="button"
          className="link-button"
          onClick={() => setMode(isRegister ? 'login' : 'register')}
        >
          {isRegister ? 'Уже есть аккаунт? Войти' : 'Нет аккаунта? Зарегистрироваться'}
        </button>
      </section>
    </main>
  );
}
