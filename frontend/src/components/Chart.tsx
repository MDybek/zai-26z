import React, { useMemo, useState } from 'react'
import { Measurement, Series } from '../types'

interface ChartProps {
  seriesList: Series[]
  measurements: Measurement[]
  selectedSeriesIds: number[]
  selectedMeasurementId: number | string | null
  onSelectMeasurement: (id: number | string) => void
}

interface Point {
  measurement: Measurement
  x: number
  y: number
  series: Series
  shapeIdx: number
}

export const Chart: React.FC<ChartProps> = ({
  seriesList,
  measurements,
  selectedSeriesIds,
  selectedMeasurementId,
  onSelectMeasurement,
}) => {
  const [hoveredPoint, setHoveredPoint] = useState<Point | null>(null)

  const seriesMap = useMemo(() => {
    return new Map<number, { series: Series; shapeIdx: number }>(
      seriesList.map((s, idx) => [s.id, { series: s, shapeIdx: idx }])
    )
  }, [seriesList])

  const filteredMeasurements = useMemo(() => {
    return measurements
      .filter((m) => selectedSeriesIds.includes(m.seriesId))
      .sort((a, b) => new Date(a.timestamp).getTime() - new Date(b.timestamp).getTime())
  }, [measurements, selectedSeriesIds])

  const { pointsBySeries, timeTicks, valueTicks } = useMemo(() => {
    if (filteredMeasurements.length === 0) {
      return {
        pointsBySeries: new Map<number, Point[]>(),
        timeTicks: [],
        valueTicks: [],
      }
    }

    const times = filteredMeasurements.map((m) => new Date(m.timestamp).getTime())
      const values = filteredMeasurements.map((m) => m.value)

      const minT = Math.min(...times)
      const maxT = Math.max(...times)
      const tSpan = maxT === minT ? 3600000 : maxT - minT

      const minV = Math.floor(Math.min(...values) * 0.95)
      const maxV = Math.ceil(Math.max(...values) * 1.05)
      const vSpan = maxV === minV ? 10 : maxV - minV

      const width = 800
      const height = 360
      const padding = { top: 30, right: 30, bottom: 45, left: 55 }
      const plotWidth = width - padding.left - padding.right
      const plotHeight = height - padding.top - padding.bottom

      const grouped = new Map<number, Point[]>()
      for (const m of filteredMeasurements) {
        const info = seriesMap.get(m.seriesId)
        if (!info) continue

        const t = new Date(m.timestamp).getTime()
        const x = padding.left + ((t - minT) / tSpan) * plotWidth
        const y = padding.top + plotHeight - ((m.value - minV) / vSpan) * plotHeight

        const pt: Point = {
          measurement: m,
          x,
          y,
          series: info.series,
          shapeIdx: info.shapeIdx,
        }

        if (!grouped.has(m.seriesId)) {
          grouped.set(m.seriesId, [])
        }
        grouped.get(m.seriesId)!.push(pt)
      }

      const tTicksCount = 5
      const tTicks = Array.from({ length: tTicksCount }, (_, i) => {
        const t = minT + (tSpan / (tTicksCount - 1)) * i
        const x = padding.left + (i / (tTicksCount - 1)) * plotWidth
        return {
          x,
          label: new Date(t).toLocaleString('pl-PL', {
            month: '2-digit',
            day: '2-digit',
            hour: '2-digit',
            minute: '2-digit',
          }),
        }
      })

      const vTicksCount = 5
      const vTicks = Array.from({ length: vTicksCount }, (_, i) => {
        const val = minV + (vSpan / (vTicksCount - 1)) * i
        const y = padding.top + plotHeight - (i / (vTicksCount - 1)) * plotHeight
        return { y, label: Math.round(val * 10) / 10 }
      })

      return {
        pointsBySeries: grouped,
        timeTicks: tTicks,
        valueTicks: vTicks,
      }
    }, [filteredMeasurements, seriesMap])

  const dashStyles = ['none', '6 4', '2 3', '8 3 2 3']

  const renderMarker = (shapeIdx: number, cx: number, cy: number, color: string, isSelected: boolean) => {
    const size = isSelected ? 7 : 4.5
    const type = shapeIdx % 4
    if (type === 0) {
      return (
        <circle
          cx={cx}
          cy={cy}
          r={size}
          fill={color}
          stroke="#ffffff"
          strokeWidth={1.5}
        />
      )
    }
    if (type === 1) {
      return (
        <rect
          x={cx - size}
          y={cy - size}
          width={size * 2}
          height={size * 2}
          fill={color}
          stroke="#ffffff"
          strokeWidth={1.5}
        />
      )
    }
    if (type === 2) {
      const h = size * 1.6
      const points = `${cx},${cy - h} ${cx - size * 1.2},${cy + size} ${cx + size * 1.2},${cy + size}`
      return (
        <polygon
          points={points}
          fill={color}
          stroke="#ffffff"
          strokeWidth={1.5}
        />
      )
    }
    const points = `${cx},${cy - size * 1.3} ${cx + size * 1.3},${cy} ${cx},${cy + size * 1.3} ${cx - size * 1.3},${cy}`
    return (
      <polygon
        points={points}
        fill={color}
        stroke="#ffffff"
        strokeWidth={1.5}
      />
    )
  }

  if (filteredMeasurements.length === 0) {
    return (
      <div className="chart-empty">
        <p>Brak pomiarów dla wybranych kryteriów filtrowania.</p>
      </div>
    )
  }

  return (
    <div className="chart-wrapper">
      <div className="chart-legend" aria-label="Legenda wykresu">
        {Array.from(pointsBySeries.entries()).map(([seriesId, pts]) => {
          const s = pts[0]?.series
          const idx = pts[0]?.shapeIdx ?? 0
          if (!s) return null
          return (
            <div key={seriesId} className="legend-item">
              <svg width="24" height="14" aria-hidden="true">
                <line
                  x1="0"
                  y1="7"
                  x2="24"
                  y2="7"
                  stroke={s.color}
                  strokeWidth="2.5"
                  strokeDasharray={dashStyles[idx % dashStyles.length]}
                />
                {renderMarker(idx, 12, 7, s.color, false)}
              </svg>
              <span>{s.name}</span>
              {s.unit && <small>({s.unit})</small>}
            </div>
          )
        })}
      </div>

      <div className="chart-svg-container">
        <svg
          viewBox="0 0 800 360"
          className="chart-svg"
          preserveAspectRatio="xMidYMid meet"
          role="img"
          aria-label="Wykres pomiarów w funkcji czasu"
        >
          {timeTicks.map((t, idx) => (
            <g key={`x-grid-${idx}`}>
              <line x1={t.x} y1={30} x2={t.x} y2={315} stroke="#e2e8f0" strokeDasharray="3 3" />
              <text x={t.x} y={335} fontSize="11" textAnchor="middle" fill="#475569">
                {t.label}
              </text>
            </g>
          ))}

          {valueTicks.map((v, idx) => (
            <g key={`y-grid-${idx}`}>
              <line x1={55} y1={v.y} x2={770} y2={v.y} stroke="#e2e8f0" strokeDasharray="3 3" />
              <text x={48} y={v.y + 4} fontSize="11" textAnchor="end" fill="#475569">
                {v.label}
              </text>
            </g>
          ))}

          <line x1={55} y1={315} x2={770} y2={315} stroke="#64748b" strokeWidth="1.5" />
          <line x1={55} y1={30} x2={55} y2={315} stroke="#64748b" strokeWidth="1.5" />

          {Array.from(pointsBySeries.entries()).map(([seriesId, pts]) => {
            const s = pts[0]?.series
            const idx = pts[0]?.shapeIdx ?? 0
            if (!s || pts.length === 0) return null

            const pathD = pts.reduce((acc, pt, i) => `${acc} ${i === 0 ? 'M' : 'L'} ${pt.x} ${pt.y}`, '')

            return (
              <g key={`series-${seriesId}`}>
                <path
                  d={pathD}
                  fill="none"
                  stroke={s.color}
                  strokeWidth="2.5"
                  strokeDasharray={dashStyles[idx % dashStyles.length]}
                />
                {pts.map((pt) => {
                  const isSelected = selectedMeasurementId === pt.measurement.id
                  return (
                    <g
                      key={`pt-${pt.measurement.id}`}
                      className={`chart-point-group ${isSelected ? 'highlighted' : ''}`}
                      onClick={() => onSelectMeasurement(pt.measurement.id)}
                      onMouseEnter={() => setHoveredPoint(pt)}
                      onMouseLeave={() => setHoveredPoint(null)}
                      style={{ cursor: 'pointer' }}
                    >
                      {isSelected && (
                        <g pointerEvents="none">
                          <circle
                            cx={pt.x}
                            cy={pt.y}
                            r={16}
                            fill="none"
                            stroke={s.color}
                            strokeWidth="2.5"
                            className="pulse-ring"
                          />
                          <rect
                            x={pt.x - 36}
                            y={Math.max(pt.y - 36, 6)}
                            width={72}
                            height={22}
                            rx={4}
                            fill={s.color}
                          />
                          <text
                            x={pt.x}
                            y={Math.max(pt.y - 21, 21)}
                            textAnchor="middle"
                            fill="#ffffff"
                            fontSize="11"
                            fontWeight="700"
                          >
                            {pt.measurement.value} {s.unit || ''}
                          </text>
                        </g>
                      )}
                      {renderMarker(idx, pt.x, pt.y, s.color, isSelected)}
                    </g>
                  )
                })}
              </g>
            )
          })}

          {hoveredPoint && (
            <g
              transform={`translate(${Math.min(Math.max(hoveredPoint.x, 100), 700)}, ${Math.max(hoveredPoint.y - 45, 40)})`}
              className="chart-tooltip"
              pointerEvents="none"
            >
              <rect
                x="-80"
                y="-30"
                width="160"
                height="45"
                rx="6"
                fill="#0f172a"
                opacity="0.92"
              />
              <text x="0" y="-12" textAnchor="middle" fill="#ffffff" fontSize="12" fontWeight="600">
                {hoveredPoint.series.name}: {hoveredPoint.measurement.value} {hoveredPoint.series.unit || ''}
              </text>
              <text x="0" y="6" textAnchor="middle" fill="#94a3b8" fontSize="10">
                {new Date(hoveredPoint.measurement.timestamp).toLocaleString('pl-PL')}
              </text>
            </g>
          )}
        </svg>
      </div>
    </div>
  )
}
