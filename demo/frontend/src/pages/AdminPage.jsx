export function AdminPage({
  adminUsers,
  selectedAdminUser,
  setSelectedAdminUserId,
  adminStatus,
  adminMessage,
  loadSyncJobs,
  formatDateTime,
  formatPercent,
}) {
  return (
    <section className="panel" aria-label="Админка">
      <div className="section-header">
        <div>
          <h2>Админка</h2>
          <p>Пользователи, служебная информация и история загрузок.</p>
        </div>
        <button type="button" className="secondary-button" disabled={adminStatus === 'loading'} onClick={loadSyncJobs}>
          Обновить
        </button>
      </div>

      {adminMessage && <div className={`notice ${adminStatus === 'error' ? 'error' : 'success'}`}>{adminMessage}</div>}

      <div className="admin-layout">
        <div className="admin-users-list">
          <div className="admin-list-header">Всего пользователей: {adminUsers.length}</div>
          {adminUsers.map((user) => (
            <button
              type="button"
              key={user.userId}
              className={selectedAdminUser?.userId === user.userId ? 'admin-user-card active' : 'admin-user-card'}
              onClick={() => setSelectedAdminUserId(user.userId)}
            >
              <span>{user.displayName || 'Без имени'}</span>
              <small>{user.email}</small>
              <em>{user.role === 'ADMIN' ? 'Администратор' : 'Пользователь'}</em>
            </button>
          ))}
          {adminUsers.length === 0 && (
            <div className="empty-state">{adminStatus === 'loading' ? 'Загрузка пользователей...' : 'Пользователи пока не найдены.'}</div>
          )}
        </div>

        {selectedAdminUser && (
          <div className="admin-detail">
            <section className="analytics-card">
              <div className="admin-detail-header">
                <div>
                  <h3>Пользователь #{selectedAdminUser.userId}</h3>
                  <p>{selectedAdminUser.displayName} · {selectedAdminUser.email}</p>
                </div>
                <span className={`status-pill ${selectedAdminUser.enabled ? 'completed' : 'failed'}`}>
                  {selectedAdminUser.enabled ? 'Активен' : 'Отключен'}
                </span>
              </div>

              <div className="admin-facts">
                <div><span>Роль</span><strong>{selectedAdminUser.role === 'ADMIN' ? 'Администратор' : 'Пользователь'}</strong></div>
                <div><span>Дата регистрации</span><strong>{formatDateTime(selectedAdminUser.createdAt)}</strong></div>
                <div><span>Обновлен</span><strong>{formatDateTime(selectedAdminUser.updatedAt)}</strong></div>
                <div><span>Налог</span><strong>{formatPercent(selectedAdminUser.taxPercent)}</strong></div>
                <div><span>WB-токен</span><strong>{selectedAdminUser.hasWildberriesToken ? 'Есть' : 'Нет'}</strong></div>
                <div><span>Последняя загрузка</span><strong>{formatDateTime(selectedAdminUser.lastSyncAt)}</strong></div>
                <div><span>RAW-строк</span><strong>{selectedAdminUser.rawRows}</strong></div>
                <div><span>Строк отчета</span><strong>{selectedAdminUser.dailyRows}</strong></div>
                <div><span>Себестоимость</span><strong>{selectedAdminUser.productCostRows}</strong></div>
                <div><span>Загрузок</span><strong>{selectedAdminUser.syncJobsCount}</strong></div>
              </div>
            </section>

            <section className="analytics-card">
              <h3>История загрузок WB API</h3>
              <div className="table-wrap compact">
                <table>
                  <thead>
                    <tr>
                      <th>Дата</th>
                      <th>Период</th>
                      <th>Статус</th>
                      <th>Получено</th>
                      <th>Сохранено</th>
                      <th>Ошибка</th>
                    </tr>
                  </thead>
                  <tbody>
                    {selectedAdminUser.syncJobs.map((job) => (
                      <tr key={job.id}>
                        <td>{formatDateTime(job.requestedAt)}</td>
                        <td>{job.dateFrom} — {job.dateTo}</td>
                        <td><span className={`status-pill ${String(job.status).toLowerCase()}`}>{job.status}</span></td>
                        <td>{job.receivedRows}</td>
                        <td>{job.insertedRows}</td>
                        <td>{job.errorMessage || '—'}</td>
                      </tr>
                    ))}
                    {selectedAdminUser.syncJobs.length === 0 && (
                      <tr>
                        <td colSpan="6">Загрузки не найдены.</td>
                      </tr>
                    )}
                  </tbody>
                </table>
              </div>
            </section>
          </div>
        )}
      </div>
    </section>
  );
}
