import { TextDecoder, TextEncoder } from 'util';
import apiClient from '../api/apiClient';
import { connectNotificationRealtime } from './notificationRealtime';

jest.mock('../api/apiClient', () => ({ getStream: jest.fn() }));
const token = (seconds = 60) => `header.${btoa(JSON.stringify({ exp: Math.floor(Date.now() / 1000) + seconds }))}.sig`;
const tick = async () => { for (let i = 0; i < 30; i++) await Promise.resolve(); };
class Socket {
  static instances = [];
  constructor(url) { this.url = url; this.readyState = 0; this.send = jest.fn(); Socket.instances.push(this); }
  close() { this.readyState = 3; this.onclose?.(); }
  open() { this.readyState = 1; this.onopen?.(); }
  receive(data) { this.onmessage?.({ data }); }
}
let clients;
function connect(options = {}) {
  const client = connectNotificationRealtime({ token: token(), WebSocketImpl: Socket, ...options });
  clients.push(client);
  return client;
}
function stream(chunks = []) {
  const read = jest.fn();
  chunks.forEach((value) => read.mockResolvedValueOnce({ value: new TextEncoder().encode(value), done: false }));
  read.mockReturnValue(new Promise(() => {}));
  const reader = { read, releaseLock: jest.fn() };
  return { headers: new Headers({ 'content-type': 'text/event-stream' }), body: { getReader: () => reader }, reader };
}
beforeEach(() => {
  jest.useFakeTimers();
  jest.clearAllMocks();
  Socket.instances = [];
  clients = [];
  global.TextDecoder = TextDecoder;
  apiClient.getStream.mockReturnValue(new Promise(() => {}));
});
afterEach(() => { clients.forEach((client) => client.disconnect()); jest.useRealTimers(); });

test('STOMP envia JWT só no CONNECT e assina uma vez a fila privada', () => {
  const jwt = token();
  const onEvent = jest.fn();
  const onState = jest.fn();
  connect({ token: jwt, onEvent, onState });
  const socket = Socket.instances[0];
  expect(new URL(socket.url).pathname).toBe('/ws');
  expect(new URL(socket.url).search).toBe('');
  socket.open();
  expect(socket.send.mock.calls[0][0]).toContain(`Authorization:Bearer ${jwt}`);
  socket.receive('\nCONNECTED\r\nversion:1.2\r\n\r\n\0CONNECTED\n\n\0');
  expect(socket.send.mock.calls.filter(([frame]) => frame.startsWith('SUBSCRIBE'))).toHaveLength(1);
  expect(socket.send).toHaveBeenLastCalledWith(expect.stringContaining('destination:/user/queue/notificacoes'));
  socket.receive('MESSAGE\n\n{"id":1,');
  socket.receive('"mensagem":"Olá"}\0');
  expect(onEvent).toHaveBeenCalledWith({ id: 1, mensagem: 'Olá' });
  expect(onState).toHaveBeenCalledWith('stomp');
});

test('sem WebSocket usa SSE autenticado e reconstrói eventos fragmentados', async () => {
  const onEvent = jest.fn();
  const onState = jest.fn();
  const reply = stream(['event:conectado\ndata:ok\n\n', 'event:notificacao\ndata:{"id":3,', '"mensagem":"SSE"}\n\n']);
  apiClient.getStream.mockResolvedValue(reply);
  connect({ WebSocketImpl: null, onEvent, onState });
  await tick();
  expect(apiClient.getStream).toHaveBeenCalledWith('/notificacoes/stream', { token: expect.any(String), signal: expect.any(AbortSignal) });
  expect(onState).toHaveBeenCalledWith('sse');
  expect(reply.reader.read).toHaveBeenCalledTimes(4);
  expect(onState).not.toHaveBeenCalledWith('offline');
  expect(onEvent).toHaveBeenCalledWith({ id: 3, mensagem: 'SSE' });
});

test('falha WebSocket ativa somente um SSE e cleanup aborta conexão', async () => {
  const client = connect();
  const socket = Socket.instances[0];
  const close = socket.onclose;
  socket.onerror();
  close();
  await tick();
  expect(apiClient.getStream).toHaveBeenCalledTimes(1);
  const signal = apiClient.getStream.mock.calls[0][1].signal;
  client.disconnect();
  expect(signal.aborted).toBe(true);
  expect(socket.onmessage).toBeNull();
  jest.advanceTimersByTime(30000);
  expect(apiClient.getStream).toHaveBeenCalledTimes(1);
});

test('timeout de CONNECT também aciona fallback', () => {
  connect();
  jest.advanceTimersByTime(5000);
  expect(apiClient.getStream).toHaveBeenCalledTimes(1);
});

test('SSE encerrado reconecta e cleanup remove nova tentativa', async () => {
  const reply = stream();
  reply.reader.read.mockResolvedValue({ done: true });
  apiClient.getStream.mockResolvedValueOnce(reply);
  const client = connect({ WebSocketImpl: null });
  await tick();
  expect(reply.reader.releaseLock).toHaveBeenCalled();
  jest.advanceTimersByTime(3000);
  expect(apiClient.getStream).toHaveBeenCalledTimes(2);
  client.disconnect();
  jest.advanceTimersByTime(60000);
  expect(apiClient.getStream).toHaveBeenCalledTimes(2);
});

test.each([null, 'invalido', token(-5)])('sessão ausente/inválida/expirada não abre transportes: %s', (jwt) => {
  const onAuthError = jest.fn();
  connect({ token: jwt, onAuthError });
  expect(onAuthError).toHaveBeenCalledTimes(1);
  expect(Socket.instances).toHaveLength(0);
  expect(apiClient.getStream).not.toHaveBeenCalled();
});

test('expiração encerra conexão ativa sem refresh nem reconexão', () => {
  const onAuthError = jest.fn();
  connect({ token: token(2), onAuthError });
  const socket = Socket.instances[0];
  socket.open();
  socket.receive('CONNECTED\n\n\0');
  jest.advanceTimersByTime(2100);
  expect(socket.readyState).toBe(3);
  expect(onAuthError).toHaveBeenCalledTimes(1);
  expect(apiClient.getStream).not.toHaveBeenCalled();
});

test('rejeição STOMP encerra sem fallback; 401 SSE também encerra', async () => {
  const onAuthError = jest.fn();
  connect({ onAuthError });
  Socket.instances[0].receive('ERROR\n\nJWT inválido\0');
  expect(onAuthError).toHaveBeenCalledTimes(1);
  apiClient.getStream.mockRejectedValue({ status: 401 });
  connect({ WebSocketImpl: null, onAuthError });
  await tick();
  expect(onAuthError).toHaveBeenCalledTimes(2);
  jest.advanceTimersByTime(10000);
  expect(apiClient.getStream).toHaveBeenCalledTimes(1);
});
