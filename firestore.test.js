const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: "127.0.0.1",
      port: 8085,
    },
  });
});

beforeEach(async () => {
  await testEnv.clearFirestore();
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

test("Unauthenticated user cannot read shared sessions", async () => {
  const unauthContext = testEnv.unauthenticatedContext();
  const db = unauthContext.firestore();
  await assertFails(db.collection("shared_sessions").get());
});

test("Authenticated user can create a shared session", async () => {
  const aliceContext = testEnv.authenticatedContext(ALICE_UID);
  const db = aliceContext.firestore();

  const now = new Date();
  await assertSucceeds(
    db.collection("shared_sessions").doc("session_1").set({
      id: "session_1",
      title: "Shared Area 1",
      createdBy: ALICE_UID,
      creatorName: "Alice",
      participantCount: 1,
      activeMetric: "MAGNETIC_FIELD",
      createdAt: now,
      updatedAt: now,
    })
  );
});

test("Authenticated user can contribute telemetry point with matching userId", async () => {
  const aliceContext = testEnv.authenticatedContext(ALICE_UID);
  const db = aliceContext.firestore();

  const now = new Date();
  await db.collection("shared_sessions").doc("session_1").set({
    id: "session_1",
    title: "Shared Area 1",
    createdBy: ALICE_UID,
    creatorName: "Alice",
    participantCount: 1,
    activeMetric: "MAGNETIC_FIELD",
    createdAt: now,
    updatedAt: now,
  });

  await assertSucceeds(
    db.collection("shared_sessions").doc("session_1").collection("points").doc("point_1").set({
      id: "point_1",
      sessionId: "session_1",
      userId: ALICE_UID,
      userDisplayName: "Alice",
      deviceId: "pixel_8",
      latitude: 55.75,
      longitude: 37.61,
      altitude: 150.0,
      accuracy: 3.5,
      speedKmh: 4.2,
      bearing: 120.0,
      relX: 5.2,
      relY: 8.4,
      accelMagnitude: 10.2,
      magMagnitude: 55.4,
      gyroMagnitude: 0.2,
      lightLux: 400,
      pressureHpa: 1013.2,
      createdAt: now,
    })
  );
});

test("User cannot contribute telemetry point spoofing another user's ID", async () => {
  const bobContext = testEnv.authenticatedContext(BOB_UID);
  const db = bobContext.firestore();

  const now = new Date();
  await assertFails(
    db.collection("shared_sessions").doc("session_1").collection("points").doc("point_fake").set({
      id: "point_fake",
      sessionId: "session_1",
      userId: ALICE_UID, // Spoofed!
      userDisplayName: "Alice",
      latitude: 55.75,
      longitude: 37.61,
      relX: 0,
      relY: 0,
      accelMagnitude: 9.8,
      magMagnitude: 45,
      createdAt: now,
    })
  );
});
