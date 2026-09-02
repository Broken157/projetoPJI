import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import sessionService from '../../auth/sessionService';
import AccountLayout from '../../components/account/AccountLayout';
import {
  CHAT_PAGE_SIZE,
  deleteMessage,
  editMessage,
  listMessages,
  listRooms,
  markRoomRead,
  sendMessage,
} from '../../services/chat/chatService';
import { connectChatRealtime } from '../../services/chat/chatRealtime';

const DELETED_MESSAGE = 'Mensagem excluída pelo autor';
const VALID_ID = /^[1-9]\d*$/;

function safeAvatar(value) {
  if (!value || typeof value !== 'string') return '/assets/avatar-perfil.png';
  try {
    const url = new URL(value, window.location.origin);
    return ['http:', 'https:'].includes(url.protocol) ? url.href : '/assets/avatar-perfil.png';
  } catch {
    return '/assets/avatar-perfil.png';
  }
}

function dateTime(value) {
  if (!value) return '';
  const date = new Date(value);
  return Number.isNaN(date.getTime())
    ? ''
    : date.toLocaleString('pt-BR', { dateStyle: 'short', timeStyle: 'short' });
}

function chronological(content = []) {
  return [...content].reverse();
}

function mergeMessages(current, incoming, prepend = false) {
  const byId = new Map();
  const source = prepend ? [...incoming, ...current] : [...current, ...incoming];
  source.forEach((message) => {
    if (message?.id != null) byId.set(Number(message.id), message);
  });
  return [...byId.values()].sort((a, b) => {
    const time = new Date(a.dataEnvio).getTime() - new Date(b.dataEnvio).getTime();
    return Number.isFinite(time) && time !== 0 ? time : Number(a.id) - Number(b.id);
  });
}

function canEdit(message, sessionId) {
  if (!message || message.excluida || Number(message.remetenteId) !== Number(sessionId)) return false;
  const sentAt = new Date(message.dataEnvio).getTime();
  return Number.isFinite(sentAt) && Date.now() - sentAt <= 15 * 60 * 1000;
}

function errorText(error, fallback) {
  return error?.message || fallback;
}

