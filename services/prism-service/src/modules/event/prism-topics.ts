/**
 * Prism domain Kafka topic constants.
 *
 * Must stay in sync with:
 * - Java: prism-events/PrismTopics.java
 * - JSON Schema: services/event-contracts/schemas/prism.task.*.schema.json
 */
export const PrismTopics = {
  TASK_COMPLETED: 'prism.task.completed',
  TASK_FAILED: 'prism.task.failed',
} as const;

export type PrismTopic = (typeof PrismTopics)[keyof typeof PrismTopics];
