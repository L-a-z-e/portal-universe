import { IsBoolean, IsOptional, IsString, MaxLength } from 'class-validator';
import { ApiPropertyOptional } from '@nestjs/swagger';
import { NoXss } from '../../../common/validators/no-xss.validator';

export class UpdateBoardDto {
  @ApiPropertyOptional({ example: 'Updated Board Name' })
  @IsOptional()
  @IsString()
  @MaxLength(100)
  @NoXss()
  name?: string;

  @ApiPropertyOptional({ example: 'Updated description' })
  @IsOptional()
  @IsString()
  @NoXss()
  description?: string;

  @ApiPropertyOptional({ example: false })
  @IsOptional()
  @IsBoolean()
  isArchived?: boolean;
}
