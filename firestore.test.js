const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ADMIN_UID = "admin_sepehr";
const ADMIN_EMAIL = "sepehrtixzor1@gmail.com";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function validConfigPayload(updatedBy) {
  return {
    subscriptionRequired: false,
    monthlyPriceToman: 99000,
    yearlyPriceToman: 790000,
    monthlyPriceLabel: "۹۹,۰۰۰ تومان / ماه",
    yearlyPriceLabel: "۷۹۰,۰۰۰ تومان / سال",
    freeTrialDays: 7,
    paywallMessage: "برای استفاده نامحدود اشتراک تهیه کنید.",
    paymentUrl: "https://eskytell.app/pro",
    bankCards: [
      { bankName: "ملت", cardNumber: "6104337900001111", holderName: "سپهر تیکسزور" },
    ],
    cryptoWallets: [
      { networkName: "USDT (TRC20)", address: "TQn9Y2khEsLJW1ChVWFMSMeRDow5KcbLSE" },
    ],
    latestVersionName: "1.0.0",
    latestVersionCode: 1,
    updateNotes: "نسخه اولیه",
    updateUrl: "https://eskytell.app/download",
    forceUpdate: false,
    enableDualSim: true,
    enableQuickTemplates: true,
    enableQuietHours: true,
    enableBleGattBridge: true,
    updatedBy,
    updatedAt: new Date(Date.now() - 1000),
  };
}

function validUserPayload(uid, email = "alice@example.com") {
  const now = new Date(Date.now() - 2000);
  return {
    userId: uid,
    email,
    displayName: "Alice",
    sim1Label: "SIM 1",
    sim2Label: "SIM 2",
    hasActiveSubscription: false,
    subscriptionTier: "FREE",
    createdAt: now,
    updatedAt: now,
  };
}

function validPaymentRequestPayload(uid, overrides = {}) {
  const now = new Date(Date.now() - 1500);
  return {
    userId: uid,
    userEmail: "alice@example.com",
    userDisplayName: "Alice",
    requestedTier: "MONTHLY",
    amountToman: 99000,
    paymentMethod: "CARD_TO_CARD",
    targetAccountLabel: "ملت • 6104337900001111",
    trackingCode: "TRK-994120",
    userNote: "واریز کارت به کارت انجام شد",
    status: "PENDING",
    createdAt: now,
    updatedAt: now,
    ...overrides,
  };
}

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
  await assertFails(unauthDb.collection("app_config").doc("global").get());
  await assertFails(unauthDb.collection("announcements").where("published", "==", true).get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).set(validUserPayload(ALICE_UID)));
  await assertFails(unauthDb.collection("payment_requests").where("userId", "==", ALICE_UID).get());
});

