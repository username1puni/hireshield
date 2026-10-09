import { useCallback, useEffect, useState } from 'react'
import { api } from '../api/client'
import type { Rule, ScamReport } from '../api/types'
import { formatDate, percent } from '../utils/format'

type Tab = 'reports' | 'rules' | 'usage'

export default function AdminPage() {
  const [tab, setTab] = useState<Tab>('reports')

  return (
    <div className="container page">
      <h1>Admin</h1>
      <div className="tabs">
        <button className={tab === 'reports' ? 'tab active' : 'tab'} onClick={() => setTab('reports')}>Pending reports</button>
        <button className={tab === 'rules' ? 'tab active' : 'tab'} onClick={() => setTab('rules')}>Rules</button>
        <button className={tab === 'usage' ? 'tab active' : 'tab'} onClick={() => setTab('usage')}>Usage and model</button>
      </div>
      {tab === 'reports' && <PendingReports />}
      {tab === 'rules' && <RulesEditor />}
      {tab === 'usage' && <Usage />}
    </div>
  )
}

function PendingReports() {
  const [reports, setReports] = useState<ScamReport[]>([])
  const load = useCallback(() => {
    api.adminReports('PENDING').then((page) => setReports(page.content))
  }, [])
  useEffect(load, [load])

  const review = async (id: number, status: 'APPROVED' | 'REJECTED') => {
    await api.reviewReport(id, status)
    load()
  }

  if (reports.length === 0) return <div className="card empty"><p>No reports waiting for review.</p></div>

  return (
    <ul className="report-list">
      {reports.map((r) => (
        <li key={r.id} className="card">
          <div className="report-head">
            <strong>{r.companyName || 'Unknown company'}</strong>
            <span className="tiny muted">#{r.id} · {r.platform} · {formatDate(r.createdAt)}</span>
          </div>
          <p className="report-ids">
            {[r.email, r.phone, r.upiId, r.domain].filter(Boolean).join(' · ')}
            {r.amountLost ? ` · lost Rs ${r.amountLost.toLocaleString('en-IN')}` : ''}
          </p>
          <p className="report-desc">{r.description}</p>
          <div className="report-actions">
            <button className="btn btn-primary btn-small" onClick={() => review(r.id, 'APPROVED')}>Approve</button>
            <button className="btn btn-ghost btn-small" onClick={() => review(r.id, 'REJECTED')}>Reject</button>
          </div>
        </li>
      ))}
    </ul>
  )
}

function RulesEditor() {
  const [rules, setRules] = useState<Rule[]>([])
  const [saved, setSaved] = useState(false)

  useEffect(() => {
    api.rules().then(setRules)
  }, [])

  const change = (code: string, patch: Partial<Rule>) => {
    setSaved(false)
    setRules((list) => list.map((r) => (r.code === code ? { ...r, ...patch } : r)))
  }

  const save = async () => {
    setRules(await api.updateRules(rules.map(({ code, points, enabled }) => ({ code, points, enabled }))))
    setSaved(true)
  }

  return (
    <div className="card table-wrap">
      <table className="checks">
        <thead><tr><th>Rule</th><th>Points</th><th>On</th></tr></thead>
        <tbody>
          {rules.map((r) => (
            <tr key={r.code}>
              <td><strong>{r.title}</strong><br /><span className="tiny muted">{r.description}</span></td>
              <td>
                <input
                  className="points-input"
                  type="number"
                  min={0}
                  max={100}
                  value={r.points}
                  onChange={(e) => change(r.code, { points: Number(e.target.value) })}
                  aria-label={`Points for ${r.title}`}
                />
              </td>
              <td><input type="checkbox" checked={r.enabled} onChange={(e) => change(r.code, { enabled: e.target.checked })} aria-label={`Enable ${r.title}`} /></td>
            </tr>
          ))}
        </tbody>
      </table>
      <div className="form-row">
        {saved && <span className="muted">Saved. New checks use these points.</span>}
        <button className="btn btn-primary" onClick={save}>Save rules</button>
      </div>
    </div>
  )
}

function Usage() {
  const [data, setData] = useState<Awaited<ReturnType<typeof api.adminMetrics>> | null>(null)
  useEffect(() => {
    api.adminMetrics().then(setData)
  }, [])
  if (!data) return null
  const usage = data.usage as Record<string, number> & { flagsByRule: Record<string, number> }
  return (
    <>
      <div className="metric-grid">
        <div className="card metric"><strong>{usage.users}</strong><span>Users</span></div>
        <div className="card metric"><strong>{usage.checks}</strong><span>Checks</span></div>
        <div className="card metric"><strong>{usage.highRisk}</strong><span>High risk</span></div>
        <div className="card metric"><strong>{usage.feedbackCorrect}/{usage.feedbackCorrect + usage.feedbackWrong}</strong><span>Feedback "correct"</span></div>
      </div>
      <div className="card table-wrap">
        <h3>Most common red flags</h3>
        <table className="checks">
          <tbody>
            {Object.entries(usage.flagsByRule).map(([code, count]) => <tr key={code}><td>{code}</td><td>{count}</td></tr>)}
          </tbody>
        </table>
        <p className="tiny muted">
          Active model: {data.model.algorithm}, F1 {data.model.test.f1.toFixed(3)}, precision {percent(data.model.test.precision)},
          recall {percent(data.model.test.recall)}.
        </p>
      </div>
    </>
  )
}
