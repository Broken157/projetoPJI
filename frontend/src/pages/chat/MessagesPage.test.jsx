import { act, fireEvent, render, screen, waitFor, within } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import AuthenticatedOnly from '../../components/account/AuthenticatedOnly';
import ApiError from '../../services/api/ApiError';
import * as chatService from '../../services/chat/chatService';
import { connectChatRealtime } from '../../services/chat/chatRealtime';
import MessagesPage from './MessagesPage';

jest.mock('../../components/account/AccountLayout', () => function Layout({ children }) { return children; });
jest.mock('../../services/chat/chatService');
jest.mock('../../services/chat/chatRealtime', () => ({ connectChatRealtime: jest.fn() }));

const now = new Date().toISOString();
const room = { salaId: 7, participanteId: 20, participanteNome: 'Pessoa B', participanteAvatar: null, ultimaMensagem: 'Oi', naoLidas: 1 };
const own = { id: 2, salaId: 7, remetenteId: 10, texto: 'Minha mensagem', lida: false, excluida: false, dataEnvio: now };
const other = { id: 1, salaId: 7, remetenteId: 20, texto: '<img src=x onerror=alert(1)>', lida: true, excluida: false, dataEnvio: new Date(Date.now() - 1000).toISOString() };
let realtime;
let consoleError;

function roomPage(overrides = {}) {
  return { content: [room], page: 0, size: 20, totalElements: 1, totalPages: 1, hasMore: false, ...overrides };
}

function messagePage(overrides = {}) {
  return { content: [own, other], page: 0, size: 20, totalElements: 2, totalPages: 1, hasMore: false, ...overrides };
}

function renderPage(path = '/mensagens?sala=7') {
  return render(
    <MemoryRouter initialEntries={[path]} future={{ v7_relativeSplatPath: true, v7_startTransition: true }}>
      <Routes><Route path="/mensagens" element={<MessagesPage />} /></Routes>
    </MemoryRouter>
  );
}

beforeEach(() => {
  jest.clearAllMocks();
  window.sessionStorage.clear();
  window.localStorage.clear();
  window.sessionStorage.setItem('palco.sessao', JSON.stringify({ token: 'jwt', id: 10, tipoUsuario: 'ARTISTA' }));
  chatService.listRooms.mockResolvedValue(roomPage());
  chatService.listMessages.mockResolvedValue(messagePage());
  chatService.markRoomRead.mockResolvedValue(null);
  chatService.sendMessage.mockResolvedValue({ ...own, id: 3, texto: 'Nova' });
  chatService.editMessage.mockImplementation((id, texto) => Promise.resolve({ ...own, id, texto }));
  chatService.deleteMessage.mockResolvedValue(null);
  realtime = { send: jest.fn(() => false), disconnect: jest.fn(), isConnected: jest.fn(() => false) };
  connectChatRealtime.mockImplementation(({ onEvent, onState }) => {
    realtime.emit = onEvent;
    onState('offline');
    return realtime;
  });
  consoleError = jest.spyOn(console, 'error').mockImplementation(() => {});
});

afterEach(() => {
  expect(consoleError).not.toHaveBeenCalled();
  consoleError.mockRestore();
});

test('guard redireciona visitante anônimo para login sem chamar o chat', () => {
  window.sessionStorage.clear();
  render(
    <MemoryRouter initialEntries={['/mensagens']} future={{ v7_relativeSplatPath: true, v7_startTransition: true }}>
      <Routes>
        <Route path="/mensagens" element={<AuthenticatedOnly><MessagesPage /></AuthenticatedOnly>} />
        <Route path="/login" element={<h1>Login</h1>} />
      </Routes>
    </MemoryRouter>
  );
  expect(screen.getByRole('heading', { name: 'Login' })).toBeInTheDocument();
  expect(chatService.listRooms).not.toHaveBeenCalled();
});

