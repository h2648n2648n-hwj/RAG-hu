<script setup lang="ts">
import type { NScrollbar } from 'naive-ui';
import { VueMarkdownItProvider } from 'vue-markdown-shiki';
import ChatMessage from '../chat/modules/chat-message.vue';
import { fetchSessionList, fetchSessionDetail, fetchDeleteSession } from '@/service/api';
import { formatDate } from '@/utils/common';

defineOptions({
  name: 'ChatHistory'
});

const router = useRouter();

// ---- 会话列表 ----
const sessions = ref<Api.Session.Item[]>([]);
const sessionsLoading = ref(false);

// ---- 当前选中的会话 ----
const selectedSession = ref<Api.Session.Item | null>(null);
const messages = ref<Api.Chat.Message[]>([]);
const messagesLoading = ref(false);

const scrollbarRef = ref<InstanceType<typeof NScrollbar>>();

function scrollToBottom() {
  setTimeout(() => {
    scrollbarRef.value?.scrollBy({
      top: 999999999999999,
      behavior: 'auto'
    });
  }, 100);
}

function normalizeSessions(data: Api.Session.ListResponse | Api.Session.Item[] | null | undefined): Api.Session.Item[] {
  if (!data) return [];
  let items: any[] = [];
  if (Array.isArray(data)) items = data as any[];
  else if (Array.isArray((data as Api.Session.ListResponse).list)) items = (data as Api.Session.ListResponse).list as any[];
  else if (Array.isArray((data as Api.Session.ListResponse).content)) items = (data as Api.Session.ListResponse).content as any[];
  else if (Array.isArray((data as Api.Session.ListResponse).data)) items = (data as Api.Session.ListResponse).data as any[];

  return items
    .map(raw => {
      const id = typeof raw?.id === 'number' ? String(raw.id) : String(raw?.id || '');
      const sessionId = typeof raw?.sessionId === 'string' ? raw.sessionId : undefined;
      const createdAt = raw?.createdAt || raw?.createTime;
      const updatedAt = raw?.updatedAt || raw?.updateTime;
      const archived = typeof raw?.archived === 'boolean' ? raw.archived : String(raw?.status || '').toUpperCase() === 'ARCHIVED';
      const title = raw?.title || '未命名会话';

      return { ...raw, id, sessionId, createdAt, updatedAt, archived, title } as Api.Session.Item;
    })
    .filter(item => item.id);
}

function normalizeMessages(data: Api.Session.DetailResponse | Api.Chat.Message[] | null | undefined): Api.Chat.Message[] {
  if (!data) return [];
  if (Array.isArray(data)) return data;
  if (Array.isArray((data as Api.Session.DetailResponse).messages)) return (data as Api.Session.DetailResponse).messages!;
  if (Array.isArray((data as Api.Session.DetailResponse).records)) return (data as Api.Session.DetailResponse).records!;
  if (Array.isArray((data as Api.Session.DetailResponse).list)) return (data as Api.Session.DetailResponse).list!;
  return [];
}

async function loadSessions() {
  sessionsLoading.value = true;
  const { error, data } = await fetchSessionList({ page: 0, size: 100, status: 'ALL' });
  if (!error) {
    sessions.value = normalizeSessions(data);
  }
  sessionsLoading.value = false;
}

async function selectSession(session: Api.Session.Item) {
  selectedSession.value = session;
  messages.value = [];
  messagesLoading.value = true;

  const { error, data } = await fetchSessionDetail(session.id);
  if (!error) {
    messages.value = normalizeMessages(data);
    scrollToBottom();
  }
  messagesLoading.value = false;
}

async function removeSession(session: Api.Session.Item) {
  const dialog = window.$dialog;

  async function doRemove() {
    const { error } = await fetchDeleteSession(session.id);
    if (error) return;

    sessions.value = sessions.value.filter(item => item.id !== session.id);
    if (selectedSession.value?.id === session.id) {
      selectedSession.value = null;
      messages.value = [];
    }
  }

  if (!dialog) {
    await doRemove();
    return;
  }

  dialog.warning({
    title: '删除会话',
    content: '删除后不可恢复，是否继续？',
    positiveText: '删除',
    negativeText: '取消',
    onPositiveClick: doRemove
  });
}

function backToList() {
  selectedSession.value = null;
  messages.value = [];
}

function continueInChat() {
  if (!selectedSession.value) return;
  router.push({ path: '/chat', query: { sessionId: selectedSession.value.id } });
}

onMounted(() => {
  loadSessions();
});
</script>

