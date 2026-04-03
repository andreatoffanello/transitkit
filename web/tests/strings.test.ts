import { describe, it, expect } from 'vitest'
import { getStrings } from '~/utils/strings'

describe('getStrings', () => {
  it('returns Italian strings for locale "it"', () => {
    const s = getStrings('it')
    expect(s.now).toBe('Ora')
    expect(s.minutes).toBe('min')
    expect(s.backToHome).toBe('Torna alla home')
    expect(s.weekdayLabels.mon).toBe('Lun')
    expect(s.weekdayGroupNames.weekdays).toBe('Lun-Ven')
    expect(s.transitTypes.bus).toBe('Bus')
  })

  it('returns English strings for locale "en"', () => {
    const s = getStrings('en')
    expect(s.now).toBe('Now')
    expect(s.backToHome).toBe('Back to home')
    expect(s.weekdayLabels.mon).toBe('Mon')
    expect(s.weekdayGroupNames.weekdays).toBe('Mon-Fri')
    expect(s.transitTypes.rail).toBe('Rail')
  })

  it('returns Italian strings when locale is undefined (default)', () => {
    const s = getStrings(undefined)
    expect(s.now).toBe('Ora')
    expect(s.backToHome).toBe('Torna alla home')
  })

  it('falls back to Italian for an unknown locale', () => {
    const s = getStrings('fr')
    expect(s.now).toBe('Ora')
  })

  it('falls back to Italian for another unknown locale', () => {
    const s = getStrings('de')
    expect(s.now).toBe('Ora')
  })

  it('strips region tag and resolves language — "en-US" returns English', () => {
    const s = getStrings('en-US')
    expect(s.now).toBe('Now')
    expect(s.weekdayLabels.sat).toBe('Sat')
  })

  it('strips region tag and resolves language — "it-IT" returns Italian', () => {
    const s = getStrings('it-IT')
    expect(s.now).toBe('Ora')
    expect(s.weekdayLabels.sat).toBe('Sab')
  })

  it('returns all required AppStrings keys for "it"', () => {
    const s = getStrings('it')
    expect(s.noDepartures).toBeDefined()
    expect(s.stopNotFound).toBeDefined()
    expect(s.updatedRealtime).toBeDefined()
    expect(s.openInGoogleMaps).toBeDefined()
    expect(s.linesPageTitle).toBeDefined()
    expect(s.lineNotFound).toBeDefined()
    expect(s.pageNotFound).toBeDefined()
    expect(s.ariaLoading).toBeDefined()
    expect(s.dockPrefix).toBeDefined()
  })

  it('returns all required AppStrings keys for "en"', () => {
    const s = getStrings('en')
    expect(s.noDepartures).toBeDefined()
    expect(s.stopNotFound).toBeDefined()
    expect(s.updatedRealtime).toBeDefined()
    expect(s.openInGoogleMaps).toBeDefined()
    expect(s.linesPageTitle).toBeDefined()
    expect(s.lineNotFound).toBeDefined()
    expect(s.pageNotFound).toBeDefined()
    expect(s.ariaLoading).toBeDefined()
    expect(s.dockPrefix).toBeDefined()
  })

  it('transitTypes contains all expected types for Italian', () => {
    const { transitTypes } = getStrings('it')
    expect(transitTypes.tram).toBe('Tram')
    expect(transitTypes.metro).toBe('Metro')
    expect(transitTypes.ferry).toBe('Ferry')
    expect(transitTypes.cable_tram).toBe('Funivia')
    expect(transitTypes.gondola).toBe('Gondola')
    expect(transitTypes.funicular).toBe('Funicolare')
    expect(transitTypes.trolleybus).toBe('Filobus')
    expect(transitTypes.monorail).toBe('Monorotaia')
  })

  it('transitTypes contains all expected types for English', () => {
    const { transitTypes } = getStrings('en')
    expect(transitTypes.tram).toBe('Tram')
    expect(transitTypes.metro).toBe('Metro')
    expect(transitTypes.ferry).toBe('Ferry')
    expect(transitTypes.cable_tram).toBe('Cable Car')
    expect(transitTypes.gondola).toBe('Gondola')
    expect(transitTypes.funicular).toBe('Funicular')
    expect(transitTypes.trolleybus).toBe('Trolleybus')
    expect(transitTypes.monorail).toBe('Monorail')
  })

  it('nextDepartureToday: IT and EN', () => {
    expect(getStrings('it').nextDepartureToday).toBe('Prossima oggi')
    expect(getStrings('en').nextDepartureToday).toBe('Next today')
  })

  it('schedulesUpdated: IT and EN', () => {
    expect(getStrings('it').schedulesUpdated).toBe('Orari aggiornati al')
    expect(getStrings('en').schedulesUpdated).toBe('Schedules updated')
  })

  it('schedulesValidUntil: IT and EN', () => {
    expect(getStrings('it').schedulesValidUntil).toBe('Validi fino al')
    expect(getStrings('en').schedulesValidUntil).toBe('Valid until')
  })

  it('lineSingular and linePlural: IT and EN', () => {
    expect(getStrings('it').lineSingular).toBe('linea')
    expect(getStrings('it').linePlural).toBe('linee')
    expect(getStrings('en').lineSingular).toBe('line')
    expect(getStrings('en').linePlural).toBe('lines')
  })

  it('shareStop: IT and EN', () => {
    expect(getStrings('it').shareStop).toBe('Condividi fermata')
    expect(getStrings('en').shareStop).toBe('Share stop')
  })

  it('updatedAt: IT and EN', () => {
    expect(getStrings('it').updatedAt).toBe('Aggiornato alle')
    expect(getStrings('en').updatedAt).toBe('Updated at')
  })

  it('onboardingHint: IT and EN', () => {
    expect(getStrings('it').onboardingHint).toBe('Cerca le linee per trovare e salvare le tue fermate preferite')
    expect(getStrings('en').onboardingHint).toBe('Browse lines to find and save your favourite stops')
  })

  it('copyLink: IT and EN', () => {
    expect(getStrings('it').copyLink).toBe('Copia link')
    expect(getStrings('en').copyLink).toBe('Copy link')
  })

  it('copiedFeedback: IT and EN', () => {
    expect(getStrings('it').copiedFeedback).toBe('Copiato!')
    expect(getStrings('en').copiedFeedback).toBe('Copied!')
  })
})
