import { useEffect, useRef, useState } from 'react';
import sessionService from '../../auth/sessionService';
import {
  countUnreadNotifications, listNotifications, markAllNotificationsRead, markNotificationRead,
} from '../../services/notifications/notificationService';
import { connectNotificationRealtime, tokenExpiresAt } from '../../services/notifications/notificationRealtime';
import { notificationContext } from '../../services/notifications/notificationLinks';

const TYPES = { CANDIDATURA: 'Candidatura', MENSAGEM: 'Mensagem', SISTEMA: 'Sistema' };
const TRANSPORT = {
  connecting: 'Conectando alertas…', 'connecting-sse': 'Conectando alertas alternativos…',
  stomp: 'Alertas em tempo real', sse: 'Alertas em tempo real (SSE)',
  offline: 'Alertas desconectados. Tentando reconectar; a lista permanece salva.',
};

function uniqueNotifications(items) {
  return [...new Map((items || []).map((item) => [String(item.id), item])).values()];
}

function displayDate(value) {
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? '' : date.toLocaleString('pt-BR');
}

export default function NotificationPanel() {
  const token = sessionService.getAccessToken();
  const [state, setState] = useState({ loading: true, items: [], count: null, page: 0, hasNext: false });
  const [error, setError] = useState('');
  const [expired, setExpired] = useState(false);
  const [busy, setBusy] = useState(false);
  const [transport, setTransport] = useState('connecting');
  const [popup, setPopup] = useState(null);
  const operations = useRef({ refresh: () => {}, mark: () => {} });

  useEffect(() => {
    let active = true;
    let expiredSession = false;
    let loading = false;
    let queued = false;
    let page = 0;
    let mutating = false;
    let realtime;
    let popupTimer;
    const controller = new AbortController();
    const options = { token, signal: controller.signal };
    const seenEvents = new Set();
    setExpired(false);
    setBusy(false);
    setError('');
    setPopup(null);
    setState({ loading: true, items: [], count: null, page: 0, hasNext: false });

    function expire() {
      if (!active || expiredSession) return;
      expiredSession = true;
      realtime?.disconnect();
      controller.abort();
      clearTimeout(popupTimer);
      if (sessionService.getAccessToken() === token) sessionService.clearLocalSession();
      setExpired(true);
      setPopup(null);
      setState({ loading: false, items: [], count: null, page: 0, hasNext: false });
    }

    function available() {
      if (!active || expiredSession) return false;
      if (tokenExpiresAt(token) <= Date.now()) { expire(); return false; }
      return true;
    }

    function handleError(failure, fallback) {
      if (!available() || failure.name === 'AbortError') return;
      if (failure.status === 401) expire();
      else setError(failure.status === 403 ? 'Você não tem permissão para esta ação.'
        : failure.status === 404 ? 'Notificação não encontrada ou indisponível para sua conta.' : fallback);
    }

    async function refresh(nextPage = page) {
      if (!available()) return;
      page = nextPage;
      if (loading) { queued = true; return; }
      loading = true;
      setState((current) => ({ ...current, loading: true }));
      do {
        queued = false;
        const requestedPage = page;
        try {
          const [result, unread] = await Promise.all([
            listNotifications(requestedPage, 20, options), countUnreadNotifications(options),
          ]);
          if (available() && requestedPage === page) {
            setState({ loading: false, items: uniqueNotifications(result.content), count: unread.count,
              page: result.page, hasNext: Boolean(result.hasNext) });
            setError('');
          }
        } catch (failure) {
          handleError(failure, 'Não foi possível atualizar as notificações. Tente novamente.');
          if (available()) setState((current) => ({ ...current, loading: false }));
        }
      } while (queued && available());
      loading = false;
    }

    async function mark(id) {
      if (!available() || mutating) return;
      mutating = true;
      setBusy(true);
      setError('');
      try {
        if (id === undefined) await markAllNotificationsRead(options);
        else await markNotificationRead(id, options);
        if (available()) await refresh();
      } catch (failure) {
        handleError(failure, 'Não foi possível marcar a leitura. Tente novamente.');
      } finally {
        mutating = false;
        if (active) setBusy(false);
      }
    }

    operations.current = { refresh, mark };
    if (available()) {
      refresh();
      realtime = connectNotificationRealtime({
        token, onAuthError: expire,
        onState(status) {
          if (!available()) return;
          setTransport(status);
          if (status === 'stomp' || status === 'sse') refresh();
        },
        onEvent(item) {
          if (!available() || seenEvents.has(String(item.id))) return;
          seenEvents.add(String(item.id));
          if (seenEvents.size > 500) seenEvents.delete(seenEvents.values().next().value);
          setPopup(item);
          clearTimeout(popupTimer);
          popupTimer = setTimeout(() => { if (active) setPopup(null); }, 5000);
          refresh(0);
        },
      });
    }
    return () => {
      active = false;
      realtime?.disconnect();
      controller.abort();
      clearTimeout(popupTimer);
    };
  }, [token]);

  return (
    <section className="account-module notifications-panel" aria-labelledby="notifications-title">
      <div className="account-module__top">
        <h2 id="notifications-title">Notificações</h2>
        {state.count !== null ? <span className="account-count" aria-label={`${state.count} notificações não lidas`}>{state.count}</span> : null}
      </div>
      {expired ? <p role="alert">Sua sessão expirou ou está ausente. <a href="/login">Entrar novamente</a>.</p> : <>
        <p className="notifications-transport" role="status">{TRANSPORT[transport]}</p>
        <div className="notifications-actions">
          <button type="button" disabled={busy || state.loading || !state.count} onClick={() => operations.current.mark()}>{busy ? 'Salvando leitura…' : 'Marcar todas como lidas'}</button>
          <button type="button" disabled={state.loading || busy} onClick={() => operations.current.refresh()}>Atualizar notificações</button>
        </div>
        {error ? <p role="alert">{error}</p> : null}
        {state.loading ? <p role="status">Carregando notificações…</p> : null}
        {!state.loading && !error && !state.items.length ? <p>Nenhuma notificação por enquanto.</p> : null}
        <ol className="notifications-list" aria-label="Lista de notificações" aria-busy={state.loading}>
          {state.items.map((item) => {
            const href = notificationContext(item.link);
            const type = TYPES[item.tipo] ? item.tipo.toLowerCase() : 'sistema';
            return <li key={item.id} className={`notifications-item${item.lida ? '' : ' notifications-item--unread'}`}>
              <div className="notifications-meta"><span className={`notifications-type notifications-type--${type}`}>{TYPES[item.tipo] || 'Sistema'}</span><span>{item.lida ? 'Lida' : 'Não lida'}</span></div>
              {href ? <a className="notifications-context" href={href}>{item.mensagem}</a> : <p className="notifications-context">{item.mensagem}</p>}
              <div className="notifications-item-footer"><time dateTime={item.data}>{displayDate(item.data)}</time>{!item.lida ? <button type="button" disabled={busy || state.loading} onClick={() => operations.current.mark(item.id)}>Marcar como lida</button> : null}</div>
            </li>;
          })}
        </ol>
        {state.page > 0 || state.hasNext ? <nav className="notifications-pagination" aria-label="Páginas de notificações">
          <button type="button" disabled={!state.page || busy || state.loading} onClick={() => operations.current.refresh(state.page - 1)}>Anteriores</button>
          <span>Página {state.page + 1}</span>
          <button type="button" disabled={!state.hasNext || busy || state.loading} onClick={() => operations.current.refresh(state.page + 1)}>Próximas</button>
        </nav> : null}
      </>}
      {popup ? <aside className="notifications-popup" role="status" aria-live="polite"><strong>Nova notificação</strong><p>{popup.mensagem}</p><button type="button" aria-label="Fechar aviso de notificação" onClick={() => setPopup(null)}>Fechar</button></aside> : null}
    </section>
  );
}
