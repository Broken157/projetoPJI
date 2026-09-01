import fs from 'fs';
import path from 'path';

const publicDir = path.join(process.cwd(), 'public');
const sourceDir = path.join(process.cwd(), 'src');

function read(base, relativePath) {
  return fs.readFileSync(path.join(base, relativePath), 'utf8');
}

test('links produtivos de conta apontam para dashboard e perfil React', () => {
  const mainScript = read(publicDir, path.join('js', 'main.js'));
  expect(mainScript).toContain("window.location.href = '/dashboard'");
  expect(mainScript).toContain("['Meu perfil', '/perfil']");
  expect(mainScript).toContain("link('Editar perfil', '/perfil'");

  [
    'confirmar-exclusao-vaga.html',
    'dashboard-contratante.html',
    'detalhe-vaga-proprietario.html',
    'detalhe-vaga.html',
    'editar-vagas-2.html',
    'editar-vagas.html',
    'mensagens.html',
    'minhas-vagas.html',
    'perfil.html',
    'publicar-vaga.html',
  ].forEach((file) => {
    const html = read(publicDir, file);
    expect(html).not.toMatch(/href=["']dashboard-contratante\.html["']/);
    expect(html).not.toMatch(/href=["']perfil\.html["']/);
  });

  expect(read(sourceDir, path.join('components', 'candidaturas', 'CandidaturaAction.jsx')))
    .toContain('href="/perfil"');
  expect(read(sourceDir, path.join('pages', 'vagas', 'VagaDetailPage.jsx')))
    .not.toContain('/dashboard-contratante.html');
});

test('arquivos legados de dashboard e perfil continuam disponíveis para rollback', () => {
  expect(fs.existsSync(path.join(publicDir, 'dashboard-contratante.html'))).toBe(true);
  expect(fs.existsSync(path.join(publicDir, 'perfil.html'))).toBe(true);
});