test("App config: admin manages Toman prices, bank cards, crypto wallets; regular user read-only", async () => {
  const adminDb = testEnv.authenticatedContext(ADMIN_UID, {
    email: ADMIN_EMAIL,
    email_verified: true,
  }).firestore();
  const aliceDb = testEnv.authenticatedContext(ALICE_UID, {
    email: "alice@example.com",
    email_verified: true,
  }).firestore();

  await assertSucceeds(adminDb.collection("app_config").doc("global").set(validConfigPayload(ADMIN_UID)));
  await assertSucceeds(aliceDb.collection("app_config").doc("global").get());

  // Regular user cannot flip subscriptionRequired or tamper with bankCards
  await assertFails(
    aliceDb.collection("app_config").doc("global").update({
      bankCards: [{ bankName: "هک", cardNumber: "1111", holderName: "Hacker" }],
      updatedBy: ALICE_UID,
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Email spoofing with email_verified: false must fail
  const spoofAdminDb = testEnv.authenticatedContext("spoof_uid", {
    email: ADMIN_EMAIL,
    email_verified: false,
  }).firestore();
  await assertFails(
    spoofAdminDb.collection("app_config").doc("global").update({
      subscriptionRequired: true,
      updatedBy: "spoof_uid",
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Admin updating prices and crypto wallets succeeds
  await assertSucceeds(
    adminDb.collection("app_config").doc("global").update({
      subscriptionRequired: true,
      monthlyPriceToman: 149000,
      yearlyPriceToman: 1190000,
      monthlyPriceLabel: "۱۴۹,۰۰۰ تومان / ماه",
      yearlyPriceLabel: "۱,۱۹۰,۰۰۰ تومان / سال",
      cryptoWallets: [
        { networkName: "TON", address: "UQD1234567890" },
      ],
      updatedBy: ADMIN_UID,
      updatedAt: new Date(Date.now() - 500),
    })
  );
});

test("Announcements: admin manages drafts & published; regular user only queries published", async () => {
  const adminDb = testEnv.authenticatedContext(ADMIN_UID, {
    email: ADMIN_EMAIL,
    email_verified: true,
  }).firestore();
  const aliceDb = testEnv.authenticatedContext(ALICE_UID, {
    email: "alice@example.com",
    email_verified: true,
  }).firestore();

  const now = new Date(Date.now() - 2000);
  await assertSucceeds(
    adminDb.collection("announcements").doc("mod_1").set({
      title: "حالت شب هوشمند",
      summary: "فعال‌سازی ساعات سکوت برای پل تماس",
      category: "FEATURE",
      actionLabel: "فعال‌سازی",
      actionValue: "https://eskytell.app/quiet",
      published: true,
      createdBy: ADMIN_UID,
      createdAt: now,
      updatedAt: now,
    })
  );
  await assertSucceeds(
    adminDb.collection("announcements").doc("mod_draft").set({
      title: "پیش‌نویس",
      summary: "هنوز منتشر نشده",
      category: "UPDATE",
      published: false,
      createdBy: ADMIN_UID,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Regular user filtered query for published succeeds
  await assertSucceeds(aliceDb.collection("announcements").where("published", "==", true).get());
  // Unfiltered list or draft get fails for regular user
  await assertFails(aliceDb.collection("announcements").get());
  await assertFails(aliceDb.collection("announcements").doc("mod_draft").get());
  // Regular user cannot create/update announcements
  await assertFails(
    aliceDb.collection("announcements").doc("mod_hack").set({
      title: "Hack",
      summary: "Fake",
      category: "FEATURE",
      published: true,
      createdBy: ALICE_UID,
      createdAt: now,
      updatedAt: now,
    })
  );
  // Admin can list all announcements including drafts
  await assertSucceeds(adminDb.collection("announcements").get());
});

test("User profiles: isolated per user, blocks self-subscription escalation, allows admin grant", async () => {
  const adminDb = testEnv.authenticatedContext(ADMIN_UID, {
    email: ADMIN_EMAIL,
    email_verified: true,
  }).firestore();
  const aliceDb = testEnv.authenticatedContext(ALICE_UID, {
    email: "alice@example.com",
    email_verified: true,
  }).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID, {
    email: "bob@example.com",
    email_verified: true,
  }).firestore();

  // Cannot create profile with active subscription directly
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).set({
      ...validUserPayload(ALICE_UID),
      hasActiveSubscription: true,
      subscriptionTier: "LIFETIME",
    })
  );

  // Valid free profile creation succeeds
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).set(validUserPayload(ALICE_UID)));

  // Shadow extra field rejected
  await assertFails(
    bobDb.collection("users").doc(BOB_UID).set({
      ...validUserPayload(BOB_UID, "bob@example.com"),
      isAdmin: true,
    })
  );

  // Alice can update her SIM labels
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      sim1Label: "خط کاری",
      sim2Label: "خط شخصی",
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Alice cannot self-grant subscription
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      hasActiveSubscription: true,
      subscriptionTier: "MONTHLY",
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Bob cannot read Alice's profile
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());

  // Admin can list users, grant subscription with expiry, and push renewal notification
  await assertSucceeds(adminDb.collection("users").get());
  await assertSucceeds(
    adminDb.collection("users").doc(ALICE_UID).update({
      hasActiveSubscription: true,
      subscriptionTier: "MONTHLY",
      subscriptionExpiresAtMillis: Date.now() + 30 * 86400000,
      updatedAt: new Date(Date.now() - 500),
    })
  );
  await assertSucceeds(
    adminDb.collection("users").doc(ALICE_UID).update({
      renewalNoticeMessage: "اشتراک شما تا ۳ روز دیگر منقضی می‌شود.",
      renewalNoticeAtMillis: Date.now(),
      updatedAt: new Date(Date.now() - 400),
    })
  );
  // Alice cannot spoof renewal notice or expiry on her own profile
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      subscriptionExpiresAtMillis: Date.now() + 999 * 86400000,
      updatedAt: new Date(Date.now() - 300),
    })
  );
});

