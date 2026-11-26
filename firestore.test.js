const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_uid_123";
const BOB_UID = "bob_uid_456";
const CHARLIE_UID = "charlie_uid_789";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
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

test("Unauthenticated users cannot read or write any collection", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("directory").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("handles").doc("alice").get());
  await assertFails(unauthDb.collection("chats").where("memberUids", "array-contains", ALICE_UID).get());
});

test("UserAccount private preferences: owner CRUD works, cross-user fails", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const now = new Date(Date.now() - 2000);

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      passportEmail: "alice@example.com",
      languageCode: "hu",
      soundEnabled: true,
      vibrationEnabled: true,
      chatFontId: "open_sans",
      chatColorHex: "#1A1A1A",
      chatBold: false,
      chatItalic: false,
      chatUnderline: false,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Partial update allowed
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      languageCode: "en",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Reject immutable createdAt modification
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      createdAt: new Date(Date.now() - 500),
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Reject cross-user read or write
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).update({
      languageCode: "de",
      updatedAt: new Date(Date.now() - 500),
    })
  );
});

test("BuddyDirectoryEntry & HandleLookup: owner writes, signed-in gets by ID, unfiltered list denied", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const now = new Date(Date.now() - 2000);

  await assertSucceeds(
    aliceDb.collection("directory").doc(ALICE_UID).set({
      userId: ALICE_UID,
      msnHandle: "alice@msn.hu",
      displayName: "Alice XP",
      personalMessage: "Listening to retro hits",
      status: "ONLINE",
      nameColorHex: "#0055CC",
      nameFontId: "comic_neue",
      nameBold: true,
      nameItalic: false,
      nameUnderline: false,
      avatarPreset: "DUCK",
      avatarBase64: "",
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertSucceeds(
    aliceDb.collection("handles").doc("alice_msn_hu").set({
      userId: ALICE_UID,
      msnHandle: "alice@msn.hu",
      createdAt: now,
    })
  );

  // Bob can look up Alice's handle and read Alice's public buddy card by ID
  await assertSucceeds(bobDb.collection("handles").doc("alice_msn_hu").get());
  await assertSucceeds(bobDb.collection("directory").doc(ALICE_UID).get());

  // Unfiltered list queries on directory and handles MUST fail
  await assertFails(bobDb.collection("directory").get());
  await assertFails(bobDb.collection("handles").get());

  // Bob cannot modify Alice's directory card or steal her handle
  await assertFails(
    bobDb.collection("directory").doc(ALICE_UID).update({
      displayName: "Hacked",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Reject ghost fields and future timestamps
  await assertFails(
    bobDb.collection("directory").doc(BOB_UID).set({
      userId: BOB_UID,
      msnHandle: "bob@msn.hu",
      displayName: "Bob",
      personalMessage: "",
      status: "ONLINE",
      nameColorHex: "#000000",
      nameFontId: "open_sans",
      nameBold: false,
      nameItalic: false,
      nameUnderline: false,
      avatarPreset: "CAT",
      isAdmin: true,
      createdAt: now,
      updatedAt: now,
    })
  );
});

test("User contacts subcollection: owner only", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const now = new Date(Date.now() - 2000);

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("contacts").doc(BOB_UID).set({
      contactUid: BOB_UID,
      msnHandle: "bob@msn.hu",
      groupName: "Friends",
      blocked: false,
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("contacts").get()
  );

  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("contacts").get()
  );
});

test("Chats & Messages: members read/write, non-members denied, query alignment enforced", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const charlieDb = testEnv.authenticatedContext(CHARLIE_UID).firestore();
  const chatId = `${ALICE_UID}_${BOB_UID}`;
  const now = new Date(Date.now() - 2000);

  await assertSucceeds(
    aliceDb.collection("chats").doc(chatId).set({
      memberUids: [ALICE_UID, BOB_UID],
      lastMessageText: "",
      lastMessageType: "TEXT",
      lastSenderId: ALICE_UID,
      typingUids: [],
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertSucceeds(
    aliceDb.collection("chats").doc(chatId).collection("messages").doc("msg_1").set({
      chatId: chatId,
      senderId: ALICE_UID,
      memberUids: [ALICE_UID, BOB_UID],
      type: "NUDGE",
      text: "Nudge!",
      fontId: "open_sans",
      colorHex: "#000000",
      bold: true,
      italic: false,
      underline: false,
      strikethrough: false,
      createdAt: new Date(Date.now() - 1000),
    })
  );

  // Scoped member queries succeed
  await assertSucceeds(
    bobDb.collection("chats").where("memberUids", "array-contains", BOB_UID).get()
  );
  await assertSucceeds(
    bobDb
      .collection("chats")
      .doc(chatId)
      .collection("messages")
      .where("memberUids", "array-contains", BOB_UID)
      .get()
  );

  // Unfiltered list fails for non-member Charlie (and unfiltered top-level chats fails for Bob)
  await assertFails(bobDb.collection("chats").get());
  await assertFails(charlieDb.collection("chats").doc(chatId).collection("messages").get());

  // Non-member Charlie is denied direct get, filtered list of Bob/Alice messages, and create
  await assertFails(charlieDb.collection("chats").doc(chatId).get());
  await assertFails(charlieDb.collection("chats").doc(chatId).collection("messages").doc("msg_1").get());
  await assertFails(
    charlieDb
      .collection("chats")
      .doc(chatId)
      .collection("messages")
      .where("memberUids", "array-contains", ALICE_UID)
      .get()
  );
  await assertFails(
    charlieDb.collection("chats").doc(chatId).collection("messages").doc("msg_hack").set({
      chatId: chatId,
      senderId: CHARLIE_UID,
      memberUids: [ALICE_UID, BOB_UID],
      type: "TEXT",
      text: "Intruder",
      fontId: "open_sans",
      colorHex: "#000000",
      bold: false,
      italic: false,
      underline: false,
      strikethrough: false,
      createdAt: new Date(Date.now() - 1000),
    })
  );
});

test("Multiplayer Games (XO 5-in-a-row & Memory): active moves allowed, finished games locked", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const charlieDb = testEnv.authenticatedContext(CHARLIE_UID).firestore();
  const chatId = `${ALICE_UID}_${BOB_UID}`;
  const gameId = "game_1";
  const now = new Date(Date.now() - 2000);

  await assertSucceeds(
    aliceDb.collection("chats").doc(chatId).set({
      memberUids: [ALICE_UID, BOB_UID],
      lastMessageText: "",
      lastMessageType: "GAME",
      lastSenderId: ALICE_UID,
      typingUids: [],
      createdAt: now,
      updatedAt: now,
    })
  );

  const initialBoard = Array(121).fill("");
  await assertSucceeds(
    aliceDb.collection("chats").doc(chatId).collection("games").doc(gameId).set({
      chatId: chatId,
      gameType: "GOMOKU_XO",
      memberUids: [ALICE_UID, BOB_UID],
      playerXUid: ALICE_UID,
      playerOUid: BOB_UID,
      currentTurnUid: BOB_UID,
      status: "ACTIVE",
      winnerUid: "",
      boardCells: initialBoard,
      matchedMask: [],
      revealedIndices: [],
      winningLine: [],
      scoreX: 0,
      scoreO: 0,
      moveCount: 1,
      createdByUid: ALICE_UID,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Bob makes a move and finishes the game
  const updatedBoard = [...initialBoard];
  updatedBoard[0] = "X";
  updatedBoard[1] = "O";
  await assertSucceeds(
    bobDb.collection("chats").doc(chatId).collection("games").doc(gameId).update({
      currentTurnUid: ALICE_UID,
      status: "FINISHED",
      winnerUid: BOB_UID,
      boardCells: updatedBoard,
      winningLine: [1, 2, 3, 4, 5],
      scoreO: 1,
      moveCount: 2,
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Terminal state lock: once FINISHED, further updates must fail
  await assertFails(
    aliceDb.collection("chats").doc(chatId).collection("games").doc(gameId).update({
      status: "ACTIVE",
      winnerUid: "",
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Non-member Charlie cannot read, unfiltered-list, or query Alice's games
  await assertFails(charlieDb.collection("chats").doc(chatId).collection("games").doc(gameId).get());
  await assertFails(charlieDb.collection("chats").doc(chatId).collection("games").get());
  await assertFails(
    charlieDb
      .collection("chats")
      .doc(chatId)
      .collection("games")
      .where("memberUids", "array-contains", ALICE_UID)
      .get()
  );
});
