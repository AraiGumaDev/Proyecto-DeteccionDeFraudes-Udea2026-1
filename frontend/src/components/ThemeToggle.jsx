import { useTheme } from '../context/ThemeContext'

export default function ThemeToggle() {
  const { theme, setTheme } = useTheme()

  return (
    <div className="theme-toggle">
      <span className="theme-toggle-label">Apariencia</span>
      <div className="theme-toggle-track" role="radiogroup" aria-label="Modo de color">
        <button
          type="button"
          role="radio"
          aria-checked={theme === 'light'}
          className={`theme-toggle-option${theme === 'light' ? ' active' : ''}`}
          onClick={() => setTheme('light')}
        >
          <span className="theme-toggle-icon" aria-hidden>☀</span>
          Claro
        </button>
        <button
          type="button"
          role="radio"
          aria-checked={theme === 'dark'}
          className={`theme-toggle-option${theme === 'dark' ? ' active' : ''}`}
          onClick={() => setTheme('dark')}
        >
          <span className="theme-toggle-icon" aria-hidden>🌙</span>
          Oscuro
        </button>
      </div>
    </div>
  )
}
