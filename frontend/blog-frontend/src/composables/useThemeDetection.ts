import { ref, onMounted, onBeforeUnmount } from 'vue'

/**
 * data-theme 속성 변경을 감지하여 다크모드 상태를 관리하는 composable.
 * MutationObserver를 사용하여 실시간 테마 전환을 감지합니다.
 */
export function useThemeDetection() {
  const isDarkMode = ref(false)
  let observer: MutationObserver | null = null

  function detectTheme() {
    const theme = document.documentElement.getAttribute('data-theme')
    isDarkMode.value = theme === 'dark'
  }

  function startObserving() {
    detectTheme()

    observer = new MutationObserver((mutations) => {
      mutations.forEach((mutation) => {
        if (mutation.type === 'attributes' && mutation.attributeName === 'data-theme') {
          detectTheme()
        }
      })
    })

    observer.observe(document.documentElement, {
      attributes: true,
      attributeFilter: ['data-theme']
    })
  }

  function stopObserving() {
    observer?.disconnect()
    observer = null
  }

  onMounted(() => {
    startObserving()
  })

  onBeforeUnmount(() => {
    stopObserving()
  })

  return { isDarkMode, detectTheme }
}
