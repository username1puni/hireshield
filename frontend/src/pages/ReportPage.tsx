import { useEffect, useState, type FormEvent } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { api, type ApiError } from '../api/client'
import type { ScamReport } from '../api/types'

const platforms = ['WhatsApp', 'Telegram', 'Email', 'SMS', 'LinkedIn', 'Job portal', 'Instagram', 'Other']

const empty = { companyName: '', email: '', phone: '', upiId: '', domain: '', platform: 'WhatsApp', amountLost: '', description: '' }

export default function ReportPage() {
  const [params] = useSearchParams()
  const [form, setForm] = useState(empty)
  const [errors, setErrors] = useState<Record<string, string>>({})
  const [error, setError] = useState<string | null>(null)
  const [saved, setSaved] = useState<ScamReport | null>(null)
  const [loading, setLoading] = useState(false)

  // Coming from a result page: prefill what we already found in that message.
  useEffect(() => {
    const from = params.get('from')
    if (!from) return
    api.analysis(Number(from)).then((a) => {
      const email = a.text.match(/[\w.+-]+@[\w-]+\.[\w.]+/)?.[0] ?? ''
      const upi = a.text.match(/\b[\w.-]{2,}@(ok[a-z]+|ybl|ibl|axl|paytm|upi)\b/i)?.[0] ?? ''
      const phone = a.text.match(/(\+91[\s-]?)?[6-9]\d{4}[\s-]?\d{5}/)?.[0] ?? ''
      setForm((f) => ({ ...f, email, upiId: upi, phone, description: a.text.slice(0, 1000) }))
    }).catch(() => undefined)
  }, [params])

  const set = (key: keyof typeof empty) => (value: string) => setForm((f) => ({ ...f, [key]: value }))

  const submit = async (e: FormEvent) => {
    e.preventDefault()
    setErrors({})
    setError(null)
    setLoading(true)
    try {
      const body = Object.fromEntries(
        Object.entries(form).filter(([, v]) => v.trim() !== '').map(([k, v]) => [k, k === 'amountLost' ? Number(v) : v.trim()]),
      )
      setSaved(await api.report(body))
      setForm(empty)
    } catch (err) {
      const apiError = err as ApiError
      if (apiError.fieldErrors) setErrors(apiError.fieldErrors)
      setError(apiError.message)
    } finally {
      setLoading(false)
    }
  }

  if (saved) {
    return (
      <div className="container page narrow">
        <div className="card">
          <h1>Thanks for reporting</h1>
          <p>Your report #{saved.id} is waiting for review. Once approved, anyone who checks a message with the same
            email, phone, UPI ID or website will see a warning.</p>
          <p>If you lost money, also call <strong>1930</strong> or file a complaint at{' '}
            <a href="https://cybercrime.gov.in" target="_blank" rel="noreferrer">cybercrime.gov.in</a>.</p>
          <Link to="/" className="btn btn-primary">Back to checking</Link>
        </div>
      </div>
    )
  }

  const field = (key: keyof typeof empty, label: string, props: Record<string, unknown> = {}) => (
    <label>
      {label}
      <input value={form[key]} onChange={(e) => set(key)(e.target.value)} {...props} />
      {errors[key] && <span className="field-error">{errors[key]}</span>}
    </label>
  )

  return (
    <div className="container page narrow">
      <form className="card form" onSubmit={submit}>
        <h1>Report a scam</h1>
        <p className="muted">Add at least one contact detail the scammer used. An admin reviews every report before it goes public.</p>
        {field('companyName', 'Company name they used')}
        <div className="grid-2">
          {field('email', 'Email', { type: 'email' })}
          {field('phone', 'Phone / WhatsApp number', { inputMode: 'tel' })}
          {field('upiId', 'UPI ID', { placeholder: 'name@okaxis' })}
          {field('domain', 'Website', { placeholder: 'example-careers.in' })}
        </div>
        <div className="grid-2">
          <label>
            Where did you see it?
            <select value={form.platform} onChange={(e) => set('platform')(e.target.value)}>
              {platforms.map((p) => <option key={p}>{p}</option>)}
            </select>
          </label>
          {field('amountLost', 'Money lost (Rs, optional)', { type: 'number', min: 0 })}
        </div>
        <label>
          What happened?
          <textarea rows={5} value={form.description} onChange={(e) => set('description')(e.target.value)} required minLength={20} />
          {errors.description && <span className="field-error">{errors.description}</span>}
        </label>
        {error && <p className="alert alert-danger" role="alert">{error}</p>}
        <button className="btn btn-primary" disabled={loading}>{loading ? 'Sending...' : 'Submit report'}</button>
      </form>
    </div>
  )
}
