export default function LoadingState({ message = 'Carregando…' }) {
  return (
    <div className="react-state" role="status" aria-live="polite">
      <span className="react-state__indicator" aria-hidden="true" />
      <p>{message}</p>
    </div>
  );
}
