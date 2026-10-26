import React from 'react'
import { Series } from '../types'

interface FilterBarProps {
  seriesList: Series[]
  selectedSeriesIds: number[]
  onToggleSeries: (id: number) => void
  onSelectAllSeries: () => void
  fromDate: string
  toDate: string
  onFromDateChange: (val: string) => void
  onToDateChange: (val: string) => void
  onSetPreset: (preset: '24h' | '7d' | 'all') => void
  onRefresh: () => void
  isLoading: boolean
}

export const FilterBar: React.FC<FilterBarProps> = ({
  seriesList,
  selectedSeriesIds,
  onToggleSeries,
  onSelectAllSeries,
  fromDate,
  toDate,
  onSetPreset,
  onFromDateChange,
  onToDateChange,
  onRefresh,
  isLoading,
}) => {
  return (
    <section className="filter-card no-print" aria-labelledby="filter-heading">
      <div className="filter-header">
        <h2 id="filter-heading">Filtrowanie Danych</h2>
        <button
          type="button"
          onClick={onRefresh}
          disabled={isLoading}
          className="btn btn-secondary btn-icon"
          title="Odśwież dane"
          aria-label="Odśwież dane"
        >
          <svg
            className={isLoading ? 'spin' : ''}
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            strokeWidth="2"
            aria-hidden="true"
          >
            <path d="M21.5 2v6h-6M21.34 15.57a10 10 0 1 1-.57-8.38l5.67-5.67" />
          </svg>
          <span>{isLoading ? 'Ładowanie...' : 'Odśwież'}</span>
        </button>
      </div>

      <div className="filter-grid">
        <div className="filter-group">
          <span className="filter-label" id="series-group-label">Serie Pomiarowe:</span>
          <div className="series-pills" role="group" aria-labelledby="series-group-label">
            <button
              type="button"
              className={`pill-btn ${selectedSeriesIds.length === seriesList.length ? 'active' : ''}`}
              onClick={onSelectAllSeries}
            >
              Wszystkie
            </button>
            {seriesList.map((s, idx) => {
              const active = selectedSeriesIds.includes(s.id)
              const shapes = ['●', '■', '▲', '◆']
              const shape = shapes[idx % shapes.length]
              return (
                <button
                  key={s.id}
                  type="button"
                  onClick={() => onToggleSeries(s.id)}
                  className={`pill-btn ${active ? 'active' : ''}`}
                  style={{
                    borderColor: active ? s.color : '#cbd5e1',
                    backgroundColor: active ? `${s.color}20` : 'transparent',
                    color: '#0f172a',
                  }}
                  aria-pressed={active}
                >
                  <span className="series-shape" style={{ color: s.color }} aria-hidden="true">{shape}</span>
                  <span>{s.name}</span>
                  {s.unit && <small className="unit-tag">({s.unit})</small>}
                </button>
              )
            })}
          </div>
        </div>

        <div className="filter-group">
          <span className="filter-label" id="time-group-label">Przedział Czasu:</span>
          <div className="preset-buttons" role="group" aria-labelledby="time-group-label">
            <button
              type="button"
              className="btn btn-sm btn-ghost"
              onClick={() => onSetPreset('24h')}
            >
              Ostatnie 24h
            </button>
            <button
              type="button"
              className="btn btn-sm btn-ghost"
              onClick={() => onSetPreset('7d')}
            >
              Ostatnie 7 dni
            </button>
            <button
              type="button"
              className="btn btn-sm btn-ghost"
              onClick={() => onSetPreset('all')}
            >
              Pełny zakres
            </button>
          </div>

          <div className="date-inputs">
            <div className="input-field">
              <label htmlFor="filter-from">Od:</label>
              <input
                id="filter-from"
                type="datetime-local"
                value={fromDate}
                onChange={(e) => onFromDateChange(e.target.value)}
              />
            </div>
            <div className="input-field">
              <label htmlFor="filter-to">Do:</label>
              <input
                id="filter-to"
                type="datetime-local"
                value={toDate}
                onChange={(e) => onToDateChange(e.target.value)}
              />
            </div>
          </div>
        </div>
      </div>
    </section>
  )
}
