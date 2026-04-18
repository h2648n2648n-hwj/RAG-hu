<script setup lang="ts">
import { formatDate } from '@/utils/common';

defineOptions({
  name: 'SessionList'
});

const chatStore = useChatStore();
const { sessions, activeSessionId, sessionsLoading } = storeToRefs(chatStore);

async function handleCreateSession() {
  await chatStore.createNewChat();
}

async function handleSelectSession(id: string) {
  await chatStore.selectSession(id);
}

async function handleDeleteSession(id: string) {
  const dialog = window.$dialog;

  if (!dialog) {
    await chatStore.removeSession(id);
    return;
  }

  dialog.warning({
    title: '删除会话',
    content: '删除后不可恢复，是否继续？',
    positiveText: '删除',
    negativeText: '取消',
    onPositiveClick: async () => {
      await chatStore.removeSession(id);
    }
  });
}
</script>

<template>
  <div class="session-panel flex h-full flex-col gap-3">
    <NButton type="primary" block @click="handleCreateSession">
      <template #icon>
        <icon-lucide:plus />
      </template>
      新建聊天
    </NButton>

    <NSpin :show="sessionsLoading" class="h-0 flex-auto">
      <NScrollbar class="h-full">
        <div class="flex flex-col gap-2 pr-2">
          <div
            v-for="item in sessions"
            :key="item.id"
            class="session-item"
            :class="{ active: item.id === activeSessionId }"
            @click="handleSelectSession(item.id)"
          >
            <div class="session-content">
              <NText :depth="item.id === activeSessionId ? 1 : 2" class="line-clamp-1 text-14px font-medium">
                {{ item.title || '未命名会话' }}
              </NText>
              <NText depth="3" class="text-12px">{{ formatDate(item.updatedAt || item.updateTime || item.createdAt || item.createTime) }}</NText>
            </div>
            <NButton
              quaternary
              circle
              size="tiny"
              type="error"
              @click.stop="handleDeleteSession(item.id)"
            >
              <template #icon>
                <icon-lucide:trash-2 />
              </template>
            </NButton>
          </div>
        </div>

        <NEmpty v-if="!sessions.length && !sessionsLoading" description="暂无会话，点击上方新建聊天" class="mt-30" />
      </NScrollbar>
    </NSpin>
  </div>
</template>

<style scoped>
.session-panel {
  min-height: 0;
}

.session-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  border: 1px solid var(--n-border-color);
  border-radius: 12px;
  padding: 10px 8px 10px 12px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.session-item:hover {
  border-color: rgb(var(--primary-color));
  background: rgb(var(--primary-color) / 0.06);
}

.session-item.active {
  border-color: rgb(var(--primary-color));
  background: rgb(var(--primary-color) / 0.12);
}

.session-content {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 4px;
}
</style>
