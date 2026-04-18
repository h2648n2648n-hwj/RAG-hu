<script setup lang="ts">
import ChatList from './modules/chat-list.vue';
import InputBox from './modules/input-box.vue';

const chatStore = useChatStore();
const route = useRoute();

function handleBeforeUnload() {
  void chatStore.endCurrentSession();
}

onMounted(() => {
  chatStore.initChatPage().then(async () => {
    const sid = route.query.sessionId;
    if (typeof sid === 'string' && sid) {
      await chatStore.loadSessionDetail(sid);
    }
  });
  window.addEventListener('beforeunload', handleBeforeUnload);
});

onBeforeUnmount(() => {
  window.removeEventListener('beforeunload', handleBeforeUnload);
});

onBeforeRouteLeave(async () => {
  await chatStore.endCurrentSession();
});

watch(
  () => route.query.sessionId,
  async sid => {
    if (typeof sid === 'string' && sid) {
      await chatStore.loadSessionDetail(sid);
    }
  }
);
</script>

<template>
  <div class="h-full flex flex-col">
    <div class="h-full min-w-0 flex-1 overflow-hidden">
      <NCard class="h-full" :bordered="false" content-class="h-full">
        <ChatList />
      </NCard>
    </div>
    <InputBox />
  </div>
</template>

<style scoped></style>