test('lista salas, abre histórico, marca recebidas e mantém texto não confiável literal', async () => {
  renderPage();
  expect(screen.getByText('Carregando conversas…')).toBeInTheDocument();
  expect(await screen.findByRole('button', { name: /Pessoa B/ })).toBeInTheDocument();
  expect(await screen.findByText('<img src=x onerror=alert(1)>')).toBeInTheDocument();
  expect(document.querySelector('img[src="x"]')).toBeNull();
  expect(chatService.listMessages).toHaveBeenCalledWith(7, 0, 20);
  expect(chatService.markRoomRead).toHaveBeenCalledWith(7);
  expect(screen.getByLabelText('0 não lidas')).toBeInTheDocument();
});

test('exibe estado vazio de salas sem inventar conversa', async () => {
  chatService.listRooms.mockResolvedValue(roomPage({ content: [], totalElements: 0 }));
  renderPage('/mensagens');
  expect(await screen.findByText('Nenhuma conversa iniciada.')).toBeInTheDocument();
  expect(screen.getByText('Selecione uma conversa para consultar o histórico.')).toBeInTheDocument();
  expect(chatService.listMessages).not.toHaveBeenCalled();
});

test('pagina o histórico e acrescenta mensagens anteriores sem duplicar', async () => {
  chatService.listMessages
    .mockResolvedValueOnce(messagePage({ hasMore: true, totalPages: 2 }))
    .mockResolvedValueOnce(messagePage({ content: [{ ...other, id: 0, texto: 'Mais antiga' }], page: 1 }));
  renderPage();
  fireEvent.click(await screen.findByRole('button', { name: 'Carregar mensagens anteriores' }));
  expect(await screen.findByText('Mais antiga')).toBeInTheDocument();
  expect(chatService.listMessages).toHaveBeenLastCalledWith(7, 1, 20);
  expect(screen.getAllByText('Minha mensagem')).toHaveLength(1);
});

test('envia por REST quando o WebSocket está offline e bloqueia reenvio durante loading', async () => {
  let resolveSend;
  chatService.listRooms
    .mockResolvedValueOnce(roomPage())
    .mockResolvedValueOnce(roomPage({ content: [{ ...room, ultimaMensagem: 'Preview atualizado' }] }));
  chatService.sendMessage.mockReturnValue(new Promise((resolve) => { resolveSend = resolve; }));
  renderPage();
  const input = await screen.findByPlaceholderText('Escreva uma mensagem…');
  fireEvent.change(input, { target: { value: '  Nova  ' } });
  const button = screen.getByRole('button', { name: 'Enviar' });
  fireEvent.click(button);
  fireEvent.click(button);
  expect(chatService.sendMessage).toHaveBeenCalledTimes(1);
  expect(chatService.sendMessage).toHaveBeenCalledWith(7, 'Nova');
  expect(screen.getByRole('button', { name: 'Enviando…' })).toBeDisabled();
  await act(async () => resolveSend({ ...own, id: 3, texto: 'Nova' }));
  expect(await screen.findByText('Nova')).toBeInTheDocument();
  expect(await screen.findByText('Preview atualizado')).toBeInTheDocument();
  expect(screen.getByPlaceholderText('Escreva uma mensagem…')).toHaveValue('');
  expect(screen.getByRole('button', { name: 'Enviar' })).toBeDisabled();
});

test('envia pelo destino realtime quando conectado sem duplicar POST REST', async () => {
  realtime.send.mockReturnValue(true);
  renderPage();
  fireEvent.change(await screen.findByPlaceholderText('Escreva uma mensagem…'), { target: { value: 'Via socket' } });
  fireEvent.click(screen.getByRole('button', { name: 'Enviar' }));
  await waitFor(() => expect(realtime.send).toHaveBeenCalledWith(7, 'Via socket'));
  expect(chatService.sendMessage).not.toHaveBeenCalled();
});

