# AntiDonut White Paper: Metrics Checklist

The white paper is ready for review. Before finalizing, the team should fill in the following metrics from their actual testing. Search the white paper for `[TEAM:` to find all placeholders.

## Hardware Metrics

- [ ] HX711 accuracy: "±[X]% vs. reference scale"
- [ ] HX711 stability detection latency: "[Z] milliseconds to determine stable reading"
- [ ] HuskyLens detection latency: "[Z] milliseconds from food placement to label ID available"
- [ ] Number of foods supported: "~[X] common foods"
- [ ] Stability detection reliability: "[W]% of measurements correctly classified"

## Calorie Calculation

- [ ] Mifflin-St Jeor BMR discrepancy: "< [X]%" vs. reference calculator
- [ ] Number of test cases for BMR verification: "[Y] test cases"
- [ ] Daily goal calculation verified for [Z] test users with varying age/weight/height/activity

## BLE & Synchronization

- [ ] BLE connection establishment latency: "[X] seconds on average"
- [ ] Notification latency (Pi to iOS): "[Z] milliseconds on average over [Y] trials"
- [ ] Notification latency (Pi to Android): "[Z] milliseconds on average over [Y] trials"
- [ ] Multi-user test: "Tested [X] concurrent users"
- [ ] BLE reconnection success rate: "[Z]% after [W] minute disconnection"

## System Reliability

- [ ] Scan-to-log latency: "[X] seconds on average"
- [ ] Offline operation tested: "[X] days without internet"
- [ ] Zero crashes during testing (or [X] crashes if any)
- [ ] UART frame reliability: "No frames dropped under high-frequency detection"
- [ ] Weight reading reliability: "[Y]% of readings within ±2g of reference"

## Software Quality

- [ ] Unit test coverage: "[X]%"
- [ ] Integration test cases: "[Y] test cases"
- [ ] Test Mode validation: "All UI screens verified with simulated hardware"
- [ ] Error message clarity: "Clear feedback for HuskyLens failure, weight instability, BLE drop"

## Edge Cases Tested

- [ ] Low battery on phone
- [ ] HuskyLens detection failure (unknown food)
- [ ] Weight instability (moving food, uneven surface)
- [ ] Multi-user session isolation (no cross-contamination)
- [ ] BLE reconnection after link loss

## Other Notable Metrics

- [ ] (Add any impressive results or standout achievements)
- [ ] (Add any additional validation data)

---

## Placeholders to Fill In (by Section)

**Abstract:** No metrics (descriptive only)

**Introduction:** No metrics (problem statement)

**Solution Overview:** No metrics (approach explanation)

**System Architecture & Data Flow:** No metrics (structural overview)

**Technical Deep Dives:**
- **Food Detection:** [X] foods, [Y]% confidence, [Z]% false positive rate, [W] ms latency
- **Hardware Integration:** [X] ms UART latency, [Y]% weight reading accuracy, confirm "No frames dropped"
- **Calorie Calculation:** < [X]% BMR discrepancy, [Z] test users verified
- **BLE Sync:** [X] seconds connection time, [Y] ms iOS latency, [Y] ms Android latency, [Z] concurrent users, [W]% reconnection success
- **Offline-First:** [X] days offline operation, [Y] food entries, [Z] food log entries per user, < [W] ms query time

**Implementation:** No metrics (architectural overview)

**Results & Validation:**
- Hardware: ±[X]%, [Z] ms, [W]%
- Calorie: < [X]%, [Y] cases, [Z] users
- BLE & Sync: [X] s, [Z] ms, [Z] ms, [X] users, [Z]%
- System Reliability: [X] s, [Z]%, [X] days, [X]% and [Y]%, "[Y] cases"
- Software Quality: [X]%, [Y] cases, "verified", "clear"

**Conclusion:** No metrics (positioning)

---

## Submission Checklist

- [ ] All metrics filled in (search white paper for `[TEAM:` — should find 0 matches)
- [ ] Diagrams embedded/linked in white paper (see placeholder comments)
- [ ] White paper proofread for typos and clarity
- [ ] Team reviewed and approved final version
- [ ] Ready for competition judges

---

## How to Use This Checklist

1. **Gather Data:** Collect metrics from your team's testing logs, hardware measurements, and software test reports.
2. **Search & Replace:** Open the white paper. Search for each `[TEAM:` placeholder and replace with actual data.
3. **Verify:** Reread sections with metrics to ensure they're realistic and well-contextualized.
4. **Check This Checklist:** Mark off each item as completed.
5. **Final Review:** Proofread entire white paper one last time.
6. **Submit:** White paper is ready for judges.

---
