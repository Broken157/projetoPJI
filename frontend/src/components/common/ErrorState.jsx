export default function ErrorState({
  error,
  title = 'Não foi possível carregar o conteúdo.',
  className = 'react-state react-state--error',
  headingLevel = 'h2',
  children,
}) {
  const message = error?.message || 'Tente novamente em instantes.';
  const Heading = headingLevel;

  return (
    <section className={className} role="alert">
      <Heading>{title}</Heading>
      <p>{message}</p>
      {children}
    </section>
  );
}