<template>
  <div class="h-full flex gap-3 overflow-hidden">
    <!-- 左侧：会话列表 -->
    <div class="w-280px flex-shrink-0">
      <NCard class="h-full" :bordered="false" content-class="h-full flex flex-col p-3! overflow-hidden">
        <div class="mb-3 flex items-center justify-between">
          <NText class="text-16px font-bold">历史会话</NText>
          <NButton size="small" quaternary circle @click="loadSessions">
            <template #icon>
              <icon-lucide:refresh-cw />
            </template>
          </NButton>
        </div>

        <NSpin :show="sessionsLoading" class="min-h-0 flex-1">
          <NScrollbar class="h-full">
            <div class="flex flex-col gap-2 pr-1">
              <div
                v-for="item in sessions"
                :key="item.id"
                class="session-item"
                :class="{ active: selectedSession?.id === item.id }"
                @click="selectSession(item)"
              >
                <div class="min-w-0 flex-1">
                  <NText
                    :depth="selectedSession?.id === item.id ? 1 : 2"
                    class="line-clamp-2 block text-13px font-medium leading-snug"
                  >
                    {{ item.title || '未命名会话' }}
                  </NText>
                  <NText depth="3" class="mt-1 block text-11px">
                    {{ formatDate(item.updatedAt || item.updateTime || item.createdAt || item.createTime, 'YYYY-MM-DD HH:mm') }}
                  </NText>
                </div>
                <NButton
                  quaternary
                  circle
                  size="tiny"
                  type="error"
                  @click.stop="removeSession(item)"
                >
                  <template #icon>
                    <icon-lucide:trash-2 />
                  </template>
                </NButton>
                <icon-lucide:chevron-right
                  class="flex-shrink-0 text-14px color-gray-400"
                  :class="{ 'color-primary': selectedSession?.id === item.id }"
                />
              </div>
            </div>
            <NEmpty
              v-if="!sessions.length && !sessionsLoading"
              description="暂无历史会话"
              class="mt-20"
            />
          </NScrollbar>
        </NSpin>
      </NCard>
    </div>

    <!-- 右侧：消息详情 -->
    <div class="min-w-0 flex-1 overflow-hidden">
      <NCard class="h-full" :bordered="false" content-class="h-full flex flex-col p-0! overflow-hidden">
        <!-- 未选中会话时的空状态 -->
        <div v-if="!selectedSession" class="flex h-full flex-col items-center justify-center gap-4">
          <icon-solar:chat-round-call-line-duotone class="text-64px color-gray-300" />
          <NText depth="3" class="text-16px">请从左侧选择一个历史会话查看详情</NText>
        </div>

        <!-- 已选中会话时显示消息 -->
        <template v-else>
          <!-- 会话标题栏 -->
          <div class="flex items-center gap-3 border-b border-gray-200 px-4 py-3 dark:border-gray-700">
            <NButton size="small" quaternary circle @click="backToList">
              <template #icon>
                <icon-lucide:arrow-left />
              </template>
            </NButton>
            <div class="min-w-0 flex-1">
              <NText class="line-clamp-1 text-15px font-semibold">
                {{ selectedSession.title || '未命名会话' }}
              </NText>
              <NText depth="3" class="text-12px">
                创建于 {{ formatDate(selectedSession.createdAt || selectedSession.createTime, 'YYYY-MM-DD HH:mm') }}
              </NText>
            </div>
            <NButton size="small" type="primary" @click="continueInChat">继续对话</NButton>
          </div>

          <!-- 消息列表 -->
          <div class="min-h-0 flex-1 overflow-hidden p-4">
            <NScrollbar ref="scrollbarRef" class="h-full">
              <NSpin :show="messagesLoading" class="h-full">
                <VueMarkdownItProvider>
                  <ChatMessage
                    v-for="(msg, index) in messages"
                    :key="index"
                    :msg="msg"
                    :session-id="selectedSession.id"
                  />
                </VueMarkdownItProvider>
                <NEmpty
                  v-if="!messages.length && !messagesLoading"
                  description="该会话暂无消息记录"
                  class="mt-20"
                />
              </NSpin>
            </NScrollbar>
          </div>
        </template>
      </NCard>
    </div>
  </div>
</template>

<style scoped lang="scss">
.session-item {
  display: flex;
  align-items: center;
  gap: 8px;
  border: 1px solid var(--n-border-color);
  border-radius: 10px;
  padding: 10px 10px 10px 12px;
  cursor: pointer;
  transition: all 0.2s ease;

  &:hover {
    border-color: rgb(var(--primary-color));
    background: rgb(var(--primary-color) / 0.06);
  }

  &.active {
    border-color: rgb(var(--primary-color));
    background: rgb(var(--primary-color) / 0.12);
  }
}

.color-primary {
  color: rgb(var(--primary-color));
}
</style>