test("Payment requests: user submits receipt/tracking code; cannot self-approve; admin approves & upgrades atomically", async () => {
  const adminDb = testEnv.authenticatedContext(ADMIN_UID, {
    email: ADMIN_EMAIL,
    email_verified: true,
  }).firestore();
  const aliceDb = testEnv.authenticatedContext(ALICE_UID, {
    email: "alice@example.com",
    email_verified: true,
  }).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID, {
    email: "bob@example.com",
    email_verified: true,
  }).firestore();

  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).set(validUserPayload(ALICE_UID)));

  // Cannot create pre-approved request
  await assertFails(
    aliceDb.collection("payment_requests").doc("req_bad_status").set(
      validPaymentRequestPayload(ALICE_UID, { status: "APPROVED" })
    )
  );

  // Cannot create empty request without trackingCode and without receiptImageBase64
  await assertFails(
    aliceDb.collection("payment_requests").doc("req_empty").set(
      validPaymentRequestPayload(ALICE_UID, { trackingCode: "", receiptImageBase64: "" })
    )
  );

  // Valid request with tracking code succeeds
  await assertSucceeds(
    aliceDb.collection("payment_requests").doc("req_1").set(validPaymentRequestPayload(ALICE_UID))
  );

  // Valid request with receipt image only (no trackingCode) succeeds
  await assertSucceeds(
    aliceDb.collection("payment_requests").doc("req_2").set(
      validPaymentRequestPayload(ALICE_UID, {
        requestedTier: "YEARLY",
        amountToman: 790000,
        paymentMethod: "CRYPTO",
        trackingCode: "",
        receiptImageBase64: "data:image/jpeg;base64,/9j/4AAQSkZJRg==",
      })
    )
  );

  // Alice can query her own requests; unfiltered list fails; Bob cannot read Alice's request
  await assertSucceeds(aliceDb.collection("payment_requests").where("userId", "==", ALICE_UID).get());
  await assertFails(aliceDb.collection("payment_requests").get());
  await assertFails(bobDb.collection("payment_requests").doc("req_1").get());

  // Alice cannot approve her own request
  await assertFails(
    aliceDb.collection("payment_requests").doc("req_1").update({
      status: "APPROVED",
      reviewedBy: ALICE_UID,
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Admin can list all requests and atomically approve + upgrade Alice's account
  await assertSucceeds(adminDb.collection("payment_requests").get());
  const batch = adminDb.batch();
  const now = new Date(Date.now() - 500);
  batch.update(adminDb.collection("payment_requests").doc("req_2"), {
    status: "APPROVED",
    adminNote: "تایید واریز سالانه",
    reviewedBy: ADMIN_UID,
    updatedAt: now,
  });
  batch.update(adminDb.collection("users").doc(ALICE_UID), {
    hasActiveSubscription: true,
    subscriptionTier: "YEARLY",
    updatedAt: now,
  });
  await assertSucceeds(batch.commit());
});
