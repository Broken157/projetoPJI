import { notificationContext } from './notificationLinks';

test.each([
  ['dashboard-contratante.html', '/dashboard'], ['/dashboard', '/dashboard'],
  ['detalhe-vaga.html?id=42', '/vagas/42'], ['/vagas/42', '/vagas/42'],
  ['mensagens?sala=7', '/mensagens?sala=7'], ['/mensagens?sala=7', '/mensagens?sala=7'],
  ['detalhe-vaga-proprietario.html?id=42', '/vagas/42/gerenciar'],
  ['perfil-publico.html?tipo=ARTISTA&id=7', '/perfis/ARTISTA/7'],
  ['https://palco.test/dashboard', '/dashboard'],
])('normaliza contexto conhecido %s', (input, expected) => {
  expect(notificationContext(input, 'https://palco.test')).toBe(expected);
});

test.each([
  'javascript:alert(1)', 'data:text/html,oi', 'https://evil.test/dashboard', '//evil.test/dashboard',
  '/admin', '/login', '/dashboard?next=https://evil.test', '/dashboard#x',
  '/detalhe-vaga.html?id=0', '/detalhe-vaga.html?id=-1', '/detalhe-vaga.html?id=abc',
  '/detalhe-vaga.html?id=1&id=2', '/detalhe-vaga.html?id=9223372036854775808',
  '/mensagens?sala=1&usuarioId=3', '/mensagens', '/mensagens?sala=1.5',
  '/vagas/%31', '/vagas/1/editar', '/perfis/ADMIN/1', null, '',
])('bloqueia URL ou ID não permitido: %s', (input) => {
  expect(notificationContext(input, 'https://palco.test')).toBeNull();
});
