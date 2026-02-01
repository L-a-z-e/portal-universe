import { useEffect, useRef, useCallback } from 'react';

export interface SseEvent {
  type: string;
  payload: Record<string, unknown>;
  timestamp: string;
}

export type SseEventHandler = (event: SseEvent) => void;

interface UseSseOptions {
  boardId: number | null;
  onEvent: SseEventHandler;
  enabled?: boolean;
}

const SSE_BASE_URL = import.meta.env.VITE_API_BASE_URL || '/api';

export function useSse({ boardId, onEvent, enabled = true }: UseSseOptions) {
  const eventSourceRef = useRef<EventSource | null>(null);
  const reconnectTimeoutRef = useRef<number | null>(null);
  const reconnectAttemptsRef = useRef(0);

  const connect = useCallback(() => {
    if (!boardId || !enabled) return;

    // Close existing connection
    if (eventSourceRef.current) {
      eventSourceRef.current.close();
    }

    const url = `${SSE_BASE_URL}/api/v1/prism/sse/boards/${boardId}`;
    const eventSource = new EventSource(url, { withCredentials: true });
    eventSourceRef.current = eventSource;

    eventSource.onopen = () => {
      console.log(`[SSE] Connected to board ${boardId}`);
      reconnectAttemptsRef.current = 0;
    };

    eventSource.onmessage = (event) => {
      try {
        const data = JSON.parse(event.data) as SseEvent;
        if (data.type !== 'heartbeat') {
          onEvent(data);
        }
      } catch (error) {
        console.error('[SSE] Failed to parse event:', error);
      }
    };

    // Listen for specific event types
    const eventTypes = [
      'task.created',
      'task.updated',
      'task.moved',
      'task.deleted',
      'execution.started',
      'execution.completed',
      'execution.failed',
    ];

    eventTypes.forEach((type) => {
      eventSource.addEventListener(type, (event: Event) => {
        const messageEvent = event as MessageEvent;
        try {
          const data = JSON.parse(messageEvent.data) as SseEvent;
          onEvent({ ...data, type });
        } catch (error) {
          console.error(`[SSE] Failed to parse ${type} event:`, error);
        }
      });
    });

    eventSource.onerror = () => {
      console.error('[SSE] Connection error');
      eventSource.close();

      // Exponential backoff reconnection
      const attempts = reconnectAttemptsRef.current;
      const delay = Math.min(1000 * Math.pow(2, attempts), 30000);

      console.log(`[SSE] Reconnecting in ${delay}ms (attempt ${attempts + 1})`);
      reconnectTimeoutRef.current = window.setTimeout(() => {
        reconnectAttemptsRef.current += 1;
        connect();
      }, delay);
    };
  }, [boardId, enabled, onEvent]);

  const disconnect = useCallback(() => {
    if (reconnectTimeoutRef.current) {
      clearTimeout(reconnectTimeoutRef.current);
      reconnectTimeoutRef.current = null;
    }
    if (eventSourceRef.current) {
      eventSourceRef.current.close();
      eventSourceRef.current = null;
    }
  }, []);

  useEffect(() => {
    connect();
    return () => disconnect();
  }, [connect, disconnect]);

  return { disconnect, reconnect: connect };
}
