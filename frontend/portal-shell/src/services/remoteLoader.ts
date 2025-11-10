// portal-shell/src/services/remoteLoader.ts

import type { RemoteConfig } from '../config/remoteRegistry';

export type RemoteLoadResult = {
  success: boolean;
  mountFn: Function | null;
  error: Error | null;
  config: RemoteConfig;
};

export class RemoteLoader {
  private cache = new Map<string, Function>();
  private remoteEntryCache = new Map<string, any>();

  /**
   * ✅ Step 1: remoteEntry.js 로드
   */
  private async loadRemoteEntry(url: string): Promise<any> {
    if (this.remoteEntryCache.has(url)) {
      console.log(`✅ [RemoteLoader] Using cached remoteEntry: ${url}`);
      return this.remoteEntryCache.get(url);
    }

    console.log(`🔍 [RemoteLoader] Loading remoteEntry: ${url}`);

    try {
      // ✅ remoteEntry.js를 동적으로 로드
      const remoteEntry = await import(/* @vite-ignore */ url);

      console.log(`✅ [RemoteLoader] remoteEntry loaded`);
      console.log(`   Exports:`, Object.keys(remoteEntry));

      this.remoteEntryCache.set(url, remoteEntry);
      return remoteEntry;
    } catch (error: any) {
      console.error(`❌ [RemoteLoader] Failed to load remoteEntry:`, error);
      throw new Error(`Failed to load remoteEntry from ${url}: ${error.message}`);
    }
  }

  /**
   * ✅ Remote 로드 (캐싱 + 에러 처리)
   */
  async loadRemote(config: RemoteConfig): Promise<RemoteLoadResult> {
    console.group(`🔍 [RemoteLoader] Loading ${config.name}`);
    console.log('Config:', config);

    // 캐시 확인
    if (this.cache.has(config.key)) {
      console.log(`✅ [RemoteLoader] Using cached ${config.name}`);
      console.groupEnd();
      return {
        success: true,
        mountFn: this.cache.get(config.key)!,
        error: null,
        config
      };
    }

    try {
      // ✅ Step 1: remoteEntry.js 로드
      const remoteEntry = await this.loadRemoteEntry(config.url);

      // ✅ Step 2: './bootstrap' 모듈 가져오기
      console.log(`🔍 [RemoteLoader] Getting module: ./bootstrap`);

      if (typeof remoteEntry.get !== 'function') {
        throw new Error('remoteEntry.get is not a function. Invalid remoteEntry format.');
      }

      const moduleFactory = await remoteEntry.get('./bootstrap');
      console.log(`✅ [RemoteLoader] Module factory obtained`);

      // ✅ Step 3: 모듈 실행하여 실제 모듈 얻기
      const module = await moduleFactory();
      console.log(`✅ [RemoteLoader] Module loaded:`, module);
      console.log(`   Available exports:`, Object.keys(module));

      // ✅ Step 4: mount 함수 찾기
      const mountFn = module[config.mountFn];

      if (!mountFn) {
        throw new Error(
          `Mount function '${config.mountFn}' not found in module. ` +
          `Available: ${Object.keys(module).join(', ')}`
        );
      }

      console.log(`✅ [RemoteLoader] Mount function found: ${typeof mountFn}`);

      this.cache.set(config.key, mountFn);
      console.log(`✅ [RemoteLoader] ${config.name} loaded successfully`);
      console.groupEnd();

      return { success: true, mountFn, error: null, config };

    } catch (error: any) {
      console.error(`❌ [RemoteLoader] Failed to load ${config.name}`);
      console.error('Error:', error);
      console.error('Stack:', error.stack);
      console.groupEnd();

      return {
        success: false,
        mountFn: null,
        error: error instanceof Error ? error : new Error(String(error)),
        config
      };
    }
  }

  /**
   * 캐시 클리어 (강제 재로딩용)
   */
  clearCache(key?: string) {
    if (key) {
      this.cache.delete(key);
      console.log(`🗑️ [RemoteLoader] Cache cleared for ${key}`);
    } else {
      this.cache.clear();
      this.remoteEntryCache.clear();
      console.log(`🗑️ [RemoteLoader] All cache cleared`);
    }
  }
}

// 싱글톤 인스턴스
export const remoteLoader = new RemoteLoader();