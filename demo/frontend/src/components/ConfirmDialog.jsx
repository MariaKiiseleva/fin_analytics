export function ConfirmDialog({ dialog, onCancel, onConfirm }) {
  if (!dialog) {
    return null;
  }

  return (
    <div className="modal-backdrop" role="presentation">
      <div className="confirm-dialog" role="dialog" aria-modal="true" aria-labelledby="confirm-title">
        <div className="confirm-icon">!</div>
        <div>
          <h2 id="confirm-title">{dialog.title}</h2>
          <p>{dialog.message}</p>
        </div>
        <div className="confirm-actions">
          <button type="button" className="secondary-button" onClick={onCancel}>
            Отмена
          </button>
          <button type="button" className="danger-button filled" onClick={onConfirm}>
            {dialog.confirmText}
          </button>
        </div>
      </div>
    </div>
  );
}
