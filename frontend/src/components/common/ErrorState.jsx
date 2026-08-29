export default function ErrorState({ error, title = 'Não foi possível carregar o conteúdo.' }) {
  const message = error?.message || 'Tente novamente em instantes.';

  return (
    <section className="react-state react-state--error" role="alert">
      <h2>{title}</h2>
      <p>{message}</p>
    </section>
  );
}
