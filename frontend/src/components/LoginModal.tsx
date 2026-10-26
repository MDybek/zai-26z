import React, { useEffect, useRef, useState } from 'react'

interface LoginModalProps {
  isOpen: boolean
  onClose: () => void
  onLogin: (user: string, pass: string) => Promise<void>
}

export const LoginModal: React.FC<LoginModalProps> = ({ isOpen, onClose, onLogin }) => {
  const [username, setUsername] = useState('admin')
  const [password, setPassword] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [isSubmitting, setIsSubmitting] = useState(false)
  const inputRef = useRef<HTMLInputElement>(null)

  useEffect(() => {
    if (isOpen) {
      setError(null)
      setPassword('')
      setTimeout(() => inputRef.current?.focus(), 50)
    }
  }, [isOpen])

  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === 'Escape' && isOpen) {
        onClose()
      }
    }
    window.addEventListener('keydown', handleKeyDown)
    return () => window.removeEventListener('keydown', handleKeyDown)
  }, [isOpen, onClose])

  if (!isOpen) return null

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setError(null)

    if (!username.trim() || !password) {
      setError('Wprowadź login i hasło.')
      return
    }

    setIsSubmitting(true)
    try {
      await onLogin(username.trim(), password)
      onClose()
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : 'Błąd logowania. Sprawdź poświadczenia.')
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <div className="modal-backdrop" onClick={onClose} role="presentation">
      <div
        className="modal-card"
        onClick={(e) => e.stopPropagation()}
        role="dialog"
        aria-modal="true"
        aria-labelledby="login-title"
      >
        <div className="modal-header">
          <h2 id="login-title">Logowanie Administratora</h2>
          <button
            type="button"
            className="btn-close"
            onClick={onClose}
            aria-label="Zamknij"
          >
            ×
          </button>
        </div>

        <form onSubmit={handleSubmit} className="modal-form">
          {error && <div className="alert alert-error" role="alert">{error}</div>}

          <div className="form-group">
            <label htmlFor="login-username">Nazwa użytkownika:</label>
            <input
              ref={inputRef}
              id="login-username"
              type="text"
              required
              value={username}
              onChange={(e) => setUsername(e.target.value)}
              placeholder="np. admin"
            />
          </div>

          <div className="form-group">
            <label htmlFor="login-password">Hasło:</label>
            <input
              id="login-password"
              type="password"
              required
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              placeholder="Wprowadź hasło"
            />
          </div>

          <div className="modal-actions">
            <button
              type="button"
              className="btn btn-secondary"
              onClick={onClose}
              disabled={isSubmitting}
            >
              Anuluj
            </button>
            <button
              type="submit"
              className="btn btn-primary"
              disabled={isSubmitting}
            >
              {isSubmitting ? 'Logowanie...' : 'Zaloguj się'}
            </button>
          </div>
        </form>
      </div>
    </div>
  )
}
