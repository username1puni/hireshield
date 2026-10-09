import { describe, expect, it } from 'vitest'
import { render, screen } from '@testing-library/react'
import ScoreGauge from './ScoreGauge'

describe('ScoreGauge', () => {
  it('shows the score and an accessible label', () => {
    render(<ScoreGauge score={87} level="HIGH" />)
    expect(screen.getByText('87')).toBeInTheDocument()
    expect(screen.getByRole('img')).toHaveAccessibleName('Scam score 87 out of 100, High risk')
  })

  it('clamps values outside 0-100', () => {
    render(<ScoreGauge score={140} level="HIGH" />)
    expect(screen.getByText('100')).toBeInTheDocument()
  })
})
