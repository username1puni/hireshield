import { useState } from 'react'
import { Link, NavLink, useNavigate } from 'react-router-dom'
import { useAuth } from '../context/useAuth'
import Logo from './Logo'

export default function Navbar() {
  const { user, logout } = useAuth()
  const [open, setOpen] = useState(false)
  const navigate = useNavigate()

  const close = () => setOpen(false)
  const handleLogout = () => {
    logout()
    close()
    navigate('/')
  }

  return (
    <header className="navbar">
      <div className="container navbar-inner">
        <Link to="/" className="brand" onClick={close}>
          <Logo />
          <span>HireShield</span>
        </Link>
        <button className="menu-toggle" aria-label="Menu" aria-expanded={open} onClick={() => setOpen(!open)}>
          <span />
          <span />
          <span />
        </button>
        <nav className={open ? 'nav-links open' : 'nav-links'}>
          <NavLink to="/" end onClick={close}>Check</NavLink>
          <NavLink to="/search" onClick={close}>Scam database</NavLink>
          <NavLink to="/model" onClick={close}>How it works</NavLink>
          {user && <NavLink to="/history" onClick={close}>My checks</NavLink>}
          {user && <NavLink to="/report" onClick={close}>Report a scam</NavLink>}
          {user?.role === 'ADMIN' && <NavLink to="/admin" onClick={close}>Admin</NavLink>}
          {user ? (
            <button className="btn btn-ghost btn-small" onClick={handleLogout}>Log out</button>
          ) : (
            <>
              <NavLink to="/login" onClick={close}>Log in</NavLink>
              <Link to="/register" className="btn btn-primary btn-small" onClick={close}>Sign up</Link>
            </>
          )}
        </nav>
      </div>
    </header>
  )
}
