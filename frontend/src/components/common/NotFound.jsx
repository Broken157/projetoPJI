export default function NotFound({
  title = 'Página não encontrada',
  message = 'A rota React informada não existe.',
  backHref = '/',
  backLabel = 'Voltar à fundação React',
  embedded = false,
}) {
  const content = (
    <section
      className={embedded ? 'dashboard-estado dashboard-estado--erro' : 'react-foundation__card'}
      aria-labelledby="not-found-title"
      role={embedded ? 'alert' : undefined}
    >
      {!embedded && <p className="react-foundation__eyebrow">Erro 404</p>}
      <h1 id="not-found-title" className={embedded ? undefined : 'titulo-display'}>
        {title}
      </h1>
      <p>{message}</p>
      <a
        className={embedded ? 'btn-dash btn-dash--primario' : 'react-foundation__link'}
        href={backHref}
      >
        {backLabel}
      </a>
    </section>
  );

  return embedded ? content : <main className="react-foundation">{content}</main>;
}
