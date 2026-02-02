import {
  Controller,
  Get,
  Param,
  Sse,
  ParseIntPipe,
  Req,
  UnauthorizedException,
} from '@nestjs/common';
import { Observable, interval, map, merge } from 'rxjs';
import type { Request } from 'express';
import { SseService } from './sse.service';
import { Public } from '../../common/decorators/public.decorator';

interface MessageEvent {
  data: string;
  type?: string;
  id?: string;
  retry?: number;
}

@Controller('sse')
export class SseController {
  constructor(private readonly sseService: SseService) {}

  @Public() // SSE는 EventSource가 Authorization 헤더를 지원하지 않아 Public 처리
  @Get('boards/:boardId')
  @Sse()
  subscribeToBoard(
    @Param('boardId', ParseIntPipe) boardId: number,
    @Req() req: Request,
  ): Observable<MessageEvent> {
    // Get userId from JWT token (set by gateway)
    const userId = this.extractUserId(req);

    // Heartbeat every 30 seconds to keep connection alive
    const heartbeat$ = interval(30000).pipe(
      map(() => ({
        data: JSON.stringify({
          type: 'heartbeat',
          timestamp: new Date().toISOString(),
        }),
        type: 'heartbeat',
      })),
    );

    // Merge board events with heartbeat
    const events$ = this.sseService.subscribe(userId, boardId);

    return merge(events$, heartbeat$);
  }

  private extractUserId(req: Request): string {
    // User ID is set by API Gateway from JWT token (UUID string)
    const userIdHeader = req.headers['x-user-id'];
    if (!userIdHeader) {
      throw new UnauthorizedException(
        'X-User-Id header is required. SSE connections must go through the API Gateway.',
      );
    }
    return String(userIdHeader);
  }
}
