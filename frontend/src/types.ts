export interface Series {
  id: number
  name: string
  minValue: number
  maxValue: number
  color: string
  icon?: string | null
  unit?: string | null
}

export interface SeriesInput {
  name: string
  minValue: number
  maxValue: number
  color: string
  icon?: string | null
  unit?: string | null
}

export interface Measurement {
  id: number | string
  seriesId: number
  sensorId?: number | null
  value: number
  timestamp: string
}

export interface Sensor {
  id: number
  name: string
  seriesId: number
  createdAt: string
  lastActivity?: string | null
}

export interface SensorCreatedResponse {
  id: number
  name: string
  seriesId: number
  apiKey: string
  createdAt: string
}

export interface AuthLoginResponse {
  accessToken: string
  tokenType: string
  expiresIn: number
}

export interface ProblemDetails {
  type?: string
  title: string
  status: number
  detail?: string
  instance?: string
  invalidParams?: Array<{ name: string; reason: string }>
}
