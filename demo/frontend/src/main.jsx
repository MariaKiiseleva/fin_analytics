import React, { useEffect, useMemo, useState } from 'react';
import { createRoot } from 'react-dom/client';
import { AuthScreen } from './components/AuthScreen.jsx';
import { ConfirmDialog } from './components/ConfirmDialog.jsx';
import { AdminPage } from './pages/AdminPage.jsx';
import { CostsPage } from './pages/CostsPage.jsx';
import { ProfilePage } from './pages/ProfilePage.jsx';
import './styles.css';

const MONTH_NAMES = [
  'Январь',
  'Февраль',
  'Март',
  'Апрель',
  'Май',
  'Июнь',
  'Июль',
  'Август',
  'Сентябрь',
  'Октябрь',
  'Ноябрь',
  'Декабрь',
];
const WEEK_DAYS = ['Пн', 'Вт', 'Ср', 'Чт', 'Пт', 'Сб', 'Вс'];
const CHART_COLORS = ['#2563eb', '#16a34a', '#f59e0b', '#dc2626', '#7c3aed', '#0891b2', '#db2777', '#65a30d', '#475569'];
const moneyFormatter = new Intl.NumberFormat('ru-RU', { style: 'currency', currency: 'RUB' });
const numberFormatter = new Intl.NumberFormat('ru-RU', { maximumFractionDigits: 2 });
const dateTimeFormatter = new Intl.DateTimeFormat('ru-RU', {
  dateStyle: 'short',
  timeStyle: 'short',
});

function formatDate(date) {
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  return `${year}-${month}-${day}`;
}

function parseDate(value) {
  const [year, month, day] = value.split('-').map(Number);
  return new Date(year, month - 1, day);
}

function toNumber(value) {
  return Number(value ?? 0);
}

function formatMoney(value) {
  return moneyFormatter.format(toNumber(value));
}

function formatPercent(value) {
  if (value === null || value === undefined) {
    return '—';
  }
  return `${numberFormatter.format(toNumber(value))}%`;
}

function formatDateTime(value) {
  if (!value) {
    return '—';
  }
  return dateTimeFormatter.format(new Date(value));
}

async function responseErrorMessage(response, fallback) {
  const text = await response.text();
  if (!text) {
    return fallback;
  }
  try {
    const payload = JSON.parse(text);
    return payload.message || payload.error || fallback;
  } catch (error) {
    return text;
  }
}

function buildPieGradient(items) {
  if (!items.length) {
    return '#e2e8f0';
  }

  let cursor = 0;
  const segments = items.map((item, index) => {
    const start = cursor;
    const end = cursor + toNumber(item.sharePercent);
    cursor = end;
    return `${CHART_COLORS[index % CHART_COLORS.length]} ${start}% ${end}%`;
  });
  return `conic-gradient(${segments.join(', ')})`;
}

function addMonths(date, amount) {
  return new Date(date.getFullYear(), date.getMonth() + amount, 1);
}

function getMonthBounds(monthDate) {
  const start = new Date(monthDate.getFullYear(), monthDate.getMonth(), 1);
  const end = new Date(monthDate.getFullYear(), monthDate.getMonth() + 1, 0);
  return { start, end };
}

function buildMonthDays(monthDate) {
  const { start, end } = getMonthBounds(monthDate);
  const firstWeekday = (start.getDay() + 6) % 7;
  const days = [];

  for (let index = 0; index < firstWeekday; index += 1) {
    days.push(null);
  }

  for (let day = 1; day <= end.getDate(); day += 1) {
    days.push(new Date(monthDate.getFullYear(), monthDate.getMonth(), day));
  }

  while (days.length % 7 !== 0) {
    days.push(null);
  }

  return days;
}

function LineChart({ points }) {
  if (!points.length) {
    return <p>Нет данных для графика.</p>;
  }

  const values = points.flatMap((point) => [toNumber(point.netRevenue), toNumber(point.totalProfit)]);
  const min = Math.min(0, ...values);
  const max = Math.max(0, ...values);
  const range = max - min || 1;
  const width = 640;
  const height = 220;
  const padding = 28;

  function coordinates(getter) {
    return points
      .map((point, index) => {
        const x = points.length === 1
          ? width / 2
          : padding + (index * (width - padding * 2)) / (points.length - 1);
        const y = height - padding - ((toNumber(getter(point)) - min) / range) * (height - padding * 2);
        return `${x},${y}`;
      })
      .join(' ');
  }

  return (
    <div className="line-chart-wrap">
      <svg className="line-chart" viewBox={`0 0 ${width} ${height}`} role="img" aria-label="График динамики выручки и прибыли">
        <line x1={padding} y1={height - padding} x2={width - padding} y2={height - padding} className="chart-axis" />
        <polyline points={coordinates((point) => point.netRevenue)} className="chart-line revenue" />
        <polyline points={coordinates((point) => point.totalProfit)} className="chart-line profit" />
      </svg>
      <div className="chart-caption">
        <span><i className="caption-dot revenue" /> Чистая выручка</span>
        <span><i className="caption-dot profit" /> Итоговая прибыль</span>
      </div>
    </div>
  );
}

