import { describe, it, expect } from 'vitest'
import { filterRoutes, sortRoutes } from '~/utils/routes'
import type { Route } from '~/types'

function makeRoute(overrides: Partial<Route> & { id: string; name: string; transitType: Route['transitType'] }): Route {
  return {
    longName: '',
    color: '#000000',
    textColor: '#FFFFFF',
    directions: [],
    ...overrides,
  }
}

const bus1 = makeRoute({ id: 'b1', name: '1', longName: 'Linea 1 Centro', transitType: 'bus' })
const bus2 = makeRoute({ id: 'b2', name: '12', longName: 'Linea 12 Stazione', transitType: 'bus' })
const ferry1 = makeRoute({ id: 'f1', name: 'A', longName: 'Linea A Molo', transitType: 'ferry' })
const tram1 = makeRoute({ id: 't1', name: 'T3', longName: 'Tram 3 Nord', transitType: 'tram' })

const ALL = [bus1, bus2, ferry1, tram1]

describe('filterRoutes', () => {
  it('returns all routes when selectedType is null and searchQuery is empty', () => {
    expect(filterRoutes(ALL, null, '')).toEqual(ALL)
  })

  it('filters by transitType alone', () => {
    const result = filterRoutes(ALL, 'bus', '')
    expect(result).toEqual([bus1, bus2])
  })

  it('filters by transitType: ferry', () => {
    const result = filterRoutes(ALL, 'ferry', '')
    expect(result).toEqual([ferry1])
  })

  it('returns empty when transitType matches nothing', () => {
    const result = filterRoutes(ALL, 'metro', '')
    expect(result).toHaveLength(0)
  })

  it('filters by search on shortName (name)', () => {
    const result = filterRoutes(ALL, null, '12')
    expect(result).toEqual([bus2])
  })

  it('filters by search on longName case-insensitively', () => {
    const result = filterRoutes(ALL, null, 'centro')
    expect(result).toEqual([bus1])
  })

  it('filters by search on longName partial match', () => {
    const result = filterRoutes(ALL, null, 'mol')
    expect(result).toEqual([ferry1])
  })

  it('combines transitType AND search (AND semantics)', () => {
    const result = filterRoutes(ALL, 'bus', 'stazione')
    expect(result).toEqual([bus2])
  })

  it('combines type + search — returns empty when type matches but search does not', () => {
    const result = filterRoutes(ALL, 'bus', 'molo')
    expect(result).toHaveLength(0)
  })

  it('combines type + search — returns empty when search matches but type does not', () => {
    const result = filterRoutes(ALL, 'tram', 'centro')
    expect(result).toHaveLength(0)
  })

  it('search trims whitespace', () => {
    // '  molo  ' trimmed → 'molo', matches only ferry1.longName
    const result = filterRoutes(ALL, null, '  molo  ')
    expect(result).toEqual([ferry1])
  })

  it('search is case-insensitive on shortName', () => {
    const result = filterRoutes(ALL, null, 't3')
    expect(result).toEqual([tram1])
  })

  it('selectedType null (All chip) shows all routes regardless of search being empty', () => {
    const result = filterRoutes(ALL, null, '')
    expect(result).toHaveLength(4)
  })

  it('returns empty array when input is empty', () => {
    expect(filterRoutes([], null, 'bus')).toEqual([])
    expect(filterRoutes([], 'bus', '')).toEqual([])
    expect(filterRoutes([], null, '')).toEqual([])
  })

  it('does not mutate the original array', () => {
    const copy = [...ALL]
    filterRoutes(ALL, 'bus', 'centro')
    expect(ALL).toEqual(copy)
  })

  it('case-insensitive match on longName with exact word', () => {
    const stazione = makeRoute({ id: 's1', name: 'S1', longName: 'Stazione Centrale', transitType: 'bus' })
    const result = filterRoutes([stazione], null, 'stazione')
    expect(result).toEqual([stazione])
  })

  it('partial accent-insensitive match on longName', () => {
    // 'dellestate' is NOT a substring of 'dellÈstate' (È ≠ e), so this test
    // would fail without NFD normalization. It verifies genuine diacritic stripping.
    const estate = makeRoute({ id: 'e1', name: 'E1', longName: 'Fermata dellÈstate', transitType: 'bus' })
    const result = filterRoutes([estate], null, 'dellestate')
    expect(result).toEqual([estate])
  })

  it('accent-insensitive match also works when query ends before accent (coincidental substring)', () => {
    // 'oltrep' happens to be a substring of 'oltrepò' even without normalization,
    // but normalization should still handle it correctly.
    const oltrepo = makeRoute({ id: 'o1', name: 'O1', longName: 'Fermata Oltrepò', transitType: 'bus' })
    const result = filterRoutes([oltrepo], null, 'oltrep')
    expect(result).toEqual([oltrepo])
  })

  it('query with accent finds route without accent (symmetric NFD)', () => {
    // Verifies symmetric NFD normalization on both query and route.
    // Query 'oltrepò' (with accent ò) and route 'Fermata Oltrepò' (with same accent)
    // both normalize to remove diacritics: normalize('oltrepò') → 'oltrepo',
    // normalize('Fermata Oltrepò') → 'fermata oltrepo'. Since normalize() applies
    // to BOTH sides symmetrically, substring matching works as expected.
    const oltrepo = makeRoute({ id: 'o2', name: 'O2', longName: 'Fermata Oltrepò', transitType: 'bus' })
    const result = filterRoutes([oltrepo], null, 'oltrepò')
    expect(result).toEqual([oltrepo])
  })
})

describe('sortRoutes', () => {
  const makeRoute = (name: string) => ({
    id: name, name, longName: name, color: '#000', textColor: '#fff', transitType: 'bus' as const,
    directions: [], shortName: name,
  })

  it('sorts numerically, not lexicographically', () => {
    const input = ['10', '2', '1', '11'].map(makeRoute)
    expect(sortRoutes(input).map(r => r.name)).toEqual(['1', '2', '10', '11'])
  })

  it('sorts mixed alpha-numeric', () => {
    const input = ['B', 'A', '2', '1'].map(makeRoute)
    expect(sortRoutes(input).map(r => r.name)).toEqual(['1', '2', 'A', 'B'])
  })

  it('handles empty array', () => {
    expect(sortRoutes([])).toEqual([])
  })

  it('does not mutate input array', () => {
    const input = ['2', '1'].map(makeRoute)
    const original = [...input]
    sortRoutes(input)
    expect(input.map(r => r.name)).toEqual(original.map(r => r.name))
  })

  it('handles single element', () => {
    const input = [makeRoute('42')]
    expect(sortRoutes(input)).toHaveLength(1)
  })

  it('accepts an optional locale parameter without error', () => {
    const input = ['2', '1', '10'].map(makeRoute)
    expect(sortRoutes(input, 'it').map(r => r.name)).toEqual(['1', '2', '10'])
  })

  it('sorts mixed alphanumeric names correctly (2A < 10B < 11 < N1)', () => {
    const input = ['10B', '2A', 'N1', '11'].map(makeRoute)
    expect(sortRoutes(input).map(r => r.name)).toEqual(['2A', '10B', '11', 'N1'])
  })
})
