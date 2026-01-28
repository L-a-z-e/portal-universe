import { Module } from '@nestjs/common';
import { AIService } from './ai.service';
import { ProviderModule } from '../provider/provider.module';

@Module({
  imports: [ProviderModule],
  providers: [AIService],
  exports: [AIService],
})
export class AIModule {}
