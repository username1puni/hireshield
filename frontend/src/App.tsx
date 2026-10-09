import { Route, Routes } from 'react-router-dom'
import Navbar from './components/Navbar'
import ProtectedRoute from './components/ProtectedRoute'
import AdminPage from './pages/AdminPage'
import AnalysisDetailPage from './pages/AnalysisDetailPage'
import CheckPage from './pages/CheckPage'
import HistoryPage from './pages/HistoryPage'
import LoginPage from './pages/LoginPage'
import ModelPage from './pages/ModelPage'
import NotFoundPage from './pages/NotFoundPage'
import RegisterPage from './pages/RegisterPage'
import ReportPage from './pages/ReportPage'
import SearchPage from './pages/SearchPage'

export default function App() {
  return (
    <>
      <Navbar />
      <main>
        <Routes>
          <Route path="/" element={<CheckPage />} />
          <Route path="/login" element={<LoginPage />} />
          <Route path="/register" element={<RegisterPage />} />
          <Route path="/search" element={<SearchPage />} />
          <Route path="/model" element={<ModelPage />} />
          <Route path="/history" element={<ProtectedRoute><HistoryPage /></ProtectedRoute>} />
          <Route path="/history/:id" element={<ProtectedRoute><AnalysisDetailPage /></ProtectedRoute>} />
          <Route path="/report" element={<ProtectedRoute><ReportPage /></ProtectedRoute>} />
          <Route path="/admin" element={<ProtectedRoute admin><AdminPage /></ProtectedRoute>} />
          <Route path="*" element={<NotFoundPage />} />
        </Routes>
      </main>
      <footer className="footer">
        <div className="container">
          <span>HireShield · final year project · not a substitute for checking with the company directly</span>
          <span>Cyber fraud helpline: <a href="tel:1930">1930</a> · <a href="https://cybercrime.gov.in" target="_blank" rel="noreferrer">cybercrime.gov.in</a></span>
        </div>
      </footer>
    </>
  )
}
