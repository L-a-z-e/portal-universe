import { useEffect, useRef, useCallback } from 'react';
import { isBridgeReady, getAdapter } from '@portal/react-bridge';

export interface SseEvent {
  type: string;
  data: Record<string, unknown>;
  timestamp: string;
}

export type SseEventHandler = (event: SseEvent) => void;

interface UseSseOptions {
  boardId: number | null;
  onEvent: SseEventHandler;
  enabled?: boolean;
}

const SSE_BASE_URL = import.meta.env.VITE_API_BASE_URL || '';
const MAX_RECONNECT_ATTEMPTS = 5;

/**
 * Get access token from various sources
 */
function getAccessToken(): string | null {
  // 1. Bridge에서 토큰 가져오기 (우선)
  if (isBridgeReady()) {
    const token = getAdapter('auth').getAccessToken?.();
    if (token) return token;
  }
  // 2. Fallback: window globals
  const globalToken = window.__PORTAL_GET_ACCESS_TOKEN__?.() ?? window.__PORTAL_ACCESS_TOKEN__;
  if (globalToken) return globalToken;
  // 3. localStorage (standalone 모드)
  return localStorage.getItem('access_token');
}

/**
 * Parse SSE event from text line
 */
function parseSseEvent(lines: string[]): { event?: string; data?: string; id?: string } {
  const result: { event?: string; data?: string; id?: string } = {};
  for (const line of lines) {
    if (line.startsWith('event:')) {
      result.event = line.slice(6).trim();
    } else if (line.startsWith('data:')) {
      result.data = line.slice(5).trim();
    } else if (line.startsWith('id:')) {
      result.id = line.slice(3).trim();
    }
  }
  return result;
}

export function useSse({ boardId, onEvent, enabled = true }: UseSseOptions) {
  const abortControllerRef = useRef<AbortController | null>(null);
  const reconnectTimeoutRef = useRef<number | null>(null);
  const reconnectAttemptsRef = useRef(0);

  const connect = useCallback(async () => {
    if (!boardId || !enabled) return;

    // Close existing connection
    if (abortControllerRef.current) {
      abortControllerRef.current.abort();
    }

    const abortController = new AbortController();
    abortControllerRef.current = abortController;

    const url = `${SSE_BASE_URL}/api/v1/prism/sse/boards/${boardId}`;
    const token = getAccessToken();

    const headers: HeadersInit = {
      'Accept': 'text/event-stream',
      'Cache-Control': 'no-cache',
    };
    if (token) {
      headers['Authorization'] = `Bearer ${token}`;
    }

    try {
      const response = await fetch(url, {
        method: 'GET',
        headers,
        credentials: 'include',
        signal: abortController.signal,
      });

      if (!response.ok) {
        throw new Error(`SSE connection failed: ${response.status}`);
      }

      console.log(`[SSE] Connected to board ${boardId}`);
      reconnectAttemptsRef.current = 0;

      const reader = response.body?.getReader();
      if (!reader) {
        throw new Error('No readable stream');
      }

      const decoder = new TextDecoder();
      let buffer = '';

      while (true) {
        const { done, value } = await reader.read();
        if (done) break;

        buffer += decoder.decode(value, { stream: true });

        // Split by double newline (SSE event separator)
        const events = buffer.split('\n\n');
        buffer = events.pop() || ''; // Keep incomplete event in buffer

        for (const eventText of events) {
          if (!eventText.trim()) continue;

          const lines = eventText.split('\n');
          const parsed = parseSseEvent(lines);

          if (parsed.event && parsed.data) {
            try {
              const data = JSON.parse(parsed.data) as SseEvent;
              onEvent({ ...data, type: parsed.event });
            } catch (error) {
              console.error(`[SSE] Failed to parse ${parsed.event} event:`, error);
            }
          }
        }
      }
    } catch (error) {
      if ((error as Error).name === 'AbortError') {
        console.log('[SSE] Connection aborted');
        return;
      }

      console.error('[SSE] Connection error:', error);

      if (!enabled) return;

      const attempts = reconnectAttemptsRef.current;
      if (attempts >= MAX_RECONNECT_ATTEMPTS) {
        console.error(`[SSE] Max reconnection attempts (${MAX_RECONNECT_ATTEMPTS}) reached. Giving up.`);
        return;
      }

      // Exponential backoff reconnection
      const delay = Math.min(1000 * Math.pow(2, attempts), 30000);

      console.log(`[SSE] Reconnecting in ${delay}ms (attempt ${attempts + 1}/${MAX_RECONNECT_ATTEMPTS})`);
      reconnectTimeoutRef.current = window.setTimeout(() => {
        reconnectAttemptsRef.current += 1;
        connect();
      }, delay);
    }
  }, [boardId, enabled, onEvent]);

  const disconnect = useCallback(() => {
    if (reconnectTimeoutRef.current) {
      clearTimeout(reconnectTimeoutRef.current);
      reconnectTimeoutRef.current = null;
    }
    if (abortControllerRef.current) {
      abortControllerRef.current.abort();
      abortControllerRef.current = null;
    }
  }, []);

  useEffect(() => {
    connect();
    return () => disconnect();
  }, [connect, disconnect]);

  return { disconnect, reconnect: connect };
}
