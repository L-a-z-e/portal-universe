import { HttpException, HttpStatus } from '@nestjs/common';

export enum PrismErrorCode {
  // P001-P099: Domain business errors
  NOT_FOUND = 'P001',
  INVALID_STATE_TRANSITION = 'P002',
  AGENT_NOT_ASSIGNED = 'P003',
  PROVIDER_CONNECTION_FAILED = 'P004',
  AI_EXECUTION_FAILED = 'P005',
  INVALID_REQUEST = 'P006',
  DUPLICATE_RESOURCE = 'P007',
  ENCRYPTION_FAILED = 'P008',
  RATE_LIMIT_EXCEEDED = 'P009',

  // P900-P999: Common/infrastructure errors
  INTERNAL_ERROR = 'P900',
  VALIDATION_ERROR = 'P901',
  UNAUTHORIZED = 'P902',
  FORBIDDEN = 'P903',
}

const ERROR_MESSAGES: Record<PrismErrorCode, string> = {
  [PrismErrorCode.NOT_FOUND]: 'Resource not found',
  [PrismErrorCode.INVALID_STATE_TRANSITION]: 'Invalid state transition',
  [PrismErrorCode.AGENT_NOT_ASSIGNED]: 'Agent not assigned to task',
  [PrismErrorCode.PROVIDER_CONNECTION_FAILED]: 'AI provider connection failed',
  [PrismErrorCode.AI_EXECUTION_FAILED]: 'AI execution failed',
  [PrismErrorCode.INVALID_REQUEST]: 'Invalid request',
  [PrismErrorCode.DUPLICATE_RESOURCE]: 'Duplicate resource',
  [PrismErrorCode.ENCRYPTION_FAILED]: 'Encryption/decryption failed',
  [PrismErrorCode.RATE_LIMIT_EXCEEDED]: 'Rate limit exceeded',
  [PrismErrorCode.INTERNAL_ERROR]: 'Internal server error',
  [PrismErrorCode.VALIDATION_ERROR]: 'Invalid input value',
  [PrismErrorCode.UNAUTHORIZED]: 'Unauthorized',
  [PrismErrorCode.FORBIDDEN]: 'Forbidden',
};

const ERROR_STATUS: Record<PrismErrorCode, HttpStatus> = {
  [PrismErrorCode.NOT_FOUND]: HttpStatus.NOT_FOUND,
  [PrismErrorCode.INVALID_STATE_TRANSITION]: HttpStatus.BAD_REQUEST,
  [PrismErrorCode.AGENT_NOT_ASSIGNED]: HttpStatus.BAD_REQUEST,
  [PrismErrorCode.PROVIDER_CONNECTION_FAILED]: HttpStatus.SERVICE_UNAVAILABLE,
  [PrismErrorCode.AI_EXECUTION_FAILED]: HttpStatus.INTERNAL_SERVER_ERROR,
  [PrismErrorCode.INVALID_REQUEST]: HttpStatus.BAD_REQUEST,
  [PrismErrorCode.DUPLICATE_RESOURCE]: HttpStatus.CONFLICT,
  [PrismErrorCode.ENCRYPTION_FAILED]: HttpStatus.INTERNAL_SERVER_ERROR,
  [PrismErrorCode.RATE_LIMIT_EXCEEDED]: HttpStatus.TOO_MANY_REQUESTS,
  [PrismErrorCode.INTERNAL_ERROR]: HttpStatus.INTERNAL_SERVER_ERROR,
  [PrismErrorCode.VALIDATION_ERROR]: HttpStatus.BAD_REQUEST,
  [PrismErrorCode.UNAUTHORIZED]: HttpStatus.UNAUTHORIZED,
  [PrismErrorCode.FORBIDDEN]: HttpStatus.FORBIDDEN,
};

export class BusinessException extends HttpException {
  readonly code: string;

  constructor(errorCode: PrismErrorCode, customMessage?: string) {
    const message = customMessage || ERROR_MESSAGES[errorCode];
    const status = ERROR_STATUS[errorCode];
    super(message, status);
    this.code = errorCode;
  }

  static notFound(resource: string): BusinessException {
    return new BusinessException(
      PrismErrorCode.NOT_FOUND,
      `${resource} not found`,
    );
  }

  static invalidStateTransition(from: string, to: string): BusinessException {
    return new BusinessException(
      PrismErrorCode.INVALID_STATE_TRANSITION,
      `Invalid state transition from ${from} to ${to}`,
    );
  }

  static agentNotAssigned(): BusinessException {
    return new BusinessException(PrismErrorCode.AGENT_NOT_ASSIGNED);
  }

  static providerConnectionFailed(provider: string): BusinessException {
    return new BusinessException(
      PrismErrorCode.PROVIDER_CONNECTION_FAILED,
      `Failed to connect to ${provider}`,
    );
  }

  static aiExecutionFailed(reason: string): BusinessException {
    return new BusinessException(PrismErrorCode.AI_EXECUTION_FAILED, reason);
  }

  static duplicateResource(resource: string): BusinessException {
    return new BusinessException(
      PrismErrorCode.DUPLICATE_RESOURCE,
      `${resource} already exists`,
    );
  }
}
