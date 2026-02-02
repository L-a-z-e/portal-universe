import {
  Controller,
  Get,
  Post,
  Put,
  Delete,
  Body,
  Param,
  Query,
  ParseIntPipe,
  HttpCode,
  HttpStatus,
} from '@nestjs/common';
import {
  ApiTags,
  ApiOperation,
  ApiResponse,
  ApiBearerAuth,
} from '@nestjs/swagger';
import { ProviderService } from './provider.service';
import { CreateProviderDto } from './dto/create-provider.dto';
import { UpdateProviderDto } from './dto/update-provider.dto';
import {
  ProviderResponseDto,
  VerifyProviderResponseDto,
} from './dto/provider-response.dto';
import { CurrentUserId } from '../../common/decorators/current-user.decorator';
import {
  PaginationDto,
  PaginatedResult,
} from '../../common/dto/pagination.dto';

@ApiTags('AI Providers')
@ApiBearerAuth()
@Controller('providers')
export class ProviderController {
  constructor(private readonly providerService: ProviderService) {}

  @Post()
  @ApiOperation({ summary: 'Register a new AI provider' })
  @ApiResponse({ status: 201, type: ProviderResponseDto })
  async create(
    @CurrentUserId() userId: string,
    @Body() dto: CreateProviderDto,
  ): Promise<ProviderResponseDto> {
    return this.providerService.create(userId, dto);
  }

  @Get()
  @ApiOperation({ summary: 'List all providers for the current user' })
  @ApiResponse({ status: 200, type: [ProviderResponseDto] })
  async findAll(
    @CurrentUserId() userId: string,
    @Query() pagination: PaginationDto,
  ): Promise<PaginatedResult<ProviderResponseDto>> {
    return this.providerService.findAll(userId, pagination);
  }

  @Get(':id')
  @ApiOperation({ summary: 'Get provider details' })
  @ApiResponse({ status: 200, type: ProviderResponseDto })
  async findOne(
    @CurrentUserId() userId: string,
    @Param('id', ParseIntPipe) id: number,
  ): Promise<ProviderResponseDto> {
    return this.providerService.findOne(userId, id);
  }

  @Put(':id')
  @ApiOperation({ summary: 'Update a provider' })
  @ApiResponse({ status: 200, type: ProviderResponseDto })
  async update(
    @CurrentUserId() userId: string,
    @Param('id', ParseIntPipe) id: number,
    @Body() dto: UpdateProviderDto,
  ): Promise<ProviderResponseDto> {
    return this.providerService.update(userId, id, dto);
  }

  @Delete(':id')
  @HttpCode(HttpStatus.NO_CONTENT)
  @ApiOperation({ summary: 'Delete a provider' })
  @ApiResponse({ status: 204, description: 'Provider deleted' })
  async remove(
    @CurrentUserId() userId: string,
    @Param('id', ParseIntPipe) id: number,
  ): Promise<void> {
    return this.providerService.remove(userId, id);
  }

  @Post(':id/verify')
  @ApiOperation({ summary: 'Verify provider connection and fetch models' })
  @ApiResponse({ status: 200, type: VerifyProviderResponseDto })
  async verify(
    @CurrentUserId() userId: string,
    @Param('id', ParseIntPipe) id: number,
  ): Promise<VerifyProviderResponseDto> {
    return this.providerService.verify(userId, id);
  }

  @Get(':id/models')
  @ApiOperation({ summary: 'Get available models for a provider' })
  @ApiResponse({ status: 200, type: [String] })
  async getModels(
    @CurrentUserId() userId: string,
    @Param('id', ParseIntPipe) id: number,
  ): Promise<string[]> {
    return this.providerService.getModels(userId, id);
  }
}
