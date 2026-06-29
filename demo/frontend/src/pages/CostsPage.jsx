export function CostsPage({
  costFile,
  setCostFile,
  costSearchNmId,
  setCostSearchNmId,
  productCosts,
  filteredProductCosts,
  costImportStatus,
  costImportMessage,
  handleDownloadCostTemplate,
  handleImportCosts,
  requestDeleteProductCost,
}) {
  return (
    <section className="panel" aria-label="Себестоимость">
      <div className="section-header">
        <div>
          <h2>Себестоимость</h2>
          <p>Загрузите себестоимость товаров из CSV/XLSX, скачайте шаблон или найдите товар по nmId.</p>
        </div>
      </div>

      <div className="cost-actions">
        <button type="button" className="secondary-button" onClick={handleDownloadCostTemplate}>
          Скачать шаблон
        </button>
        <label>
          Файл себестоимости
          <input type="file" accept=".csv,.xlsx,.xls" onChange={(event) => setCostFile(event.target.files?.[0] ?? null)} />
        </label>
        <button type="button" className="primary-button" disabled={!costFile || costImportStatus === 'loading'} onClick={handleImportCosts}>
          {costImportStatus === 'loading' ? 'Загрузка...' : 'Загрузить себестоимость'}
        </button>
      </div>

      {costImportMessage && <div className={`notice ${costImportStatus === 'error' ? 'error' : 'success'}`}>{costImportMessage}</div>}

      <div className="cost-table-header">
        <label>
          Поиск по nmId
          <input
            type="search"
            value={costSearchNmId}
            onChange={(event) => setCostSearchNmId(event.target.value)}
            placeholder="Например, 125167917"
          />
        </label>
        <span>Показано: {filteredProductCosts.length} из {productCosts.length}</span>
      </div>

      <div className="table-wrap">
        <table>
          <thead>
            <tr>
              <th>NM ID</th>
              <th>Товар</th>
              <th>Действует с</th>
              <th>Себестоимость</th>
              <th></th>
            </tr>
          </thead>
          <tbody>
            {filteredProductCosts.map((item) => (
              <tr key={item.id}>
                <td>{item.nmId}</td>
                <td>{item.productName}</td>
                <td>{item.validFrom}</td>
                <td>{item.costAmount}</td>
                <td>
                  <button
                    type="button"
                    className="table-danger-button"
                    onClick={() => requestDeleteProductCost(item)}
                  >
                    Удалить
                  </button>
                </td>
              </tr>
            ))}
            {filteredProductCosts.length === 0 && (
              <tr>
                <td colSpan="5">Себестоимость пока не загружена.</td>
              </tr>
            )}
          </tbody>
        </table>
      </div>
    </section>
  );
}
