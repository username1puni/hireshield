export type RiskLevel = 'LOW' | 'CAUTION' | 'HIGH'
export type InputType = 'TEXT' | 'IMAGE' | 'PDF' | 'URL'

export interface User {
  id: number
  name: string
  email: string
  role: 'USER' | 'ADMIN'
  createdAt: string
}

export interface AuthResponse {
  token: string
  expiresInSeconds: number
  user: User
}

export interface Flag {
  code: string
  title: string
  points: number
  evidence: string
}

export interface DomainReport {
  domain: string
  registeredOn: string | null
  ageDays: number | null
  registrar: string | null
  hasMx: boolean | null
  freeEmailProvider: boolean
  lookalikeOf: string | null
  governmentDomain: boolean
}

export interface LinkReport {
  url: string
  domain: string
  verdict: 'SAFE' | 'SUSPICIOUS' | 'DANGEROUS'
  reasons: string[]
}

export interface Analysis {
  id: number
  score: number
  riskLevel: RiskLevel
  ruleScore: number
  mlProbability: number
  trustScore: number
  flags: Flag[]
  suspiciousTerms: string[]
  domains: DomainReport[]
  links: LinkReport[]
  reportedIdentifiers: string[]
  explanation: string
  explanationSource: 'TEMPLATE' | 'LLM'
  inputType: InputType
  text: string
  modelVersion: string | null
  createdAt: string
}

export interface AnalysisSummary {
  id: number
  score: number
  riskLevel: RiskLevel
  inputType: InputType
  preview: string
  flagCount: number
  createdAt: string
}

export interface Page<T> {
  content: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}

export interface PublicStats {
  totalChecks: number
  highRiskFound: number
  checksToday: number
  approvedReports: number
  reportedIdentifiers: number
}

export interface UserStats {
  total: number
  byRisk: Record<RiskLevel, number>
}

export type ReportStatus = 'PENDING' | 'APPROVED' | 'REJECTED'

export interface ScamReport {
  id: number
  companyName: string | null
  email: string | null
  phone: string | null
  upiId: string | null
  domain: string | null
  platform: string | null
  amountLost: number | null
  description: string
  status: ReportStatus
  createdAt: string
}

export interface BlacklistEntry {
  type: 'EMAIL' | 'PHONE' | 'UPI' | 'DOMAIN'
  value: string
  reportCount: number
  firstSeen: string
  lastSeen: string
}

export interface Rule {
  code: string
  title: string
  description: string
  points: number
  enabled: boolean
}

export interface Metrics {
  version: string
  trainedAt: string
  algorithm: string
  threshold: number
  dataset: Record<string, number | string>
  features: Record<string, number | string>
  test: {
    precision: number
    recall: number
    f1: number
    accuracy: number
    rocAuc: number
    prAuc: number
    confusionMatrix: { truePositive: number; falsePositive: number; trueNegative: number; falseNegative: number }
  }
  crossValidation: Array<{ model: string; precision: number; recall: number; f1: number; prAuc: number }>
  baselineAlwaysReal: { accuracy: number; recall: number }
  secondTest?: {
    name: string
    rows: number
    selectedModel: { caught: number; missed: number; recall: number }
    allModels?: Record<string, { caught: number; recall: number }>
  }
}

export interface ApiErrorBody {
  status: number
  message: string
  fieldErrors?: Record<string, string>
}
