const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = "demo-no-project";
const DB_NAME = "ai-studio-android-omnichat-62a9f9f4-0611-4a1e-a2f8-46878162b961";
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

// --- SECURITY & BOUNDARY TESTS ---

test("Unauthenticated user: cannot read universal room messages", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("rooms").doc("global").collection("messages").get());
});

test("Authenticated user: can read universal room messages", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(aliceDb.collection("rooms").doc("global").collection("messages").get());
});

test("Authenticated user: can post message to universal room with own UID", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("rooms").doc("global").collection("messages").doc("msg_1").set({
      roomId: "global",
      senderId: ALICE_UID,
      senderName: "Alice",
      text: "Hello everyone from Alice!",
      createdAt: new Date(),
    })
  );
});

test("Authenticated user: cannot impersonate another user in universal message", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(
    aliceDb.collection("rooms").doc("global").collection("messages").doc("msg_fake").set({
      roomId: "global",
      senderId: BOB_UID, // Spoofed!
      senderName: "Bob",
      text: "I am Bob (actually Alice)",
      createdAt: new Date(),
    })
  );
});

test("Authenticated user: cannot delete another user's message", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("rooms").doc("global").collection("messages").doc("msg_bob").set({
      roomId: "global",
      senderId: BOB_UID,
      senderName: "Bob",
      text: "Bob's original message",
      createdAt: new Date(),
    });
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(aliceDb.collection("rooms").doc("global").collection("messages").doc("msg_bob").delete());
});

test("Authenticated user: can update own user profile but not another user profile", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Alice In Wonderland",
      createdAt: new Date(),
      updatedAt: new Date(),
      isOnline: true,
    })
  );

  // Alice cannot write Bob's profile
  await assertFails(
    aliceDb.collection("users").doc(BOB_UID).set({
      userId: BOB_UID,
      displayName: "Bob",
      createdAt: new Date(),
      updatedAt: new Date(),
    })
  );
});
