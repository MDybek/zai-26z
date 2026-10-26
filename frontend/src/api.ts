import {
  AuthLoginResponse,
  Measurement,
  ProblemDetails,
  Sensor,
  SensorCreatedResponse,
  Series,
  SeriesInput,
} from './types'

const TOKEN_KEY = 'zai_access_token'

export function getStoredToken(): string | null {
  return localStorage.getItem(TOKEN_KEY)
}

export function setStoredToken(token: string): void {
  localStorage.setItem(TOKEN_KEY, token)
}

export function removeStoredToken(): void {
  localStorage.removeItem(TOKEN_KEY)
}

export class ApiError extends Error {
  status: number
  problem?: ProblemDetails

  constructor(status: number, message: string, problem?: ProblemDetails) {
    super(message)
    this.name = 'ApiError'
    this.status = status
    this.problem = problem
  }
}

async function request<T>(endpoint: string, options: RequestInit = {}): Promise<T> {
  const headers = new Headers(options.headers || {})
  headers.set('Accept', 'application/json')

  const token = getStoredToken()
  if (token) {
    headers.set('Authorization', `Bearer ${token}`)
  }

  if (options.body && !(options.body instanceof FormData)) {
    headers.set('Content-Type', 'application/json')
  }

  const response = await fetch(endpoint, {
    ...options,
    headers,
  })

  if (response.status === 204) {
    return undefined as unknown as T
  }

  const contentType = response.headers.get('content-type') || ''
  const isJson = contentType.includes('application/json') || contentType.includes('application/problem+json')
  const data = isJson ? await response.json() : await response.text()

  if (!response.ok) {
    if (response.status === 401) {
      removeStoredToken()
    }
    const problem = isJson ? (data as ProblemDetails) : undefined
    const message = problem?.detail || problem?.title || (typeof data === 'string' ? data : response.statusText)
    throw new ApiError(response.status, message, problem)
  }

  return data as T
}

export const api = {
  getSeries(): Promise<Series[]> {
    return request<Series[]>('/api/series')
  },

  createSeries(data: SeriesInput): Promise<Series> {
    return request<Series>('/api/series', {
      method: 'POST',
      body: JSON.stringify(data),
    })
  },

  updateSeries(id: number, data: SeriesInput): Promise<Series> {
    return request<Series>(`/api/series/${id}`, {
      method: 'PUT',
      body: JSON.stringify(data),
    })
  },

  deleteSeries(id: number): Promise<void> {
    return request<void>(`/api/series/${id}`, {
      method: 'DELETE',
    })
  },

  getMeasurements(params?: {
    series?: number[]
    from?: string
    to?: string
    sort?: string
    limit?: number
  }): Promise<Measurement[]> {
    const searchParams = new URLSearchParams()
    if (params?.series && params.series.length > 0) {
      searchParams.set('series', params.series.join(','))
    }
    if (params?.from) {
      searchParams.set('from', params.from)
    }
    if (params?.to) {
      searchParams.set('to', params.to)
    }
    if (params?.sort) {
      searchParams.set('sort', params.sort)
    }
    if (params?.limit) {
      searchParams.set('limit', String(params.limit))
    }
    const qs = searchParams.toString()
    return request<Measurement[]>(`/api/measurements${qs ? `?${qs}` : ''}`)
  },

  getSensors(): Promise<Sensor[]> {
    return request<Sensor[]>('/api/sensors')
  },

  createSensor(data: { name: string; seriesId: number }): Promise<SensorCreatedResponse> {
    return request<SensorCreatedResponse>('/api/sensors', {
      method: 'POST',
      body: JSON.stringify(data),
    })
  },

  deleteSensor(id: number): Promise<void> {
    return request<void>(`/api/sensors/${id}`, {
      method: 'DELETE',
    })
  },

  async login(username: string, password: string): Promise<AuthLoginResponse> {
    const res = await request<AuthLoginResponse>('/api/auth/login', {
      method: 'POST',
      body: JSON.stringify({ username, password }),
    })
    setStoredToken(res.accessToken)
    return res
  },

  async logout(): Promise<void> {
    try {
      await request<void>('/api/auth/logout', { method: 'POST' })
    } finally {
      removeStoredToken()
    }
  },

  changePassword(currentPassword: string, newPassword: string): Promise<void> {
    return request<void>('/api/auth/password', {
      method: 'POST',
      body: JSON.stringify({ currentPassword, newPassword }),
    })
  },
}
