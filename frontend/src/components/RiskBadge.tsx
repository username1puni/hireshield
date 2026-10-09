import type { RiskLevel } from '../api/types'
import { riskLabel } from '../utils/format'

export default function RiskBadge({ level }: { level: RiskLevel }) {
  return <span className={`badge badge-${level.toLowerCase()}`}>{riskLabel[level]}</span>
}
