const LABELS = {
  ABERTA: 'Aberta',
  PAUSADA: 'Pausada',
  ENCERRADA: 'Encerrada',
  CANCELADA: 'Cancelada',
};

export default function VacancyStatusBadge({ status }) {
  const normalized = String(status || 'DESCONHECIDA').toUpperCase();
  return (
    <span className={`management-status management-status--${normalized.toLowerCase()}`}>
      {LABELS[normalized] || normalized}
    </span>
  );
}

export { LABELS as VACANCY_STATUS_LABELS };
