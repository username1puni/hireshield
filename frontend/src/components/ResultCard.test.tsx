import { describe, expect, it } from 'vitest'
import { render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import type { Analysis } from '../api/types'
import { AuthProvider } from '../context/AuthContext'
import ResultCard from './ResultCard'

const result: Analysis = {
  id: 1,
  score: 91,
  riskLevel: 'HIGH',
  ruleScore: 75,
  mlProbability: 0.83,
  trustScore: 0,
  flags: [{ code: 'FEE_REQUEST', title: 'Asks you to pay money', points: 30, evidence: 'pay Rs 1999 registration fee' }],
  suspiciousTerms: ['data entry'],
  domains: [],
  links: [],
  reportedIdentifiers: ['fakehr@okaxis'],
  explanation: 'This looks like a scam.',
  explanationSource: 'TEMPLATE',
  inputType: 'TEXT',
  text: 'sample',
  modelVersion: 'emscad-test',
  createdAt: new Date().toISOString(),
}

function renderCard() {
  return render(
    <MemoryRouter>
      <AuthProvider>
        <ResultCard result={result} />
      </AuthProvider>
    </MemoryRouter>,
  )
}

describe('ResultCard', () => {
  it('shows flags with evidence', () => {
    renderCard()
    expect(screen.getByText('Asks you to pay money')).toBeInTheDocument()
    expect(screen.getByText('pay Rs 1999 registration fee')).toBeInTheDocument()
    expect(screen.getByText('+30')).toBeInTheDocument()
  })

  it('warns about identifiers reported by others', () => {
    renderCard()
    expect(screen.getByText(/Already reported by other users/)).toBeInTheDocument()
    expect(screen.getByText(/fakehr@okaxis/)).toBeInTheDocument()
  })

  it('links to the cyber crime portal for risky results', () => {
    renderCard()
    expect(screen.getByRole('link', { name: /cybercrime.gov.in/ })).toHaveAttribute('href', 'https://cybercrime.gov.in')
  })
})
