require 'xcodeproj'

project_path = '/Users/andreatoffanello/GitHub/transit-engine/ios/TransitKit.xcodeproj'
project = Xcodeproj::Project.open(project_path)

# ── 1. Add ScheduleDecodingTests.swift to existing TransitKitTests target ──
tests_target = project.targets.find { |t| t.name == 'TransitKitTests' }
raise "TransitKitTests target not found" unless tests_target

# Find (or create) the Tests group — it exists at TransitKit/Tests
tests_group = project.main_group.find_subpath('TransitKit/Tests', false)
if tests_group.nil?
  puts "Tests group not found by path, searching by name..."
  tests_group = project.main_group.recursive_children.find { |c| c.respond_to?(:name) && c.name == 'Tests' }
end
raise "Tests group not found" unless tests_group

# Check if ScheduleDecodingTests.swift already in group
existing = tests_group.children.find { |c| c.respond_to?(:path) && c.path.to_s.include?('ScheduleDecodingTests') }
if existing.nil?
  file_ref = tests_group.new_file('ScheduleDecodingTests.swift')
  tests_target.source_build_phase.add_file_reference(file_ref)
  puts "Added ScheduleDecodingTests.swift to TransitKitTests"
else
  puts "ScheduleDecodingTests.swift already in group, skipping"
end

# ── 2. Create TransitKitUITests target ──
ui_target_existing = project.targets.find { |t| t.name == 'TransitKitUITests' }
if ui_target_existing
  puts "TransitKitUITests target already exists, skipping creation"
  ui_target = ui_target_existing
else
  # Create UI test target
  ui_target = project.new_target(:ui_test_bundle, 'TransitKitUITests', :ios, '16.0')
  puts "Created TransitKitUITests target"

  # Set bundle identifier
  ui_target.build_configurations.each do |config|
    config.build_settings['PRODUCT_BUNDLE_IDENTIFIER'] = 'com.transitkit.TransitKitUITests'
    config.build_settings['SWIFT_VERSION'] = '5.0'
    config.build_settings['IPHONEOS_DEPLOYMENT_TARGET'] = '16.0'
    config.build_settings['TEST_TARGET_NAME'] = 'TransitKit'
  end

  # Add dependency on main app target
  main_target = project.targets.find { |t| t.name == 'TransitKit' }
  ui_target.add_dependency(main_target) if main_target
end

# Create or find TransitKitUITests group
ui_group = project.main_group.find_subpath('TransitKitUITests', false)
if ui_group.nil?
  ui_group = project.main_group.new_group('TransitKitUITests', 'TransitKitUITests')
  puts "Created TransitKitUITests group"
end

# Add AppLaunchTests.swift
existing_ui = ui_group.children.find { |c| c.respond_to?(:path) && c.path.to_s.include?('AppLaunchTests') }
if existing_ui.nil?
  ui_file_ref = ui_group.new_file('AppLaunchTests.swift')
  ui_target.source_build_phase.add_file_reference(ui_file_ref)
  puts "Added AppLaunchTests.swift to TransitKitUITests"
else
  puts "AppLaunchTests.swift already in group, skipping"
end

project.save
puts "Done — project saved."