test('recebe evento realtime sem duplicação e aplica recibo de leitura', async () => {
  renderPage();
  await screen.findByText('Minha mensagem');
  const incoming = { ...other, id: 8, texto: 'Chegou em tempo real', lida: false };
  act(() => {
    realtime.emit({ tipo: 'NOVA_MENSAGEM', salaId: 7, mensagem: incoming });
    realtime.emit({ tipo: 'NOVA_MENSAGEM', salaId: 7, mensagem: incoming });
  });
  expect(screen.getAllByText('Chegou em tempo real', { selector: 'p' })).toHaveLength(1);
  expect(chatService.markRoomRead).toHaveBeenCalledWith(7);
  expect(chatService.markRoomRead).toHaveBeenCalledTimes(2);
  expect(screen.getByText('Chegou em tempo real', { selector: 'small' })).toBeInTheDocument();

  act(() => realtime.emit({ tipo: 'LEITURA', salaId: 7, mensagemIds: [2] }));
  expect(within(screen.getByText('Minha mensagem').closest('article')).getByText('Lida')).toBeInTheDocument();
});

test('atualiza uma conversa inativa em tempo real sem duplicar o contador', async () => {
  const inactiveRoom = {
    ...room,
    salaId: 8,
    participanteId: 30,
    participanteNome: 'Pessoa C',
    ultimaMensagem: 'Anterior',
    naoLidas: 0,
  };
  chatService.listRooms.mockResolvedValue(roomPage({ content: [room, inactiveRoom], totalElements: 2 }));
  renderPage();
  await screen.findByText('Minha mensagem');
  const incoming = { ...other, id: 9, salaId: 8, remetenteId: 30, texto: 'Nova na outra sala' };

  act(() => {
    realtime.emit({ tipo: 'NOVA_MENSAGEM', salaId: 8, mensagem: incoming });
    realtime.emit({ tipo: 'NOVA_MENSAGEM', salaId: 8, mensagem: incoming });
  });

  const inactiveButton = screen.getByRole('button', { name: /Pessoa C/ });
  expect(within(inactiveButton).getByText('Nova na outra sala')).toBeInTheDocument();
  expect(within(inactiveButton).getByText('1')).toBeInTheDocument();
  expect(screen.queryByText('Nova na outra sala', { selector: 'p' })).not.toBeInTheDocument();
  expect(chatService.markRoomRead).toHaveBeenCalledTimes(1);
});

test('permite editar apenas mensagem própria dentro da regra atual', async () => {
  renderPage();
  await screen.findByText('Minha mensagem');
  expect(screen.getAllByRole('button', { name: 'Editar' })).toHaveLength(1);
  fireEvent.click(screen.getByRole('button', { name: 'Editar' }));
  fireEvent.change(screen.getByLabelText('Editar mensagem'), { target: { value: 'Texto editado' } });
  fireEvent.click(screen.getByRole('button', { name: 'Salvar' }));
  expect(await screen.findByText('Texto editado')).toBeInTheDocument();
  expect(chatService.editMessage).toHaveBeenCalledWith(2, 'Texto editado');
});

test('permite excluir apenas mensagem própria e mantém placeholder', async () => {
  jest.spyOn(window, 'confirm').mockReturnValue(true);
  renderPage();
  await screen.findByText('Minha mensagem');
  expect(screen.getAllByRole('button', { name: 'Excluir' })).toHaveLength(1);
  fireEvent.click(screen.getByRole('button', { name: 'Excluir' }));
  expect(await screen.findByText('Mensagem excluída pelo autor')).toBeInTheDocument();
  expect(chatService.deleteMessage).toHaveBeenCalledWith(2);
  window.confirm.mockRestore();
});

test('sala adulterada depende do backend, mostra 404 e não libera composer', async () => {
  chatService.listMessages.mockRejectedValue(new ApiError({ status: 404, message: 'Sala não encontrada.' }));
  renderPage('/mensagens?sala=999');
  expect(await screen.findByRole('alert')).toHaveTextContent('Sala não encontrada.');
  expect(chatService.listMessages).toHaveBeenCalledWith(999, 0, 20);
  expect(screen.queryByPlaceholderText('Escreva uma mensagem…')).not.toBeInTheDocument();
});

test('falha ao listar salas encerra loading e mostra erro', async () => {
  chatService.listRooms.mockRejectedValue(new ApiError({ status: 500, message: 'Chat indisponível.' }));
  renderPage('/mensagens');
  expect(await screen.findByRole('alert')).toHaveTextContent('Chat indisponível.');
  expect(screen.queryByText('Carregando conversas…')).not.toBeInTheDocument();
});
