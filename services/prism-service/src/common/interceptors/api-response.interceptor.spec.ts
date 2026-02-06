import { ApiResponseInterceptor } from './api-response.interceptor';
import { ExecutionContext, CallHandler } from '@nestjs/common';
import { of, lastValueFrom } from 'rxjs';

describe('ApiResponseInterceptor', () => {
  let interceptor: ApiResponseInterceptor<any>;

  function createMockContext(contentType?: string): ExecutionContext {
    return {
      switchToHttp: jest.fn().mockReturnValue({
        getResponse: jest.fn().mockReturnValue({
          getHeader: jest.fn().mockReturnValue(contentType || 'application/json'),
        }),
      }),
    } as unknown as ExecutionContext;
  }

  function createMockHandler(data: any): CallHandler {
    return {
      handle: jest.fn().mockReturnValue(of(data)),
    };
  }

  beforeEach(() => {
    interceptor = new ApiResponseInterceptor();
  });

  it('should wrap plain data with ApiResponse.success', async () => {
    const context = createMockContext();
    const handler = createMockHandler({ id: 1, name: 'test' });

    const result$ = interceptor.intercept(context, handler);
    const result = await lastValueFrom(result$);

    expect(result).toEqual({
      success: true,
      data: { id: 1, name: 'test' },
      error: null,
    });
  });

  it('should not wrap SSE (text/event-stream) responses', async () => {
    const context = createMockContext('text/event-stream');
    const handler = createMockHandler({ type: 'event', data: 'test' });

    const result$ = interceptor.intercept(context, handler);
    const result = await lastValueFrom(result$);

    expect(result).toEqual({ type: 'event', data: 'test' });
  });

  it('should not double-wrap already wrapped ApiResponse', async () => {
    const context = createMockContext();
    const alreadyWrapped = { success: true, data: { id: 1 }, error: null };
    const handler = createMockHandler(alreadyWrapped);

    const result$ = interceptor.intercept(context, handler);
    const result = await lastValueFrom(result$);

    // Should return as-is, not wrapped again
    expect(result).toEqual(alreadyWrapped);
  });

  it('should handle null data', async () => {
    const context = createMockContext();
    const handler = createMockHandler(null);

    const result$ = interceptor.intercept(context, handler);
    const result = await lastValueFrom(result$);

    expect(result).toEqual({
      success: true,
      data: null,
      error: null,
    });
  });

  it('should handle array data', async () => {
    const context = createMockContext();
    const handler = createMockHandler([1, 2, 3]);

    const result$ = interceptor.intercept(context, handler);
    const result = await lastValueFrom(result$);

    expect(result).toEqual({
      success: true,
      data: [1, 2, 3],
      error: null,
    });
  });

  it('should handle string data', async () => {
    const context = createMockContext();
    const handler = createMockHandler('hello');

    const result$ = interceptor.intercept(context, handler);
    const result = await lastValueFrom(result$);

    expect(result).toEqual({
      success: true,
      data: 'hello',
      error: null,
    });
  });
});
