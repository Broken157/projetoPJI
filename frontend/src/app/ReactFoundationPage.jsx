export default function ReactFoundationPage() {
  return (
    <main className="react-foundation">
      <section className="react-foundation__card" aria-labelledby="react-foundation-title">
        <p className="react-foundation__eyebrow">Migração incremental</p>
        <h1 id="react-foundation-title" className="titulo-display">
          Fundação React do Palco
        </h1>
        <p>
          A infraestrutura React está preparada. As páginas funcionais continuam no frontend
          legado durante a migração.
        </p>
        <a className="react-foundation__link" href="/home.html">
          Abrir home legada
        </a>
      </section>
    </main>
  );
}
