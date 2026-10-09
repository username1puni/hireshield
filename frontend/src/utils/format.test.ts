import { describe, expect, it } from 'vitest'
import { domainAge, percent } from './format'

describe('format helpers', () => {
  it('formats percentages', () => {
    expect(percent(0.8527)).toBe('85.3%')
    expect(percent(0.5, 0)).toBe('50%')
  })

  it('describes domain age in simple words', () => {
    expect(domainAge(null)).toBe('unknown')
    expect(domainAge(12)).toBe('12 days')
    expect(domainAge(200)).toBe('7 months')
    expect(domainAge(4000)).toBe('10 years')
  })
})
