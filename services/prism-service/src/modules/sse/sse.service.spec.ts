import { SseService } from './sse.service';
import { firstValueFrom, take } from 'rxjs';

describe('SseService', () => {
  let service: SseService;

  beforeEach(() => {
    service = new SseService();
  });

  it('should have 0 active connections initially', () => {
    expect(service.activeConnectionCount).toBe(0);
  });

  it('should subscribe and receive matching events', async () => {
    const userId = 'user-1';
    const boardId = 1;

    const observable = service.subscribe(userId, boardId);

    // Set up collection before emitting
    const resultPromise = firstValueFrom(observable.pipe(take(1)));

    service.emit({
      type: 'task.created',
      boardId: 1,
      userId: 'user-1',
      payload: { task: { id: 1 } },
      timestamp: '2026-01-01T00:00:00Z',
    });

    const result = await resultPromise;
    const parsed = JSON.parse(result.data);
    expect(parsed.type).toBe('task.created');
    expect(parsed.data).toEqual({ task: { id: 1 } });
    expect(result.type).toBe('task.created');
  });

  it('should filter events by boardId', async () => {
    const observable = service.subscribe('user-1', 1);

    const resultPromise = firstValueFrom(observable.pipe(take(1)));

    // Emit for different board first (should be filtered)
    service.emit({
      type: 'task.created',
      boardId: 999,
      userId: 'user-1',
      payload: {},
      timestamp: new Date().toISOString(),
    });

    // Emit for matching board
    service.emit({
      type: 'task.updated',
      boardId: 1,
      userId: 'user-1',
      payload: { task: { id: 2 } },
      timestamp: new Date().toISOString(),
    });

    const result = await resultPromise;
    const parsed = JSON.parse(result.data);
    expect(parsed.type).toBe('task.updated');
  });

  it('should filter events by userId', async () => {
    const observable = service.subscribe('user-1', 1);

    const resultPromise = firstValueFrom(observable.pipe(take(1)));

    // Emit for different user (should be filtered)
    service.emit({
      type: 'task.created',
      boardId: 1,
      userId: 'user-other',
      payload: {},
      timestamp: new Date().toISOString(),
    });

    // Emit for matching user
    service.emit({
      type: 'task.deleted',
      boardId: 1,
      userId: 'user-1',
      payload: { taskId: 5 },
      timestamp: new Date().toISOString(),
    });

    const result = await resultPromise;
    const parsed = JSON.parse(result.data);
    expect(parsed.type).toBe('task.deleted');
  });

  it('should track connection count', () => {
    service.subscribe('user-1', 1);
    service.subscribe('user-1', 2);
    service.subscribe('user-2', 1);

    expect(service.activeConnectionCount).toBe(3);
  });

  it('should clean up on unsubscribe (finalize)', () => {
    const observable = service.subscribe('user-1', 1);

    expect(service.activeConnectionCount).toBe(1);

    // Subscribe then unsubscribe to trigger finalize
    const subscription = observable.subscribe();
    subscription.unsubscribe();

    // After finalize, connection should be cleaned
    expect(service.activeConnectionCount).toBe(0);
  });

  describe('convenience methods', () => {
    it('emitTaskCreated should emit task.created event', async () => {
      const observable = service.subscribe('user-1', 1);
      const resultPromise = firstValueFrom(observable.pipe(take(1)));

      service.emitTaskCreated('user-1', 1, { id: 1, title: 'New' });

      const result = await resultPromise;
      const parsed = JSON.parse(result.data);
      expect(parsed.type).toBe('task.created');
      expect(parsed.data.task).toEqual({ id: 1, title: 'New' });
    });

    it('emitTaskMoved should emit task.moved event', async () => {
      const observable = service.subscribe('user-1', 1);
      const resultPromise = firstValueFrom(observable.pipe(take(1)));

      service.emitTaskMoved('user-1', 1, 5, 'TODO', 'IN_PROGRESS', 0);

      const result = await resultPromise;
      const parsed = JSON.parse(result.data);
      expect(parsed.type).toBe('task.moved');
      expect(parsed.data).toEqual({
        taskId: 5,
        fromStatus: 'TODO',
        toStatus: 'IN_PROGRESS',
        position: 0,
      });
    });
  });
});
