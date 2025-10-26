import { defineStore } from 'pinia';

/**
 * Pinia 스토어: 다크/라이트 모드 테마 상태를 전역적으로 관리합니다.
 */
export const useThemeStore = defineStore('theme', {
  /**
   * 스토어의 상태(State)를 정의합니다.
   * @returns {{isDark: boolean}} - isDark: 현재 다크 모드 활성화 여부
   */
  state: () => ({
    isDark: false,
  }),

  /**
   * 스토어의 액션(Actions)을 정의합니다.
   */
  actions: {
    /**
     * 현재 테마를 토글(전환)합니다.
     * 변경된 상태는 <html> 태그의 클래스와 localStorage에 즉시 반영됩니다.
     */
    toggle() {
      this.isDark = !this.isDark;
      if (this.isDark) {
        document.documentElement.classList.add('dark');
        localStorage.setItem('theme', 'dark');
      } else {
        document.documentElement.classList.remove('dark');
        localStorage.setItem('theme', 'light');
      }
    },

    /**
     * 애플리케이션 초기화 시, localStorage에 저장된 테마 설정을 불러와 적용합니다.
     * 이를 통해 사용자가 설정한 테마가 페이지 새로고침 후에도 유지됩니다.
     */
    initialize() {
      const saved = localStorage.getItem('theme');
      if (saved === 'dark') {
        this.isDark = true;
        document.documentElement.classList.add('dark');
      } else {
        this.isDark = false;
        document.documentElement.classList.remove('dark');
      }
    }
  }
});
