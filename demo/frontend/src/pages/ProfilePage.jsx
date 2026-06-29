export function ProfilePage({
  displayName,
  setDisplayName,
  email,
  setEmail,
  newPassword,
  setNewPassword,
  repeatPassword,
  setRepeatPassword,
  wildberriesToken,
  setWildberriesToken,
  taxPercent,
  setTaxPercent,
  profileStatus,
  profileMessage,
  handleLogout,
  handleSaveAccount,
  handleChangePassword,
  handleSaveToken,
  handleSaveTaxPercent,
  requestDeleteAccount,
}) {
  return (
    <section className="panel" aria-label="Профиль продавца">
      <div className="section-header">
        <div>
          <h2>Профиль</h2>
          <p>Данные аккаунта, настройки безопасности и параметры продавца.</p>
        </div>
        <button type="button" className="secondary-button logout-button" onClick={handleLogout}>
          Выйти из аккаунта
        </button>
      </div>

      <div className="settings-block">
        <h3>Аккаунт</h3>
        <div className="account-grid">
          <label>
            Имя
            <input type="text" value={displayName} onChange={(event) => setDisplayName(event.target.value)} />
          </label>
          <label>
            Email
            <input type="email" value={email} onChange={(event) => setEmail(event.target.value)} />
          </label>
          <button type="button" className="secondary-button" disabled={profileStatus === 'loading'} onClick={handleSaveAccount}>
            Сохранить данные
          </button>
        </div>
      </div>

      <div className="settings-block">
        <h3>Безопасность</h3>
        <div className="account-grid">
          <label>
            Новый пароль
            <input
              type="password"
              value={newPassword}
              onChange={(event) => setNewPassword(event.target.value)}
              placeholder="Введите новый пароль"
            />
          </label>
          <label>
            Повторите пароль
            <input
              type="password"
              value={repeatPassword}
              onChange={(event) => setRepeatPassword(event.target.value)}
              placeholder="Повторите новый пароль"
            />
          </label>
          <button
            type="button"
            className="secondary-button"
            disabled={profileStatus === 'loading' || newPassword === '' || repeatPassword === ''}
            onClick={handleChangePassword}
          >
            Сменить пароль
          </button>
        </div>
      </div>

      <div className="settings-block">
        <h3>Настройки продавца</h3>
        <p>WB-токен хранится в зашифрованном виде и используется только для read-only загрузки отчетов.</p>
      </div>

      <div className="profile-grid">
        <label>
          WB API token
          <input
            type="password"
            value={wildberriesToken}
            onChange={(event) => setWildberriesToken(event.target.value)}
            placeholder="Вставьте read-only ключ"
          />
        </label>
        <button
          type="button"
          className="secondary-button"
          disabled={profileStatus === 'loading' || wildberriesToken.trim() === ''}
          onClick={handleSaveToken}
        >
          Сохранить токен
        </button>
        <label>
          Налог, %
          <input
            type="number"
            min="0"
            step="0.01"
            value={taxPercent}
            onChange={(event) => setTaxPercent(event.target.value)}
          />
        </label>
        <button type="button" className="secondary-button" disabled={profileStatus === 'loading'} onClick={handleSaveTaxPercent}>
          Сохранить налог
        </button>
      </div>

      {profileMessage && <div className={`notice ${profileStatus === 'error' ? 'error' : 'success'}`}>{profileMessage}</div>}

      <div className="danger-zone">
        <div>
          <h3>Удаление аккаунта</h3>
          <p>В будущем здесь будет удаление аккаунта и связанных пользовательских данных.</p>
        </div>
        <button type="button" className="danger-button" disabled={profileStatus === 'loading'} onClick={requestDeleteAccount}>
          Удалить аккаунт
        </button>
      </div>
    </section>
  );
}
