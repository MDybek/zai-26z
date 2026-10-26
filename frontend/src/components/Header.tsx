import React from 'react'

interface HeaderProps {
  isAdmin: boolean
  onOpenLogin: () => void
  onOpenChangePassword: () => void
  onLogout: () => void
}

export const Header: React.FC<HeaderProps> = ({
  isAdmin,
  onOpenLogin,
  onOpenChangePassword,
  onLogout,
}) => {
  return (
    <header className="app-header">
      <div className="header-brand">
        <svg
          className="brand-icon"
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          strokeWidth="2"
          aria-hidden="true"
        >
          <path d="M12 3v18M3 12h18M5.3 5.3l13.4 13.4M5.3 18.7L18.7 5.3" />
        </svg>
        <div>
          <h1>ZAI 26Z</h1>
          <p className="header-subtitle">Monitoring Jakości Powietrza</p>
        </div>
      </div>

      <nav className="header-actions" aria-label="Nawigacja nagłówka">
        <button
          type="button"
          onClick={() => window.print()}
          className="btn btn-secondary no-print"
          title="Drukuj wykres i tabelę"
          aria-label="Wydrukuj raport"
        >
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" aria-hidden="true">
            <polyline points="6 9 6 2 18 2 18 9" />
            <path d="M6 18H4a2 2 0 0 1-2-2v-5a2 2 0 0 1 2-2h16a2 2 0 0 1 2 2v5a2 2 0 0 1-2 2h-2" />
            <rect x="6" y="14" width="12" height="8" />
          </svg>
          <span>Drukuj</span>
        </button>

        {isAdmin ? (
          <div className="admin-menu no-print">
            <span className="badge badge-admin">Administrator</span>
            <button
              type="button"
              onClick={onOpenChangePassword}
              className="btn btn-secondary"
            >
              Zmień hasło
            </button>
            <button
              type="button"
              onClick={onLogout}
              className="btn btn-danger"
            >
              Wyloguj
            </button>
          </div>
        ) : (
          <button
            type="button"
            onClick={onOpenLogin}
            className="btn btn-primary no-print"
          >
            Panel Administratora
          </button>
        )}
      </nav>
    </header>
  )
}
