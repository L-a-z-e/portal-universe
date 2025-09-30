<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount, watch } from "vue";
import { useRoute } from "vue-router";
import type { Router } from "vue-router";

const props = defineProps<{
  mountFn: (el: HTMLElement) => { router: Router, unmount: () => void };
  basePath: string;
}>();

const container = ref<HTMLElement | null>(null);
const shellRoute = useRoute();

let remoteApp: { router: Router, unmount: () => void } | null = null;

onMounted(() => {
  if (container.value) {
    remoteApp = props.mountFn(container.value);
    sycnRoute();
  }
});

onBeforeUnmount(() => {
  remoteApp?.unmount();
  remoteApp = null;
});

watch(() => shellRoute.path, sycnRoute, { immediate: true });

function sycnRoute(newPath?: string, oldPath?: string) {
  if (!remoteApp || newPath === oldPath) {
    return;
  }

  const subPath = shellRoute.path.substring(props.basePath.length) || '/';

  if (remoteApp.router.currentRoute.value.path !== subPath) {
    remoteApp.router.push(subPath);
  }
}
</script>

<template>
  <div ref="container"></div>
</template>

<style scoped>

</style>