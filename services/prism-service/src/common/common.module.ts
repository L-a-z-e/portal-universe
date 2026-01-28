import { Global, Module } from '@nestjs/common';
import { EncryptionUtil } from './utils/encryption.util';

@Global()
@Module({
  providers: [EncryptionUtil],
  exports: [EncryptionUtil],
})
export class CommonModule {}
