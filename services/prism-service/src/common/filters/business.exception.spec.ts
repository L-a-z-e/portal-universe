import { BusinessException, PrismErrorCode } from './business.exception';
import { HttpStatus } from '@nestjs/common';

describe('BusinessException', () => {
  describe('PrismErrorCode enum values', () => {
    it('should have correct code values', () => {
      expect(PrismErrorCode.NOT_FOUND).toBe('P001');
      expect(PrismErrorCode.INVALID_STATE_TRANSITION).toBe('P002');
      expect(PrismErrorCode.AGENT_NOT_ASSIGNED).toBe('P003');
      expect(PrismErrorCode.PROVIDER_CONNECTION_FAILED).toBe('P004');
      expect(PrismErrorCode.AI_EXECUTION_FAILED).toBe('P005');
      expect(PrismErrorCode.INVALID_REQUEST).toBe('P006');
      expect(PrismErrorCode.DUPLICATE_RESOURCE).toBe('P007');
      expect(PrismErrorCode.ENCRYPTION_FAILED).toBe('P008');
      expect(PrismErrorCode.RATE_LIMIT_EXCEEDED).toBe('P009');
    });
  });

  describe('constructor', () => {
    it('should create with default message for NOT_FOUND', () => {
      const ex = new BusinessException(PrismErrorCode.NOT_FOUND);
      expect(ex.code).toBe('P001');
      expect(ex.message).toBe('Resource not found');
      expect(ex.getStatus()).toBe(HttpStatus.NOT_FOUND);
    });

    it('should create with custom message', () => {
      const ex = new BusinessException(
        PrismErrorCode.NOT_FOUND,
        'Provider not found',
      );
      expect(ex.code).toBe('P001');
      expect(ex.message).toBe('Provider not found');
      expect(ex.getStatus()).toBe(HttpStatus.NOT_FOUND);
    });

    it('should set correct status for each error code', () => {
      expect(new BusinessException(PrismErrorCode.NOT_FOUND).getStatus()).toBe(
        HttpStatus.NOT_FOUND,
      );
      expect(
        new BusinessException(
          PrismErrorCode.INVALID_STATE_TRANSITION,
        ).getStatus(),
      ).toBe(HttpStatus.BAD_REQUEST);
      expect(
        new BusinessException(
          PrismErrorCode.PROVIDER_CONNECTION_FAILED,
        ).getStatus(),
      ).toBe(HttpStatus.SERVICE_UNAVAILABLE);
      expect(
        new BusinessException(PrismErrorCode.DUPLICATE_RESOURCE).getStatus(),
      ).toBe(HttpStatus.CONFLICT);
      expect(
        new BusinessException(PrismErrorCode.RATE_LIMIT_EXCEEDED).getStatus(),
      ).toBe(HttpStatus.TOO_MANY_REQUESTS);
    });
  });

  describe('static factory methods', () => {
    it('notFound should create with resource name', () => {
      const ex = BusinessException.notFound('Task');
      expect(ex.code).toBe('P001');
      expect(ex.message).toBe('Task not found');
      expect(ex.getStatus()).toBe(HttpStatus.NOT_FOUND);
    });

    it('invalidStateTransition should include from and to', () => {
      const ex = BusinessException.invalidStateTransition('TODO', 'approve');
      expect(ex.code).toBe('P002');
      expect(ex.message).toContain('TODO');
      expect(ex.message).toContain('approve');
    });

    it('agentNotAssigned should use default message', () => {
      const ex = BusinessException.agentNotAssigned();
      expect(ex.code).toBe('P003');
      expect(ex.message).toBe('Agent not assigned to task');
    });

    it('providerConnectionFailed should include provider name', () => {
      const ex = BusinessException.providerConnectionFailed('OpenAI');
      expect(ex.code).toBe('P004');
      expect(ex.message).toBe('Failed to connect to OpenAI');
    });

    it('aiExecutionFailed should use reason as message', () => {
      const ex = BusinessException.aiExecutionFailed('Rate limited');
      expect(ex.code).toBe('P005');
      expect(ex.message).toBe('Rate limited');
    });

    it('duplicateResource should include resource name', () => {
      const ex = BusinessException.duplicateResource('Provider');
      expect(ex.code).toBe('P007');
      expect(ex.message).toBe('Provider already exists');
    });
  });
});
