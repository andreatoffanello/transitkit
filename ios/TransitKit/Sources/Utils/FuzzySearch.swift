import Foundation

/// Shared subsequence fuzzy scorer used by every search surface (lines list,
/// stops list, map line/stop picker) so ranking behaviour stays identical
/// across the app — one source of truth, no per-screen drift.
///
/// Score: exact prefix `100` > contains `80` > subsequence `50` > no match `0`.
enum FuzzySearch {
    static func score(_ text: String, query: String) -> Int {
        let t = text.lowercased()
        let q = query.lowercased()
        // Exact prefix = highest score
        if t.hasPrefix(q) { return 100 }
        // Contains = high score
        if t.contains(q) { return 80 }
        // Subsequence check
        var qi = q.startIndex
        for char in t {
            if qi < q.endIndex && char == q[qi] {
                qi = q.index(after: qi)
            }
        }
        return qi == q.endIndex ? 50 : 0
    }
}
