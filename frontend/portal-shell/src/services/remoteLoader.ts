import type { RemoteConfig } from '../config/remoteRegistry';

/**
 * Remote 앱 로딩 결과를 나타내는 타입입니다.
 */
export type RemoteLoadResult = {
  success: boolean;            // 로딩 성공 여부
  mountFn: Function | null;    // 로딩된 마운트 함수
  error: Error | null;         // 로딩 중 발생한 에러
  config: RemoteConfig;        // 로드를 시도한 Remote의 설정 정보
};

/**
 * Module Federation을 사용하여 Remote 앱을 동적으로 로드하고 관리하는 클래스입니다.
 * 로드된 모듈과 마운트 함수를 캐싱하여 성능을 최적화합니다.
 */
export class RemoteLoader {
  // 로드된 마운트 함수를 캐싱하는 맵 (key: remote key)
  private mountFnCache = new Map<string, Function>();
  // 로드된 remoteEntry.js 모듈을 캐싱하는 맵 (key: remoteEntry.js URL)
  private remoteEntryCache = new Map<string, any>();

  /**
   * 원격 URL로부터 remoteEntry.js 파일을 동적으로 로드합니다.
   * @param url remoteEntry.js의 전체 URL
   * @returns 로드된 remoteEntry 모듈
   */
  private async loadRemoteEntry(url: string): Promise<any> {
    if (this.remoteEntryCache.has(url)) {
      console.log(`✅ [RemoteLoader] Using cached remoteEntry: ${url}`);
      return this.remoteEntryCache.get(url);
    }

    console.log(`🔍 [RemoteLoader] Loading remoteEntry: ${url}`);
    try {
      // Vite는 동적 import 경로를 정적으로 분석할 수 없으므로, /* @vite-ignore */ 주석을 사용하여 경고를 비활성화합니다.
      const remoteEntry = await import(/* @vite-ignore */ url);
      console.log(`✅ [RemoteLoader] remoteEntry loaded for ${url}`);
      this.remoteEntryCache.set(url, remoteEntry);
      return remoteEntry;
    } catch (error: any) {
      console.error(`❌ [RemoteLoader] Failed to load remoteEntry:`, error);
      throw new Error(`Failed to load remoteEntry from ${url}: ${error.message}`);
    }
  }

  /**
   * 주어진 설정(config)에 따라 Remote 앱을 로드하고 마운트 함수를 반환합니다.
   * @param config 로드할 Remote의 설정 정보
   * @returns {Promise<RemoteLoadResult>} 로딩 결과
   */
  async loadRemote(config: RemoteConfig): Promise<RemoteLoadResult> {
    console.group(`🔍 [RemoteLoader] Loading ${config.name}`);

    // 1. 마운트 함수 캐시 확인
    if (this.mountFnCache.has(config.key)) {
      console.log(`✅ [RemoteLoader] Using cached mount function for ${config.name}`);
      console.groupEnd();
      return {
        success: true,
        mountFn: this.mountFnCache.get(config.key)!,
        error: null,
        config
      };
    }

    try {
      // 2. remoteEntry.js 로드
      const remoteEntry = await this.loadRemoteEntry(config.url);

      // 3. remoteEntry에서 노출된 모듈(e.g., './bootstrap') 가져오기
      if (typeof remoteEntry.get !== 'function') {
        throw new Error('Invalid remoteEntry format: `get` function not found.');
      }
      const moduleFactory = await remoteEntry.get(config.module.replace(`${config.key}/`, './'));

      // 4. 모듈 실행하여 실제 모듈 내용 얻기
      const module = await moduleFactory();

      // 5. 모듈에서 마운트 함수 찾기
      const mountFn = module[config.mountFn];
      if (!mountFn) {
        throw new Error(`Mount function '${config.mountFn}' not found in module.`);
      }

      // 6. 마운트 함수 캐싱
      this.mountFnCache.set(config.key, mountFn);
      console.log(`✅ [RemoteLoader] ${config.name} loaded and cached successfully`);
      console.groupEnd();

      return { success: true, mountFn, error: null, config };

    } catch (error: any) {
      console.error(`❌ [RemoteLoader] Failed to load ${config.name}:`, error);
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
   * 캐시를 초기화합니다. (강제 재로딩 필요 시 사용)
   * @param key 특정 Remote의 캐시만 지우고 싶을 때 사용하는 키
   */
  clearCache(key?: string) {
    if (key) {
      this.mountFnCache.delete(key);
      // URL을 역으로 찾아 remoteEntryCache도 지울 수 있지만, 복잡성을 위해 생략
      console.log(`🗑️ [RemoteLoader] Mount function cache cleared for ${key}`);
    } else {
      this.mountFnCache.clear();
      this.remoteEntryCache.clear();
      console.log(`🗑️ [RemoteLoader] All remote caches cleared`);
    }
  }
}

// 싱글톤 인스턴스로 내보내어 애플리케이션 전체에서 동일한 로더와 캐시를 사용하도록 합니다.
export const remoteLoader = new RemoteLoader();
