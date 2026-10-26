import React, { useEffect, useRef, useState } from 'react'

interface ChangePasswordModalProps {
  isOpen: boolean
  onClose: () => void
  onChangePassword: (curr: string, next: string) => Promise<void>
}

export const ChangePasswordModal: React.FC<ChangePasswordModalProps> = ({
  isOpen,
  onClose,
  onChangePassword,
}) => {
  const [currentPassword, setCurrentPassword] = useState('')
  const [newPassword, setNewPassword] = useState('')
  const [confirmPassword, setConfirmPassword] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [success, setSuccess] = useState(false)
  const [isSubmitting, setIsSubmitting] = useState(false)
  const inputRef = useRef<HTMLInputElement>(null)

  useEffect(() => {
    if (isOpen) {
      setCurrentPassword('')
      setNewPassword('')
      setConfirmPassword('')
      setError(null)
      setSuccess(false)
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

    if (!currentPassword) {
      setError('Podaj aktualne hasło.')
      return
    }
    if (newPassword.length < 8) {
      setError('Nowe hasło musi mieć co najmniej 8 znaków.')
      return
    }
    if (newPassword !== confirmPassword) {
      setError('Nowe hasła nie są identyczne.')
      return
    }

    setIsSubmitting(true)
    try {
      await onChangePassword(currentPassword, newPassword)
      setSuccess(true)
      setTimeout(() => {
        onClose()
      }, 1500)
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : 'Nie udało się zmienić hasła.')
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
        aria-labelledby="password-title"
      >
        <div className="modal-header">
          <h2 id="password-title">Zmiana Hasła Administratora</h2>
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
          {success && (
            <div className="alert alert-success" role="alert">
              Hasło zostało pomyślnie zmienione!
            </div>
          )}

          <div className="form-group">
            <label htmlFor="curr-pass">Aktualne hasło:</label>
            <input
              ref={inputRef}
              id="curr-pass"
              type="password"
              required
              value={currentPassword}
              onChange={(e) => setCurrentPassword(e.target.value)}
            />
          </div>

          <div className="form-group">
            <label htmlFor="new-pass">Nowe hasło (min. 8 znaków):</label>
            <input
              id="new-pass"
              type="password"
              required
              value={newPassword}
              onChange={(e) => setNewPassword(e.target.value)}
            />
          </div>

          <div className="form-group">
            <label htmlFor="confirm-pass">Powtórz nowe hasło:</label>
            <input
              id="confirm-pass"
              type="password"
              required
              value={confirmPassword}
              onChange={(e) => setConfirmPassword(e.target.value)}
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
              disabled={isSubmitting || success}
            >
              {isSubmitting ? 'Zmienianie...' : 'Zmień hasło'}
            </button>
          </div>
        </form>
      </div>
    </div>
  )
}
