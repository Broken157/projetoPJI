import { API_BASE_URL } from '../../config/apiConfig';

const CHAT_QUEUE = '/user/queue/chat';

function websocketUrl() {
  const apiUrl = new URL(API_BASE_URL, window.location.origin);
  apiUrl.protocol = apiUrl.protocol === 'https:' ? 'wss:' : 'ws:';
  apiUrl.pathname = `${apiUrl.pathname.replace(/\/?api\/?$/, '')}/ws`.replace(/\/+/g, '/');
  apiUrl.search = '';
  apiUrl.hash = '';
  return apiUrl.href;
}

function stompFrame(command, headers = {}, body = '') {
  const lines = [command, ...Object.entries(headers).map(([key, value]) => `${key}:${value}`)];
  return `${lines.join('\n')}\n\n${body}\0`;
}

function readFrames(raw) {
  return String(raw)
    .split('\0')
    .filter(Boolean)
    .map((frame) => {
      const separator = frame.indexOf('\n\n');
      const header = separator >= 0 ? frame.slice(0, separator) : frame;
      return {
        command: header.split('\n')[0],
        body: separator >= 0 ? frame.slice(separator + 2) : '',
      };
    });
}

export function connectChatRealtime({
  token,
  onEvent,
  onState,
  WebSocketImpl = window.WebSocket,
  schedule = window.setTimeout.bind(window),
  cancelSchedule = window.clearTimeout.bind(window),
}) {
  if (!WebSocketImpl || !token) {
    onState?.('offline');
    return { send: () => false, disconnect: () => {}, isConnected: () => false };
  }

  let socket;
  let connected = false;
  let stopped = false;
  let reconnectTimer;
  let reconnectAttempt = 0;
  let connectionSequence = 0;

  function open() {
    if (stopped) return;
    const sequence = ++connectionSequence;
    let closeHandled = false;
    onState?.('connecting');
    socket = new WebSocketImpl(websocketUrl());

    socket.onopen = () => {
      if (stopped || sequence !== connectionSequence) return;
      socket.send(stompFrame('CONNECT', {
        'accept-version': '1.2',
        host: window.location.host,
        Authorization: `Bearer ${token}`,
        'heart-beat': '10000,10000',
      }));
    };

    socket.onmessage = ({ data }) => {
      if (stopped || sequence !== connectionSequence) return;
      readFrames(data).forEach(({ command, body }) => {
        if (command === 'CONNECTED' && !connected) {
          connected = true;
          reconnectAttempt = 0;
          socket.send(stompFrame('SUBSCRIBE', {
            id: `chat-${sequence}`,
            destination: CHAT_QUEUE,
            ack: 'auto',
          }));
          onState?.('connected');
        } else if (command === 'MESSAGE') {
          try {
            onEvent?.(JSON.parse(body));
          } catch {
            onState?.('error');
          }
        } else if (command === 'ERROR') {
          onState?.('error');
        }
      });
    };

    socket.onerror = () => socket.close();
    socket.onclose = () => {
      if (sequence !== connectionSequence || closeHandled) return;
      closeHandled = true;
      connected = false;
      onState?.('offline');
      if (!stopped) {
        const delay = Math.min(1000 * (2 ** reconnectAttempt), 10000);
        reconnectAttempt += 1;
        reconnectTimer = schedule(open, delay);
      }
    };
  }

  open();

  return {
    send(roomId, texto) {
      if (!connected || !socket || socket.readyState !== WebSocketImpl.OPEN) return false;
      const body = JSON.stringify({ texto });
      socket.send(stompFrame('SEND', {
        destination: `/app/chat/salas/${roomId}/mensagens`,
        'content-type': 'application/json',
      }, body));
      return true;
    },
    disconnect() {
      stopped = true;
      connected = false;
      connectionSequence += 1;
      if (reconnectTimer) cancelSchedule(reconnectTimer);
      if (socket && socket.readyState < WebSocketImpl.CLOSING) socket.close();
    },
    isConnected() {
      return connected;
    },
  };
}

export { readFrames, stompFrame, websocketUrl };
