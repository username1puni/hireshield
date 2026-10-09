import { useEffect, useRef, useState, type DragEvent, type FormEvent } from 'react'
import { Link } from 'react-router-dom'
import { ApiError, api } from '../api/client'
import type { Analysis, PublicStats } from '../api/types'
import ResultCard from '../components/ResultCard'
import { useAuth } from '../context/useAuth'
import { samples } from '../data/samples'

type Mode = 'text' | 'file' | 'url'

const MAX_FILE_MB = 5

export default function CheckPage() {
  const { user } = useAuth()
  const [mode, setMode] = useState<Mode>('text')
  const [text, setText] = useState('')
  const [url, setUrl] = useState('')
  const [file, setFile] = useState<File | null>(null)
  const [language, setLanguage] = useState('en')
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [result, setResult] = useState<Analysis | null>(null)
  const [stats, setStats] = useState<PublicStats | null>(null)
  const [dragging, setDragging] = useState(false)
  const resultRef = useRef<HTMLDivElement>(null)

  useEffect(() => {
    api.publicStats().then(setStats).catch(() => setStats(null))
  }, [result])

  useEffect(() => {
    if (result) resultRef.current?.scrollIntoView({ behavior: 'smooth', block: 'start' })
  }, [result])

  const pickFile = (picked: File | undefined) => {
    setError(null)
    if (!picked) return
    if (picked.size > MAX_FILE_MB * 1024 * 1024) {
      setError(`File is too large. The limit is ${MAX_FILE_MB} MB.`)
      return
    }
    setFile(picked)
  }

  const onDrop = (e: DragEvent<HTMLLabelElement>) => {
    e.preventDefault()
    setDragging(false)
    pickFile(e.dataTransfer.files[0])
  }

  const submit = async (e: FormEvent) => {
    e.preventDefault()
    setError(null)
    setLoading(true)
    try {
      let analysis: Analysis
      if (mode === 'file') {
        if (!file) throw new ApiError({ status: 400, message: 'Choose a screenshot or PDF first' })
        analysis = await api.analyzeFile(file, language)
      } else if (mode === 'url') {
        analysis = await api.analyzeUrl(url.trim(), language)
      } else {
        analysis = await api.analyzeText(text.trim(), language)
      }
      setResult(analysis)
    } catch (err) {
      const apiError = err as ApiError
      const fieldMessage = apiError.fieldErrors ? Object.values(apiError.fieldErrors)[0] : null
      setError(fieldMessage ? `${Object.keys(apiError.fieldErrors!)[0]} ${fieldMessage}` : apiError.message)
    } finally {
      setLoading(false)
    }
  }

  const needsLogin = mode !== 'text' && !user
  const canSubmit = !loading && !needsLogin && (mode === 'text' ? text.trim().length >= 20 : mode === 'url' ? url.trim().length > 8 : file !== null)

  return (
    <div className="container page">
      <section className="hero">
        <h1>Is this job offer real?</h1>
        <p className="lead">
          Paste a job post, offer letter or WhatsApp message. HireShield checks it for common scam signs and
          gives you a score from 0 to 100 with the reasons.
        </p>
      </section>

      <form className="card check-form" onSubmit={submit}>
        <div className="tabs" role="tablist">
          <button type="button" role="tab" aria-selected={mode === 'text'} className={mode === 'text' ? 'tab active' : 'tab'} onClick={() => setMode('text')}>Paste text</button>
          <button type="button" role="tab" aria-selected={mode === 'file'} className={mode === 'file' ? 'tab active' : 'tab'} onClick={() => setMode('file')}>Screenshot / PDF</button>
          <button type="button" role="tab" aria-selected={mode === 'url'} className={mode === 'url' ? 'tab active' : 'tab'} onClick={() => setMode('url')}>Job link</button>
        </div>

        {mode === 'text' && (
          <>
            <label htmlFor="post" className="sr-only">Job post or message</label>
            <textarea
              id="post"
              value={text}
              onChange={(e) => setText(e.target.value)}
              placeholder="Paste the full message here, including any email IDs, phone numbers and links..."
              rows={8}
              maxLength={20000}
            />
            <div className="samples">
              <span className="muted tiny">Try an example:</span>
              {samples.map((s) => (
                <button type="button" key={s.label} className="chip chip-button" onClick={() => setText(s.text)}>{s.label}</button>
              ))}
            </div>
          </>
        )}

        {mode === 'file' && (
          <label
            className={dragging ? 'dropzone dragging' : 'dropzone'}
            onDragOver={(e) => { e.preventDefault(); setDragging(true) }}
            onDragLeave={() => setDragging(false)}
            onDrop={onDrop}
          >
            <input type="file" accept="image/png,image/jpeg,image/webp,application/pdf" onChange={(e) => pickFile(e.target.files?.[0])} />
            {file ? (
              <span><strong>{file.name}</strong> ({(file.size / 1024).toFixed(0)} KB). Tap to change.</span>
            ) : (
              <span>Drop a screenshot or PDF offer letter here, or tap to choose. PNG, JPG or PDF up to {MAX_FILE_MB} MB.</span>
            )}
          </label>
        )}

        {mode === 'url' && (
          <>
            <label htmlFor="url" className="sr-only">Job post link</label>
            <input id="url" type="url" value={url} onChange={(e) => setUrl(e.target.value)} placeholder="https://example.com/jobs/data-entry-executive" />
          </>
        )}

        {needsLogin && (
          <p className="alert alert-info">
            <Link to="/login">Log in</Link> or <Link to="/register">create a free account</Link> to check screenshots, PDFs and links.
          </p>
        )}

        <div className="form-row">
          <label className="inline-label">
            Explanation in
            <select value={language} onChange={(e) => setLanguage(e.target.value)}>
              <option value="en">English</option>
              <option value="hi">Hindi</option>
            </select>
          </label>
          <button type="submit" className="btn btn-primary" disabled={!canSubmit}>
            {loading ? (mode === 'file' ? 'Reading the file...' : 'Checking...') : 'Check for scam'}
          </button>
        </div>
        {error && <p className="alert alert-danger" role="alert">{error}</p>}
      </form>

      <div ref={resultRef}>{result && <ResultCard result={result} />}</div>

      {stats && (
        <section className="stats-strip">
          <div><strong>{stats.totalChecks.toLocaleString('en-IN')}</strong><span>checks done</span></div>
          <div><strong>{stats.highRiskFound.toLocaleString('en-IN')}</strong><span>high-risk offers caught</span></div>
          <div><strong>{stats.reportedIdentifiers.toLocaleString('en-IN')}</strong><span>scam IDs in the database</span></div>
        </section>
      )}

      <section className="tips">
        <h2>Quick rules that never change</h2>
        <ul>
          <li>A real employer never asks you to pay for a job, training, a laptop or a "security deposit".</li>
          <li>Recruiters from real companies write from the company domain, not Gmail or Yahoo.</li>
          <li>Never share an OTP, your UPI PIN, or Aadhaar/PAN before you have a written offer from a verified company.</li>
          <li>"Like videos and earn" and "rate hotels" tasks are always scams.</li>
          <li>Lost money? Call 1930 or report at cybercrime.gov.in as soon as possible.</li>
        </ul>
      </section>
    </div>
  )
}
