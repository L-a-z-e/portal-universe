import {
  IsEnum,
  IsNotEmpty,
  IsOptional,
  IsString,
  IsUrl,
  MaxLength,
  ValidateIf,
} from 'class-validator';
import { ApiProperty, ApiPropertyOptional } from '@nestjs/swagger';
import { ProviderType } from '../provider.entity';
import { NoXss } from '../../../common/validators/no-xss.validator';

export class CreateProviderDto {
  @ApiProperty({ enum: ProviderType, example: ProviderType.OPENAI })
  @IsEnum(ProviderType)
  @IsNotEmpty()
  providerType: ProviderType;

  @ApiProperty({ example: 'My OpenAI Account' })
  @IsString()
  @IsNotEmpty()
  @MaxLength(100)
  @NoXss()
  name: string;

  @ApiPropertyOptional({ example: 'sk-xxxxxxxxxxxxxxxx' })
  @IsString()
  @IsOptional()
  @ValidateIf(
    (o: CreateProviderDto) =>
      ![ProviderType.OLLAMA, ProviderType.LOCAL].includes(o.providerType),
  )
  @IsNotEmpty()
  apiKey?: string;

  @ApiPropertyOptional({ example: 'https://api.openai.com/v1' })
  @IsOptional()
  @ValidateIf(
    (o: CreateProviderDto) =>
      ![ProviderType.OLLAMA, ProviderType.LOCAL].includes(o.providerType) ||
      !!o.baseUrl,
  )
  @IsUrl()
  @MaxLength(255)
  baseUrl?: string;
}
