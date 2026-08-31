import fs from 'fs';
import path from 'path';

const publicDir = path.join(process.cwd(), 'public');

function readPublic(relativePath) {
  return fs.readFileSync(path.join(publicDir, relativePath), 'utf8');
}

test('links produtivos de autenticação apontam para as rotas React', () => {
  expect(readPublic('home.html')).toContain('href="/login"');
  expect(readPublic('home.html')).toContain('href="/cadastro"');
  expect(readPublic('buscar-vagas.html')).toContain('href="/login"');
  expect(readPublic('login.html')).toContain('href="/cadastro"');
  expect(readPublic('cadastro-contratante.html')).toContain('href="/login"');
  expect(readPublic(path.join('js', 'main.js'))).toContain("window.location.href = '/login'");
  expect(readPublic(path.join('js', 'main.js'))).toContain("'/login?cadastro=sucesso'");
});

test('páginas legadas de login e cadastro permanecem disponíveis para rollback', () => {
  expect(fs.existsSync(path.join(publicDir, 'login.html'))).toBe(true);
  expect(fs.existsSync(path.join(publicDir, 'cadastro-contratante.html'))).toBe(true);
});
