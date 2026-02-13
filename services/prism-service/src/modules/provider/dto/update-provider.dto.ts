import {
  IsBoolean,
  IsOptional,
  IsString,
  IsUrl,
  MaxLength,
} from 'class-validator';
import { ApiPropertyOptional } from '@nestjs/swagger';
import { NoXss } from '../../../common/validators/no-xss.validator';

export class UpdateProviderDto {
  @ApiPropertyOptional({ example: 'Updated Provider Name' })
  @IsOptional()
  @IsString()
  @MaxLength(100)
  @NoXss()
  name?: string;

  @ApiPropertyOptional({ example: 'sk-newkey...' })
  @IsOptional()
  @IsString()
  apiKey?: string;

  @ApiPropertyOptional({ example: 'https://api.openai.com/v1' })
  @IsOptional()
  @IsUrl()
  @MaxLength(255)
  baseUrl?: string;

  @ApiPropertyOptional({ example: true })
  @IsOptional()
  @IsBoolean()
  isActive?: boolean;
}
