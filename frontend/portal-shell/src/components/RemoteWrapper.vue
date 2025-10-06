<script setup lang="ts">
import { ref, onMounted, onUnmounted, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import type { Router } from "vue-router";
import type { MountOptions } from "blog_remote/bootstrap";

type RemoteApp = {
  router: Router;
  onParentNavigate: (path: string) => void;
  unmount: () => void;
};

const props = defineProps<{
  mountFn: (el: HTMLElement, options: MountOptions) => RemoteApp;
  basePath: string;
}>();

const container = ref<HTMLElement | null>(null);
const shellRoute = useRoute();
const shellRouter = useRouter();

let remoteApp: RemoteApp | null = null;

const onRemoteNavigate = (path: string) => {
  const newPath = `${props.basePath}${path === '/' ? '' : path}`;
  if (shellRoute.path !== newPath) {
    shellRouter.push(newPath);
  }
};

watch(() => shellRoute.path, (newPath) => {
  if (remoteApp) {
    const remotePath = newPath.substring(props.basePath.length) || '/';
    remoteApp.onParentNavigate(remotePath);
  }
}, { immediate: true });

onMounted(() => {
  if (container.value) {
    const initialPath = shellRoute.path.substring(props.basePath.length) || '/';
    remoteApp = props.mountFn(container.value, {
      initialPath,
      onNavigate: onRemoteNavigate,
    });
  }
});

onUnmounted(() => {
  remoteApp?.unmount();
  remoteApp = null;
});
</script>

<template>
  <div ref="container"></div>
</template>

<style scoped>
</style>