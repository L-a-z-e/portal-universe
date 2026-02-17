<script setup lang="ts">
import { ref, onMounted, watch } from 'vue';
import { Spinner, Card } from '@portal/design-vue';
import { fetchRoleHierarchy } from '@/api/admin';
import { useRoleDag, type DagNode } from '@/composables/useRoleDag';

const props = defineProps<{
  selectedRoleKey?: string;
}>();

const emit = defineEmits<{
  selectRole: [roleKey: string];
}>();

const graph = ref<Record<string, string[]>>({});
const loading = ref(false);
const collapsed = ref(false);

const { layout, NODE_WIDTH, NODE_HEIGHT } = useRoleDag(graph);

async function loadHierarchy() {
  loading.value = true;
  try {
    const res = await fetchRoleHierarchy();
    graph.value = res.graph;
  } catch {
    // non-critical
  } finally {
    loading.value = false;
  }
}

function nodeColor(node: DagNode): string {
  if (node.key === props.selectedRoleKey) return '#3b82f6';
  if (node.level === 0) return '#6366f1';
  return '#64748b';
}

function nodeTextColor(node: DagNode): string {
  if (node.key === props.selectedRoleKey) return '#ffffff';
  if (node.level === 0) return '#ffffff';
  return '#ffffff';
}

function truncateKey(key: string): string {
  if (key.length <= 16) return key;
  return key.slice(0, 14) + '...';
}

onMounted(loadHierarchy);

watch(() => props.selectedRoleKey, () => {
  // Reload when role changes to reflect any updates
});
</script>

<template>
  <Card variant="elevated" padding="none">
    <button
      class="w-full flex items-center justify-between p-5 text-left hover:bg-bg-hover transition-colors"
      @click="collapsed = !collapsed"
    >
      <h3 class="text-sm font-semibold text-text-heading">Role Hierarchy (DAG)</h3>
      <span class="material-symbols-outlined text-text-muted transition-transform" :class="{ 'rotate-180': !collapsed }" style="font-size: 20px;">
        expand_more
      </span>
    </button>

    <div v-show="!collapsed" class="px-5 pb-5">
      <div v-if="loading" class="flex justify-center py-4">
        <Spinner size="sm" />
      </div>

      <div v-else-if="layout.nodes.length === 0" class="text-sm text-text-meta py-2">
        No hierarchy data available
      </div>

      <div v-else class="overflow-x-auto rounded-lg border border-border-default bg-bg-base">
        <svg
          :width="layout.width"
          :height="layout.height"
          class="dag-svg"
          data-testid="role-dag-svg"
        >
          <!-- Edges -->
          <path
            v-for="edge in layout.edges"
            :key="`${edge.from}-${edge.to}`"
            :d="edge.path"
            fill="none"
            stroke="#94a3b8"
            stroke-width="1.5"
            stroke-dasharray="4 2"
            marker-end="url(#arrowhead)"
          />

          <!-- Arrow marker -->
          <defs>
            <marker id="arrowhead" markerWidth="8" markerHeight="6" refX="8" refY="3" orient="auto">
              <polygon points="0 0, 8 3, 0 6" fill="#94a3b8" />
            </marker>
          </defs>

          <!-- Nodes -->
          <g
            v-for="node in layout.nodes"
            :key="node.key"
            class="cursor-pointer"
            @click="emit('selectRole', node.key)"
          >
            <rect
              :x="node.x"
              :y="node.y"
              :width="NODE_WIDTH"
              :height="NODE_HEIGHT"
              :rx="6"
              :fill="nodeColor(node)"
              :stroke="node.key === selectedRoleKey ? '#2563eb' : 'transparent'"
              stroke-width="2"
              class="transition-colors"
            />
            <text
              :x="node.x + NODE_WIDTH / 2"
              :y="node.y + NODE_HEIGHT / 2 + 1"
              text-anchor="middle"
              dominant-baseline="middle"
              :fill="nodeTextColor(node)"
              font-size="11"
              font-family="monospace"
            >
              {{ truncateKey(node.key) }}
            </text>
          </g>
        </svg>
      </div>
    </div>
  </Card>
</template>
