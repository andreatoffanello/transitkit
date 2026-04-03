// web/composables/useTheme.ts
export function useTheme() {
  const isDark = useState<boolean>('isDark', () => false)

  function initTheme() {
    if (import.meta.client) {
      const stored = localStorage.getItem('theme')
      const prefersDark = window.matchMedia('(prefers-color-scheme: dark)').matches
      const dark = stored === 'dark' || (!stored && prefersDark)
      isDark.value = dark
      applyTheme(dark)
    }
  }

  function toggleTheme() {
    isDark.value = !isDark.value
    applyTheme(isDark.value)
    if (import.meta.client) {
      localStorage.setItem('theme', isDark.value ? 'dark' : 'light')
    }
  }

  function applyTheme(dark: boolean) {
    if (import.meta.client) {
      if (dark) {
        document.documentElement.classList.add('dark')
      } else {
        document.documentElement.classList.remove('dark')
      }
    }
  }

  return { isDark, initTheme, toggleTheme }
}
