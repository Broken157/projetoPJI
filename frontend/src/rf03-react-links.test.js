import fs from 'fs';
import path from 'path';

const srcDir = path.join(__dirname);

test('links produtivos migrados usam React e rollback legado permanece disponível', () => {
  const detail = fs.readFileSync(path.join(srcDir, 'pages', 'vagas', 'VagaDetailPage.jsx'), 'utf8');
  const router = fs.readFileSync(path.join(srcDir, 'app', 'router', 'AppRouter.jsx'), 'utf8');
  const publicDir = path.join(srcDir, '..', 'public');

  expect(detail).not.toContain('/buscar-vagas.html');
  expect(detail).toContain('href="/vagas"');
  expect(router).toContain('path="/" element={<HomePage />}');
  expect(router).toContain('path="/vagas" element={<VacancySearchPage />}');
  expect(fs.existsSync(path.join(publicDir, 'home.html'))).toBe(true);
  expect(fs.existsSync(path.join(publicDir, 'buscar-vagas.html'))).toBe(true);
  expect(fs.existsSync(path.join(publicDir, 'js', 'home.js'))).toBe(true);
  expect(fs.existsSync(path.join(publicDir, 'js', 'buscar-vagas.js'))).toBe(true);
});
