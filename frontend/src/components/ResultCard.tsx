import { useState } from 'react'
import { Link } from 'react-router-dom'
import { api } from '../api/client'
import type { Analysis } from '../api/types'
import { useAuth } from '../context/useAuth'
import { domainAge, percent } from '../utils/format'
import RiskBadge from './RiskBadge'
import ScoreGauge from './ScoreGauge'

interface Props {
  result: Analysis
  showText?: boolean
}

export default function ResultCard({ result, showText = false }: Props) {
  const { user } = useAuth()
  const [feedback, setFeedback] = useState<'sent' | 'error' | null>(null)

  const sendFeedback = async (correct: boolean) => {
    try {
      await api.feedback(result.id, correct)
      setFeedback('sent')
    } catch {
      setFeedback('error')
    }
  }

  const reportLink = `/report?from=${result.id}`

  return (
    <section className={`card result result-${result.riskLevel.toLowerCase()}`} aria-live="polite">
      <div className="result-top">
        <ScoreGauge score={result.score} level={result.riskLevel} />
        <div className="result-summary">
          <RiskBadge level={result.riskLevel} />
          <p className="explanation">{result.explanation}</p>
          {result.explanationSource === 'LLM' && <p className="tiny muted">Explanation written by an AI model from the checks below.</p>}
        </div>
      </div>

      <div className="breakdown">
        <div>
          <span className="breakdown-value">{result.ruleScore}</span>
          <span className="breakdown-label">Red-flag points</span>
        </div>
        <div>
          <span className="breakdown-value">{percent(result.mlProbability, 0)}</span>
          <span className="breakdown-label">ML scam probability</span>
        </div>
        <div>
          <span className="breakdown-value">{result.trustScore}</span>
          <span className="breakdown-label">Website and contact risk</span>
        </div>
      </div>

      {result.reportedIdentifiers.length > 0 && (
        <div className="alert alert-danger">
          <strong>Already reported by other users:</strong> {result.reportedIdentifiers.join(', ')}
        </div>
      )}

      <h3>Why we think so</h3>
      {result.flags.length === 0 ? (
        <p className="muted">No red-flag rules matched this text.</p>
      ) : (
        <ul className="flags">
          {result.flags.map((flag) => (
            <li key={flag.code}>
              <div className="flag-head">
                <span className="flag-title">{flag.title}</span>
                <span className="flag-points">+{flag.points}</span>
              </div>
              <q className="flag-evidence">{flag.evidence}</q>
            </li>
          ))}
        </ul>
      )}

      {result.suspiciousTerms.length > 0 && result.riskLevel !== 'LOW' && (
        <>
          <h3>Words the model found suspicious</h3>
          <div className="chips">
            {result.suspiciousTerms.map((term) => <span className="chip" key={term}>{term}</span>)}
          </div>
        </>
      )}

      {(result.domains.length > 0 || result.links.length > 0) && (
        <>
          <h3>Website and email checks</h3>
          <div className="table-wrap">
            <table className="checks">
              <thead>
                <tr><th>Domain</th><th>Age</th><th>Mail server</th><th>Notes</th></tr>
              </thead>
              <tbody>
                {result.domains.map((d) => (
                  <tr key={d.domain}>
                    <td>{d.domain}</td>
                    <td>{d.freeEmailProvider ? '-' : domainAge(d.ageDays)}</td>
                    <td>{d.hasMx === null ? '-' : d.hasMx ? 'Yes' : 'No'}</td>
                    <td>
                      {d.freeEmailProvider && 'Free email service. '}
                      {d.lookalikeOf && <span className="text-danger">Imitates {d.lookalikeOf}. </span>}
                      {d.governmentDomain && 'Government domain. '}
                      {!d.freeEmailProvider && d.ageDays !== null && d.ageDays < 180 && <span className="text-danger">Very new. </span>}
                    </td>
                  </tr>
                ))}
                {result.links.filter((l) => l.verdict !== 'SAFE').map((l) => (
                  <tr key={l.url}>
                    <td>{l.domain}</td>
                    <td colSpan={2}>Link: {l.verdict.toLowerCase()}</td>
                    <td>{l.reasons.join('. ')}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </>
      )}

      {showText && (
        <details className="checked-text">
          <summary>Text that was checked</summary>
          <pre>{result.text}</pre>
        </details>
      )}

      <div className="result-actions">
        {result.riskLevel !== 'LOW' && (
          <a className="btn btn-danger-outline" href="https://cybercrime.gov.in" target="_blank" rel="noreferrer">
            Report on cybercrime.gov.in / call 1930
          </a>
        )}
        {user ? (
          <Link className="btn btn-ghost" to={reportLink}>Add to scam database</Link>
        ) : (
          <Link className="btn btn-ghost" to="/register">Sign up to save checks and report scams</Link>
        )}
      </div>

      {user && (
        <div className="feedback">
          {feedback === 'sent' ? (
            <span className="muted">Thanks, your feedback helps improve the model.</span>
          ) : (
            <>
              <span>Was this result right?</span>
              <button className="btn btn-small btn-ghost" onClick={() => sendFeedback(true)}>Yes</button>
              <button className="btn btn-small btn-ghost" onClick={() => sendFeedback(false)}>No</button>
              {feedback === 'error' && <span className="text-danger tiny">Could not send, try again.</span>}
            </>
          )}
        </div>
      )}
    </section>
  )
}
