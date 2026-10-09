import { useEffect, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { api } from '../api/client'
import type { Analysis } from '../api/types'
import ResultCard from '../components/ResultCard'
import { formatDate } from '../utils/format'

export default function AnalysisDetailPage() {
  const { id } = useParams()
  const navigate = useNavigate()
  const [analysis, setAnalysis] = useState<Analysis | null>(null)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    api.analysis(Number(id)).then(setAnalysis).catch((e) => setError(e.message))
  }, [id])

  const remove = async () => {
    if (!analysis || !window.confirm('Delete this check?')) return
    await api.deleteAnalysis(analysis.id)
    navigate('/history')
  }

  return (
    <div className="container page">
      <p><Link to="/history">&larr; Back to my checks</Link></p>
      {error && <p className="alert alert-danger">{error}</p>}
      {analysis && (
        <>
          <div className="detail-head">
            <span className="muted">Checked on {formatDate(analysis.createdAt)}</span>
            <button className="btn btn-ghost btn-small" onClick={remove}>Delete</button>
          </div>
          <ResultCard result={analysis} showText />
        </>
      )}
    </div>
  )
}
