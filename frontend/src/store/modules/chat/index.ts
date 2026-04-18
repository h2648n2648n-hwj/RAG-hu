import { useWebSocket } from '@vueuse/core';
import { fetchCreateSession, fetchDeleteSession, fetchSessionDetail, fetchSessionList, fetchAutoUpdateSessionTitle, fetchArchiveSession } from '@/service/api';

export const useChatStore = defineStore(SetupStoreId.Chat, () => {
  const conversationId = ref<string>('');
  const input = ref<Api.Chat.Input>({ message: '' });
  const list = ref<Api.Chat.Message[]>([]);
  const sessions = ref<Api.Session.Item[]>([]);
  const activeSessionId = ref<string>('');
  const sessionsLoading = ref(false);
  const messageLoading = ref(false);

  const store = useAuthStore();

  const sessionId = ref<string>('');

  const wsUrl = computed(() => {
    const sid = sessionId.value ? `?sessionId=${encodeURIComponent(sessionId.value)}` : '';
    return `/proxy-ws/chat/${store.token}${sid}`;
  });

  const {
    status: wsStatus,
    data: wsData,
    send: wsSend,
    open: wsOpen,
    close: wsClose
  } = useWebSocket(wsUrl, {
    autoReconnect: true,
    immediate: false
  });

  watch(wsData, (val) => {
    if (!val) return;

    const assistant = list.value[list.value.length - 1];

    try {
      const data = JSON.parse(val);

      if (!assistant || assistant.role !== 'assistant') return;

      if (data.type === 'completion' && data.status === 'finished' && assistant.status !== 'error') {
        assistant.status = 'finished';
        const userCount = list.value.filter(item => item.role === 'user').length;
        if (activeSessionId.value && userCount === 1) {
          fetchAutoUpdateSessionTitle(activeSessionId.value).then(() => loadSessions());
        }
      }
      if (data.error) {
        assistant.status = 'error';
      } else if (data.chunk) {
        assistant.status = 'loading';
        assistant.content += data.chunk;
      }
    } catch (_error) {
      if (!assistant || assistant.role !== 'assistant') return;

      assistant.status = 'loading';
      assistant.content += val;
    }
  });

  const scrollToBottom = ref<null | (() => void)>(null);

  function toStr(val: unknown): string {
    if (typeof val === 'number') return String(val);
    if (typeof val === 'string') return val;
    return '';
  }

  function normalizeSessionItem(raw: any): Api.Session.Item {
    const id = toStr(raw?.id);
    const sid = toStr(raw?.sessionId);
    const createdAt = toStr(raw?.createdAt || raw?.createTime);
    const updatedAt = toStr(raw?.updatedAt || raw?.updateTime);
    const archived = typeof raw?.archived === 'boolean' ? raw.archived : String(raw?.status || '').toUpperCase() === 'ARCHIVED';
    const title = toStr(raw?.title) || '未命名会话';

    return {
      ...raw,
      id,
      sessionId: sid || undefined,
      title,
      archived,
      createdAt: createdAt || undefined,
      updatedAt: updatedAt || undefined
    };
  }

  function normalizeSessions(data: Api.Session.ListResponse | Api.Session.Item[] | null | undefined) {
    if (!data) return [];

    let items: any[] = [];

    if (Array.isArray(data)) items = data as any[];
    else if (Array.isArray(data.list)) items = data.list as any[];
    else if (Array.isArray(data.content)) items = data.content as any[];
    else if (Array.isArray(data.data)) items = data.data as any[];

    return items.map(normalizeSessionItem).filter(item => item.id);
  }

  function normalizeMessages(data: Api.Session.DetailResponse | Api.Chat.Message[] | null | undefined) {
    if (!data) return [];

    if (Array.isArray(data)) return data;

    if (Array.isArray(data.messages)) return data.messages;
    if (Array.isArray(data.records)) return data.records;
    if (Array.isArray(data.list)) return data.list;

    return [];
  }

  async function loadSessions() {
    sessionsLoading.value = true;
    const { error, data } = await fetchSessionList({ page: 0, size: 50, status: 'ALL' });

    if (!error) {
      sessions.value = normalizeSessions(data);
    }

    sessionsLoading.value = false;
  }

  async function loadSessionDetail(id: string) {
    if (!id) return;

    messageLoading.value = true;
    const { error, data } = await fetchSessionDetail(id);

    if (!error) {
      list.value = normalizeMessages(data);
      activeSessionId.value = id;
      const current = sessions.value.find(item => item.id === id);
      const sid = current?.sessionId || '';
      conversationId.value = sid;
      sessionId.value = sid;
      wsClose();
      if (sessionId.value) wsOpen();
      scrollToBottom.value?.();
    }

    messageLoading.value = false;
  }

  async function createSession() {
    const { error, data } = await fetchCreateSession();

    if (error) {
      return null;
    }

    const session = normalizeSessionItem(data as any);
    if (!session.id || !session.sessionId) {
      return null;
    }

    const exists = sessions.value.some(item => item.id === session.id);
    if (!exists) sessions.value.unshift(session);

    activeSessionId.value = session.id;
    conversationId.value = session.sessionId;
    sessionId.value = session.sessionId;
    list.value = [];
    messageLoading.value = false;
    wsClose();
    wsOpen();

    return session;
  }

  async function ensureActiveSession() {
    if (activeSessionId.value && sessionId.value) {
      if (wsStatus.value !== 'OPEN') wsOpen();
      return true;
    }

    const created = await createSession();
    return Boolean(created);
  }

  async function selectSession(id: string) {
    if (!id || id === activeSessionId.value) return;

    await loadSessionDetail(id);
  }

  async function removeSession(id: string) {
    if (!id) return;

    const { error } = await fetchDeleteSession(id);
    if (error) return;

    sessions.value = sessions.value.filter(item => item.id !== id);

    if (activeSessionId.value !== id) return;

    const next = sessions.value[0]?.id;
    if (next) {
      await loadSessionDetail(next);
      return;
    }

    activeSessionId.value = '';
    conversationId.value = '';
    sessionId.value = '';
    list.value = [];
  }

  async function initChatPage() {
    await loadSessions();
    
    activeSessionId.value = '';
    conversationId.value = '';
    sessionId.value = '';
    list.value = [];
    wsClose();
  }

  async function endCurrentSession() {
    const id = activeSessionId.value;
    if (!id) return;

    const hasUserMessage = list.value.some(item => item.role === 'user' && Boolean(item.content?.trim()));
    if (!hasUserMessage) {
      try {
        await fetchDeleteSession(id);
      } catch (_e) {}

      sessions.value = sessions.value.filter(item => item.id !== id);
      activeSessionId.value = '';
      conversationId.value = '';
      sessionId.value = '';
      list.value = [];
      wsClose();
      return;
    }

    try {
      await fetchAutoUpdateSessionTitle(id);
    } catch (_e) {}

    try {
      await fetchArchiveSession(id, true);
    } catch (_e) {}

    await loadSessions();
  }

  async function createNewChat() {
    await endCurrentSession();
    activeSessionId.value = '';
    conversationId.value = '';
    sessionId.value = '';
    list.value = [];
    wsClose();
  }

  return {
    input,
    conversationId,
    list,
    sessions,
    activeSessionId,
    sessionsLoading,
    messageLoading,
    wsStatus,
    wsData,
    wsSend,
    wsOpen,
    wsClose,
    sessionId,
    scrollToBottom,
    loadSessions,
    loadSessionDetail,
    createSession,
    ensureActiveSession,
    selectSession,
    removeSession,
    initChatPage,
    endCurrentSession,
    createNewChat
  };
});
