import { TaskStatus } from './task.entity';
import { BusinessException } from '../../common/filters/business.exception';

/**
 * Task State Machine
 *
 * State transitions:
 * TODO ──[execute]──> IN_PROGRESS ──[complete]──> IN_REVIEW ──[approve]──> DONE
 *   │                     │                          │
 *   └──[cancel]──>        └──[cancel]──>             ├──[retry]──> IN_PROGRESS
 *                CANCELLED              CANCELLED    └──[cancel]──> CANCELLED
 *
 * DONE ──[reopen]──> TODO
 */
export class TaskStateMachine {
  private static readonly transitions: Record<
    TaskStatus,
    Record<string, TaskStatus>
  > = {
    [TaskStatus.TODO]: {
      execute: TaskStatus.IN_PROGRESS,
      cancel: TaskStatus.CANCELLED,
    },
    [TaskStatus.IN_PROGRESS]: {
      complete: TaskStatus.IN_REVIEW,
      cancel: TaskStatus.CANCELLED,
    },
    [TaskStatus.IN_REVIEW]: {
      approve: TaskStatus.DONE,
      retry: TaskStatus.IN_PROGRESS,
      cancel: TaskStatus.CANCELLED,
    },
    [TaskStatus.DONE]: {
      reopen: TaskStatus.TODO,
    },
    [TaskStatus.CANCELLED]: {
      reopen: TaskStatus.TODO,
    },
  };

  /**
   * Get available actions for a given status
   */
  static getAvailableActions(status: TaskStatus): string[] {
    return Object.keys(this.transitions[status] || {});
  }

  /**
   * Check if a transition is valid
   */
  static canTransition(from: TaskStatus, action: string): boolean {
    return this.transitions[from]?.[action] !== undefined;
  }

  /**
   * Get the target status for a transition
   */
  static getTargetStatus(from: TaskStatus, action: string): TaskStatus {
    const target = this.transitions[from]?.[action];
    if (!target) {
      throw BusinessException.invalidStateTransition(from, action);
    }
    return target;
  }

  /**
   * Validate and perform transition
   */
  static transition(from: TaskStatus, action: string): TaskStatus {
    if (!this.canTransition(from, action)) {
      const available = this.getAvailableActions(from);
      throw BusinessException.invalidStateTransition(
        from,
        `${action} (available: ${available.join(', ')})`,
      );
    }
    return this.getTargetStatus(from, action);
  }
}

export type TaskAction =
  | 'execute'
  | 'complete'
  | 'approve'
  | 'retry'
  | 'cancel'
  | 'reopen';
