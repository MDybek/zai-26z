import { useCallback, useEffect, useState } from 'react'
import { api, getStoredToken } from './api'
import { AdminPanel } from './components/AdminPanel'
import { ChangePasswordModal } from './components/ChangePasswordModal'
import { Chart } from './components/Chart'
import { FilterBar } from './components/FilterBar'
import { Header } from './components/Header'
import { LoginModal } from './components/LoginModal'
import { MeasurementTable } from './components/MeasurementTable'
import { Measurement, Sensor, SensorCreatedResponse, Series, SeriesInput } from './types'
import './App.css'

export function App() {
  const [isAdmin, setIsAdmin] = useState<boolean>(() => Boolean(getStoredToken()))
  const [seriesList, setSeriesList] = useState<Series[]>([])
  const [selectedSeriesIds, setSelectedSeriesIds] = useState<number[]>([])
  const [measurements, setMeasurements] = useState<Measurement[]>([])
  const [sensorsList, setSensorsList] = useState<Sensor[]>([])
  const [selectedMeasurementId, setSelectedMeasurementId] = useState<number | null>(null)

  const [fromDate, setFromDate] = useState<string>('')
  const [toDate, setToDate] = useState<string>('')

  const [isLoading, setIsLoading] = useState<boolean>(false)
  const [globalError, setGlobalError] = useState<string | null>(null)

  const [isLoginModalOpen, setIsLoginModalOpen] = useState<boolean>(false)
  const [isChangePasswordOpen, setIsChangePasswordOpen] = useState<boolean>(false)

  const loadSeries = useCallback(async () => {
    try {
      const data = await api.getSeries()
      setSeriesList(data)
      setSelectedSeriesIds((prev) => (prev.length === 0 ? data.map((s) => s.id) : prev))
      return data
    } catch (err: unknown) {
      setGlobalError(err instanceof Error ? err.message : 'Nie udało się pobrać listy serii.')
      return []
    }
  }, [])

  const loadMeasurements = useCallback(async (seriesIds?: number[], from?: string, to?: string) => {
    setIsLoading(true)
    try {
      const fromIso = from ? new Date(from).toISOString() : undefined
      const toIso = to ? new Date(to).toISOString() : undefined

      const data = await api.getMeasurements({
        series: seriesIds && seriesIds.length > 0 ? seriesIds : undefined,
        from: fromIso,
        to: toIso,
        limit: 10000,
        sort: 'timestamp',
      })
      setMeasurements(data)
      setGlobalError(null)
    } catch (err: unknown) {
      setGlobalError(err instanceof Error ? err.message : 'Błąd podczas pobierania pomiarów.')
    } finally {
      setIsLoading(false)
    }
  }, [])

  const loadSensors = useCallback(async () => {
    if (!isAdmin) return
    try {
      const data = await api.getSensors()
      setSensorsList(data)
    } catch {
      setSensorsList([])
    }
  }, [isAdmin])

  useEffect(() => {
    loadSeries().then((loaded) => {
      loadMeasurements(loaded.map((s) => s.id), fromDate, toDate)
    })
  }, [loadSeries, loadMeasurements, fromDate, toDate])

  useEffect(() => {
    if (isAdmin) {
      loadSensors()
    }
  }, [isAdmin, loadSensors])

  const handleToggleSeries = (id: number) => {
    setSelectedSeriesIds((prev) => {
      const exists = prev.includes(id)
      if (exists) {
        if (prev.length === 1) return prev
        return prev.filter((item) => item !== id)
      }
      return [...prev, id]
    })
  }

  const handleSelectAllSeries = () => {
    setSelectedSeriesIds(seriesList.map((s) => s.id))
  }

  const handleSetPreset = (preset: '24h' | '7d' | 'all') => {
    const now = new Date()
    if (preset === 'all') {
      setFromDate('')
      setToDate('')
      loadMeasurements(selectedSeriesIds, '', '')
      return
    }

    const past = new Date(now)
    if (preset === '24h') {
      past.setHours(past.getHours() - 24)
    } else if (preset === '7d') {
      past.setDate(past.getDate() - 7)
    }

    const formatToLocal = (d: Date) => {
      const pad = (n: number) => String(n).padStart(2, '0')
      return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}`
    }

    const fromStr = formatToLocal(past)
    const toStr = formatToLocal(now)
    setFromDate(fromStr)
    setToDate(toStr)
    loadMeasurements(selectedSeriesIds, fromStr, toStr)
  }

  const handleRefresh = () => {
    loadMeasurements(selectedSeriesIds, fromDate, toDate)
    if (isAdmin) {
      loadSensors()
    }
  }

  const handleLogin = async (user: string, pass: string) => {
    await api.login(user, pass)
    setIsAdmin(true)
    await loadSensors()
  }

  const handleLogout = async () => {
    await api.logout()
    setIsAdmin(false)
    setSensorsList([])
  }

  const handleChangePassword = async (curr: string, next: string) => {
    await api.changePassword(curr, next)
  }

  const handleCreateSeries = async (data: SeriesInput) => {
    await api.createSeries(data)
    const updated = await loadSeries()
    setSelectedSeriesIds(updated.map((s) => s.id))
    loadMeasurements(updated.map((s) => s.id), fromDate, toDate)
  }

  const handleUpdateSeries = async (id: number, data: SeriesInput) => {
    await api.updateSeries(id, data)
    await loadSeries()
    loadMeasurements(selectedSeriesIds, fromDate, toDate)
  }

  const handleDeleteSeries = async (id: number) => {
    await api.deleteSeries(id)
    const updated = await loadSeries()
    setSelectedSeriesIds((prev) => prev.filter((item) => item !== id))
    loadMeasurements(updated.map((s) => s.id), fromDate, toDate)
  }

  const handleCreateSensor = async (name: string, seriesId: number): Promise<SensorCreatedResponse | null> => {
    const res = await api.createSensor({ name, seriesId })
    await loadSensors()
    return res
  }

  const handleDeleteSensor = async (id: number) => {
    await api.deleteSensor(id)
    await loadSensors()
  }

  return (
    <div className="app-container">
      <Header
        isAdmin={isAdmin}
        onOpenLogin={() => setIsLoginModalOpen(true)}
        onOpenChangePassword={() => setIsChangePasswordOpen(true)}
        onLogout={handleLogout}
      />

      <main className="app-main" id="main-content">
        {globalError && (
          <div className="alert alert-error main-alert no-print" role="alert">
            <span>{globalError}</span>
            <button
              type="button"
              className="btn-close"
              onClick={() => setGlobalError(null)}
              aria-label="Zamknij powiadomienie"
            >
              ×
            </button>
          </div>
        )}

        <FilterBar
          seriesList={seriesList}
          selectedSeriesIds={selectedSeriesIds}
          onToggleSeries={handleToggleSeries}
          onSelectAllSeries={handleSelectAllSeries}
          fromDate={fromDate}
          toDate={toDate}
          onFromDateChange={(val) => {
            setFromDate(val)
            loadMeasurements(selectedSeriesIds, val, toDate)
          }}
          onToDateChange={(val) => {
            setToDate(val)
            loadMeasurements(selectedSeriesIds, fromDate, val)
          }}
          onSetPreset={handleSetPreset}
          onRefresh={handleRefresh}
          isLoading={isLoading}
        />

        <section className="chart-section" aria-labelledby="chart-title">
          <div className="section-header">
            <h2 id="chart-title">Wykres Przebiegu Pomiarów</h2>
            {selectedMeasurementId && (
              <button
                type="button"
                className="btn btn-xs btn-secondary no-print"
                onClick={() => setSelectedMeasurementId(null)}
              >
                Resetuj wyróżnienie (#{selectedMeasurementId})
              </button>
            )}
          </div>
          <Chart
            seriesList={seriesList}
            measurements={measurements}
            selectedSeriesIds={selectedSeriesIds}
            selectedMeasurementId={selectedMeasurementId}
            onSelectMeasurement={(id) => setSelectedMeasurementId(id)}
          />
        </section>

        <MeasurementTable
          seriesList={seriesList}
          measurements={measurements}
          selectedSeriesIds={selectedSeriesIds}
          selectedMeasurementId={selectedMeasurementId}
          onSelectMeasurement={(id) => setSelectedMeasurementId(id)}
        />

        {isAdmin && (
          <AdminPanel
            seriesList={seriesList}
            sensorsList={sensorsList}
            onCreateSeries={handleCreateSeries}
            onUpdateSeries={handleUpdateSeries}
            onDeleteSeries={handleDeleteSeries}
            onCreateSensor={handleCreateSensor}
            onDeleteSensor={handleDeleteSensor}
          />
        )}
      </main>

      <footer className="app-footer no-print">
        <p>ZAI 26Z – System Monitoringu Jakości Powietrza IoT</p>
      </footer>

      <LoginModal
        isOpen={isLoginModalOpen}
        onClose={() => setIsLoginModalOpen(false)}
        onLogin={handleLogin}
      />

      <ChangePasswordModal
        isOpen={isChangePasswordOpen}
        onClose={() => setIsChangePasswordOpen(false)}
        onChangePassword={handleChangePassword}
      />
    </div>
  )
}
export default App
