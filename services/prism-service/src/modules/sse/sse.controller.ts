import { Controller, Get, Param, Sse, ParseIntPipe } from '@nestjs/common';
import { Observable, interval, map, merge, from, switchMap } from 'rxjs';
import { SseService } from './sse.service';
import { BoardService } from '../board/board.service';
import { CurrentUserId } from '../../common/decorators/current-user.decorator';

interface MessageEvent {
  data: string;
  type?: string;
  id?: string;
  retry?: number;
}

@Controller('sse')
export class SseController {
  constructor(
    private readonly sseService: SseService,
    private readonly boardService: BoardService,
  ) {}

  @Get('boards/:boardId')
  @Sse()
  subscribeToBoard(
    @Param('boardId', ParseIntPipe) boardId: number,
    @CurrentUserId() userId: string,
  ): Observable<MessageEvent> {
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

    // Verify board ownership (throws 404 if not owner), then subscribe
    const events$ = from(this.boardService.getBoardEntity(userId, boardId)).pipe(
      switchMap(() => this.sseService.subscribe(userId, boardId)),
    );

    return merge(events$, heartbeat$);
  }
}
