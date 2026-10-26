import React, { useState } from 'react'
import { Sensor, SensorCreatedResponse, Series, SeriesInput } from '../types'

interface AdminPanelProps {
  seriesList: Series[]
  sensorsList: Sensor[]
  onCreateSeries: (data: SeriesInput) => Promise<void>
  onUpdateSeries: (id: number, data: SeriesInput) => Promise<void>
  onDeleteSeries: (id: number) => Promise<void>
  onCreateSensor: (name: string, seriesId: number) => Promise<SensorCreatedResponse | null>
  onDeleteSensor: (id: number) => Promise<void>
}

export const AdminPanel: React.FC<AdminPanelProps> = ({
  seriesList,
  sensorsList,
  onCreateSeries,
  onUpdateSeries,
  onDeleteSeries,
  onCreateSensor,
  onDeleteSensor,
}) => {
  const [activeTab, setActiveTab] = useState<'series' | 'sensors'>('series')
  const [editingSeries, setEditingSeries] = useState<Series | null>(null)

  const [seriesName, setSeriesName] = useState('')
  const [seriesMin, setSeriesMin] = useState('0')
  const [seriesMax, setSeriesMax] = useState('100')
  const [seriesColor, setSeriesColor] = useState('#2563eb')
  const [seriesUnit, setSeriesUnit] = useState('µg/m³')
  const [seriesError, setSeriesError] = useState<string | null>(null)
  const [isSubmittingSeries, setIsSubmittingSeries] = useState(false)

  const [sensorName, setSensorName] = useState('')
  const [sensorSeriesId, setSensorSeriesId] = useState<number>(seriesList[0]?.id || 1)
  const [createdApiKey, setCreatedApiKey] = useState<string | null>(null)
  const [sensorError, setSensorError] = useState<string | null>(null)
  const [isSubmittingSensor, setIsSubmittingSensor] = useState(false)
  const [copiedKey, setCopiedKey] = useState(false)

  const handleCreateOrUpdateSeries = async (e: React.FormEvent) => {
    e.preventDefault()
    setSeriesError(null)

    const min = parseFloat(seriesMin)
    const max = parseFloat(seriesMax)

    if (!seriesName.trim()) {
      setSeriesError('Nazwa serii jest wymagana.')
      return
    }
    if (isNaN(min) || isNaN(max)) {
      setSeriesError('Wartości minimalna i maksymalna muszą być liczbami.')
      return
    }
    if (min >= max) {
      setSeriesError('Wartość minimalna musi być mniejsza od maksymalnej.')
      return
    }

    setIsSubmittingSeries(true)
    try {
      const payload: SeriesInput = {
        name: seriesName.trim(),
        minValue: min,
        maxValue: max,
        color: seriesColor,
        unit: seriesUnit.trim() || undefined,
      }

      if (editingSeries) {
        await onUpdateSeries(editingSeries.id, payload)
        setEditingSeries(null)
      } else {
        await onCreateSeries(payload)
      }

      setSeriesName('')
      setSeriesMin('0')
      setSeriesMax('100')
      setSeriesColor('#2563eb')
      setSeriesUnit('µg/m³')
    } catch (err: unknown) {
      setSeriesError(err instanceof Error ? err.message : 'Wystąpił błąd podczas zapisu serii.')
    } finally {
      setIsSubmittingSeries(false)
    }
  }

  const handleStartEdit = (s: Series) => {
    setEditingSeries(s)
    setSeriesName(s.name)
    setSeriesMin(String(s.minValue))
    setSeriesMax(String(s.maxValue))
    setSeriesColor(s.color)
    setSeriesUnit(s.unit || '')
    setSeriesError(null)
  }

  const handleCancelEdit = () => {
    setEditingSeries(null)
    setSeriesName('')
    setSeriesMin('0')
    setSeriesMax('100')
    setSeriesColor('#2563eb')
    setSeriesUnit('µg/m³')
    setSeriesError(null)
  }

  const handleCreateSensor = async (e: React.FormEvent) => {
    e.preventDefault()
    setSensorError(null)

    if (!sensorName.trim()) {
      setSensorError('Nazwa czujnika jest wymagana.')
      return
    }

    setIsSubmittingSensor(true)
    try {
      const res = await onCreateSensor(sensorName.trim(), Number(sensorSeriesId))
      if (res?.apiKey) {
        setCreatedApiKey(res.apiKey)
        setSensorName('')
      }
    } catch (err: unknown) {
      setSensorError(err instanceof Error ? err.message : 'Nie udało się zarejestrować czujnika.')
    } finally {
      setIsSubmittingSensor(false)
    }
  }

  const handleCopyKey = () => {
    if (createdApiKey) {
      navigator.clipboard.writeText(createdApiKey)
      setCopiedKey(true)
      setTimeout(() => setCopiedKey(false), 3000)
    }
  }

  return (
    <section className="admin-panel no-print" aria-labelledby="admin-heading">
      <div className="admin-header">
        <h2 id="admin-heading">Panel Zarządzania (Administrator)</h2>
        <div className="tab-buttons" role="tablist">
          <button
            type="button"
            role="tab"
            aria-selected={activeTab === 'series'}
            className={`tab-btn ${activeTab === 'series' ? 'active' : ''}`}
            onClick={() => setActiveTab('series')}
          >
            Serie Pomiarowe ({seriesList.length})
          </button>
          <button
            type="button"
            role="tab"
            aria-selected={activeTab === 'sensors'}
            className={`tab-btn ${activeTab === 'sensors' ? 'active' : ''}`}
            onClick={() => setActiveTab('sensors')}
          >
            Czujniki IoT ({sensorsList.length})
          </button>
        </div>
      </div>

      {activeTab === 'series' && (
        <div className="tab-content">
          <div className="admin-grid">
            <form onSubmit={handleCreateOrUpdateSeries} className="form-card">
              <h3>{editingSeries ? `Edycja Serii: ${editingSeries.name}` : 'Nowa Seria Pomiarowa'}</h3>

              {seriesError && <div className="alert alert-error" role="alert">{seriesError}</div>}

              <div className="form-group">
                <label htmlFor="series-name">Nazwa serii:</label>
                <input
                  id="series-name"
                  type="text"
                  required
                  value={seriesName}
                  onChange={(e) => setSeriesName(e.target.value)}
                  placeholder="np. PM2.5, Wilgotność"
                />
              </div>

              <div className="form-row">
                <div className="form-group">
                  <label htmlFor="series-min">Wartość min:</label>
                  <input
                    id="series-min"
                    type="number"
                    step="any"
                    required
                    value={seriesMin}
                    onChange={(e) => setSeriesMin(e.target.value)}
                  />
                </div>
                <div className="form-group">
                  <label htmlFor="series-max">Wartość max:</label>
                  <input
                    id="series-max"
                    type="number"
                    step="any"
                    required
                    value={seriesMax}
                    onChange={(e) => setSeriesMax(e.target.value)}
                  />
                </div>
              </div>

              <div className="form-row">
                <div className="form-group">
                  <label htmlFor="series-color">Kolor wykresu:</label>
                  <div className="color-picker-row">
                    <input
                      id="series-color"
                      type="color"
                      value={seriesColor}
                      onChange={(e) => setSeriesColor(e.target.value)}
                    />
                    <code>{seriesColor}</code>
                  </div>
                </div>
                <div className="form-group">
                  <label htmlFor="series-unit">Jednostka:</label>
                  <input
                    id="series-unit"
                    type="text"
                    value={seriesUnit}
                    onChange={(e) => setSeriesUnit(e.target.value)}
                    placeholder="np. µg/m³, °C, %"
                  />
                </div>
              </div>

              <div className="form-actions">
                <button
                  type="submit"
                  disabled={isSubmittingSeries}
                  className="btn btn-primary"
                >
                  {isSubmittingSeries ? 'Zapisywanie...' : editingSeries ? 'Zapisz zmiany' : 'Dodaj serię'}
                </button>
                {editingSeries && (
                  <button
                    type="button"
                    onClick={handleCancelEdit}
                    className="btn btn-secondary"
                  >
                    Anuluj
                  </button>
                )}
              </div>
            </form>

            <div className="list-card">
              <h3>Istniejące Serie</h3>
              <div className="table-responsive">
                <table className="compact-table">
                  <thead>
                    <tr>
                      <th>Kolor</th>
                      <th>Nazwa</th>
                      <th>Przedział</th>
                      <th>Jednostka</th>
                      <th>Akcje</th>
                    </tr>
                  </thead>
                  <tbody>
                    {seriesList.map((s) => (
                      <tr key={s.id}>
                        <td>
                          <span
                            className="color-dot"
                            style={{ backgroundColor: s.color }}
                            title={s.color}
                          />
                        </td>
                        <td><strong>{s.name}</strong></td>
                        <td>[{s.minValue}, {s.maxValue}]</td>
                        <td>{s.unit || '—'}</td>
                        <td>
                          <div className="row-actions">
                            <button
                              type="button"
                              className="btn btn-xs btn-secondary"
                              onClick={() => handleStartEdit(s)}
                            >
                              Edytuj
                            </button>
                            <button
                              type="button"
                              className="btn btn-xs btn-danger"
                              onClick={() => {
                                if (confirm(`Czy na pewno usunąć serię "${s.name}" wraz ze wszystkimi wynikami?`)) {
                                  onDeleteSeries(s.id)
                                }
                              }}
                            >
                              Usuń
                            </button>
                          </div>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </div>
          </div>
        </div>
      )}

      {activeTab === 'sensors' && (
        <div className="tab-content">
          <div className="admin-grid">
            <form onSubmit={handleCreateSensor} className="form-card">
              <h3>Rejestracja Nowego Czujnika (F12)</h3>

              {sensorError && <div className="alert alert-error" role="alert">{sensorError}</div>}

              {createdApiKey && (
                <div className="alert alert-success api-key-box" role="alert">
                  <div className="api-key-header">
                    <strong>Nowo wygenerowany klucz API:</strong>
                    <button
                      type="button"
                      className="btn btn-xs btn-secondary"
                      onClick={handleCopyKey}
                    >
                      {copiedKey ? 'Skopiowano!' : 'Kopiuj'}
                    </button>
                  </div>
                  <code className="api-key-code">{createdApiKey}</code>
                  <p className="api-key-warn">
                    Uwaga: Ten klucz jest wyświetlany tylko raz. Zapisz go bezpiecznie lub przekaż do generatora.
                  </p>
                </div>
              )}

              <div className="form-group">
                <label htmlFor="sensor-name">Nazwa czujnika:</label>
                <input
                  id="sensor-name"
                  type="text"
                  required
                  value={sensorName}
                  onChange={(e) => setSensorName(e.target.value)}
                  placeholder="np. Czujnik Pokój 101"
                />
              </div>

              <div className="form-group">
                <label htmlFor="sensor-series">Przypisana seria:</label>
                <select
                  id="sensor-series"
                  value={sensorSeriesId}
                  onChange={(e) => setSensorSeriesId(Number(e.target.value))}
                >
                  {seriesList.map((s) => (
                    <option key={s.id} value={s.id}>
                      {s.name} ({s.unit || 'brak'})
                    </option>
                  ))}
                </select>
              </div>

              <button
                type="submit"
                disabled={isSubmittingSensor}
                className="btn btn-primary"
              >
                {isSubmittingSensor ? 'Rejestrowanie...' : 'Zarejestruj czujnik'}
              </button>
            </form>

            <div className="list-card">
              <h3>Zarejestrowane Czujniki</h3>
              <div className="table-responsive">
                <table className="compact-table">
                  <thead>
                    <tr>
                      <th>ID</th>
                      <th>Nazwa</th>
                      <th>Seria</th>
                      <th>Utworzono</th>
                      <th>Ostatnia aktywność</th>
                      <th>Akcje</th>
                    </tr>
                  </thead>
                  <tbody>
                    {sensorsList.length === 0 ? (
                      <tr>
                        <td colSpan={6} className="table-empty">
                          Brak zarejestrowanych czujników
                        </td>
                      </tr>
                    ) : (
                      sensorsList.map((sensor) => {
                        const s = seriesList.find((item) => item.id === sensor.seriesId)
                        return (
                          <tr key={sensor.id}>
                            <td>#{sensor.id}</td>
                            <td><strong>{sensor.name}</strong></td>
                            <td>{s?.name || `Seria ${sensor.seriesId}`}</td>
                            <td>{new Date(sensor.createdAt).toLocaleDateString('pl-PL')}</td>
                            <td>
                              {sensor.lastActivity
                                ? new Date(sensor.lastActivity).toLocaleString('pl-PL')
                                : 'Brak pomiarów'}
                            </td>
                            <td>
                              <button
                                type="button"
                                className="btn btn-xs btn-danger"
                                onClick={() => {
                                  if (confirm(`Wyrejestrować czujnik "${sensor.name}"? Jego klucz API natychmiast przestanie działać.`)) {
                                    onDeleteSensor(sensor.id)
                                  }
                                }}
                              >
                                Wyrejestruj
                              </button>
                            </td>
                          </tr>
                        )
                      })
                    )}
                  </tbody>
                </table>
              </div>
            </div>
          </div>
        </div>
      )}
    </section>
  )
}
