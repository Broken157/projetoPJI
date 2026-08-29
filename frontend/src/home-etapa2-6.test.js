import fs from 'fs';
import path from 'path';

const html = fs.readFileSync(
  path.join(process.cwd(), 'public', 'home.html'),
  'utf8'
);

const script = fs.readFileSync(
  path.join(process.cwd(), 'public', 'js', 'home.js'),
  'utf8'
);

function montarLandingComLayout() {
  const corpo = html.match(/<body[^>]*>([\s\S]*?)<\/body>/i)[1]
    .replace(/<script[\s\S]*?<\/script>/gi, '');
  document.body.innerHTML = corpo;
  document.body.className = 'pagina-home';

  const medidas = [
    { client: 769, scroll: 2552, item: 300, passo: 328, inicio: 234 },
    { client: 892, scroll: 1866, item: 240, passo: 290, inicio: 326 },
    { client: 1000, scroll: 832, item: 190, passo: 214, inicio: 0 }
  ];

  document.querySelectorAll('[data-carrossel-trilha]').forEach((trilha, indice) => {
    const medida = medidas[indice];
    Object.defineProperty(trilha, 'clientWidth', { configurable: true, value: medida.client });
    Object.defineProperty(trilha, 'scrollWidth', { configurable: true, value: medida.scroll });
    trilha.scrollLeft = 0;

    Array.from(trilha.children).forEach((item, posicao) => {
      Object.defineProperty(item, 'offsetWidth', { configurable: true, value: medida.item });
      Object.defineProperty(item, 'offsetLeft', {
        configurable: true,
        value: medida.inicio + posicao * medida.passo
      });
    });
  });
}

test('markup da landing contém demos, Comunidade e categorias completas sem rotas falsas', () => {
  const documento = new DOMParser().parseFromString(html, 'text/html');
  expect(documento.querySelectorAll('.vaga-mini')).toHaveLength(8);
  expect(documento.querySelectorAll('.artista-card')).toHaveLength(7);
  expect(documento.querySelector('#titulo-comunidades').textContent)
    .toMatch(/Nossas\s*comunidades/i);
  expect(documento.querySelector('.comunidades__cta').getAttribute('aria-disabled')).toBe('true');
  expect(documento.querySelector('.comunidades__cta').hasAttribute('href')).toBe(false);
  expect(Array.from(documento.querySelectorAll('[data-categoria]')).map(card => card.dataset.categoria))
    .toEqual(expect.arrayContaining(['musica', 'artes-visuais', 'artes-cenicas', 'danca', 'arte-integrada']));
});

test('filtros, overflow, índice lógico e voltar ao topo funcionam localmente e com reduced motion', () => {
  montarLandingComLayout();

  let scrollAtual = 0;
  let tempo = 0;
  Object.defineProperty(window, 'scrollY', { configurable: true, get: () => scrollAtual });
  window.scrollTo = jest.fn();
  window.matchMedia = jest.fn(() => ({
    matches: true,
    addEventListener: jest.fn(),
    removeEventListener: jest.fn()
  }));
  window.requestAnimationFrame = jest.fn(callback => {
    tempo += 400;
    callback(tempo);
    return tempo;
  });
  window.cancelAnimationFrame = jest.fn();
  window.ResizeObserver = class {
    observe() {}
    disconnect() {}
  };

  window.eval(script);
  document.dispatchEvent(new Event('DOMContentLoaded'));

  const carrosseis = document.querySelectorAll('[data-carrossel]');
  const hero = carrosseis[0];
  const equipe = carrosseis[2];

  expect(hero).toHaveAttribute('data-carrossel-overflow', 'true');
  expect(hero).toHaveAttribute('data-carrossel-indice', '1');
  hero.querySelector('[data-carrossel-proximo]').click();
  expect(hero).toHaveAttribute('data-carrossel-indice', '2');
  expect(hero.querySelectorAll('.vaga-mini')[2]).toHaveClass('vaga-mini--destaque');

  expect(equipe).toHaveAttribute('data-carrossel-overflow', 'false');
  expect(equipe.querySelector('[data-carrossel-anterior]')).not.toBeVisible();
  expect(equipe.querySelector('[data-carrossel-proximo]')).not.toBeVisible();

  const musica = document.querySelector('[data-portfolio-filtro="musica"]');
  musica.click();
  expect(musica).toHaveAttribute('aria-pressed', 'true');
  expect(Array.from(document.querySelectorAll('.portfolio-card')).filter(card => !card.hidden))
    .toHaveLength(1);
  expect(document.querySelector('.portfolio-card:not([hidden])')).toHaveAttribute('data-categoria', 'musica');

  document.querySelector('[data-portfolio-filtro="todos"]').click();
  expect(Array.from(document.querySelectorAll('.portfolio-card')).filter(card => !card.hidden))
    .toHaveLength(5);

  const voltar = document.querySelector('[data-voltar-topo]');
  expect(voltar).toHaveAttribute('aria-hidden', 'true');
  scrollAtual = 350;
  window.dispatchEvent(new Event('scroll'));
  expect(voltar).toHaveAttribute('aria-hidden', 'false');
  voltar.click();
  expect(window.scrollTo).toHaveBeenCalledWith({ top: 0, left: 0, behavior: 'auto' });
});
