const test = require("node:test");
const assert = require("node:assert/strict");
const admin = require("firebase-admin");
if (!admin.apps.length) {
  admin.initializeApp({ projectId: "rozkaam-unit-tests" });
}
const {
  createProofSubmissionUpdate,
  validateRating,
  validateTaskProofQr,
  validateUpiId
} = require("../lib/lifecycle");

const proofPath = "task-proofs/task_123/worker_1/123e4567-e89b-12d3-a456-426614174000.jpg";

test("submitProof accepts assigned worker started task and persists review fields", () => {
  assert.deepEqual(
    createProofSubmissionUpdate(
      "worker_1",
      "task_123",
      { workerId: "worker_1", status: "started" },
      proofPath,
      "  Work completed as requested.  "
    ),
    {
      status: "submitted",
      workerId: "worker_1",
      proofPath,
      proofDescription: "Work completed as requested."
    }
  );
});

test("submitProof rejects wrong worker, wrong status and another storage path", () => {
  assert.throws(
    () => createProofSubmissionUpdate(
      "worker_2",
      "task_123",
      { workerId: "worker_1", status: "started" },
      "task-proofs/task_123/worker_2/123e4567-e89b-12d3-a456-426614174000.jpg",
      "Work completed as requested."
    ),
    /Only the assigned worker/
  );
  assert.throws(
    () => createProofSubmissionUpdate(
      "worker_1",
      "task_123",
      { workerId: "worker_1", status: "accepted" },
      proofPath,
      "Work completed as requested."
    ),
    /must be started/
  );
  assert.throws(
    () => createProofSubmissionUpdate(
      "worker_1",
      "task_123",
      { workerId: "worker_1", status: "started" },
      "task-proofs/task_other/worker_1/123e4567-e89b-12d3-a456-426614174000.jpg",
      "Work completed as requested."
    ),
    /assigned task's proof folder/
  );
});

test("rating and UPI inputs are validated and normalized", () => {
  assert.equal(validateRating(5, "  Great work  "), "Great work");
  assert.throws(() => validateRating(0, ""), /between 1 and 5/);
  assert.equal(validateUpiId(" worker.name@upi "), "worker.name@upi");
  assert.throws(() => validateUpiId("not-a-upi"), /valid UPI ID/);
});

test("proof QR verification is task-specific, assigned-worker-only, and one-time", () => {
  const secret = { proofQrToken: "a".repeat(64), proofQrFailedAttempts: 0 };
  const submittedTask = { workerId: "worker_1", status: "submitted" };

  assert.doesNotThrow(() => validateTaskProofQr(
    "worker_1",
    "task_123",
    "task_123",
    secret.proofQrToken,
    submittedTask,
    secret
  ));
  assert.throws(() => validateTaskProofQr(
    "worker_2", "task_123", "task_123", secret.proofQrToken, submittedTask, secret
  ), /assigned worker/);
  assert.throws(() => validateTaskProofQr(
    "worker_1", "task_123", "task_other", secret.proofQrToken, submittedTask, secret
  ), /different task/);
  assert.throws(() => validateTaskProofQr(
    "worker_1", "task_123", "task_123", "b".repeat(64), submittedTask, secret
  ), /QR code is invalid/);
  assert.throws(() => validateTaskProofQr(
    "worker_1", "task_123", "task_123", secret.proofQrToken,
    { ...submittedTask, proofQrVerifiedAt: new Date() },
    secret
  ), /already been used/);
});
