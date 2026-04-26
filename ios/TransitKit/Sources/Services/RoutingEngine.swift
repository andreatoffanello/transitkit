import Foundation

// MARK: - RoutingEngine
//
// Connection Scan Algorithm (CSA) su grafo intermodale.
// Ported from civici/DoVe TripPlannerEngine — adapted for TransitKit
// using PlannerStop and APIRoute instead of DoVe domain types.
//
// Formato connections nel JSON:
//   [dep_stop_idx, arr_stop_idx, dep_sec, arr_sec, trip_idx, line_idx]
// Formato footpaths:
//   [stop_a_idx, stop_b_idx, walk_sec]

actor RoutingEngine {

    // MARK: - Internal types

    struct Connection {
        let depStopIdx: Int
        let arrStopIdx: Int
        let depSec: Int
        let arrSec: Int
        let tripIdx: Int
        let lineIdx: Int
    }

    private enum Via {
        case transit(Connection)
        case walk(fromIdx: Int, walkSec: Int)
    }

    // MARK: - State

    private(set) var isReady = false
    private var stops: [PlannerStop] = []
    private var stopIdxMap: [String: Int] = [:]
    private var lineNames: [String] = []
    private var tripIds: [String] = []
    private var connections: [Connection] = []
    private var connectionsByArrDesc: [Connection] = []
    private var footpathsByStop: [Int: [(to: Int, walkSec: Int)]] = [:]
    private var footpathsTo: [Int: [(from: Int, walkSec: Int)]] = [:]

    // Per-index color/text arrays populated from the connections JSON.
    // Files that include line_colors/line_text_colors use these directly.
    // Older files fall back to name-keyed dicts populated from APIRoute.
    private var lineColorByIdx: [String] = []
    private var lineTextColorByIdx: [String] = []
    private var lineColorByName: [String: String] = [:]
    private var lineTextColorByName: [String: String] = [:]

    // MARK: - Load from compressed JSON

    func load(connectionsData: Data, allRoutes: [APIRoute]) throws {
        isReady = false

        // Decompress. Support both raw deflate (wbits=-15) and zlib-framed (78 9C / DA / 01).
        let jsonData: Data
        let firstTwo = connectionsData.prefix(2)
        if firstTwo == Data([0x78, 0x9C]) ||
           firstTwo == Data([0x78, 0xDA]) ||
           firstTwo == Data([0x78, 0x01]) {
            let raw = connectionsData.dropFirst(2).dropLast(4)
            jsonData = try (raw as NSData).decompressed(using: .zlib) as Data
        } else {
            jsonData = try (connectionsData as NSData).decompressed(using: .zlib) as Data
        }

        guard let root = try JSONSerialization.jsonObject(with: jsonData) as? [String: Any]
        else { throw EngineError.invalidFormat }

        // Stops
        guard let rawStops = root["stops"] as? [[String: Any]] else { throw EngineError.invalidFormat }
        var builtStops: [PlannerStop] = []
        var builtIdxMap: [String: Int] = [:]
        for (i, rs) in rawStops.enumerated() {
            guard let id = rs["id"] as? String, let name = rs["name"] as? String else { continue }
            let lat = rs["lat"] as? Double ?? 0
            let lng = rs["lng"] as? Double ?? 0
            builtStops.append(PlannerStop(id: id, name: name, lat: lat, lng: lng))
            builtIdxMap[id] = i
        }

        let builtLineNames = root["line_names"] as? [String] ?? []
        let builtTripIds = root["trip_ids"] as? [String] ?? []
        let builtLineColors = root["line_colors"] as? [String] ?? []
        let builtLineTextColors = root["line_text_colors"] as? [String] ?? []

        // Connections
        guard let rawConnsAny = root["connections"] as? [[Any]] else { throw EngineError.invalidFormat }
        var builtConns: [Connection] = []
        builtConns.reserveCapacity(rawConnsAny.count)
        for c in rawConnsAny {
            guard c.count >= 6,
                  let dep = (c[0] as? NSNumber)?.intValue,
                  let arr = (c[1] as? NSNumber)?.intValue,
                  let dSec = (c[2] as? NSNumber)?.intValue,
                  let aSec = (c[3] as? NSNumber)?.intValue,
                  let tIdx = (c[4] as? NSNumber)?.intValue,
                  let lIdx = (c[5] as? NSNumber)?.intValue else { continue }
            builtConns.append(Connection(
                depStopIdx: dep, arrStopIdx: arr,
                depSec: dSec, arrSec: aSec,
                tripIdx: tIdx, lineIdx: lIdx
            ))
        }

        // Footpaths
        let rawFpAny = root["footpaths"] as? [[Any]] ?? []
        var builtFp: [Int: [(to: Int, walkSec: Int)]] = [:]
        var builtFpTo: [Int: [(from: Int, walkSec: Int)]] = [:]
        for fp in rawFpAny {
            guard fp.count >= 3,
                  let a = (fp[0] as? NSNumber)?.intValue,
                  let b = (fp[1] as? NSNumber)?.intValue,
                  let w = (fp[2] as? NSNumber)?.intValue else { continue }
            builtFp[a, default: []].append((to: b, walkSec: w))
            builtFpTo[b, default: []].append((from: a, walkSec: w))
        }

        // Route colors from APIRoute (name-keyed fallback)
        var colByName: [String: String] = [:]
        var txtByName: [String: String] = [:]
        for r in allRoutes {
            colByName[r.name] = r.color ?? ""
            txtByName[r.name] = r.textColor ?? ""
        }

        // Per-index arrays: prefer JSON-embedded values, fall back to name lookup
        let nLines = builtLineNames.count
        let colByIdx: [String]
        let txtByIdx: [String]
        if builtLineColors.count == nLines {
            colByIdx = builtLineColors
            txtByIdx = builtLineTextColors.count == nLines
                ? builtLineTextColors
                : Array(repeating: "", count: nLines)
        } else {
            colByIdx = builtLineNames.map { colByName[$0] ?? "" }
            txtByIdx = builtLineNames.map { txtByName[$0] ?? "" }
        }

        // Commit
        stops = builtStops
        stopIdxMap = builtIdxMap
        lineNames = builtLineNames
        tripIds = builtTripIds
        connections = builtConns
        connectionsByArrDesc = builtConns.sorted { $0.arrSec > $1.arrSec }
        footpathsByStop = builtFp
        footpathsTo = builtFpTo
        lineColorByIdx = colByIdx
        lineTextColorByIdx = txtByIdx
        lineColorByName = colByName
        lineTextColorByName = txtByName
        isReady = true
    }

    // MARK: - Public API

    /// Up to 5 journeys in the 90-minute window after `afterDate`.
    func query(originId: String, destId: String, afterDate: Date) -> [Journey] {
        guard isReady,
              let originIdx = stopIdxMap[originId],
              let destIdx = stopIdxMap[destId],
              originIdx != destIdx else { return [] }

        let cal = Calendar.current
        let comps = cal.dateComponents([.hour, .minute, .second], from: afterDate)
        let baseSec = (comps.hour ?? 0) * 3600 + (comps.minute ?? 0) * 60 + (comps.second ?? 0)
        let windowSec = baseSec + 5400
        let baseDate = cal.startOfDay(for: afterDate)

        var journeys: [Journey] = []
        var searchAfterSec = baseSec

        for _ in 0..<5 {
            guard let journey = runCSA(
                originIdx: originIdx, destIdx: destIdx,
                afterSec: searchAfterSec, windowSec: windowSec,
                baseDate: baseDate
            ) else { break }
            let depSec = Int(journey.departureTime.timeIntervalSince(baseDate))
            if !journeys.contains(where: { Int($0.departureTime.timeIntervalSince(baseDate)) == depSec }) {
                journeys.append(journey)
            }
            searchAfterSec = depSec + 60
            if searchAfterSec >= windowSec { break }
        }

        return Self.prune(journeys)
    }

    /// Up to 5 journeys arriving by `beforeDate`. Latest departure first.
    func queryArriveBy(originId: String, destId: String, beforeDate: Date) -> [Journey] {
        guard isReady,
              let originIdx = stopIdxMap[originId],
              let destIdx = stopIdxMap[destId],
              originIdx != destIdx else { return [] }

        let cal = Calendar.current
        let comps = cal.dateComponents([.hour, .minute, .second], from: beforeDate)
        let targetSec = (comps.hour ?? 0) * 3600 + (comps.minute ?? 0) * 60 + (comps.second ?? 0)
        let minDepSec = targetSec - 5400
        let baseDate = cal.startOfDay(for: beforeDate)

        var journeys: [Journey] = []
        var searchBeforeSec = targetSec

        for _ in 0..<5 {
            guard let journey = runCSABackward(
                originIdx: originIdx, destIdx: destIdx,
                beforeSec: searchBeforeSec, minDepSec: minDepSec,
                baseDate: baseDate
            ) else { break }
            let arrSec = Int(journey.arrivalTime.timeIntervalSince(baseDate))
            if !journeys.contains(where: { Int($0.arrivalTime.timeIntervalSince(baseDate)) == arrSec }) {
                journeys.append(journey)
            }
            searchBeforeSec = arrSec - 1
            if searchBeforeSec <= minDepSec { break }
        }

        return Self.prune(journeys)
    }

    // MARK: - Pruning

    private static func prune(_ journeys: [Journey]) -> [Journey] {
        guard journeys.count > 1 else { return journeys }
        let min = journeys.map { $0.transfers }.min() ?? 0
        return journeys.filter { $0.transfers <= min + 1 }
    }

    // MARK: - CSA Forward Scan
    //
    // Transfer penalty: each change adds 360s virtual cost to effective arrival time
    // so the algorithm naturally prefers direct routes over slightly faster ones
    // with many transfers — matches OTP/Google Maps behaviour.

    private static let transferPenaltySec = 360

    private func runCSA(
        originIdx: Int, destIdx: Int,
        afterSec: Int, windowSec: Int,
        baseDate: Date
    ) -> Journey? {
        var realArr: [Int: Int] = [originIdx: afterSec]
        var effective: [Int: Int] = [originIdx: afterSec]
        var tripsTaken: [Int: Int] = [originIdx: 0]
        var via: [Int: Via] = [:]

        // Footpaths from origin (walk before boarding)
        if let fps = footpathsByStop[originIdx] {
            for fp in fps {
                let arr = afterSec + fp.walkSec
                if arr < (effective[fp.to] ?? Int.max) {
                    realArr[fp.to] = arr
                    effective[fp.to] = arr
                    tripsTaken[fp.to] = 0
                    via[fp.to] = .walk(fromIdx: originIdx, walkSec: fp.walkSec)
                }
            }
        }

        for conn in connections {
            if conn.depSec > windowSec { break }
            guard let depReal = realArr[conn.depStopIdx], conn.depSec >= depReal else { continue }

            let depEntry = via[conn.depStopIdx]
            let isExtension: Bool
            if case .transit(let prev) = depEntry, prev.tripIdx == conn.tripIdx {
                isExtension = true
            } else {
                isExtension = false
            }
            let depTrips = tripsTaken[conn.depStopIdx] ?? 0
            let newTrips = isExtension ? depTrips : depTrips + 1
            let newTransfers = max(0, newTrips - 1)
            let effArr = conn.arrSec + newTransfers * Self.transferPenaltySec

            guard effArr < (effective[conn.arrStopIdx] ?? Int.max) else { continue }

            realArr[conn.arrStopIdx] = conn.arrSec
            effective[conn.arrStopIdx] = effArr
            tripsTaken[conn.arrStopIdx] = newTrips
            via[conn.arrStopIdx] = .transit(conn)

            if let fps = footpathsByStop[conn.arrStopIdx] {
                for fp in fps {
                    let walkArr = conn.arrSec + fp.walkSec
                    let effWalk = walkArr + newTransfers * Self.transferPenaltySec
                    if effWalk < (effective[fp.to] ?? Int.max) {
                        realArr[fp.to] = walkArr
                        effective[fp.to] = effWalk
                        tripsTaken[fp.to] = newTrips
                        via[fp.to] = .walk(fromIdx: conn.arrStopIdx, walkSec: fp.walkSec)
                    }
                }
            }
        }

        guard via[destIdx] != nil else { return nil }
        return reconstruct(via: via, originIdx: originIdx, destIdx: destIdx, baseDate: baseDate)
    }

    // MARK: - Path Reconstruction (forward)

    private func reconstruct(
        via: [Int: Via], originIdx: Int, destIdx: Int, baseDate: Date
    ) -> Journey? {
        var legs: [Leg] = []
        var current = destIdx

        while current != originIdx {
            guard let entry = via[current] else { return nil }

            switch entry {
            case .transit(let conn):
                var tripConns: [Connection] = [conn]
                var prev = conn.depStopIdx
                while prev != originIdx {
                    guard let pe = via[prev],
                          case .transit(let pc) = pe,
                          pc.tripIdx == conn.tripIdx else { break }
                    tripConns.insert(pc, at: 0)
                    prev = pc.depStopIdx
                }

                let boardIdx = tripConns.first!.depStopIdx
                let alightIdx = tripConns.last!.arrStopIdx
                let boardStop = stops[boardIdx]
                let alightStop = stops[alightIdx]
                let boardTime = baseDate.addingTimeInterval(TimeInterval(tripConns.first!.depSec))
                let alightTime = baseDate.addingTimeInterval(TimeInterval(tripConns.last!.arrSec))
                let (colHex, txtHex) = lineColors(for: conn.lineIdx)
                let tripId = tripIds.indices.contains(conn.tripIdx) ? tripIds[conn.tripIdx] : ""
                let lName = lineNames.indices.contains(conn.lineIdx) ? lineNames[conn.lineIdx] : "?"

                let intermediate: [IntermediateStop] = tripConns.dropLast().map { c in
                    let s = stops[c.arrStopIdx]
                    return IntermediateStop(id: s.id, name: s.name, time: Self.secondsToTime(c.arrSec))
                }

                legs.insert(.transit(TransitLeg(
                    id: UUID(), lineName: lName,
                    routeColor: colHex, routeTextColor: txtHex,
                    headsign: alightStop.name,
                    boardStop: boardStop, alightStop: alightStop,
                    boardTime: boardTime, alightTime: alightTime,
                    tripId: tripId, intermediateStops: intermediate
                )), at: 0)
                current = prev

            case .walk(let fromIdx, let walkSec):
                legs.insert(.walking(WalkingLeg(
                    id: UUID(),
                    fromStop: stops[fromIdx],
                    toStop: stops[current],
                    walkSeconds: walkSec
                )), at: 0)
                current = fromIdx
            }
        }

        return buildJourney(from: legs)
    }

    // MARK: - CSA Backward Scan

    private func runCSABackward(
        originIdx: Int, destIdx: Int,
        beforeSec: Int, minDepSec: Int,
        baseDate: Date
    ) -> Journey? {
        var latestReal: [Int: Int] = [destIdx: beforeSec]
        var effective: [Int: Int] = [destIdx: beforeSec]
        var tripsFromHere: [Int: Int] = [destIdx: 0]
        var viaBack: [Int: Via] = [:]

        if let destWalks = footpathsTo[destIdx] {
            for fp in destWalks {
                let candidate = beforeSec - fp.walkSec
                if candidate < minDepSec { continue }
                if candidate > (effective[fp.from] ?? Int.min) {
                    latestReal[fp.from] = candidate
                    effective[fp.from] = candidate
                    tripsFromHere[fp.from] = 0
                    viaBack[fp.from] = .walk(fromIdx: destIdx, walkSec: fp.walkSec)
                }
            }
        }

        for conn in connectionsByArrDesc {
            if conn.depSec < minDepSec { continue }
            guard let arrTarget = latestReal[conn.arrStopIdx],
                  conn.arrSec <= arrTarget else { continue }

            let arrEntry = viaBack[conn.arrStopIdx]
            let isExtension: Bool
            if case .transit(let ac) = arrEntry, ac.tripIdx == conn.tripIdx {
                isExtension = true
            } else {
                isExtension = false
            }
            let arrTrips = tripsFromHere[conn.arrStopIdx] ?? 0
            let newTrips = isExtension ? arrTrips : arrTrips + 1
            let newTransfers = max(0, newTrips - 1)
            let effDep = conn.depSec - newTransfers * Self.transferPenaltySec

            guard effDep > (effective[conn.depStopIdx] ?? Int.min) else { continue }

            latestReal[conn.depStopIdx] = conn.depSec
            effective[conn.depStopIdx] = effDep
            tripsFromHere[conn.depStopIdx] = newTrips
            viaBack[conn.depStopIdx] = .transit(conn)

            if let walks = footpathsTo[conn.depStopIdx] {
                for fp in walks {
                    let candidate = conn.depSec - fp.walkSec
                    if candidate < minDepSec { continue }
                    let effWalk = candidate - newTransfers * Self.transferPenaltySec
                    if effWalk > (effective[fp.from] ?? Int.min) {
                        latestReal[fp.from] = candidate
                        effective[fp.from] = effWalk
                        tripsFromHere[fp.from] = newTrips
                        viaBack[fp.from] = .walk(fromIdx: conn.depStopIdx, walkSec: fp.walkSec)
                    }
                }
            }
        }

        guard viaBack[originIdx] != nil else { return nil }
        return reconstructForward(viaBack: viaBack, originIdx: originIdx, destIdx: destIdx, baseDate: baseDate)
    }

    // MARK: - Path Reconstruction (from backward via map)

    private func reconstructForward(
        viaBack: [Int: Via], originIdx: Int, destIdx: Int, baseDate: Date
    ) -> Journey? {
        var legs: [Leg] = []
        var current = originIdx

        while current != destIdx {
            guard let entry = viaBack[current] else { return nil }
            switch entry {
            case .transit(let conn):
                var tripConns: [Connection] = [conn]
                var next = conn.arrStopIdx
                while next != destIdx {
                    guard let ne = viaBack[next],
                          case .transit(let nc) = ne,
                          nc.tripIdx == conn.tripIdx else { break }
                    tripConns.append(nc)
                    next = nc.arrStopIdx
                }

                let boardStop = stops[tripConns.first!.depStopIdx]
                let alightStop = stops[tripConns.last!.arrStopIdx]
                let boardTime = baseDate.addingTimeInterval(TimeInterval(tripConns.first!.depSec))
                let alightTime = baseDate.addingTimeInterval(TimeInterval(tripConns.last!.arrSec))
                let (colHex, txtHex) = lineColors(for: conn.lineIdx)
                let tripId = tripIds.indices.contains(conn.tripIdx) ? tripIds[conn.tripIdx] : ""
                let lName = lineNames.indices.contains(conn.lineIdx) ? lineNames[conn.lineIdx] : "?"

                let intermediate: [IntermediateStop] = tripConns.dropLast().map { c in
                    let s = stops[c.arrStopIdx]
                    return IntermediateStop(id: s.id, name: s.name, time: Self.secondsToTime(c.arrSec))
                }

                legs.append(.transit(TransitLeg(
                    id: UUID(), lineName: lName,
                    routeColor: colHex, routeTextColor: txtHex,
                    headsign: alightStop.name,
                    boardStop: boardStop, alightStop: alightStop,
                    boardTime: boardTime, alightTime: alightTime,
                    tripId: tripId, intermediateStops: intermediate
                )))
                current = tripConns.last!.arrStopIdx

            case .walk(let toIdx, let walkSec):
                legs.append(.walking(WalkingLeg(
                    id: UUID(),
                    fromStop: stops[current],
                    toStop: stops[toIdx],
                    walkSeconds: walkSec
                )))
                current = toIdx
            }
        }

        return buildJourney(from: legs)
    }

    // MARK: - Helpers

    private func buildJourney(from legs: [Leg]) -> Journey? {
        guard !legs.isEmpty else { return nil }
        let tLegs = legs.compactMap { if case .transit(let l) = $0 { return l } else { return nil } }
        guard let first = tLegs.first, let last = tLegs.last else { return nil }
        let walk = legs.reduce(0) { acc, leg in
            if case .walking(let w) = leg { return acc + w.walkSeconds } else { return acc }
        }
        return Journey(
            id: UUID(), legs: legs,
            departureTime: first.boardTime, arrivalTime: last.alightTime,
            totalWalkSeconds: walk
        )
    }

    private func lineColors(for lineIdx: Int) -> (color: String, textColor: String) {
        let col = lineColorByIdx.indices.contains(lineIdx)
            ? lineColorByIdx[lineIdx]
            : (lineNames.indices.contains(lineIdx) ? (lineColorByName[lineNames[lineIdx]] ?? "") : "")
        let txt = lineTextColorByIdx.indices.contains(lineIdx)
            ? lineTextColorByIdx[lineIdx]
            : (lineNames.indices.contains(lineIdx) ? (lineTextColorByName[lineNames[lineIdx]] ?? "") : "")
        return (col.isEmpty ? "000000" : col, txt)
    }

    enum EngineError: Error { case invalidFormat }

    static func timeToSeconds(_ time: String) -> Int {
        let parts = time.split(separator: ":").compactMap { Int($0) }
        guard parts.count >= 2 else { return 0 }
        return parts[0] * 3600 + parts[1] * 60
    }

    static func secondsToTime(_ sec: Int) -> String {
        String(format: "%02d:%02d", sec / 3600, (sec % 3600) / 60)
    }
}
