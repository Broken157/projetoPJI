import { connectChatRealtime, readFrames, websocketUrl } from './chatRealtime';

class FakeWebSocket {
  static OPEN = 1;
  static CLOSING = 2;
  static instances = [];

  constructor(url) {
    this.url = url;
    this.readyState = 0;
    this.sent = [];
    FakeWebSocket.instances.push(this);
  }

  send(frame) { this.sent.push(frame); }
  close() { this.readyState = 3; this.onclose?.(); }
  open() { this.readyState = FakeWebSocket.OPEN; this.onopen?.(); }
  receive(frame) { this.onmessage?.({ data: frame }); }
}

beforeEach(() => { FakeWebSocket.instances = []; });

test('conecta no /ws, assina uma vez e envia somente pelo destino STOMP existente', () => {
  const onEvent = jest.fn();
  const client = connectChatRealtime({
    token: 'jwt-chat', onEvent, WebSocketImpl: FakeWebSocket,
    schedule: jest.fn(), cancelSchedule: jest.fn(),
  });
  const socket = FakeWebSocket.instances[0];

  expect(socket.url).toBe(websocketUrl());
  socket.open();
  expect(socket.sent[0]).toContain('CONNECT\n');
  expect(socket.sent[0]).toContain('Authorization:Bearer jwt-chat');

  socket.receive('CONNECTED\nversion:1.2\n\n\0');
  socket.receive('CONNECTED\nversion:1.2\n\n\0');
  expect(socket.sent.filter((frame) => frame.startsWith('SUBSCRIBE'))).toHaveLength(1);
  expect(client.send(7, 'Olá')).toBe(true);
  expect(socket.sent.at(-1)).toContain('destination:/app/chat/salas/7/mensagens');

  socket.receive('MESSAGE\ndestination:/user/queue/chat\n\n{"tipo":"NOVA_MENSAGEM","salaId":7}\0');
  expect(onEvent).toHaveBeenCalledWith({ tipo: 'NOVA_MENSAGEM', salaId: 7 });
  client.disconnect();
});

test('reconecta sem reaproveitar assinatura e ignora frames da conexão anterior', () => {
  let reconnect;
  const onEvent = jest.fn();
  connectChatRealtime({
    token: 'jwt', onEvent, WebSocketImpl: FakeWebSocket,
    schedule: (callback) => { reconnect = callback; return 9; }, cancelSchedule: jest.fn(),
  });
  const first = FakeWebSocket.instances[0];
  first.open();
  first.receive('CONNECTED\n\n\0');
  first.close();
  reconnect();
  const second = FakeWebSocket.instances[1];
  second.open();
  second.receive('CONNECTED\n\n\0');

  first.receive('MESSAGE\n\n{"salaId":7}\0');
  second.receive('MESSAGE\n\n{"salaId":8}\0');
  expect(onEvent).toHaveBeenCalledTimes(1);
  expect(onEvent).toHaveBeenCalledWith({ salaId: 8 });
  expect(second.sent.filter((frame) => frame.startsWith('SUBSCRIBE'))).toHaveLength(1);
});

test('agenda somente uma reconexão quando erro e fechamento chegam juntos', () => {
  const schedule = jest.fn();
  connectChatRealtime({
    token: 'jwt', WebSocketImpl: FakeWebSocket, schedule, cancelSchedule: jest.fn(),
  });
  const socket = FakeWebSocket.instances[0];
  socket.open();
  socket.onerror();
  socket.onclose();
  expect(schedule).toHaveBeenCalledTimes(1);
});

test('parser aceita múltiplos frames e preserva corpos JSON', () => {
  expect(readFrames('CONNECTED\n\n\0MESSAGE\n\n{"salaId":4}\0')).toEqual([
    { command: 'CONNECTED', body: '' },
    { command: 'MESSAGE', body: '{"salaId":4}' },
  ]);
});
