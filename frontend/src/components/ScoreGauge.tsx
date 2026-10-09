import type { RiskLevel } from '../api/types'
import { riskColor, riskLabel } from '../utils/format'

interface Props {
  score: number
  level: RiskLevel
  size?: number
}

/** Half-circle gauge that fills up to the score. */
export default function ScoreGauge({ score, level, size = 220 }: Props) {
  const radius = 80
  const circumference = Math.PI * radius
  const clamped = Math.max(0, Math.min(100, score))
  const offset = circumference * (1 - clamped / 100)

  return (
    <div className="gauge" style={{ width: size }} role="img" aria-label={`Scam score ${clamped} out of 100, ${riskLabel[level]}`}>
      <svg viewBox="0 0 200 115" width={size} height={size * 0.575}>
        <path d="M20 100 A80 80 0 0 1 180 100" fill="none" stroke="var(--gauge-track)" strokeWidth="16" strokeLinecap="round" />
        <path
          d="M20 100 A80 80 0 0 1 180 100"
          fill="none"
          stroke={riskColor(level)}
          strokeWidth="16"
          strokeLinecap="round"
          strokeDasharray={circumference}
          strokeDashoffset={offset}
          className="gauge-value"
        />
      </svg>
      <div className="gauge-label">
        <span className="gauge-score">{clamped}</span>
        <span className="gauge-max">/100</span>
      </div>
    </div>
  )
}
