import {
  IsEnum,
  IsNotEmpty,
  IsOptional,
  IsString,
  IsUrl,
  MaxLength,
} from 'class-validator';
import { ApiProperty, ApiPropertyOptional } from '@nestjs/swagger';
import { ProviderType } from '../provider.entity';

export class CreateProviderDto {
  @ApiProperty({ enum: ProviderType, example: ProviderType.OPENAI })
  @IsEnum(ProviderType)
  @IsNotEmpty()
  providerType: ProviderType;

  @ApiProperty({ example: 'My OpenAI Account' })
  @IsString()
  @IsNotEmpty()
  @MaxLength(100)
  name: string;

  @ApiProperty({ example: 'sk-xxxxxxxxxxxxxxxx' })
  @IsString()
  @IsNotEmpty()
  apiKey: string;

  @ApiPropertyOptional({ example: 'https://api.openai.com/v1' })
  @IsOptional()
  @IsUrl()
  @MaxLength(255)
  baseUrl?: string;
}