export default function MessagesPage() {
  const session = useMemo(() => sessionService.getSession(), []);
  const [searchParams, setSearchParams] = useSearchParams();
  const requestedRoom = searchParams.get('sala');
  const [roomsState, setRoomsState] = useState({ loading: true, rooms: [], page: 0, hasMore: false });
  const [selectedRoomId, setSelectedRoomId] = useState(null);
  const [messagesState, setMessagesState] = useState({ loading: false, messages: [], page: 0, hasMore: false });
  const [connection, setConnection] = useState('connecting');
  const [error, setError] = useState('');
  const [draft, setDraft] = useState('');
  const [sending, setSending] = useState(false);
  const [editing, setEditing] = useState(null);
  const realtimeRef = useRef(null);
  const selectedRoomRef = useRef(null);
  const eventHandlerRef = useRef(() => {});
  const seenEventsRef = useRef(new Set());

  const selectedRoom = roomsState.rooms.find(
    (room) => Number(room.salaId) === Number(selectedRoomId)
  );

  const refreshRooms = useCallback(async () => {
    const page = await listRooms(0, CHAT_PAGE_SIZE);
    setRoomsState({
      loading: false,
      rooms: page.content || [],
      page: page.page || 0,
      hasMore: Boolean(page.hasMore),
    });
    return page.content || [];
  }, []);

  const loadRoom = useCallback(async (roomId) => {
    const normalizedId = Number(roomId);
    setMessagesState({ loading: true, messages: [], page: 0, hasMore: false });
    setError('');
    try {
      const page = await listMessages(normalizedId, 0, CHAT_PAGE_SIZE);
      setMessagesState({
        loading: false,
        messages: chronological(page.content),
        page: page.page || 0,
        hasMore: Boolean(page.hasMore),
      });
      await markRoomRead(normalizedId);
      setRoomsState((current) => ({
        ...current,
        rooms: current.rooms.map((room) =>
          Number(room.salaId) === normalizedId ? { ...room, naoLidas: 0 } : room
        ),
      }));
    } catch (loadError) {
      setMessagesState({ loading: false, messages: [], page: 0, hasMore: false });
      setError(errorText(loadError, 'Não foi possível carregar esta conversa.'));
    }
  }, []);

  useEffect(() => {
    document.title = 'Mensagens — Palco';
    let active = true;
    refreshRooms()
      .then((rooms) => {
        if (!active) return;
        const requested = requestedRoom && VALID_ID.test(requestedRoom) ? Number(requestedRoom) : null;
        const roomId = requested || rooms[0]?.salaId || null;
        if (roomId) setSelectedRoomId(Number(roomId));
      })
      .catch((loadError) => {
        if (!active) return;
        setRoomsState({ loading: false, rooms: [], page: 0, hasMore: false });
        setError(errorText(loadError, 'Não foi possível carregar suas conversas.'));
      });
    return () => { active = false; };
  }, [refreshRooms, requestedRoom]);

  useEffect(() => {
    selectedRoomRef.current = selectedRoomId;
    if (selectedRoomId) loadRoom(selectedRoomId);
  }, [loadRoom, selectedRoomId]);

  const handleRealtimeEvent = useCallback((event) => {
    if (!event?.salaId) return;

    const eventKey = event.mensagem
      ? `${event.tipo}:${event.salaId}:${event.mensagem.id}:${event.mensagem.texto}:${event.mensagem.lida}:${event.mensagem.excluida}`
      : `${event.tipo}:${event.salaId}:${(event.mensagemIds || []).join(',')}`;
    if (seenEventsRef.current.has(eventKey)) return;
    seenEventsRef.current.add(eventKey);
    if (seenEventsRef.current.size > 500) {
      seenEventsRef.current.delete(seenEventsRef.current.values().next().value);
    }

    const activeRoom = Number(event.salaId) === Number(selectedRoomRef.current);

    if (event.mensagem) {
      setRoomsState((current) => ({
        ...current,
        rooms: current.rooms.map((room) => {
          if (Number(room.salaId) !== Number(event.salaId)) return room;
          const receivedFromOther = event.tipo === 'NOVA_MENSAGEM'
            && Number(event.mensagem.remetenteId) !== Number(session?.id);
          return {
            ...room,
            ultimaMensagem: event.mensagem.texto,
            ultimaMensagemData: event.mensagem.dataEnvio,
            naoLidas: activeRoom ? 0 : Number(room.naoLidas || 0) + (receivedFromOther ? 1 : 0),
          };
        }),
      }));
    }

    if (!activeRoom) return;

    if (event.tipo === 'LEITURA') {
      const ids = new Set((event.mensagemIds || []).map(Number));
      setMessagesState((current) => ({
        ...current,
        messages: current.messages.map((message) =>
          ids.has(Number(message.id)) ? { ...message, lida: true } : message
        ),
      }));
      return;
    }

    if (event.mensagem) {
      setMessagesState((current) => ({
        ...current,
        messages: mergeMessages(current.messages, [event.mensagem]),
      }));
      if (event.tipo === 'NOVA_MENSAGEM'
          && Number(event.mensagem.remetenteId) !== Number(session?.id)) {
        markRoomRead(event.salaId).catch(() => {});
      }
    }
  }, [session?.id]);

  eventHandlerRef.current = handleRealtimeEvent;

  useEffect(() => {
    realtimeRef.current = connectChatRealtime({
      token: session?.token,
      onEvent: (event) => eventHandlerRef.current(event),
      onState: setConnection,
    });
    return () => realtimeRef.current?.disconnect();
  }, [session?.token]);

  function selectRoom(roomId) {
    const id = Number(roomId);
    if (id === selectedRoomId) return;
    setSelectedRoomId(id);
    setSearchParams({ sala: String(id) });
    setEditing(null);
  }

  async function loadMoreRooms() {
    const nextPage = roomsState.page + 1;
    try {
      const page = await listRooms(nextPage, CHAT_PAGE_SIZE);
      setRoomsState((current) => ({
        loading: false,
        rooms: [...current.rooms, ...(page.content || [])]
          .filter((room, index, all) => all.findIndex((item) => item.salaId === room.salaId) === index),
        page: page.page,
        hasMore: Boolean(page.hasMore),
      }));
    } catch (loadError) {
      setError(errorText(loadError, 'Não foi possível carregar mais conversas.'));
    }
  }

  async function loadOlderMessages() {
    const nextPage = messagesState.page + 1;
    try {
      const page = await listMessages(selectedRoomId, nextPage, CHAT_PAGE_SIZE);
      const older = chronological(page.content);
      setMessagesState((current) => ({
        loading: false,
        messages: mergeMessages(current.messages, older, true),
        page: page.page,
        hasMore: Boolean(page.hasMore),
      }));
    } catch (loadError) {
      setError(errorText(loadError, 'Não foi possível carregar mensagens anteriores.'));
    }
  }

  async function submitMessage(event) {
    event.preventDefault();
    const text = draft.trim();
    if (!text || !selectedRoomId || sending) return;
    setSending(true);
    setError('');
    try {
      const sentRealtime = realtimeRef.current?.send(selectedRoomId, text);
      if (!sentRealtime) {
        const message = await sendMessage(selectedRoomId, text);
        setMessagesState((current) => ({
          ...current,
          messages: mergeMessages(current.messages, [message]),
        }));
        await refreshRooms();
      }
      setDraft('');
    } catch (sendError) {
      setError(errorText(sendError, 'Não foi possível enviar a mensagem.'));
    } finally {
      setSending(false);
    }
  }

  async function submitEdit(event) {
    event.preventDefault();
    const text = editing?.text.trim();
    if (!text) return;
    try {
      const updated = await editMessage(editing.id, text);
      setMessagesState((current) => ({
        ...current,
        messages: mergeMessages(current.messages, [updated]),
      }));
      setEditing(null);
    } catch (editError) {
      setError(errorText(editError, 'Não foi possível editar a mensagem.'));
    }
  }

  async function removeMessage(messageId) {
    if (!window.confirm('Excluir esta mensagem? O histórico mostrará o aviso de exclusão.')) return;
    try {
      await deleteMessage(messageId);
      setMessagesState((current) => ({
        ...current,
        messages: current.messages.map((message) =>
          Number(message.id) === Number(messageId)
            ? { ...message, texto: DELETED_MESSAGE, excluida: true }
            : message
        ),
      }));
    } catch (deleteError) {
      setError(errorText(deleteError, 'Não foi possível excluir a mensagem.'));
    }
  }

  return (
    <AccountLayout>
      <main className="chat-page" aria-busy={roomsState.loading || messagesState.loading}>
        <header className="chat-heading">
          <div>
            <p className="account-eyebrow">Conversas profissionais</p>
            <h1>Mensagens</h1>
            <p>Fale diretamente com artistas e contratantes com quem você se conecta.</p>
          </div>
          <span className={`chat-connection chat-connection--${connection}`} role="status">
            {connection === 'connected' ? 'Tempo real ativo' : connection === 'connecting' ? 'Conectando…' : 'Atualização por recarga'}
          </span>
        </header>

        {error ? <section className="chat-error" role="alert">{error}</section> : null}

        <div className="chat-layout">
          <aside className="chat-rooms" aria-label="Conversas">
            <div className="chat-rooms__heading">
              <h2>Conversas</h2>
              <span className="chat-badge" aria-label={`${roomsState.rooms.reduce((sum, room) => sum + Number(room.naoLidas || 0), 0)} não lidas`}>
                {roomsState.rooms.reduce((sum, room) => sum + Number(room.naoLidas || 0), 0)}
              </span>
            </div>
            <div className="chat-rooms__list">
              {roomsState.loading ? <p className="chat-empty">Carregando conversas…</p> : null}
              {!roomsState.loading && roomsState.rooms.length === 0 ? <p className="chat-empty">Nenhuma conversa iniciada.</p> : null}
              {roomsState.rooms.map((room) => (
                <button
                  className={`chat-room${Number(room.salaId) === Number(selectedRoomId) ? ' chat-room--active' : ''}`}
                  type="button"
                  key={room.salaId}
                  onClick={() => selectRoom(room.salaId)}
                >
                  <img src={safeAvatar(room.participanteAvatar)} alt="" />
                  <span className="chat-room__copy">
                    <strong>{room.participanteNome}</strong>
                    <small>{room.ultimaMensagem || 'Conversa iniciada'}</small>
                  </span>
                  {room.naoLidas ? <span className="chat-badge">{room.naoLidas}</span> : null}
                </button>
              ))}
              {roomsState.hasMore ? <button className="chat-more" type="button" onClick={loadMoreRooms}>Carregar mais conversas</button> : null}
            </div>
          </aside>

          <section className="chat-conversation" aria-label="Conversa selecionada">
            <header className="chat-conversation__heading">
              <p>Conversando com</p>
              <h2>{selectedRoom?.participanteNome || (selectedRoomId ? 'Conversa' : 'Selecione uma conversa')}</h2>
            </header>

            <div className="chat-history" aria-live="polite">
              {messagesState.hasMore ? <button className="chat-more" type="button" onClick={loadOlderMessages}>Carregar mensagens anteriores</button> : null}
              {messagesState.loading ? <p className="chat-empty">Carregando histórico…</p> : null}
              {!messagesState.loading && selectedRoomId && messagesState.messages.length === 0 && !error
                ? <p className="chat-empty">Ainda não há mensagens nesta conversa.</p>
                : null}
              {!selectedRoomId ? <p className="chat-empty">Selecione uma conversa para consultar o histórico.</p> : null}
              {messagesState.messages.map((message) => {
                const own = Number(message.remetenteId) === Number(session?.id);
                return (
                  <article
                    className={`chat-message${own ? ' chat-message--own' : ''}${message.excluida ? ' chat-message--deleted' : ''}`}
                    key={message.id}
                    data-message-id={message.id}
                  >
                    {editing?.id === message.id ? (
                      <form className="chat-edit" onSubmit={submitEdit}>
                        <label htmlFor={`edit-${message.id}`}>Editar mensagem</label>
                        <textarea
                          id={`edit-${message.id}`}
                          maxLength="4000"
                          required
                          value={editing.text}
                          onChange={(event) => setEditing({ ...editing, text: event.target.value })}
                        />
                        <div><button type="submit">Salvar</button><button type="button" onClick={() => setEditing(null)}>Cancelar</button></div>
                      </form>
                    ) : <p className="chat-message__text">{message.texto}</p>}
                    <footer className="chat-message__meta">
                      <time>{dateTime(message.dataEnvio)}</time>
                      {own ? <span>{message.lida ? 'Lida' : 'Enviada'}</span> : null}
                      {own && !message.excluida ? (
                        <span className="chat-message__actions">
                          {canEdit(message, session?.id) ? <button type="button" onClick={() => setEditing({ id: message.id, text: message.texto })}>Editar</button> : null}
                          <button type="button" onClick={() => removeMessage(message.id)}>Excluir</button>
                        </span>
                      ) : null}
                    </footer>
                  </article>
                );
              })}
            </div>

            {selectedRoomId && !error ? (
              <form className="chat-composer" onSubmit={submitMessage}>
                <label className="sr-only" htmlFor="chat-message">Mensagem</label>
                <textarea
                  id="chat-message"
                  maxLength="4000"
                  rows="2"
                  required
                  placeholder="Escreva uma mensagem…"
                  value={draft}
                  onChange={(event) => setDraft(event.target.value)}
                />
                <span className="chat-composer__count">{draft.length}/4000</span>
                <button type="submit" disabled={sending || !draft.trim()}>{sending ? 'Enviando…' : 'Enviar'}</button>
              </form>
            ) : null}
          </section>
        </div>

        <aside className="chat-disclaimer" role="note">
          A plataforma apenas fornece o meio de comunicação e não se responsabiliza por acordos realizados entre os usuários.
        </aside>
      </main>
    </AccountLayout>
  );
}

export { canEdit, chronological, mergeMessages, safeAvatar };
