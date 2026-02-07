import { Injectable, Logger } from '@nestjs/common';
import { Subject, Observable, filter, map, finalize } from 'rxjs';

export interface SseEvent {
  type:
    | 'task.created'
    | 'task.updated'
    | 'task.moved'
    | 'task.deleted'
    | 'execution.started'
    | 'execution.completed'
    | 'execution.failed';
  boardId: number;
  userId: string;
  payload: Record<string, unknown>;
  timestamp: string;
}

interface MessageEvent {
  data: string;
  type?: string;
  id?: string;
  retry?: number;
}

@Injectable()
export class SseService {
  private readonly logger = new Logger(SseService.name);
  private readonly events$ = new Subject<SseEvent>();
  private readonly connections = new Map<string, Set<string>>();

  get activeConnectionCount(): number {
    let count = 0;
    for (const boards of this.connections.values()) {
      count += boards.size;
    }
    return count;
  }

  emit(event: SseEvent): void {
    this.logger.debug(`SSE Event: ${event.type} for board ${event.boardId}`);
    this.events$.next(event);
  }

  subscribe(userId: string, boardId: number): Observable<MessageEvent> {
    // Track connection
    if (!this.connections.has(userId)) {
      this.connections.set(userId, new Set());
    }
    this.connections.get(userId)!.add(String(boardId));
    this.logger.log(
      `User ${userId} subscribed to board ${boardId} (active: ${this.activeConnectionCount})`,
    );

    return this.events$.pipe(
      filter((event) => event.boardId === boardId && event.userId === userId),
      map((event) => ({
        data: JSON.stringify({
          type: event.type,
          data: event.payload,
          timestamp: event.timestamp,
        }),
        type: event.type,
      })),
      finalize(() => {
        // Clean up on disconnect
        const boards = this.connections.get(userId);
        if (boards) {
          boards.delete(String(boardId));
          if (boards.size === 0) {
            this.connections.delete(userId);
          }
        }
        this.logger.log(
          `User ${userId} disconnected from board ${boardId} (active: ${this.activeConnectionCount})`,
        );
      }),
    );
  }

  emitTaskCreated(
    userId: string,
    boardId: number,
    task: Record<string, unknown>,
  ): void {
    this.emit({
      type: 'task.created',
      boardId,
      userId,
      payload: { task },
      timestamp: new Date().toISOString(),
    });
  }

  emitTaskUpdated(
    userId: string,
    boardId: number,
    task: Record<string, unknown>,
  ): void {
    this.emit({
      type: 'task.updated',
      boardId,
      userId,
      payload: { task },
      timestamp: new Date().toISOString(),
    });
  }

  emitTaskMoved(
    userId: string,
    boardId: number,
    taskId: number,
    fromStatus: string,
    toStatus: string,
    position: number,
  ): void {
    this.emit({
      type: 'task.moved',
      boardId,
      userId,
      payload: { taskId, fromStatus, toStatus, position },
      timestamp: new Date().toISOString(),
    });
  }

  emitTaskDeleted(userId: string, boardId: number, taskId: number): void {
    this.emit({
      type: 'task.deleted',
      boardId,
      userId,
      payload: { taskId },
      timestamp: new Date().toISOString(),
    });
  }

  emitExecutionStarted(
    userId: string,
    boardId: number,
    taskId: number,
    executionId: number,
  ): void {
    this.emit({
      type: 'execution.started',
      boardId,
      userId,
      payload: { taskId, executionId },
      timestamp: new Date().toISOString(),
    });
  }

  emitExecutionCompleted(
    userId: string,
    boardId: number,
    taskId: number,
    executionId: number,
    result: string,
  ): void {
    this.emit({
      type: 'execution.completed',
      boardId,
      userId,
      payload: { taskId, executionId, result },
      timestamp: new Date().toISOString(),
    });
  }

  emitExecutionFailed(
    userId: string,
    boardId: number,
    taskId: number,
    executionId: number,
    error: string,
  ): void {
    this.emit({
      type: 'execution.failed',
      boardId,
      userId,
      payload: { taskId, executionId, error },
      timestamp: new Date().toISOString(),
    });
  }
}
