import { useState, type FormEvent } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import type { ApiError } from '../api/client'
import { useAuth } from '../context/useAuth'

export default function RegisterPage() {
  const { register } = useAuth()
  const navigate = useNavigate()
  const [name, setName] = useState('')
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [errors, setErrors] = useState<Record<string, string>>({})
  const [error, setError] = useState<string | null>(null)
  const [loading, setLoading] = useState(false)

  const submit = async (e: FormEvent) => {
    e.preventDefault()
    setError(null)
    setErrors({})
    setLoading(true)
    try {
      await register(name.trim(), email.trim(), password)
      navigate('/')
    } catch (err) {
      const apiError = err as ApiError
      if (apiError.fieldErrors) setErrors(apiError.fieldErrors)
      else setError(apiError.message)
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="container page narrow">
      <form className="card form" onSubmit={submit} noValidate>
        <h1>Create your account</h1>
        <p className="muted">Save your checks, upload screenshots and help others by reporting scams.</p>
        <label>
          Name
          <input value={name} onChange={(e) => setName(e.target.value)} autoComplete="name" required />
          {errors.name && <span className="field-error">Name {errors.name}</span>}
        </label>
        <label>
          Email
          <input type="email" value={email} onChange={(e) => setEmail(e.target.value)} autoComplete="email" required />
          {errors.email && <span className="field-error">Email {errors.email}</span>}
        </label>
        <label>
          Password
          <input type="password" value={password} onChange={(e) => setPassword(e.target.value)} autoComplete="new-password" minLength={8} required />
          <span className="tiny muted">At least 8 characters</span>
          {errors.password && <span className="field-error">Password {errors.password}</span>}
        </label>
        {error && <p className="alert alert-danger" role="alert">{error}</p>}
        <button className="btn btn-primary" disabled={loading}>{loading ? 'Creating account...' : 'Sign up'}</button>
        <p className="muted">Already have an account? <Link to="/login">Log in</Link></p>
      </form>
    </div>
  )
}
