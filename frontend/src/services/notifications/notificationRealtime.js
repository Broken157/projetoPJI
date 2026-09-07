import apiClient from '../api/apiClient';
import { stompFrame, websocketUrl } from '../chat/chatRealtime';

export function tokenExpiresAt(token) {
  try {
    const payload = JSON.parse(atob(token.split('.')[1].replace(/-/g, '+').replace(/_/g, '/')));
    return Number.isFinite(payload.exp) ? payload.exp * 1000 : 0;
  } catch {
    return 0;
  }
}

export function connectNotificationRealtime({
  token, onEvent, onState, onAuthError,
  WebSocketImpl = window.WebSocket,
}) {
  let stopped = false;
  let socket;
  let streamController;
  let retryTimer;
  let connectTimer;
  let expiryTimer;
  let attempts = 0;

  function closeSocket() {
    clearTimeout(connectTimer);
    if (!socket) return;
    socket.onopen = null;
    socket.onmessage = null;
    socket.onerror = null;
    socket.onclose = null;
    if (socket.readyState < 2) socket.close();
    socket = null;
  }

  function disconnect() {
    stopped = true;
    clearTimeout(retryTimer);
    clearTimeout(expiryTimer);
    closeSocket();
    streamController?.abort();
  }

  function expire() {
    if (stopped) return;
    disconnect();
    onAuthError?.();
  }

  function sessionValid() {
    if (stopped) return false;
    if (tokenExpiresAt(token) <= Date.now()) {
      expire();
      return false;
    }
    return true;
  }

  function emit(body) {
    if (!sessionValid()) return;
    try {
      const item = JSON.parse(body);
      if (item && /^[1-9][0-9]*$/.test(String(item.id)) && typeof item.mensagem === 'string') {
        onEvent?.(item);
      }
    } catch {
      // A malformed event never replaces the authoritative persisted REST list.
    }
  }

  async function openSse() {
    if (!sessionValid() || streamController) return;
    closeSocket();
    onState?.('connecting-sse');
    const controller = new AbortController();
    streamController = controller;
    let reader;
    try {
      const response = await apiClient.getStream('/notificacoes/stream', {
        token, signal: controller.signal,
      });
      if (stopped) return;
      if (!response.body || !response.headers.get('content-type')?.includes('text/event-stream')) {
        throw new Error('Streaming indisponível.');
      }
      reader = response.body.getReader();
      attempts = 0;
      onState?.('sse');
      const decoder = new TextDecoder();
      let buffer = '';
      while (sessionValid()) {
        const chunk = await reader.read();
        if (chunk.done || stopped) break;
        buffer += decoder.decode(chunk.value, { stream: true }).replace(/\r/g, '');
        if (buffer.length > 1024 * 1024) throw new Error('Evento inválido.');
        let boundary;
        while ((boundary = buffer.indexOf('\n\n')) >= 0) {
          const lines = buffer.slice(0, boundary).split('\n');
          buffer = buffer.slice(boundary + 2);
          if (lines.some((line) => /^event:\s*notificacao$/.test(line))) {
            emit(lines.filter((line) => line.startsWith('data:')).map((line) => line.slice(5).trimStart()).join('\n'));
          }
        }
      }
    } catch (error) {
      if (error.status === 401 || error.status === 403) expire();
    } finally {
      reader?.releaseLock();
      controller.abort();
      streamController = null;
      if (sessionValid()) {
        onState?.('offline');
        retryTimer = setTimeout(openSse, Math.min(3000 * (2 ** attempts++), 30000));
      }
    }
  }

  function openSocket() {
    if (!sessionValid()) return;
    onState?.('connecting');
    let buffer = '';
    let subscribed = false;
    try {
      socket = new WebSocketImpl(websocketUrl());
    } catch {
      openSse();
      return;
    }
    connectTimer = setTimeout(openSse, 5000);
    socket.onopen = () => {
      if (!sessionValid()) return;
      socket.send(stompFrame('CONNECT', {
        'accept-version': '1.2', host: window.location.host,
        Authorization: `Bearer ${token}`, 'heart-beat': '0,0',
      }));
    };
    socket.onmessage = ({ data }) => {
      if (!sessionValid()) return;
      buffer += String(data).replace(/\r/g, '');
      if (buffer.length > 1024 * 1024) { openSse(); return; }
      let boundary;
      while ((boundary = buffer.indexOf('\0')) >= 0 && !stopped) {
        const frame = buffer.slice(0, boundary).replace(/^\n+/, '');
        buffer = buffer.slice(boundary + 1);
        const separator = frame.indexOf('\n\n');
        const command = frame.split('\n')[0];
        if (command === 'CONNECTED' && !subscribed) {
          subscribed = true;
          clearTimeout(connectTimer);
          socket.send(stompFrame('SUBSCRIBE', {
            id: 'rf36-notificacoes', destination: '/user/queue/notificacoes', ack: 'auto',
          }));
          onState?.('stomp');
        } else if (command === 'MESSAGE' && subscribed && separator >= 0) {
          emit(frame.slice(separator + 2));
        } else if (command === 'ERROR') {
          // Never retry a rejected authenticated connection or introduce RF25 refresh.
          expire();
        }
      }
    };
    socket.onerror = () => { openSse(); };
    socket.onclose = () => { openSse(); };
  }

  if (sessionValid()) {
    expiryTimer = setTimeout(expire, Math.min(tokenExpiresAt(token) - Date.now(), 2147483647));
    if (WebSocketImpl) openSocket();
    else openSse();
  }
  return { disconnect };
}
