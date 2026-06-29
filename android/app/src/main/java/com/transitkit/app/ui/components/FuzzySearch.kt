package com.transitkit.app.ui.components

// Token-aware scorer: prefix(100) > word-prefix(90) > contains(80) > subsequence(50, gated to 5+ char queries).
// Single source of truth shared by the Lines list, Stops list, and map line/stop picker.
fun fuzzyScore(text: String, query: String): Int {
    val t = text.lowercase()
    val q = query.lowercase()
    if (q.isEmpty()) return 0
    if (t.startsWith(q)) return 100
    // Word-prefix: any whitespace-separated token starts with q. Captures
    // "Boone" matching "Daniel Boone Inn" without falling to subsequence.
    if (t.split(' ', '-', '/').any { it.startsWith(q) }) return 90
    if (t.contains(q)) return 80
    if (q.length < 5) return 0
    var qi = 0
    for (char in t) {
        if (qi < q.length && char == q[qi]) qi++
    }
    return if (qi == q.length) 50 else 0
}
