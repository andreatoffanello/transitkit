import XCTest
@testable import TransitKit

final class ScheduleDecodingTests: XCTestCase {

    let scheduleJSON = """
    {
      "operator": {
        "id": "testop",
        "name": "Test Operator",
        "url": "https://test.example",
        "timezone": "America/New_York",
        "features": {
          "enableMap": true,
          "enableGeolocation": false,
          "enableFavorites": true,
          "enableNotifications": false
        }
      },
      "lastUpdated": "2026-04-10T12:00:00Z",
      "routes": [
        {
          "id": "R1",
          "name": "1",
          "longName": "Test Route",
          "color": "165F9C",
          "textColor": "FFFFFF",
          "transitType": 3,
          "directions": [
            {
              "directionId": 0,
              "headsign": "Park Ave",
              "stopIds": ["testop_main_st", "testop_park_ave"],
              "shapePolyline": null
            }
          ]
        }
      ],
      "stops": [
        {
          "id": "testop_main_st",
          "name": "Main St",
          "lat": 36.2168,
          "lng": -81.6746,
          "platformCode": null,
          "dockLetter": null,
          "departures": [
            {
              "tripId": "T1",
              "routeId": "R1",
              "routeName": "1",
              "routeColor": "165F9C",
              "routeTextColor": "FFFFFF",
              "headsign": "Park Ave",
              "departureTime": "07:35:00",
              "serviceDays": ["monday", "tuesday", "wednesday", "thursday", "friday"]
            }
          ]
        }
      ]
    }
    """.data(using: .utf8)!

    func testDecodeScheduleResponse() throws {
        let response = try JSONDecoder().decode(ScheduleResponse.self, from: scheduleJSON)
        XCTAssertEqual(response.operator_.id, "testop")
        XCTAssertEqual(response.operator_.name, "Test Operator")
        XCTAssertEqual(response.operator_.timezone, "America/New_York")
        XCTAssertFalse(response.routes.isEmpty)
        XCTAssertFalse(response.stops.isEmpty)
        XCTAssertEqual(response.lastUpdated, "2026-04-10T12:00:00Z")
    }

    func testRouteDecoding() throws {
        let response = try JSONDecoder().decode(ScheduleResponse.self, from: scheduleJSON)
        let route = try XCTUnwrap(response.routes.first)
        XCTAssertEqual(route.id, "R1")
        XCTAssertEqual(route.name, "1")
        XCTAssertEqual(route.transitType, 3)
        XCTAssertEqual(route.color, "165F9C")
        XCTAssertEqual(route.textColor, "FFFFFF")
        XCTAssertFalse(route.directions.isEmpty)
    }

    func testRouteTransitType() throws {
        let response = try JSONDecoder().decode(ScheduleResponse.self, from: scheduleJSON)
        let route = try XCTUnwrap(response.routes.first)
        XCTAssertEqual(route.resolvedTransitType, .bus)
    }

    func testDirectionDecoding() throws {
        let response = try JSONDecoder().decode(ScheduleResponse.self, from: scheduleJSON)
        let dir = try XCTUnwrap(response.routes.first?.directions.first)
        XCTAssertEqual(dir.directionId, 0)
        XCTAssertEqual(dir.headsign, "Park Ave")
        XCTAssertEqual(dir.stopIds, ["testop_main_st", "testop_park_ave"])
        XCTAssertNil(dir.shapePolyline)
    }

    func testStopDecoding() throws {
        let response = try JSONDecoder().decode(ScheduleResponse.self, from: scheduleJSON)
        let stop = try XCTUnwrap(response.stops.first)
        XCTAssertEqual(stop.id, "testop_main_st")
        XCTAssertEqual(stop.name, "Main St")
        XCTAssertEqual(stop.lat, 36.2168, accuracy: 0.0001)
        XCTAssertEqual(stop.lng, -81.6746, accuracy: 0.0001)
        XCTAssertNil(stop.platformCode)
        XCTAssertNil(stop.dockLetter)
        XCTAssertFalse(stop.departures.isEmpty)
    }

    func testDepartureDecoding() throws {
        let response = try JSONDecoder().decode(ScheduleResponse.self, from: scheduleJSON)
        let dep = try XCTUnwrap(response.stops.first?.departures.first)
        XCTAssertEqual(dep.tripId, "T1")
        XCTAssertEqual(dep.routeId, "R1")
        XCTAssertEqual(dep.routeName, "1")
        XCTAssertEqual(dep.routeColor, "165F9C")
        XCTAssertEqual(dep.headsign, "Park Ave")
        XCTAssertEqual(dep.departureTime, "07:35:00")
        XCTAssertEqual(dep.serviceDays, ["monday","tuesday","wednesday","thursday","friday"])
    }

    func testDepartureMinutesFromMidnight() throws {
        let response = try JSONDecoder().decode(ScheduleResponse.self, from: scheduleJSON)
        let dep = try XCTUnwrap(response.stops.first?.departures.first)
        XCTAssertEqual(dep.minutesFromMidnight, 7 * 60 + 35)
    }

    func testOperatorFeaturesDecoding() throws {
        let response = try JSONDecoder().decode(ScheduleResponse.self, from: scheduleJSON)
        XCTAssertTrue(response.operator_.features["enableMap"] ?? false)
        XCTAssertFalse(response.operator_.features["enableGeolocation"] ?? true)
        XCTAssertTrue(response.operator_.features["enableFavorites"] ?? false)
    }
}
