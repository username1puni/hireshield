import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { api } from '../api/client'
import type { AnalysisSummary, Page, RiskLevel, UserStats } from '../api/types'
import RiskBadge from '../components/RiskBadge'
import { formatDate, riskColor } from '../utils/format'

const filters: Array<{ label: string; value?: RiskLevel }> = [
  { label: 'All' },
  { label: 'High risk', value: 'HIGH' },
  { label: 'Be careful', value: 'CAUTION' },
  { label: 'Low risk', value: 'LOW' },
]

const inputLabel = { TEXT: 'Text', IMAGE: 'Screenshot', PDF: 'PDF', URL: 'Link' }

export default function HistoryPage() {
  const [risk, setRisk] = useState<RiskLevel | undefined>()
  const [page, setPage] = useState(0)
  const [data, setData] = useState<Page<AnalysisSummary> | null>(null)
  const [stats, setStats] = useState<UserStats | null>(null)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    api.myStats().then(setStats).catch(() => setStats(null))
  }, [])

  useEffect(() => {
    api.history(page, risk).then(setData).catch((e) => setError(e.message))
  }, [page, risk])

  const total = stats?.total ?? 0

  return (
    <div className="container page">
      <h1>My checks</h1>

      {stats && total > 0 && (
        <div className="card stat-cards">
          <div className="stat"><strong>{total}</strong><span>Total checks</span></div>
          {(['HIGH', 'CAUTION', 'LOW'] as RiskLevel[]).map((level) => (
            <div className="stat" key={level}>
              <strong style={{ color: riskColor(level) }}>{stats.byRisk[level]}</strong>
              <span><RiskBadge level={level} /></span>
            </div>
          ))}
          <div className="risk-bar" aria-hidden="true">
            {(['HIGH', 'CAUTION', 'LOW'] as RiskLevel[]).map((level) => (
              <span key={level} style={{ width: `${(stats.byRisk[level] / total) * 100}%`, background: riskColor(level) }} />
            ))}
          </div>
        </div>
      )}

      <div className="filter-row">
        {filters.map((f) => (
          <button
            key={f.label}
            className={risk === f.value ? 'chip chip-button active' : 'chip chip-button'}
            onClick={() => { setRisk(f.value); setPage(0) }}
          >
            {f.label}
          </button>
        ))}
      </div>

      {error && <p className="alert alert-danger">{error}</p>}
      {data && data.content.length === 0 && (
        <div className="card empty">
          <p>No checks yet. <Link to="/">Check your first job offer</Link>.</p>
        </div>
      )}

      <ul className="history-list">
        {data?.content.map((item) => (
          <li key={item.id}>
            <Link to={`/history/${item.id}`} className="card history-item">
              <span className="history-score" style={{ borderColor: riskColor(item.riskLevel) }}>{item.score}</span>
              <span className="history-body">
                <span className="history-preview">{item.preview}</span>
                <span className="tiny muted">
                  {inputLabel[item.inputType]} · {item.flagCount} red flag{item.flagCount === 1 ? '' : 's'} · {formatDate(item.createdAt)}
                </span>
              </span>
              <RiskBadge level={item.riskLevel} />
            </Link>
          </li>
        ))}
      </ul>

      {data && data.totalPages > 1 && (
        <div className="pager">
          <button className="btn btn-ghost btn-small" disabled={page === 0} onClick={() => setPage(page - 1)}>Previous</button>
          <span className="muted">Page {page + 1} of {data.totalPages}</span>
          <button className="btn btn-ghost btn-small" disabled={page + 1 >= data.totalPages} onClick={() => setPage(page + 1)}>Next</button>
        </div>
      )}
    </div>
  )
}
