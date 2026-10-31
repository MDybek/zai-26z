import React, { useEffect, useMemo, useState } from 'react'
import { Measurement, Series } from '../types'

interface MeasurementTableProps {
  seriesList: Series[]
  measurements: Measurement[]
  selectedSeriesIds: number[]
  selectedMeasurementId: number | string | null
  onSelectMeasurement: (id: number | string) => void
}

export const MeasurementTable: React.FC<MeasurementTableProps> = ({
  seriesList,
  measurements,
  selectedSeriesIds,
  selectedMeasurementId,
  onSelectMeasurement,
}) => {
  const [sortAsc, setSortAsc] = useState<boolean>(false)
  const [currentPage, setCurrentPage] = useState<number>(1)
  const pageSize = 15

  const seriesMap = useMemo(() => {
    return new Map<number, Series>(seriesList.map((s) => [s.id, s]))
  }, [seriesList])

  const filteredMeasurements = useMemo(() => {
    return measurements
      .filter((m) => selectedSeriesIds.includes(m.seriesId))
      .sort((a, b) => {
        const diff = new Date(a.timestamp).getTime() - new Date(b.timestamp).getTime()
        return sortAsc ? diff : -diff
      })
  }, [measurements, selectedSeriesIds, sortAsc])

  useEffect(() => {
    if (!selectedMeasurementId) return
    const idx = filteredMeasurements.findIndex((m) => m.id === selectedMeasurementId)
    if (idx !== -1) {
      setCurrentPage(Math.floor(idx / pageSize) + 1)
    }
  }, [selectedMeasurementId, filteredMeasurements])

  const totalPages = Math.ceil(filteredMeasurements.length / pageSize) || 1
  const paginatedRows = useMemo(() => {
    const start = (currentPage - 1) * pageSize
    return filteredMeasurements.slice(start, start + pageSize)
  }, [filteredMeasurements, currentPage])

  return (
    <section className="table-card" aria-labelledby="table-heading">
      <div className="table-header">
        <div>
          <h2 id="table-heading">Tabela Pomiarów</h2>
          <p className="table-count">
            Łącznie: <strong>{filteredMeasurements.length}</strong> pomiarów (strona {currentPage} z {totalPages})
          </p>
        </div>

        <div className="table-actions no-print">
          <button
            type="button"
            className="btn btn-sm btn-secondary"
            onClick={() => setSortAsc(!sortAsc)}
          >
            Sortuj czas: {sortAsc ? 'Rosnąco ↑' : 'Malejąco ↓'}
          </button>
        </div>
      </div>

      <div className="table-responsive">
        <table className="data-table">
          <thead>
            <tr>
              <th scope="col">ID</th>
              <th scope="col">Data i czas</th>
              <th scope="col">Seria pomiarowa</th>
              <th scope="col">Wartość</th>
              <th scope="col">Czujnik</th>
            </tr>
          </thead>
          <tbody>
            {paginatedRows.length === 0 ? (
              <tr>
                <td colSpan={5} className="table-empty">
                  Brak wyników do wyświetlenia
                </td>
              </tr>
            ) : (
              paginatedRows.map((m) => {
                const s = seriesMap.get(m.seriesId)
                const isSelected = selectedMeasurementId === m.id
                return (
                  <tr
                    key={m.id}
                    onClick={() => onSelectMeasurement(m.id)}
                    className={`clickable-row ${isSelected ? 'row-selected' : ''}`}
                    tabIndex={0}
                    role="button"
                    onKeyDown={(e) => {
                      if (e.key === 'Enter' || e.key === ' ') {
                        e.preventDefault()
                        onSelectMeasurement(m.id)
                      }
                    }}
                    aria-label={`Pomiar ${m.id}, seria ${s?.name || m.seriesId}, wartość ${m.value}`}
                  >
                    <td>
                      <span className="id-badge">#{m.id}</span>
                    </td>
                    <td>{new Date(m.timestamp).toLocaleString('pl-PL')}</td>
                    <td>
                      <span
                        className="series-tag"
                        style={{
                          backgroundColor: `${s?.color || '#3b82f6'}20`,
                          color: s?.color || '#3b82f6',
                          borderColor: s?.color || '#3b82f6',
                        }}
                      >
                        {s?.name || `Seria ${m.seriesId}`}
                      </span>
                    </td>
                    <td className="value-cell">
                      <strong>{m.value}</strong> {s?.unit || ''}
                    </td>
                    <td>{m.sensorId ? `Sensor #${m.sensorId}` : '—'}</td>
                  </tr>
                )
              })
            )}
          </tbody>
        </table>
      </div>

      {totalPages > 1 && (
        <div className="table-pagination no-print">
          <button
            type="button"
            className="btn btn-sm btn-secondary"
            disabled={currentPage === 1}
            onClick={() => setCurrentPage((p) => Math.max(p - 1, 1))}
          >
            Poprzednia
          </button>
          <span className="page-indicator">
            {currentPage} / {totalPages}
          </span>
          <button
            type="button"
            className="btn btn-sm btn-secondary"
            disabled={currentPage === totalPages}
            onClick={() => setCurrentPage((p) => Math.min(p + 1, totalPages))}
          >
            Następna
          </button>
        </div>
      )}
    </section>
  )
}
