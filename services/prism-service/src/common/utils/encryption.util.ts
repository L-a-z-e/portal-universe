import * as crypto from 'crypto';
import { ConfigService } from '@nestjs/config';
import { Injectable, Logger } from '@nestjs/common';
import {
  BusinessException,
  PrismErrorCode,
} from '../filters/business.exception';

const DEFAULT_KEY = 'default-32-byte-encryption-key!!';

@Injectable()
export class EncryptionUtil {
  private readonly logger = new Logger(EncryptionUtil.name);
  private readonly algorithm = 'aes-256-gcm';
  private readonly key: Buffer;

  constructor(private configService: ConfigService) {
    const encryptionKey = this.configService.get<string>('encryption.key');
    if (!encryptionKey || encryptionKey.length < 32) {
      throw new Error('Encryption key must be at least 32 characters');
    }
    if (encryptionKey === DEFAULT_KEY) {
      if (process.env.NODE_ENV === 'production') {
        throw new Error(
          'Default encryption key is not allowed in production. Set ENCRYPTION_KEY environment variable.',
        );
      }
      this.logger.warn(
        'Using default encryption key. Set ENCRYPTION_KEY for production use.',
      );
    }
    this.key = Buffer.from(encryptionKey.slice(0, 32), 'utf-8');
  }

  encrypt(text: string): string {
    try {
      const iv = crypto.randomBytes(16);
      const cipher = crypto.createCipheriv(this.algorithm, this.key, iv);

      let encrypted = cipher.update(text, 'utf8', 'hex');
      encrypted += cipher.final('hex');

      const authTag = cipher.getAuthTag();

      // Format: iv:authTag:encrypted
      return `${iv.toString('hex')}:${authTag.toString('hex')}:${encrypted}`;
    } catch {
      throw new BusinessException(
        PrismErrorCode.ENCRYPTION_FAILED,
        'Failed to encrypt data',
      );
    }
  }

  decrypt(encryptedText: string): string {
    try {
      const parts = encryptedText.split(':');
      if (parts.length !== 3) {
        throw new Error('Invalid encrypted format');
      }

      const [ivHex, authTagHex, encrypted] = parts;
      const iv = Buffer.from(ivHex, 'hex');
      const authTag = Buffer.from(authTagHex, 'hex');

      const decipher = crypto.createDecipheriv(this.algorithm, this.key, iv);
      decipher.setAuthTag(authTag);

      let decrypted = decipher.update(encrypted, 'hex', 'utf8');
      decrypted += decipher.final('utf8');

      return decrypted;
    } catch {
      throw new BusinessException(
        PrismErrorCode.ENCRYPTION_FAILED,
        'Failed to decrypt data',
      );
    }
  }

  /**
   * Mask API key for display (e.g., sk-...xxxx)
   */
  maskApiKey(apiKey: string): string {
    if (apiKey.length <= 8) {
      return '****';
    }
    const prefix = apiKey.slice(0, 3);
    const suffix = apiKey.slice(-4);
    return `${prefix}...${suffix}`;
  }
}
