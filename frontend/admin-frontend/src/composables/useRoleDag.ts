import { computed, type Ref } from 'vue';

export interface DagNode {
  key: string;
  x: number;
  y: number;
  level: number;
}

export interface DagEdge {
  from: string;
  to: string;
  path: string;
}

export interface DagLayout {
  nodes: DagNode[];
  edges: DagEdge[];
  width: number;
  height: number;
}

const NODE_WIDTH = 140;
const NODE_HEIGHT = 36;
const LEVEL_GAP_Y = 80;
const NODE_GAP_X = 30;
const PADDING = 40;

export function useRoleDag(graph: Ref<Record<string, string[]>>) {
  const layout = computed<DagLayout>(() => {
    const g = graph.value;
    if (!g || Object.keys(g).length === 0) {
      return { nodes: [], edges: [], width: 0, height: 0 };
    }

    // Collect all node keys
    const allKeys = new Set<string>();
    for (const [parent, children] of Object.entries(g)) {
      allKeys.add(parent);
      children.forEach((c) => allKeys.add(c));
    }

    // Build reverse map: child → parents
    const parents = new Map<string, Set<string>>();
    for (const key of allKeys) parents.set(key, new Set());
    for (const [parent, children] of Object.entries(g)) {
      for (const child of children) {
        parents.get(child)!.add(parent);
      }
    }

    // BFS level assignment from roots (nodes with no parents)
    const levels = new Map<string, number>();
    const roots = [...allKeys].filter((k) => parents.get(k)!.size === 0);

    // If no roots found (cycle), pick first key as root
    if (roots.length === 0 && allKeys.size > 0) {
      roots.push([...allKeys][0]!);
    }

    const queue = roots.map((r) => ({ key: r, level: 0 }));
    for (const root of roots) levels.set(root, 0);

    while (queue.length > 0) {
      const { key, level } = queue.shift()!;
      const children = g[key] ?? [];
      for (const child of children) {
        const current = levels.get(child);
        const newLevel = level + 1;
        if (current === undefined || newLevel > current) {
          levels.set(child, newLevel);
          queue.push({ key: child, level: newLevel });
        }
      }
    }

    // Ensure all nodes have a level
    for (const key of allKeys) {
      if (!levels.has(key)) levels.set(key, 0);
    }

    // Group by level
    const byLevel = new Map<number, string[]>();
    for (const [key, level] of levels) {
      if (!byLevel.has(level)) byLevel.set(level, []);
      byLevel.get(level)!.push(key);
    }

    // Sort keys within each level for stability
    for (const keys of byLevel.values()) keys.sort();

    const maxLevel = Math.max(0, ...byLevel.keys());
    const maxWidth = Math.max(...[...byLevel.values()].map((arr) => arr.length));

    // Calculate positions
    const nodes: DagNode[] = [];
    const keyToPos = new Map<string, { x: number; y: number }>();

    for (let level = 0; level <= maxLevel; level++) {
      const keys = byLevel.get(level) ?? [];
      const totalWidth = keys.length * NODE_WIDTH + (keys.length - 1) * NODE_GAP_X;
      const maxTotalWidth = maxWidth * NODE_WIDTH + (maxWidth - 1) * NODE_GAP_X;
      const startX = PADDING + (maxTotalWidth - totalWidth) / 2;
      const y = PADDING + level * LEVEL_GAP_Y;

      keys.forEach((key, i) => {
        const x = startX + i * (NODE_WIDTH + NODE_GAP_X);
        nodes.push({ key, x, y, level });
        keyToPos.set(key, { x: x + NODE_WIDTH / 2, y: y + NODE_HEIGHT / 2 });
      });
    }

    // Calculate edges with bezier curves
    const edges: DagEdge[] = [];
    for (const [parent, children] of Object.entries(g)) {
      const from = keyToPos.get(parent);
      if (!from) continue;
      for (const child of children) {
        const to = keyToPos.get(child);
        if (!to) continue;
        const fromY = from.y + NODE_HEIGHT / 2;
        const toY = to.y - NODE_HEIGHT / 2;
        const midY = (fromY + toY) / 2;
        const path = `M ${from.x} ${fromY} C ${from.x} ${midY}, ${to.x} ${midY}, ${to.x} ${toY}`;
        edges.push({ from: parent, to: child, path });
      }
    }

    const width = maxWidth * NODE_WIDTH + (maxWidth - 1) * NODE_GAP_X + PADDING * 2;
    const height = (maxLevel + 1) * LEVEL_GAP_Y + PADDING * 2 - (LEVEL_GAP_Y - NODE_HEIGHT);

    return { nodes, edges, width: Math.max(width, 200), height: Math.max(height, 100) };
  });

  return { layout, NODE_WIDTH, NODE_HEIGHT };
}
