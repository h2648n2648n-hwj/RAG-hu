<script setup lang="ts">
import { computed } from 'vue';
import type { Component } from 'vue';
import { loginModuleRecord } from '@/constants/app';
import { useAppStore } from '@/store/modules/app';
import { useThemeStore } from '@/store/modules/theme';
import { $t } from '@/locales';
import PwdLogin from './modules/pwd-login.vue';
import CodeLogin from './modules/code-login.vue';
import Register from './modules/register.vue';
import ResetPwd from './modules/reset-pwd.vue';
import BindWechat from './modules/bind-wechat.vue';

interface Props {
  /** The login module */
  module?: UnionKey.LoginModule;
}

const props = defineProps<Props>();

const appStore = useAppStore();
const themeStore = useThemeStore();

interface LoginModule {
  label: string;
  component: Component;
}

const moduleMap: Record<UnionKey.LoginModule, LoginModule> = {
  'pwd-login': { label: loginModuleRecord['pwd-login'], component: PwdLogin },
  'code-login': { label: loginModuleRecord['code-login'], component: CodeLogin },
  register: { label: loginModuleRecord.register, component: Register },
  'reset-pwd': { label: loginModuleRecord['reset-pwd'], component: ResetPwd },
  'bind-wechat': { label: loginModuleRecord['bind-wechat'], component: BindWechat }
};

const activeModule = computed(() => moduleMap[props.module || 'pwd-login']);
</script>

<template>
  <div class="login-shell relative size-full flex-center">
    <div class="orb orb-one"></div>
    <div class="orb orb-two"></div>
    <div class="grid-mask"></div>

    <NCard :bordered="false" class="relative z-4 card-wrapper">
      <div class="login-panel">
        <header class="header-row">
          <div class="brand-block">
            <div class="logo-wrap">
              <SystemLogo class="brand-logo" />
            </div>
            <div class="brand-text-wrap">
              <h1 class="brand-title">{{ $t('system.title') }}</h1>
              <p class="brand-subtitle">Knowledge Graph + RAG + Multi-Modal Intelligence</p>
            </div>
          </div>
          <div class="switch-block">
            <ThemeSchemaSwitch
              :theme-schema="themeStore.themeScheme"
              :show-tooltip="false"
              class="text-20px lt-sm:text-18px"
              @switch="themeStore.toggleThemeScheme"
            />
            <LangSwitch
              v-if="themeStore.header.multilingual.visible"
              :lang="appStore.locale"
              :lang-options="appStore.localeOptions"
              :show-tooltip="false"
              @change-lang="appStore.changeLocale"
            />
          </div>
        </header>
        <main class="module-wrap">
          <h3 class="module-title">{{ $t(activeModule.label) }}</h3>
          <div class="module-form">
            <Transition :name="themeStore.page.animateMode" mode="out-in" appear>
              <component :is="activeModule.component" />
            </Transition>
          </div>
        </main>
      </div>
    </NCard>
  </div>
</template>

<style scoped>
.login-shell {
  overflow: hidden;
  background:
    radial-gradient(circle at 22% 18%, rgb(63 126 255 / 20%), transparent 35%),
    radial-gradient(circle at 80% 10%, rgb(0 214 255 / 18%), transparent 30%),
    linear-gradient(145deg, #071225 0%, #0b1a35 42%, #08152d 100%);
}

.grid-mask {
  position: absolute;
  inset: 0;
  background-image:
    linear-gradient(rgb(255 255 255 / 4%) 1px, transparent 1px),
    linear-gradient(90deg, rgb(255 255 255 / 4%) 1px, transparent 1px);
  background-size: 34px 34px;
  mask-image: radial-gradient(circle at center, rgb(0 0 0 / 95%) 35%, transparent 100%);
}

.orb {
  position: absolute;
  border-radius: 999px;
  filter: blur(2px);
}

.orb-one {
  top: 16%;
  left: 18%;
  width: 220px;
  height: 220px;
  background: radial-gradient(circle, rgb(75 145 255 / 36%) 0%, rgb(75 145 255 / 0%) 72%);
}

.orb-two {
  right: 12%;
  bottom: 12%;
  width: 280px;
  height: 280px;
  background: radial-gradient(circle, rgb(0 223 255 / 28%) 0%, rgb(0 223 255 / 0%) 74%);
}

.card-wrapper {
  width: min(560px, calc(100vw - 40px));
  border: 1px solid rgb(120 182 255 / 34%);
  border-radius: 24px;
  background: linear-gradient(160deg, rgb(9 25 51 / 84%), rgb(8 17 35 / 88%));
  box-shadow:
    0 24px 48px rgb(3 9 22 / 50%),
    0 0 0 1px rgb(152 202 255 / 10%) inset,
    0 0 42px rgb(66 143 255 / 18%);
  backdrop-filter: blur(12px);
}

.login-panel {
  padding: 8px;
}

.header-row {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.brand-block {
  display: flex;
  align-items: center;
  gap: 14px;
}

.logo-wrap {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 62px;
  height: 62px;
  border-radius: 18px;
  background: linear-gradient(145deg, rgb(79 151 255 / 24%), rgb(0 213 255 / 15%));
  border: 1px solid rgb(133 199 255 / 32%);
  box-shadow: 0 0 24px rgb(66 143 255 / 24%);
}

.brand-logo {
  font-size: 40px;
  color: #7fc9ff;
}

.brand-text-wrap {
  min-width: 0;
}

.brand-title {
  margin: 0;
  font-size: 28px;
  line-height: 1.15;
  font-weight: 600;
  color: #dceeff;
}

.brand-subtitle {
  margin: 8px 0 0;
  font-size: 12px;
  letter-spacing: 0.09em;
  color: rgb(168 213 255 / 72%);
  text-transform: uppercase;
}

.switch-block {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
  color: #d4ebff;
}

.module-wrap {
  padding-top: 26px;
}

.module-title {
  margin: 0;
  font-size: 20px;
  font-weight: 500;
  color: #b9dcff;
}

.module-form {
  padding-top: 22px;
}

:deep(.n-form-item) {
  margin-bottom: 18px;
}

:deep(.n-input) {
  --n-border: 1px solid rgb(131 196 255 / 22%);
  --n-border-hover: 1px solid rgb(131 196 255 / 45%);
  --n-border-focus: 1px solid #65b8ff;
  --n-box-shadow-focus: 0 0 0 2px rgb(101 184 255 / 25%);
  --n-color: rgb(9 23 47 / 66%);
  --n-text-color: #edf7ff;
  --n-placeholder-color: rgb(185 218 248 / 56%);
}

:deep(.n-button--primary-type) {
  --n-color: #2b7cff;
  --n-color-hover: #4a93ff;
  --n-color-pressed: #1f63da;
  --n-text-color: #f5fbff;
}

:deep(.n-divider .n-divider__title) {
  color: rgb(177 212 244 / 70%);
}

:deep(.n-form-item .n-form-item-feedback-wrapper) {
  min-height: 18px;
}

@media (max-width: 640px) {
  .card-wrapper {
    width: calc(100vw - 24px);
    border-radius: 18px;
  }

  .brand-title {
    font-size: 20px;
  }

  .brand-subtitle {
    font-size: 10px;
  }

  .logo-wrap {
    width: 52px;
    height: 52px;
    border-radius: 14px;
  }

  .brand-logo {
    font-size: 33px;
  }
}
</style>
