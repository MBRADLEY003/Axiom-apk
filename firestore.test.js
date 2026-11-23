const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_axiom_01";
const BOB_UID = "bob_axiom_02";
const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

function pastDate(offsetMs = 5000) {
  return new Date(Date.now() - offsetMs);
}

test("Unauthenticated users cannot read or write any AXIOM collection", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("vehicles").get());
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("vehicles").doc("veh_1").set({
      companyId: "comp_1",
      registrationNumber: "AXM-101",
      vin: "1HGCM82633A004352",
      makeModel: "Volvo FH16",
      category: "HEAVY_TRUCK",
      assignedDriverId: "",
      assignedDriverName: "",
      fuelType: "DIESEL",
      tankCapacityLiters: 600,
      currentMileageKm: 45000,
      status: "ACTIVE",
      purchaseDate: "2024-01-15",
      insuranceExpiry: "2027-01-15",
      licenseExpiry: "2027-03-01",
      baselineKmPerLiter: 3.2,
      operationalCostTotal: 12000,
      demoData: true,
      createdAt: pastDate(),
      updatedAt: pastDate(),
    })
  );
});

test("Cross-user access is strictly denied across user profile and subcollections", async () => {
  const created = pastDate(10000);
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const db = ctx.firestore();
    await db.collection("users").doc(BOB_UID).set({
      userId: BOB_UID,
      displayName: "Bob Fleet",
      email: "bob@axiom.io",
      activeCompanyId: "comp_bob",
      activeRole: "SUPER_ADMIN",
      sessionTimeoutMinutes: 30,
      emailAlertsEnabled: true,
      smsAlertsEnabled: false,
      createdAt: created,
      updatedAt: created,
    });
    await db.collection("users").doc(BOB_UID).collection("vehicles").doc("bob_v1").set({
      companyId: "comp_bob",
      registrationNumber: "BOB-900",
      vin: "JH4KA8260MC000123",
      makeModel: "Scania R500",
      category: "HEAVY_TRUCK",
      assignedDriverId: "",
      assignedDriverName: "",
      fuelType: "DIESEL",
      tankCapacityLiters: 500,
      currentMileageKm: 12000,
      status: "ACTIVE",
      purchaseDate: "2025-01-01",
      insuranceExpiry: "2027-01-01",
      licenseExpiry: "2027-01-01",
      baselineKmPerLiter: 3.1,
      operationalCostTotal: 5000,
      demoData: false,
      createdAt: created,
      updatedAt: created,
    });
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(aliceDb.collection("users").doc(BOB_UID).get());
  await assertFails(aliceDb.collection("users").doc(BOB_UID).collection("vehicles").get());
  await assertFails(aliceDb.collection("users").doc(BOB_UID).collection("vehicles").doc("bob_v1").update({
    status: "INACTIVE",
    updatedAt: pastDate(),
  }));
});

