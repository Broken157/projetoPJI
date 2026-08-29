export default function NotFound() {
  return (
    <main className="react-foundation">
      <section className="react-foundation__card" aria-labelledby="not-found-title">
        <p className="react-foundation__eyebrow">Erro 404</p>
        <h1 id="not-found-title" className="titulo-display">
          Página não encontrada
        </h1>
        <p>A rota React informada não existe.</p>
        <a className="react-foundation__link" href="/">
          Voltar à fundação React
        </a>
      </section>
    </main>
  );
}
