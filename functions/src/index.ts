import * as admin from "firebase-admin";

// Initialize Firebase Admin once at module load
if (!admin.apps.length) {
  admin.initializeApp();
}

// Export functions from modules
export { syncPublicProfile } from "./profile";
export {
  createTopUpOrder,
  verifyTopUp,
  razorpayWebhook,
} from "./topup";
export { postTask, acceptTask } from "./tasks";
export {
  approveTask,
  autoReleaseTasks,
  cancelTask,
  getCounterpartContact,
  onTaskStatusChanged,
  processWithdrawal,
  rateWorker,
  requestWithdrawal,
  startTask,
  submitTaskProof,
  verifyTaskProofQr,
} from "./lifecycle";
