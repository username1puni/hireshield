import type { RiskLevel } from '../api/types'

export const riskLabel: Record<RiskLevel, string> = {
  LOW: 'Low risk',
  CAUTION: 'Be careful',
  HIGH: 'High risk',
}

export function riskColor(level: RiskLevel): string {
  switch (level) {
    case 'HIGH':
      return 'var(--danger)'
    case 'CAUTION':
      return 'var(--warning)'
    default:
      return 'var(--safe)'
  }
}

export function formatDate(iso: string): string {
  return new Date(iso).toLocaleString('en-IN', {
    day: 'numeric',
    month: 'short',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  })
}

export function percent(value: number, digits = 1): string {
  return `${(value * 100).toFixed(digits)}%`
}

export function domainAge(days: number | null): string {
  if (days === null) return 'unknown'
  if (days < 60) return `${days} days`
  if (days < 730) return `${Math.round(days / 30)} months`
  return `${Math.floor(days / 365)} years`
}
