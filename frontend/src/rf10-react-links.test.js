const fs = require('fs');
const path = require('path');

const publicDir = path.join(process.cwd(), 'public');
const mainScript = fs.readFileSync(path.join(publicDir, 'js', 'main.js'), 'utf8');

test('links produtivos do perfil público apontam para a rota React', () => {
  expect(mainScript).toContain("'/perfis/ARTISTA/' + encodeURIComponent(candidatura.artistaId)");
  expect(mainScript).toContain("'/perfis/ARTISTA/' + encodeURIComponent(talento.artistaId)");
});

test('perfil privado e rollback legado permanecem disponíveis', () => {
  expect(fs.existsSync(path.join(publicDir, 'perfil.html'))).toBe(true);
  expect(fs.existsSync(path.join(publicDir, 'perfil-publico.html'))).toBe(true);
  expect(fs.existsSync(path.join(publicDir, 'js', 'perfil-publico.js'))).toBe(true);
  expect(fs.existsSync(path.join(publicDir, 'css', 'perfil-publico.css'))).toBe(true);
});
