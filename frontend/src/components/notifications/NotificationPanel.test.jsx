import { act, fireEvent, render, screen, waitFor, within } from '@testing-library/react';
import sessionService from '../../auth/sessionService';
import * as service from '../../services/notifications/notificationService';
import { connectNotificationRealtime } from '../../services/notifications/notificationRealtime';
import NotificationPanel from './NotificationPanel';

jest.mock('../../services/notifications/notificationService');
jest.mock('../../services/notifications/notificationRealtime', () => ({
  ...jest.requireActual('../../services/notifications/notificationRealtime'),
  connectNotificationRealtime: jest.fn(),
}));

const item = { id: 7, tipo: 'CANDIDATURA', mensagem: 'Nova candidatura recebida.', link: 'dashboard-contratante.html', lida: false, data: '2026-09-07T10:00:00' };
const page = (items = [item], rest = {}) => ({ content: items, page: 0, hasNext: false, ...rest });
let realtime;
let disconnect;
beforeEach(() => {
  jest.clearAllMocks();
  sessionStorage.clear();
  localStorage.clear();
  sessionService.saveSession({ token: `header.${btoa(JSON.stringify({ exp: Math.floor(Date.now() / 1000) + 3600 }))}.sig`, id: 2 });
  service.listNotifications.mockResolvedValue(page());
  service.countUnreadNotifications.mockResolvedValue({ count: 1 });
  service.markNotificationRead.mockResolvedValue(null);
  service.markAllNotificationsRead.mockResolvedValue(null);
  disconnect = jest.fn();
  connectNotificationRealtime.mockImplementation((callbacks) => { realtime = callbacks; return { disconnect }; });
});

test('loading inicial, lista cronológica e count vêm da API', async () => {
  render(<NotificationPanel />);
  expect(screen.getByText('Carregando notificações…')).toBeInTheDocument();
  expect(await screen.findByRole('link', { name: item.mensagem })).toHaveAttribute('href', '/dashboard');
  expect(screen.getByLabelText('1 notificações não lidas')).toHaveTextContent('1');
  expect(screen.getByText('Candidatura')).toBeInTheDocument();
  expect(service.listNotifications).toHaveBeenCalledWith(0, 20, expect.objectContaining({ token: expect.any(String), signal: expect.any(AbortSignal) }));
});

test('vazio só é exibido após resposta real sem notificações', async () => {
  service.listNotifications.mockResolvedValue(page([]));
  service.countUnreadNotifications.mockResolvedValue({ count: 0 });
  render(<NotificationPanel />);
  expect(await screen.findByText('Nenhuma notificação por enquanto.')).toBeInTheDocument();
  expect(screen.getByRole('button', { name: 'Marcar todas como lidas' })).toBeDisabled();
});

test('erro não inventa count ou estado vazio e permite tentar novamente', async () => {
  service.listNotifications.mockRejectedValueOnce(new Error('SQL segredo'));
  render(<NotificationPanel />);
  expect(await screen.findByRole('alert')).toHaveTextContent('Não foi possível atualizar');
  expect(screen.queryByText(/SQL segredo/)).toBeNull();
  expect(screen.queryByText('Nenhuma notificação por enquanto.')).toBeNull();
  fireEvent.click(screen.getByRole('button', { name: 'Atualizar notificações' }));
  expect(await screen.findByRole('link', { name: item.mensagem })).toBeInTheDocument();
});

test('marca uma lida, bloqueia duplo clique e recarrega count persistido', async () => {
  let resolve;
  service.markNotificationRead.mockReturnValue(new Promise((done) => { resolve = done; }));
  render(<NotificationPanel />);
  await screen.findByRole('link', { name: item.mensagem });
  const button = screen.getByRole('button', { name: 'Marcar como lida' });
  fireEvent.click(button);
  fireEvent.click(button);
  expect(button).toBeDisabled();
  expect(service.markNotificationRead).toHaveBeenCalledTimes(1);
  expect(service.markNotificationRead).toHaveBeenCalledWith(7, expect.objectContaining({ token: expect.any(String) }));
  service.listNotifications.mockResolvedValue(page([{ ...item, lida: true }]));
  service.countUnreadNotifications.mockResolvedValue({ count: 0 });
  await act(async () => { resolve(); });
  expect(await screen.findByText('Lida', { exact: true })).toBeInTheDocument();
  expect(screen.getByLabelText('0 notificações não lidas')).toBeInTheDocument();
});

test('marcar todas atualiza somente após confirmação e preserva falha de leitura', async () => {
  service.markAllNotificationsRead.mockRejectedValueOnce({ status: 404 });
  render(<NotificationPanel />);
  await screen.findByRole('link', { name: item.mensagem });
  fireEvent.click(screen.getByRole('button', { name: 'Marcar todas como lidas' }));
  expect(await screen.findByRole('alert')).toHaveTextContent('Notificação não encontrada');
  expect(screen.getByLabelText('1 notificações não lidas')).toBeInTheDocument();
  service.listNotifications.mockResolvedValue(page([{ ...item, lida: true }]));
  service.countUnreadNotifications.mockResolvedValue({ count: 0 });
  fireEvent.click(screen.getByRole('button', { name: 'Marcar todas como lidas' }));
  expect(await screen.findByText('Lida', { exact: true })).toBeInTheDocument();
  expect(service.markAllNotificationsRead).toHaveBeenCalledTimes(2);
});

