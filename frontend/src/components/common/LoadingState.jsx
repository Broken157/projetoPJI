export default function LoadingState({
  message = 'Carregando…',
  className = 'react-state',
  indicatorClassName = 'react-state__indicator',
}) {
  return (
    <div className={className} role="status" aria-live="polite">
      <span className={indicatorClassName} aria-hidden="true" />
      <p>{message}</p>
    </div>
  );
}
