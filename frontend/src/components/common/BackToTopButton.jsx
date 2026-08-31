import { useEffect, useState } from 'react';

export default function BackToTopButton({ variant = 'search' }) {
  const [visible, setVisible] = useState(() => window.scrollY > 300);

  useEffect(() => {
    let frame = 0;
    const requestFrame = window.requestAnimationFrame || ((callback) => window.setTimeout(callback, 0));
    const cancelFrame = window.cancelAnimationFrame || window.clearTimeout;

    function update() {
      frame = 0;
      setVisible(window.scrollY > 300);
    }

    function onScroll() {
      if (!frame) frame = requestFrame(update);
    }

    update();
    window.addEventListener('scroll', onScroll, { passive: true });
    return () => {
      window.removeEventListener('scroll', onScroll);
      if (frame) cancelFrame(frame);
    };
  }, []);

  if (!visible) return null;

  const reducedMotion = window.matchMedia?.('(prefers-reduced-motion: reduce)').matches;
  const className = variant === 'home' ? 'voltar-topo voltar-topo--visivel' : 'busca-voltar-topo busca-voltar-topo--visivel';

  return (
    <button
      className={className}
      type="button"
      aria-label="Voltar ao topo"
      onClick={() => window.scrollTo({ top: 0, left: 0, behavior: reducedMotion ? 'auto' : 'smooth' })}
    >
      {variant === 'home' ? <img src="/assets/seta-select.svg" alt="" /> : <span aria-hidden="true">↑</span>}
    </button>
  );
}
