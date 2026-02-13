import { EncryptionUtil } from './encryption.util';
import { ConfigService } from '@nestjs/config';
import { BusinessException } from '../filters/business.exception';

describe('EncryptionUtil', () => {
  const VALID_KEY = 'test-32-byte-encryption-key-ok!!'; // exactly 32 chars

  function createUtil(key: string): EncryptionUtil {
    const configService = {
      get: jest.fn().mockReturnValue(key),
    } as unknown as ConfigService;
    return new EncryptionUtil(configService);
  }

  describe('constructor', () => {
    it('should throw if key is shorter than 32 characters', () => {
      expect(() => createUtil('short-key')).toThrow(
        'Encryption key must be at least 32 characters',
      );
    });

    it('should throw if key is empty', () => {
      const configService = {
        get: jest.fn().mockReturnValue(undefined),
      } as unknown as ConfigService;
      expect(() => new EncryptionUtil(configService)).toThrow(
        'Encryption key must be at least 32 characters',
      );
    });

    it('should warn when using default key in non-production', () => {
      const originalEnv = process.env.NODE_ENV;
      process.env.NODE_ENV = 'test';
      const configService = {
        get: jest.fn().mockReturnValue('default-32-byte-encryption-key!!'),
      } as unknown as ConfigService;
      // Should not throw, just warn
      expect(() => new EncryptionUtil(configService)).not.toThrow();
      process.env.NODE_ENV = originalEnv;
    });

    it('should throw when using default key in production', () => {
      const originalEnv = process.env.NODE_ENV;
      process.env.NODE_ENV = 'production';
      const configService = {
        get: jest.fn().mockReturnValue('default-32-byte-encryption-key!!'),
      } as unknown as ConfigService;
      expect(() => new EncryptionUtil(configService)).toThrow(
        'Default encryption key is not allowed in production',
      );
      process.env.NODE_ENV = originalEnv;
    });
  });

  describe('encrypt/decrypt', () => {
    let util: EncryptionUtil;

    beforeEach(() => {
      util = createUtil(VALID_KEY);
    });

    it('should encrypt and decrypt round-trip correctly', () => {
      const text = 'sk-my-secret-api-key';
      const encrypted = util.encrypt(text);
      const decrypted = util.decrypt(encrypted);
      expect(decrypted).toBe(text);
    });

    it('should produce different ciphertext for the same input (random IV)', () => {
      const text = 'same-input-text';
      const encrypted1 = util.encrypt(text);
      const encrypted2 = util.encrypt(text);
      expect(encrypted1).not.toBe(encrypted2);
    });

    it('should produce colon-separated format: iv:authTag:encrypted', () => {
      const encrypted = util.encrypt('test');
      const parts = encrypted.split(':');
      expect(parts).toHaveLength(3);
      // IV is 16 bytes = 32 hex chars
      expect(parts[0]).toHaveLength(32);
      // AuthTag is 16 bytes = 32 hex chars
      expect(parts[1]).toHaveLength(32);
      // Encrypted part should be non-empty hex
      expect(parts[2].length).toBeGreaterThan(0);
    });

    it('should encrypt and decrypt empty string', () => {
      const encrypted = util.encrypt('');
      const decrypted = util.decrypt(encrypted);
      expect(decrypted).toBe('');
    });

    it('should encrypt and decrypt unicode text', () => {
      const text = 'Hello, World!';
      const encrypted = util.encrypt(text);
      const decrypted = util.decrypt(encrypted);
      expect(decrypted).toBe(text);
    });

    it('should encrypt and decrypt long text', () => {
      const text = 'a'.repeat(10000);
      const encrypted = util.encrypt(text);
      const decrypted = util.decrypt(encrypted);
      expect(decrypted).toBe(text);
    });

    it('should throw BusinessException on invalid format during decrypt', () => {
      expect(() => util.decrypt('invalid-format')).toThrow(BusinessException);
    });

    it('should throw BusinessException on tampered data', () => {
      const encrypted = util.encrypt('test');
      const parts = encrypted.split(':');
      // tamper with the encrypted part
      parts[2] = 'ff' + parts[2].slice(2);
      const tampered = parts.join(':');
      expect(() => util.decrypt(tampered)).toThrow(BusinessException);
    });
  });

  describe('maskApiKey', () => {
    let util: EncryptionUtil;

    beforeEach(() => {
      util = createUtil(VALID_KEY);
    });

    it('should mask a normal API key showing prefix and suffix', () => {
      const masked = util.maskApiKey('sk-1234567890abcdef');
      expect(masked).toBe('sk-...cdef');
    });

    it('should return **** for short keys (<=8)', () => {
      expect(util.maskApiKey('short')).toBe('****');
      expect(util.maskApiKey('12345678')).toBe('****');
    });
  });
});
