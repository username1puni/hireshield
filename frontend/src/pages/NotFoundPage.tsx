import { Link } from 'react-router-dom'

export default function NotFoundPage() {
  return (
    <div className="container page narrow">
      <div className="card">
        <h1>Page not found</h1>
        <p><Link to="/">Go to the checker</Link></p>
      </div>
    </div>
  )
}