test('STOMP/SSE e REST não duplicam itens, count nem popup do mesmo evento', async () => {
  render(<NotificationPanel />);
  await screen.findByRole('link', { name: item.mensagem });
  const message = { ...item, id: 8, tipo: 'MENSAGEM', mensagem: 'Nova mensagem.', link: '/mensagens?sala=9' };
  service.listNotifications.mockResolvedValue(page([message, message, item]));
  service.countUnreadNotifications.mockResolvedValue({ count: 2 });
  await act(async () => { realtime.onState('stomp'); realtime.onEvent(message); realtime.onEvent(message); });
  expect(screen.getByText('Alertas em tempo real')).toBeInTheDocument();
  expect(screen.getAllByText('Nova notificação')).toHaveLength(1);
  expect(within(screen.getByRole('list')).getAllByRole('listitem')).toHaveLength(2);
  expect(screen.getByLabelText('2 notificações não lidas')).toBeInTheDocument();
  expect(screen.getByRole('link', { name: 'Nova mensagem.' })).toHaveAttribute('href', '/mensagens?sala=9');
  fireEvent.click(screen.getByRole('button', { name: 'Fechar aviso de notificação' }));
  await act(async () => { realtime.onState('sse'); realtime.onEvent(message); });
  expect(screen.getByText('Alertas em tempo real (SSE)')).toBeInTheDocument();
  expect(screen.queryByText('Nova notificação')).toBeNull();
});

test('evento durante REST pendente agenda reconciliação e recupera notificação persistida', async () => {
  let resolve;
  service.listNotifications.mockReturnValueOnce(new Promise((done) => { resolve = done; }));
  render(<NotificationPanel />);
  act(() => realtime.onEvent(item));
  expect(service.listNotifications).toHaveBeenCalledTimes(1);
  await act(async () => resolve(page([])));
  await waitFor(() => expect(service.listNotifications).toHaveBeenCalledTimes(2));
  expect(await screen.findByRole('link', { name: item.mensagem })).toBeInTheDocument();
});

test('links históricos de vaga migram e texto/URL maliciosos não viram HTML executável', async () => {
  const malicious = { ...item, id: 8, mensagem: '<img src=x onerror=alert(1)>', link: 'javascript:alert(1)' };
  service.listNotifications.mockResolvedValue(page([{ ...item, link: 'detalhe-vaga.html?id=22' }, malicious]));
  render(<NotificationPanel />);
  expect(await screen.findByRole('link', { name: item.mensagem })).toHaveAttribute('href', '/vagas/22');
  expect(screen.getByText(malicious.mensagem)).toBeInTheDocument();
  expect(screen.queryByRole('link', { name: malicious.mensagem })).toBeNull();
  expect(document.querySelector('img[src=x]')).toBeNull();
});

test('pagina histórico pela API mantendo tamanho 20', async () => {
  service.listNotifications.mockResolvedValueOnce(page([item], { hasNext: true }));
  render(<NotificationPanel />);
  await screen.findByRole('link', { name: item.mensagem });
  service.listNotifications.mockResolvedValue(page([{ ...item, id: 1, mensagem: 'Mais antiga' }], { page: 1 }));
  fireEvent.click(screen.getByRole('button', { name: 'Próximas' }));
  expect(await screen.findByRole('link', { name: 'Mais antiga' })).toBeInTheDocument();
  expect(screen.getByText('Página 2')).toBeInTheDocument();
  expect(service.listNotifications).toHaveBeenLastCalledWith(1, 20, expect.any(Object));
});

test('cleanup desconecta, aborta requisições e ignora callbacks tardios', async () => {
  const { unmount } = render(<NotificationPanel />);
  await screen.findByRole('link', { name: item.mensagem });
  const signal = service.listNotifications.mock.calls[0][2].signal;
  unmount();
  expect(disconnect).toHaveBeenCalledTimes(1);
  expect(signal.aborted).toBe(true);
  realtime.onEvent({ ...item, id: 9 });
  expect(service.listNotifications).toHaveBeenCalledTimes(1);
});

test.each(['ausente', 'expirada'])('sessão %s não chama API nem realtime', async (mode) => {
  sessionService.clearLocalSession();
  if (mode === 'expirada') sessionService.saveSession({ token: `h.${btoa(JSON.stringify({ exp: 1 }))}.s` });
  render(<NotificationPanel />);
  expect(await screen.findByRole('link', { name: 'Entrar novamente' })).toHaveAttribute('href', '/login');
  expect(service.listNotifications).not.toHaveBeenCalled();
  expect(connectNotificationRealtime).not.toHaveBeenCalled();
});

test('401 limpa sessão e encerra realtime sem criar refresh', async () => {
  service.listNotifications.mockRejectedValue({ status: 401 });
  render(<NotificationPanel />);
  expect(await screen.findByRole('link', { name: 'Entrar novamente' })).toBeInTheDocument();
  expect(sessionService.getSession()).toBeNull();
  expect(disconnect).toHaveBeenCalled();
  expect(screen.queryByRole('listitem')).toBeNull();
});

test('403 é erro de permissão e não invalida sessão autenticada', async () => {
  service.listNotifications.mockRejectedValue({ status: 403 });
  render(<NotificationPanel />);
  expect(await screen.findByRole('alert')).toHaveTextContent('Você não tem permissão');
  expect(sessionService.getAccessToken()).not.toBeNull();
});