function ProductRating({ title, rows, emptyText }) {
  return (
    <section className="analytics-card">
      <h3>{title}</h3>
      <div className="table-wrap compact">
        <table>
          <thead>
            <tr>
              <th>NM ID</th>
              <th>Товар</th>
              <th>Выручка</th>
              <th>Прибыль</th>
              <th>Маржа</th>
            </tr>
          </thead>
          <tbody>
            {rows.length === 0 && (
              <tr>
                <td colSpan="5">{emptyText}</td>
              </tr>
            )}
            {rows.map((row) => (
              <tr key={row.nmId}>
                <td>{row.nmId}</td>
                <td>{row.productName}</td>
                <td>{formatMoney(row.netRevenue)}</td>
                <td>{formatMoney(row.profit)}</td>
                <td>{formatPercent(row.marginPercent)}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </section>
  );
}

function App() {
  const [activePage, setActivePage] = useState('finance');
  const [currentUser, setCurrentUser] = useState(null);
  const [authMode, setAuthMode] = useState('login');
  const [authForm, setAuthForm] = useState({ displayName: '', email: '', password: '' });
  const [authStatus, setAuthStatus] = useState('loading');
  const [authMessage, setAuthMessage] = useState('');
  const [loadDateFrom, setLoadDateFrom] = useState('2026-06-21');
  const [loadDateTo, setLoadDateTo] = useState('2026-06-24');
  const [reportDateFrom, setReportDateFrom] = useState('2026-06-21');
  const [reportDateTo, setReportDateTo] = useState('2026-06-24');
  const [deleteDateFrom, setDeleteDateFrom] = useState('2026-06-21');
  const [deleteDateTo, setDeleteDateTo] = useState('2026-06-24');
  const [visibleMonth, setVisibleMonth] = useState(() => parseDate('2026-06-01'));

  const [wildberriesToken, setWildberriesToken] = useState('');
  const [displayName, setDisplayName] = useState('');
  const [email, setEmail] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [repeatPassword, setRepeatPassword] = useState('');
  const [taxPercent, setTaxPercent] = useState('0');
  const [profileStatus, setProfileStatus] = useState('idle');
  const [profileMessage, setProfileMessage] = useState('');

  const [costFile, setCostFile] = useState(null);
  const [costSearchNmId, setCostSearchNmId] = useState('');
  const [productCosts, setProductCosts] = useState([]);
  const [costImportStatus, setCostImportStatus] = useState('idle');
  const [costImportMessage, setCostImportMessage] = useState('');

  const [coverage, setCoverage] = useState([]);
  const [coverageStatus, setCoverageStatus] = useState('idle');
  const [coverageReloadKey, setCoverageReloadKey] = useState(0);

  const [syncStatus, setSyncStatus] = useState('idle');
  const [syncMessage, setSyncMessage] = useState('');
  const [deleteStatus, setDeleteStatus] = useState('idle');
  const [deleteMessage, setDeleteMessage] = useState('');
  const [recalculateStatus, setRecalculateStatus] = useState('idle');
  const [recalculateMessage, setRecalculateMessage] = useState('');
  const [adminUsers, setAdminUsers] = useState([]);
  const [selectedAdminUserId, setSelectedAdminUserId] = useState(null);
  const [adminStatus, setAdminStatus] = useState('idle');
  const [adminMessage, setAdminMessage] = useState('');
  const [analyticsReport, setAnalyticsReport] = useState(null);
  const [confirmDialog, setConfirmDialog] = useState(null);
  const userId = currentUser?.userId;
  const isAdmin = currentUser?.role === 'ADMIN';

  const coverageByDate = useMemo(() => {
    return new Map(coverage.map((day) => [day.date, day]));
  }, [coverage]);

  const visibleMonthDays = useMemo(() => {
    return buildMonthDays(visibleMonth);
  }, [visibleMonth]);

  const filteredProductCosts = useMemo(() => {
    const query = costSearchNmId.trim();
    if (!query) {
      return productCosts;
    }
    return productCosts.filter((item) => String(item.nmId).includes(query));
  }, [costSearchNmId, productCosts]);

  const selectedAdminUser = useMemo(() => {
    return adminUsers.find((user) => user.userId === selectedAdminUserId) ?? adminUsers[0] ?? null;
  }, [adminUsers, selectedAdminUserId]);

  useEffect(() => {
    loadCurrentUser();
  }, []);

  async function loadCurrentUser() {
    try {
      const response = await fetch('/api/auth/me', { credentials: 'include' });

      if (!response.ok) {
        throw new Error(`HTTP ${response.status}`);
      }

      const responseText = await response.text();
      const user = responseText ? JSON.parse(responseText) : null;
      setCurrentUser(user);
      setAuthStatus(user ? 'success' : 'idle');
    } catch (error) {
      setCurrentUser(null);
      setAuthStatus('idle');
    }
  }

  async function handleAuthSubmit() {
    setAuthStatus('loading');
    setAuthMessage('');

    if (!authForm.email.trim() || !authForm.password.trim()) {
      setAuthStatus('error');
      setAuthMessage('Введите email и пароль.');
      return;
    }
    if (authMode === 'register' && !authForm.displayName.trim()) {
      setAuthStatus('error');
      setAuthMessage('Введите имя.');
      return;
    }
    if (authMode === 'register' && authForm.password.length < 8) {
      setAuthStatus('error');
      setAuthMessage('Пароль должен быть не короче 8 символов.');
      return;
    }

    try {
      const endpoint = authMode === 'register' ? '/api/auth/register' : '/api/auth/login';
      const body = authMode === 'register'
        ? authForm
        : { email: authForm.email, password: authForm.password };
      const response = await fetch(endpoint, {
        method: 'POST',
        credentials: 'include',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(body),
      });

      if (!response.ok) {
        throw new Error(await responseErrorMessage(response, 'Не удалось выполнить вход.'));
      }

      const user = await response.json();
      setCurrentUser(user);
      setDisplayName(user.displayName ?? '');
      setEmail(user.email ?? '');
      setAuthStatus('success');
      setAuthMessage('');
    } catch (error) {
      setCurrentUser(null);
      setAuthStatus('error');
      setAuthMessage(error.message);
    }
  }

  async function handleLogout() {
    await fetch('/api/auth/logout', { method: 'POST', credentials: 'include' });
    setCurrentUser(null);
    setAuthStatus('idle');
    setActivePage('finance');
    setAnalyticsReport(null);
    setCoverage([]);
    setProductCosts([]);
  }

  useEffect(() => {
    if (!userId) {
      return;
    }
    const controller = new AbortController();
    const { start, end } = getMonthBounds(visibleMonth);

    async function loadCoverage() {
      setCoverageStatus('loading');

      try {
        const params = new URLSearchParams({
          userId: String(userId),
          dateFrom: formatDate(start),
          dateTo: formatDate(end),
        });
        const response = await fetch(`/api/reports/coverage?${params}`, {
          signal: controller.signal,
        });

        if (!response.ok) {
          throw new Error(`HTTP ${response.status}`);
        }

        setCoverage(await response.json());
        setCoverageStatus('loaded');
      } catch (error) {
        if (error.name !== 'AbortError') {
          setCoverage([]);
          setCoverageStatus('error');
        }
      }
    }

    loadCoverage();

    return () => controller.abort();
  }, [visibleMonth, coverageReloadKey, userId]);

  useEffect(() => {
    if (activePage === 'costs' && userId) {
      loadProductCosts();
    }
  }, [activePage, userId]);

  useEffect(() => {
    if (activePage === 'profile' && userId) {
      loadUserSettings();
    }
  }, [activePage, userId]);

  useEffect(() => {
    if (activePage === 'admin' && userId && isAdmin) {
      loadSyncJobs();
    }
  }, [activePage, userId, isAdmin]);

  useEffect(() => {
    if (activePage === 'admin' && !isAdmin) {
      setActivePage('finance');
    }
  }, [activePage, isAdmin]);

  async function loadSyncJobs() {
    setAdminStatus('loading');
    setAdminMessage('');

    try {
      const response = await fetch('/api/admin/users', { credentials: 'include' });

      if (!response.ok) {
        throw new Error(await responseErrorMessage(response, `HTTP ${response.status}`));
      }

      const users = await response.json();
      setAdminUsers(users);
      setSelectedAdminUserId((current) => current ?? users[0]?.userId ?? null);
      setAdminStatus('success');
    } catch (error) {
      setAdminUsers([]);
      setSelectedAdminUserId(null);
      setAdminStatus('error');
      setAdminMessage(error.message || 'Не удалось загрузить пользователей.');
    }
  }

  async function loadUserSettings() {
    setProfileStatus('loading');
    setProfileMessage('');

    try {
      const response = await fetch(`/api/users/${userId}/settings`);

      if (!response.ok) {
        throw new Error(`HTTP ${response.status}`);
      }

      const settings = await response.json();
      setDisplayName(settings.displayName ?? '');
      setEmail(settings.email ?? '');
      setTaxPercent(String(settings.taxPercent ?? 0));
      setProfileStatus('idle');
    } catch (error) {
      setProfileStatus('error');
      setProfileMessage('Не удалось загрузить настройки профиля.');
    }
  }

  async function handleSaveAccount() {
    setProfileStatus('loading');
    setProfileMessage('');

    try {
      const response = await fetch(`/api/users/${userId}/account`, {
        method: 'PATCH',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ displayName, email }),
      });

      if (!response.ok) {
        throw new Error(`HTTP ${response.status}`);
      }

      const settings = await response.json();
      setDisplayName(settings.displayName ?? '');
      setEmail(settings.email ?? '');
      setTaxPercent(String(settings.taxPercent ?? 0));
      setProfileStatus('success');
      setProfileMessage('Данные аккаунта сохранены.');
    } catch (error) {
      setProfileStatus('error');
      setProfileMessage('Не удалось сохранить данные аккаунта.');
    }
  }

  async function handleChangePassword() {
    setProfileMessage('');

    if (newPassword.length < 8) {
      setProfileStatus('error');
      setProfileMessage('Пароль должен быть не короче 8 символов.');
      return;
    }

    if (newPassword !== repeatPassword) {
      setProfileStatus('error');
      setProfileMessage('Пароли не совпадают.');
      return;
    }

    setProfileStatus('loading');

    try {
      const response = await fetch(`/api/users/${userId}/password`, {
        method: 'PATCH',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ newPassword }),
      });

      if (!response.ok) {
        throw new Error(`HTTP ${response.status}`);
      }

      setNewPassword('');
      setRepeatPassword('');
      setProfileStatus('success');
      setProfileMessage('Пароль изменен.');
    } catch (error) {
      setProfileStatus('error');
      setProfileMessage('Не удалось изменить пароль.');
    }
  }

  async function handleSaveToken() {
    setProfileStatus('loading');
    setProfileMessage('');

    try {
      const response = await fetch('/api/credentials/wildberries', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          userId: userId,
          token: wildberriesToken,
        }),
      });

      if (!response.ok) {
        throw new Error(`HTTP ${response.status}`);
      }

      setWildberriesToken('');
      setProfileStatus('success');
      setProfileMessage('WB-токен сохранен.');
    } catch (error) {
      setProfileStatus('error');
      setProfileMessage('Не удалось сохранить WB-токен.');
    }
  }

  async function handleSaveTaxPercent() {
    setProfileStatus('loading');
    setProfileMessage('');

    try {
      const response = await fetch(`/api/users/${userId}/tax-percent`, {
        method: 'PATCH',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ taxPercent }),
      });

      if (!response.ok) {
        throw new Error(`HTTP ${response.status}`);
      }

      const settings = await response.json();
      setTaxPercent(String(settings.taxPercent ?? taxPercent));
      setProfileStatus('success');
      setProfileMessage('Процент налога сохранен.');
    } catch (error) {
      setProfileStatus('error');
      setProfileMessage('Не удалось сохранить процент налога.');
    }
  }

  async function handleSync() {
    setSyncStatus('loading');
    setSyncMessage('');

    try {
      const response = await fetch('/api/sync/wildberries', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          userId: userId,
          dateFrom: loadDateFrom,
          dateTo: loadDateTo,
        }),
      });

      if (!response.ok) {
        throw new Error(`HTTP ${response.status}`);
      }

      await response.json();
      setReportDateFrom(loadDateFrom);
      setReportDateTo(loadDateTo);
      setDeleteDateFrom(loadDateFrom);
      setDeleteDateTo(loadDateTo);
      setVisibleMonth(new Date(parseDate(loadDateFrom).getFullYear(), parseDate(loadDateFrom).getMonth(), 1));
      setCoverageReloadKey((value) => value + 1);
      setSyncStatus('success');
      setSyncMessage('Данные загружены.');
    } catch (error) {
      setSyncStatus('error');
      setSyncMessage('Не удалось загрузить данные. Проверьте backend, токен WB и выбранный период.');
    }
  }

  function requestDeletePeriod() {
    setConfirmDialog({
      type: 'delete-period',
      title: 'Удалить период?',
      message: `Финансовые данные за период ${deleteDateFrom} - ${deleteDateTo} будут удалены из отчета и календаря покрытия.`,
      confirmText: 'Удалить период',
    });
  }

  async function handleDeletePeriod() {
    setDeleteStatus('loading');
    setDeleteMessage('');

    try {
      const response = await fetch('/api/reports/period', {
        method: 'DELETE',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          userId: userId,
          dateFrom: deleteDateFrom,
          dateTo: deleteDateTo,
        }),
      });

      if (!response.ok) {
        throw new Error(`HTTP ${response.status}`);
      }

      await response.json();
      setCoverageReloadKey((value) => value + 1);
      setAnalyticsReport(null);
      setDeleteStatus('success');
      setDeleteMessage('Период удален.');
    } catch (error) {
      setDeleteStatus('error');
      setDeleteMessage('Не удалось удалить период. Проверьте backend и выбранные даты.');
    }
  }

  async function handleRecalculate() {
    setRecalculateStatus('loading');
    setRecalculateMessage('');

    try {
      const coverageParams = new URLSearchParams({
        userId: String(userId),
        dateFrom: reportDateFrom,
        dateTo: reportDateTo,
      });
      const coverageResponse = await fetch(`/api/reports/coverage?${coverageParams}`);

      if (!coverageResponse.ok) {
        throw new Error(`HTTP ${coverageResponse.status}`);
      }

      const periodCoverage = await coverageResponse.json();
      const missingDates = periodCoverage
        .filter((day) => !day.hasRawData && !day.hasDailyReport)
        .map((day) => day.date);

      if (missingDates.length > 0) {
        setAnalyticsReport(null);
        setRecalculateStatus('error');
        setRecalculateMessage(`Недостаточно данных для выбранного периода. Загрузите недостающие даты: ${missingDates.join(', ')}.`);
        return;
      }

      const response = await fetch('/api/reports/daily/recalculate', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          userId: userId,
          dateFrom: reportDateFrom,
          dateTo: reportDateTo,
        }),
      });

      if (!response.ok) {
        throw new Error(`HTTP ${response.status}`);
      }

      await response.json();

      const params = new URLSearchParams({
        userId: String(userId),
        dateFrom: reportDateFrom,
        dateTo: reportDateTo,
      });
      const analyticsResponse = await fetch(`/api/reports/analytics?${params}`);

      if (!analyticsResponse.ok) {
        throw new Error(`HTTP ${analyticsResponse.status}`);
      }

      setAnalyticsReport(await analyticsResponse.json());
      setRecalculateStatus('success');
      setRecalculateMessage('Аналитика построена.');
    } catch (error) {
      setRecalculateStatus('error');
      setAnalyticsReport(null);
      setRecalculateMessage('Не удалось построить аналитику. Проверьте backend и наличие данных за период.');
    }
  }

  function handleDownloadXlsx() {
    const params = new URLSearchParams({
      userId: String(userId),
      dateFrom: reportDateFrom,
      dateTo: reportDateTo,
    });
    window.location.href = `/api/reports/daily/export.xlsx?${params}`;
  }

  function handleDownloadCostTemplate() {
    window.location.href = '/api/product-costs/template.xlsx';
  }

  async function loadProductCosts() {
    try {
      const response = await fetch(`/api/product-costs?userId=${userId}`);

      if (!response.ok) {
        throw new Error(`HTTP ${response.status}`);
      }

      setProductCosts(await response.json());
    } catch (error) {
      setProductCosts([]);
      setCostImportStatus('error');
      setCostImportMessage('Не удалось загрузить таблицу себестоимости.');
    }
  }

  async function handleImportCosts() {
    if (!costFile) {
      return;
    }

    const lowerName = costFile.name.toLowerCase();
    const endpoint = lowerName.endsWith('.csv')
      ? '/api/product-costs/import.csv'
      : '/api/product-costs/import.xlsx';
    const formData = new FormData();
    formData.append('file', costFile);

    setCostImportStatus('loading');
    setCostImportMessage('');

    try {
      const response = await fetch(`${endpoint}?userId=${userId}`, {
        method: 'POST',
        body: formData,
      });

      if (!response.ok) {
        throw new Error(`HTTP ${response.status}`);
      }

      await response.json();
      await loadProductCosts();
      setCostImportStatus('success');
      setCostImportMessage('Себестоимость импортирована.');
    } catch (error) {
      setCostImportStatus('error');
      setCostImportMessage('Не удалось импортировать себестоимость. Проверьте формат файла.');
    }
  }

  function requestDeleteProductCost(item) {
    setConfirmDialog({
      type: 'delete-product-cost',
      productCostId: item.id,
      title: 'Удалить себестоимость?',
      message: `Строка для артикула ${item.nmId} с датой ${item.validFrom} будет удалена. Отчеты после этого могут пересчитаться иначе.`,
      confirmText: 'Удалить строку',
    });
  }

  function requestDeleteAccount() {
    setConfirmDialog({
      type: 'delete-account',
      title: 'Удалить аккаунт?',
      message: 'Будут удалены профиль, WB-токен, себестоимость, загрузки и финансовые отчеты. В локальной демо-версии для продолжения понадобится создать нового пользователя.',
      confirmText: 'Удалить аккаунт',
    });
  }

  async function handleDeleteProductCost(productCostId) {
    setCostImportStatus('loading');
    setCostImportMessage('');

    try {
      const response = await fetch(`/api/product-costs/${productCostId}?userId=${userId}`, {
        method: 'DELETE',
      });

      if (!response.ok) {
        throw new Error(`HTTP ${response.status}`);
      }

      await loadProductCosts();
      setCostImportStatus('success');
      setCostImportMessage('Строка себестоимости удалена.');
    } catch (error) {
      setCostImportStatus('error');
      setCostImportMessage('Не удалось удалить строку себестоимости.');
    }
  }

  async function handleDeleteAccount() {
    setProfileStatus('loading');
    setProfileMessage('');

    try {
      const response = await fetch(`/api/users/${userId}`, {
        method: 'DELETE',
      });

      if (!response.ok) {
        throw new Error(`HTTP ${response.status}`);
      }

      setDisplayName('');
      setEmail('');
      setTaxPercent('0');
      setWildberriesToken('');
      setProductCosts([]);
      setCoverage([]);
      setAnalyticsReport(null);
      setCurrentUser(null);
      setAuthStatus('idle');
      setProfileStatus('success');
      setProfileMessage('Аккаунт удален.');
    } catch (error) {
      setProfileStatus('error');
      setProfileMessage('Не удалось удалить аккаунт.');
    }
  }

  async function handleConfirmAction() {
    const dialog = confirmDialog;
    setConfirmDialog(null);
    if (!dialog) {
      return;
    }
    if (dialog.type === 'delete-period') {
      await handleDeletePeriod();
    }
    if (dialog.type === 'delete-product-cost') {
      await handleDeleteProductCost(dialog.productCostId);
    }
    if (dialog.type === 'delete-account') {
      await handleDeleteAccount();
    }
  }

  if (authStatus === 'loading' && !currentUser) {
    return (
      <main className="auth-page">
        <section className="auth-card">
          <h1>Загрузка</h1>
          <p>Проверяем текущую сессию.</p>
        </section>
      </main>
    );
  }

  if (!currentUser) {
    return (
      <AuthScreen
        mode={authMode}
        setMode={setAuthMode}
        form={authForm}
        setForm={setAuthForm}
        status={authStatus}
        message={authMessage}
        onSubmit={handleAuthSubmit}
      />
    );
  }

  return (
    <main className="app">
      <header className="toolbar">
        <div className="topbar">
          <div>
            <h1>Финансовая аналитика</h1>
          </div>
          <div className="user-menu">
            <div className="user-identity">
              <span>{currentUser.displayName}</span>
              <small>{currentUser.email}</small>
            </div>
          </div>
        </div>

        <nav className="main-nav" aria-label="Разделы приложения">
          <button type="button" className={activePage === 'finance' ? 'nav-button active' : 'nav-button'} onClick={() => setActivePage('finance')}>
            Финансовая аналитика
          </button>
          <button type="button" className={activePage === 'profile' ? 'nav-button active' : 'nav-button'} onClick={() => setActivePage('profile')}>
            Профиль
          </button>
          <button type="button" className={activePage === 'costs' ? 'nav-button active' : 'nav-button'} onClick={() => setActivePage('costs')}>
            Себестоимость
          </button>
          {isAdmin && (
            <button type="button" className={activePage === 'admin' ? 'nav-button active' : 'nav-button'} onClick={() => setActivePage('admin')}>
              Админка
            </button>
          )}
        </nav>
      </header>

      {activePage === 'profile' && (
        <ProfilePage
          displayName={displayName}
          setDisplayName={setDisplayName}
          email={email}
          setEmail={setEmail}
          newPassword={newPassword}
          setNewPassword={setNewPassword}
          repeatPassword={repeatPassword}
          setRepeatPassword={setRepeatPassword}
          wildberriesToken={wildberriesToken}
          setWildberriesToken={setWildberriesToken}
          taxPercent={taxPercent}
          setTaxPercent={setTaxPercent}
          profileStatus={profileStatus}
          profileMessage={profileMessage}
          handleLogout={handleLogout}
          handleSaveAccount={handleSaveAccount}
          handleChangePassword={handleChangePassword}
          handleSaveToken={handleSaveToken}
          handleSaveTaxPercent={handleSaveTaxPercent}
          requestDeleteAccount={requestDeleteAccount}
        />
      )}

      {activePage === 'finance' && (
        <>
          <section className="panel" aria-label="Загрузка данных WB">
            <div className="section-header">
              <div>
                <h2>Загрузка периода</h2>
                <p>Данные загружаются из WB Statistics API в режиме только чтения.</p>
              </div>
            </div>

            <div className="period-form">
              <label>
                С даты
                <input type="date" value={loadDateFrom} onChange={(event) => setLoadDateFrom(event.target.value)} />
              </label>
              <label>
                По дату
                <input type="date" value={loadDateTo} onChange={(event) => setLoadDateTo(event.target.value)} />
              </label>
              <button type="button" className="primary-button" disabled={syncStatus === 'loading'} onClick={handleSync}>
                {syncStatus === 'loading' ? 'Загрузка...' : 'Загрузить данные'}
              </button>
            </div>

            {syncMessage && <div className={`notice ${syncStatus === 'error' ? 'error' : 'success'}`}>{syncMessage}</div>}
          </section>

          <section className="panel" aria-label="Календарь покрытия дат">
            <div className="section-header">
              <div>
                <h2>Календарь покрытия</h2>
              </div>
              <span>{coverageStatus === 'loading' ? 'Загрузка...' : ''}</span>
            </div>

            <div className="legend">
              <span><b className="dot has-data"></b>Данные есть</span>
              <span><b className="dot empty"></b>Нет данных</span>
            </div>

            {coverageStatus === 'error' && (
              <div className="notice error">
                Не удалось загрузить покрытие. Проверьте, что Spring Boot запущен на 8080.
              </div>
            )}

            <div className="month-calendar">
              <div className="calendar-header">
                <button type="button" className="icon-button" onClick={() => setVisibleMonth((date) => addMonths(date, -1))}>
                  ‹
                </button>
                <strong>{MONTH_NAMES[visibleMonth.getMonth()]} {visibleMonth.getFullYear()}</strong>
                <button type="button" className="icon-button" onClick={() => setVisibleMonth((date) => addMonths(date, 1))}>
                  ›
                </button>
              </div>

              <div className="calendar-weekdays">
                {WEEK_DAYS.map((day) => <span key={day}>{day}</span>)}
              </div>

              <div className="calendar-month-grid">
                {visibleMonthDays.map((date, index) => {
                  if (!date) {
                    return <span className="calendar-cell blank" key={`blank-${index}`}></span>;
                  }
                  const isoDate = formatDate(date);
                  const day = coverageByDate.get(isoDate);
                  const hasData = Boolean(day?.hasDailyReport);

                  return (
                    <span className={hasData ? 'calendar-cell has-data' : 'calendar-cell'} key={isoDate}>
                      {date.getDate()}
                    </span>
                  );
                })}
              </div>
            </div>

            <div className="delete-period">
              <label>
                Удалить с
                <input type="date" value={deleteDateFrom} onChange={(event) => setDeleteDateFrom(event.target.value)} />
              </label>
              <label>
                Удалить по
                <input type="date" value={deleteDateTo} onChange={(event) => setDeleteDateTo(event.target.value)} />
              </label>
              <button type="button" className="danger-button" disabled={deleteStatus === 'loading'} onClick={requestDeletePeriod}>
                {deleteStatus === 'loading' ? 'Удаление...' : 'Удалить период'}
              </button>
            </div>

            {deleteMessage && <div className={`notice ${deleteStatus === 'error' ? 'error' : 'success'}`}>{deleteMessage}</div>}
          </section>

          <section className="panel" aria-label="Построение отчета">
            <div className="section-header">
              <div>
                <h2>Аналитика за период</h2>
              </div>
            </div>

            <div className="period-form">
              <label>
                С даты
                <input type="date" value={reportDateFrom} onChange={(event) => setReportDateFrom(event.target.value)} />
              </label>
              <label>
                По дату
                <input type="date" value={reportDateTo} onChange={(event) => setReportDateTo(event.target.value)} />
              </label>
              <button type="button" className="primary-button" disabled={recalculateStatus === 'loading'} onClick={handleRecalculate}>
                {recalculateStatus === 'loading' ? 'Построение...' : 'Показать аналитику'}
              </button>
              <button type="button" className="secondary-button" onClick={handleDownloadXlsx}>
                Скачать Excel-отчет
              </button>
            </div>

            {recalculateMessage && <div className={`notice ${recalculateStatus === 'error' ? 'error' : 'success'}`}>{recalculateMessage}</div>}

            {analyticsReport && (
              <div className="analytics-block">
                <div className="kpi-grid">
                  <div className="kpi-card">
                    <span>Чистая выручка</span>
                    <strong>{formatMoney(analyticsReport.summary.netRevenue)}</strong>
                  </div>
                  <div className="kpi-card">
                    <span>Расходы WB</span>
                    <strong>{formatMoney(analyticsReport.summary.wildberriesExpenses)}</strong>
                  </div>
                  <div className="kpi-card">
                    <span>Себестоимость</span>
                    <strong>{formatMoney(analyticsReport.summary.costAmount)}</strong>
                  </div>
                  <div className="kpi-card">
                    <span>Итоговая прибыль</span>
                    <strong>{formatMoney(analyticsReport.summary.totalProfit)}</strong>
                  </div>
                  <div className="kpi-card">
                    <span>Маржинальность</span>
                    <strong>{formatPercent(analyticsReport.summary.marginPercent)}</strong>
                  </div>
                  <div className="kpi-card">
                    <span>Без себестоимости</span>
                    <strong>{analyticsReport.summary.productsWithoutCost}</strong>
                  </div>
                </div>

                <div className="analytics-grid">
                  <section className="analytics-card">
                    <h3>Динамика прибыли за период</h3>
                    <LineChart points={analyticsReport.dynamics} />
                  </section>

                  <section className="analytics-card">
                    <h3>Структура затрат</h3>
                    <div className="cost-chart">
                      <div
                        className="donut-chart"
                        style={{ background: buildPieGradient(analyticsReport.costStructure) }}
                        aria-label="Круговая диаграмма структуры затрат"
                      />
                      <div className="cost-legend">
                        {analyticsReport.costStructure.length === 0 && <p>Нет затрат за выбранный период.</p>}
                        {analyticsReport.costStructure.map((item, index) => (
                          <div className="cost-legend-row" key={item.code}>
                            <span className="legend-color" style={{ background: CHART_COLORS[index % CHART_COLORS.length] }} />
                            <span>{item.label}</span>
                            <strong>{formatMoney(item.amount)}</strong>
                            <em>{formatPercent(item.sharePercent)}</em>
                          </div>
                        ))}
                      </div>
                    </div>
                  </section>
                </div>

                <div className="analytics-grid">
                  <ProductRating title="Топ-5 прибыльных товаров" rows={analyticsReport.topProducts} emptyText="Нет товаров за выбранный период." />
                  <ProductRating title="Убыточные товары" rows={analyticsReport.lossProducts} emptyText="Убыточных товаров нет." />
                </div>
              </div>
            )}
          </section>
        </>
      )}

      {activePage === 'costs' && (
        <CostsPage
          costFile={costFile}
          setCostFile={setCostFile}
          costSearchNmId={costSearchNmId}
          setCostSearchNmId={setCostSearchNmId}
          productCosts={productCosts}
          filteredProductCosts={filteredProductCosts}
          costImportStatus={costImportStatus}
          costImportMessage={costImportMessage}
          handleDownloadCostTemplate={handleDownloadCostTemplate}
          handleImportCosts={handleImportCosts}
          requestDeleteProductCost={requestDeleteProductCost}
        />
      )}

      {activePage === 'admin' && isAdmin && (
        <AdminPage
          adminUsers={adminUsers}
          selectedAdminUser={selectedAdminUser}
          setSelectedAdminUserId={setSelectedAdminUserId}
          adminStatus={adminStatus}
          adminMessage={adminMessage}
          loadSyncJobs={loadSyncJobs}
          formatDateTime={formatDateTime}
          formatPercent={formatPercent}
        />
      )}

      <ConfirmDialog
        dialog={confirmDialog}
        onCancel={() => setConfirmDialog(null)}
        onConfirm={handleConfirmAction}
      />
    </main>
  );
}

createRoot(document.getElementById('root')).render(<App />);
