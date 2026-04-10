import XCTest

@MainActor
final class AppLaunchTests: XCTestCase {

    var app: XCUIApplication!

    override func setUpWithError() throws {
        continueAfterFailure = false
        app = XCUIApplication()
        app.launchArguments = ["--reset-schedule-cache"]
        app.launch()
    }

    override func tearDownWithError() throws {
        app.terminate()
    }

    func testAppLaunchesToHomeTab() throws {
        let tabBar = app.tabBars.firstMatch
        XCTAssertTrue(tabBar.waitForExistence(timeout: 15), "Tab bar should be visible after launch")
    }

    func testScheduleLoadsWithinTimeout() throws {
        let tabBar = app.tabBars.firstMatch
        XCTAssertTrue(tabBar.waitForExistence(timeout: 15))
        let spinner = app.activityIndicators.firstMatch
        if spinner.waitForExistence(timeout: 2) {
            let gone = NSPredicate(format: "isHittable == false OR exists == false")
            expectation(for: gone, evaluatedWith: spinner)
            waitForExpectations(timeout: 30, handler: nil)
        }
    }

    func testLinesTabShowsRoutes() throws {
        let tabBar = app.tabBars.firstMatch
        XCTAssertTrue(tabBar.waitForExistence(timeout: 15))
        tabBar.buttons.element(boundBy: 1).tap()
        let routeCell = app.cells.firstMatch
        XCTAssertTrue(routeCell.waitForExistence(timeout: 20),
                      "At least one route should appear in Lines tab")
    }

    func testTapRouteOpensLineDetail() throws {
        let tabBar = app.tabBars.firstMatch
        XCTAssertTrue(tabBar.waitForExistence(timeout: 15))
        tabBar.buttons.element(boundBy: 1).tap()
        let firstRoute = app.cells.firstMatch
        XCTAssertTrue(firstRoute.waitForExistence(timeout: 20))
        firstRoute.tap()
        let content = app.scrollViews.firstMatch.exists || app.tables.firstMatch.exists
        XCTAssertTrue(content, "Line detail should show content")
    }

    func testStopsTabShowsStops() throws {
        let tabBar = app.tabBars.firstMatch
        XCTAssertTrue(tabBar.waitForExistence(timeout: 15))
        tabBar.buttons.element(boundBy: 2).tap()
        let stopCell = app.cells.firstMatch
        XCTAssertTrue(stopCell.waitForExistence(timeout: 20),
                      "At least one stop should appear in Stops tab")
    }

    func testTapStopOpensDepartureList() throws {
        let tabBar = app.tabBars.firstMatch
        XCTAssertTrue(tabBar.waitForExistence(timeout: 15))
        tabBar.buttons.element(boundBy: 2).tap()
        let firstStop = app.cells.firstMatch
        XCTAssertTrue(firstStop.waitForExistence(timeout: 20))
        firstStop.tap()
        let detail = app.scrollViews.firstMatch
        XCTAssertTrue(detail.waitForExistence(timeout: 10),
                      "Stop detail should appear after tapping a stop")
    }

    func testHomeScreenshot() throws {
        let tabBar = app.tabBars.firstMatch
        XCTAssertTrue(tabBar.waitForExistence(timeout: 15))
        Thread.sleep(forTimeInterval: 1.0)
        let attachment = XCTAttachment(screenshot: app.screenshot())
        attachment.name = "home-after-cdn-load"
        attachment.lifetime = .keepAlways
        add(attachment)
    }
}
