import type {
  Analysis, AnalysisSummary, AuthResponse, BlacklistEntry, Metrics, Page, PublicStats,
  ReportStatus, RiskLevel, Rule, ScamReport, User, UserStats, ApiErrorBody,
} from './types'

const TOKEN_KEY = 'hireshield.token'

export class ApiError extends Error {
  status: number
  fieldErrors?: Record<string, string>

  constructor(body: ApiErrorBody) {
    super(body.message)
    this.status = body.status
    this.fieldErrors = body.fieldErrors
  }
}

export const tokenStore = {
  get: () => localStorage.getItem(TOKEN_KEY),
  set: (token: string) => localStorage.setItem(TOKEN_KEY, token),
  clear: () => localStorage.removeItem(TOKEN_KEY),
}

async function request<T>(path: string, options: RequestInit = {}): Promise<T> {
  const headers = new Headers(options.headers)
  const token = tokenStore.get()
  if (token) headers.set('Authorization', `Bearer ${token}`)
  if (options.body && !(options.body instanceof FormData)) headers.set('Content-Type', 'application/json')

  let response: Response
  try {
    response = await fetch(path, { ...options, headers })
  } catch {
    throw new ApiError({ status: 0, message: 'Cannot reach the server. Check your internet connection.' })
  }

  if (response.status === 401 && token) {
    // Token expired or the user was removed
    tokenStore.clear()
    window.dispatchEvent(new Event('hireshield:logout'))
  }
  if (!response.ok) {
    let body: ApiErrorBody = { status: response.status, message: 'Something went wrong. Please try again.' }
    try {
      body = await response.json()
    } catch {
      // not JSON, keep the generic message
    }
    throw new ApiError(body)
  }
  if (response.status === 204) return undefined as T
  return response.json() as Promise<T>
}

const json = (data: unknown) => JSON.stringify(data)

export const api = {
  register: (name: string, email: string, password: string) =>
    request<AuthResponse>('/api/auth/register', { method: 'POST', body: json({ name, email, password }) }),
  login: (email: string, password: string) =>
    request<AuthResponse>('/api/auth/login', { method: 'POST', body: json({ email, password }) }),
  me: () => request<User>('/api/users/me'),
  myStats: () => request<UserStats>('/api/users/me/stats'),

  analyzeText: (text: string, language = 'en') =>
    request<Analysis>('/api/analyze/text', { method: 'POST', body: json({ text, language }) }),
  analyzeUrl: (url: string, language = 'en') =>
    request<Analysis>('/api/analyze/url', { method: 'POST', body: json({ url, language }) }),
  analyzeFile: (file: File, language = 'en') => {
    const form = new FormData()
    form.append('file', file)
    form.append('language', language)
    return request<Analysis>('/api/analyze/file', { method: 'POST', body: form })
  },

  history: (page = 0, risk?: RiskLevel) =>
    request<Page<AnalysisSummary>>(`/api/analyses?page=${page}&size=10${risk ? `&risk=${risk}` : ''}`),
  analysis: (id: number) => request<Analysis>(`/api/analyses/${id}`),
  deleteAnalysis: (id: number) => request<void>(`/api/analyses/${id}`, { method: 'DELETE' }),
  feedback: (id: number, correct: boolean, comment?: string) =>
    request<void>(`/api/analyses/${id}/feedback`, { method: 'POST', body: json({ correct, comment }) }),

  report: (data: Partial<ScamReport>) => request<ScamReport>('/api/reports', { method: 'POST', body: json(data) }),
  searchReports: (q: string) => request<BlacklistEntry[]>(`/api/reports/search?q=${encodeURIComponent(q)}`),

  publicStats: () => request<PublicStats>('/api/stats/public'),
  metrics: () => request<Metrics>('/api/model/metrics'),

  adminReports: (status: ReportStatus = 'PENDING') => request<Page<ScamReport>>(`/api/admin/reports?status=${status}`),
  reviewReport: (id: number, status: ReportStatus) =>
    request<ScamReport>(`/api/admin/reports/${id}`, { method: 'PATCH', body: json({ status }) }),
  rules: () => request<Rule[]>('/api/admin/rules'),
  updateRules: (rules: Pick<Rule, 'code' | 'points' | 'enabled'>[]) =>
    request<Rule[]>('/api/admin/rules', { method: 'PUT', body: json(rules) }),
  adminMetrics: () => request<{ model: Metrics; usage: Record<string, unknown> }>('/api/admin/metrics'),
}