test("Owner can create and partially update UserProfile, Vehicle, FuelTransaction, RiskAlert, and AuditLog", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const created = pastDate(10000);

  // UserProfile
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Alice Operator",
      email: "alice@axiom.io",
      activeCompanyId: "comp_apex",
      activeRole: "SUPER_ADMIN",
      sessionTimeoutMinutes: 60,
      emailAlertsEnabled: true,
      smsAlertsEnabled: false,
      createdAt: created,
      updatedAt: created,
    })
  );

  // Partial update on UserProfile (role switch)
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      activeRole: "FLEET_MANAGER",
      updatedAt: pastDate(),
    })
  );

  // Vehicle create (filtered by companyId in Kotlin query)
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("vehicles").doc("veh_01").set({
      companyId: "comp_apex",
      registrationNumber: "AXM-404",
      vin: "YV2RT40A8LB123456",
      makeModel: "Volvo FH16 750",
      category: "HEAVY_TRUCK",
      assignedDriverId: "drv_01",
      assignedDriverName: "Marcus Vance",
      fuelType: "DIESEL",
      tankCapacityLiters: 600,
      currentMileageKm: 84200,
      status: "ACTIVE",
      purchaseDate: "2024-03-10",
      insuranceExpiry: "2027-03-10",
      licenseExpiry: "2027-06-15",
      baselineKmPerLiter: 3.1,
      operationalCostTotal: 28400,
      demoData: true,
      createdAt: created,
      updatedAt: created,
    })
  );

  // Filtered Kotlin query succeeds
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("vehicles").where("companyId", "==", "comp_apex").get()
  );

  // Reject ghost field & id field
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("vehicles").doc("veh_bad").set({
      id: "veh_bad",
      companyId: "comp_apex",
      registrationNumber: "BAD-1",
      vin: "YV2RT40A8LB123457",
      makeModel: "Test",
      category: "HEAVY_TRUCK",
      assignedDriverId: "",
      assignedDriverName: "",
      fuelType: "DIESEL",
      tankCapacityLiters: 400,
      currentMileageKm: 100,
      status: "ACTIVE",
      purchaseDate: "2025-01-01",
      insuranceExpiry: "2027-01-01",
      licenseExpiry: "2027-01-01",
      baselineKmPerLiter: 3.0,
      operationalCostTotal: 0,
      demoData: false,
      createdAt: created,
      updatedAt: created,
    })
  );

  // FuelTransaction create
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("fuelTransactions").doc("tx_01").set({
      companyId: "comp_apex",
      vehicleId: "veh_01",
      vehicleReg: "AXM-404",
      driverId: "drv_01",
      driverName: "Marcus Vance",
      routeName: "Corridor North A1",
      liters: 420.5,
      costAmount: 693.8,
      odometerKm: 84200,
      distanceSinceLastKm: 1260,
      fuelType: "DIESEL",
      stationName: "Shell Commercial Hub 4",
      transactionRef: "TXN-88412",
      txDateLabel: "2026-11-20",
      txTimestampMillis: Date.now() - 86400000,
      demoData: true,
      createdAt: created,
      updatedAt: created,
    })
  );

  // Reject future timestamp
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("fuelTransactions").doc("tx_future").set({
      companyId: "comp_apex",
      vehicleId: "veh_01",
      vehicleReg: "AXM-404",
      driverId: "drv_01",
      driverName: "Marcus Vance",
      routeName: "Corridor North A1",
      liters: 100,
      costAmount: 165,
      odometerKm: 84300,
      distanceSinceLastKm: 100,
      fuelType: "DIESEL",
      stationName: "Shell",
      transactionRef: "TXN-FUTURE",
      txDateLabel: "2026-11-23",
      txTimestampMillis: Date.now(),
      demoData: false,
      createdAt: new Date(Date.now() + 60000),
      updatedAt: created,
    })
  );

  // Partial update on Vehicle succeeds
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("vehicles").doc("veh_01").update({
      currentMileageKm: 85100,
      status: "MAINTENANCE",
      updatedAt: pastDate(),
    })
  );

  // RiskAlert create + partial resolution update succeeds
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("riskAlerts").doc("risk_01").set({
      companyId: "comp_apex",
      category: "Fuel Investigation Signal",
      severity: "HIGH",
      entityType: "VEHICLE",
      entityId: "veh_01",
      entityLabel: "AXM-404",
      supportingData: "Observed 2.0 km/L vs baseline 3.1 km/L (Z=-2.6)",
      detectionMethod: "Z-Score + CUSUM",
      historicalComparison: "3.1 km/L 30-day average",
      recommendedInvestigation: "Audit pump receipt & inspect fuel line",
      confidenceScore: 0.89,
      resolutionStatus: "OPEN",
      resolutionNotes: "",
      demoData: false,
      createdAt: created,
      updatedAt: created,
    })
  );
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("riskAlerts").doc("risk_01").update({
      resolutionStatus: "RESOLVED",
      resolutionNotes: "Verified highway detour",
      updatedAt: pastDate(),
    })
  );

  // AuditLog create succeeds, update/delete fail (immutable)
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("auditLogs").doc("log_01").set({
      companyId: "comp_apex",
      actorEmail: "alice@axiom.io",
      actorRole: "SUPER_ADMIN",
      action: "VEHICLE_CREATED",
      targetEntity: "AXM-404",
      details: "Registered Volvo FH16 750 into fleet",
      createdAt: created,
    })
  );
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("auditLogs").doc("log_01").update({
      action: "TAMPERED",
    })
  );
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("auditLogs").doc("log_01").delete()
  );
});
