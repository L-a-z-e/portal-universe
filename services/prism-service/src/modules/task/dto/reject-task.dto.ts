import { IsOptional, IsString } from 'class-validator';
import { ApiPropertyOptional } from '@nestjs/swagger';
import { NoXss } from '../../../common/validators/no-xss.validator';

export class RejectTaskDto {
  @ApiPropertyOptional({ example: 'Please add error handling' })
  @IsOptional()
  @IsString()
  @NoXss()
  feedback?: string;
}
