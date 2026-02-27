import { Global, Module } from '@nestjs/common';
import { SseController } from './sse.controller';
import { SseService } from './sse.service';
import { BoardModule } from '../board/board.module';

@Global()
@Module({
  imports: [BoardModule],
  controllers: [SseController],
  providers: [SseService],
  exports: [SseService],
})
export class SseModule {}
