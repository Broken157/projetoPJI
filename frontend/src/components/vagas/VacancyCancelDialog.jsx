import { useEffect, useRef, useState } from 'react';

export default function VacancyCancelDialog({ open, vacancyTitle, onClose, onConfirm }) {
  const [reason, setReason] = useState('');
  const [confirmed, setConfirmed] = useState(false);
  const [error, setError] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const submittingRef = useRef(false);

  useEffect(() => {
    if (!open) return;
    setReason('');
    setConfirmed(false);
    setError('');
  }, [open]);

  if (!open) return null;

  async function submit(event) {
    event.preventDefault();
    if (submittingRef.current) return;
    if (!confirmed) {
      setError('Confirme que deseja cancelar a vaga.');
      return;
    }
    if (!reason.trim()) {
      setError('Informe o motivo do cancelamento.');
      return;
    }
    submittingRef.current = true;
    setSubmitting(true);
    setError('');
    try {
      await onConfirm(reason.trim());
    } finally {
      submittingRef.current = false;
      setSubmitting(false);
    }
  }

  return (
    <div className="management-modal-overlay" role="presentation" onMouseDown={(event) => { if (event.target === event.currentTarget && !submitting) onClose(); }}>
      <section className="management-modal" role="dialog" aria-modal="true" aria-labelledby="cancel-title">
        <p className="management-eyebrow">Cancelamento lógico</p>
        <h2 id="cancel-title">Cancelar “{vacancyTitle}”?</h2>
        <p>A vaga e suas candidaturas permanecerão no histórico. Esta ação não pode ser revertida pela interface.</p>
        <form onSubmit={submit} noValidate>
          <label className="campo"><span className="campo__rotulo">Motivo do cancelamento</span><textarea className="campo__input" value={reason} onChange={(event) => setReason(event.target.value)} required /></label>
          <label className="management-confirm"><input type="checkbox" checked={confirmed} onChange={(event) => setConfirmed(event.target.checked)} /> Confirmo o cancelamento desta vaga.</label>
          {error ? <p className="management-feedback management-feedback--error" role="alert">{error}</p> : null}
          <div className="management-modal__actions"><button className="btn management-button-secondary" type="button" onClick={onClose} disabled={submitting}>Voltar</button><button className="btn management-button-danger" type="submit" disabled={submitting}>{submitting ? 'Cancelando…' : 'Cancelar vaga'}</button></div>
        </form>
      </section>
    </div>
  );
}
