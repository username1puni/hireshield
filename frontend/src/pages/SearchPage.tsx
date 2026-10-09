import { useState, type FormEvent } from 'react'
import { api } from '../api/client'
import type { BlacklistEntry } from '../api/types'
import { formatDate } from '../utils/format'

const typeLabel = { EMAIL: 'Email', PHONE: 'Phone', UPI: 'UPI ID', DOMAIN: 'Website' }

export default function SearchPage() {
  const [query, setQuery] = useState('')
  const [results, setResults] = useState<BlacklistEntry[] | null>(null)
  const [error, setError] = useState<string | null>(null)

  const search = async (e: FormEvent) => {
    e.preventDefault()
    setError(null)
    if (query.trim().length < 3) {
      setError('Type at least 3 characters')
      return
    }
    try {
      setResults(await api.searchReports(query.trim()))
    } catch (err) {
      setError((err as Error).message)
    }
  }

  return (
    <div className="container page">
      <h1>Community scam database</h1>
      <p className="lead">Check a phone number, email, UPI ID or website against scams reported by other users and approved by an admin.</p>
      <form className="card search-form" onSubmit={search}>
        <input value={query} onChange={(e) => setQuery(e.target.value)} placeholder="e.g. 9876543210, hr.jobs@gmail.com or name@okaxis" aria-label="Search" />
        <button className="btn btn-primary">Search</button>
      </form>
      {error && <p className="alert alert-danger">{error}</p>}
      {results && results.length === 0 && (
        <div className="card empty">
          <p>Nothing found for "{query}". That does not prove it is safe, only that nobody has reported it yet.</p>
        </div>
      )}
      {results && results.length > 0 && (
        <div className="card table-wrap">
          <table className="checks">
            <thead><tr><th>Type</th><th>Value</th><th>Reports</th><th>Last reported</th></tr></thead>
            <tbody>
              {results.map((r) => (
                <tr key={`${r.type}-${r.value}`}>
                  <td>{typeLabel[r.type]}</td>
                  <td className="text-danger">{r.value}</td>
                  <td>{r.reportCount}</td>
                  <td>{formatDate(r.lastSeen)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  )
}
