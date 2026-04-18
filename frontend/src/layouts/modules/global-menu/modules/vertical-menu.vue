<script setup lang="ts">
import { computed, h, ref, watch } from 'vue';
import { useRoute } from 'vue-router';
import { SimpleScrollbar } from '@sa/materials';
import { GLOBAL_SIDER_MENU_ID } from '@/constants/app';
import { useAppStore } from '@/store/modules/app';
import { useThemeStore } from '@/store/modules/theme';
import { useRouteStore } from '@/store/modules/route';
import { useRouterPush } from '@/hooks/common/router';
import { useMenu } from '../../../context';

defineOptions({
  name: 'VerticalMenu'
});

const route = useRoute();
const appStore = useAppStore();
const themeStore = useThemeStore();
const routeStore = useRouteStore();
const { routerPushByKeyWithMetaQuery } = useRouterPush();
const { selectedKey } = useMenu();
const chatStore = useChatStore();

const inverted = computed(() => !themeStore.darkMode && themeStore.sider.inverted);

const expandedKeys = ref<string[]>([]);

// 自定义菜单选项：在"聊天助手"后添加"新建聊天"按钮
const menusWithNewChat = computed(() => {
  const menus = [...routeStore.menus];
  // 找到"聊天助手"（chat）的位置
  const chatIndex = menus.findIndex(menu => menu.key === 'chat');
  if (chatIndex !== -1) {
    // 在"聊天助手"后面插入"新建聊天"按钮
    menus.splice(chatIndex + 1, 0, {
      key: 'new-chat-btn',
      label: '新建聊天',
      icon: () => h('icon-lucide:plus'),
      routeKey: 'chat',
      routePath: '/chat'
    } as any);
  }
  return menus;
});

function updateExpandedKeys() {
  if (appStore.siderCollapse || !selectedKey.value) {
    expandedKeys.value = [];
    return;
  }
  expandedKeys.value = routeStore.getSelectedMenuKeyPath(selectedKey.value);
}

watch(
  () => route.name,
  () => {
    updateExpandedKeys();
  },
  { immediate: true }
);

async function handleMenuClick(key: string) {
  // 如果是"新建聊天"按钮，调用创建会话逻辑
  if (key === 'new-chat-btn') {
    await chatStore.createNewChat();
    routerPushByKeyWithMetaQuery('chat');
    return;
  }
  // 否则正常路由跳转
  routerPushByKeyWithMetaQuery(key);
}
</script>

<template>
  <Teleport :to="`#${GLOBAL_SIDER_MENU_ID}`">
    <SimpleScrollbar class="relative">
      <NMenu
        v-model:expanded-keys="expandedKeys"
        mode="vertical"
        :value="selectedKey"
        :collapsed="appStore.siderCollapse"
        :collapsed-width="themeStore.sider.collapsedWidth"
        :collapsed-icon-size="22"
        :options="menusWithNewChat"
        :inverted="inverted"
        :indent="18"
        @update:value="handleMenuClick"
      />
      <MenuToggler
        v-if="!appStore.isMobile"
        class="absolute bottom-0 w-full"
        :collapsed="appStore.siderCollapse"
        @click="appStore.toggleSiderCollapse"
      />
    </SimpleScrollbar>
  </Teleport>
</template>

<style scoped></style>
