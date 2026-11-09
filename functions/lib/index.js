"use strict";
var __createBinding = (this && this.__createBinding) || (Object.create ? (function(o, m, k, k2) {
    if (k2 === undefined) k2 = k;
    var desc = Object.getOwnPropertyDescriptor(m, k);
    if (!desc || ("get" in desc ? !m.__esModule : desc.writable || desc.configurable)) {
      desc = { enumerable: true, get: function() { return m[k]; } };
    }
    Object.defineProperty(o, k2, desc);
}) : (function(o, m, k, k2) {
    if (k2 === undefined) k2 = k;
    o[k2] = m[k];
}));
var __setModuleDefault = (this && this.__setModuleDefault) || (Object.create ? (function(o, v) {
    Object.defineProperty(o, "default", { enumerable: true, value: v });
}) : function(o, v) {
    o["default"] = v;
});
var __importStar = (this && this.__importStar) || (function () {
    var ownKeys = function(o) {
        ownKeys = Object.getOwnPropertyNames || function (o) {
            var ar = [];
            for (var k in o) if (Object.prototype.hasOwnProperty.call(o, k)) ar[ar.length] = k;
            return ar;
        };
        return ownKeys(o);
    };
    return function (mod) {
        if (mod && mod.__esModule) return mod;
        var result = {};
        if (mod != null) for (var k = ownKeys(mod), i = 0; i < k.length; i++) if (k[i] !== "default") __createBinding(result, mod, k[i]);
        __setModuleDefault(result, mod);
        return result;
    };
})();
Object.defineProperty(exports, "__esModule", { value: true });
exports.verifyTaskProofQr = exports.submitTaskProof = exports.startTask = exports.requestWithdrawal = exports.rateWorker = exports.processWithdrawal = exports.onTaskStatusChanged = exports.getCounterpartContact = exports.cancelTask = exports.autoReleaseTasks = exports.approveTask = exports.acceptTask = exports.postTask = exports.razorpayWebhook = exports.verifyTopUp = exports.createTopUpOrder = exports.syncPublicProfile = void 0;
const admin = __importStar(require("firebase-admin"));
// Initialize Firebase Admin once at module load
if (!admin.apps.length) {
    admin.initializeApp();
}
// Export functions from modules
var profile_1 = require("./profile");
Object.defineProperty(exports, "syncPublicProfile", { enumerable: true, get: function () { return profile_1.syncPublicProfile; } });
var topup_1 = require("./topup");
Object.defineProperty(exports, "createTopUpOrder", { enumerable: true, get: function () { return topup_1.createTopUpOrder; } });
Object.defineProperty(exports, "verifyTopUp", { enumerable: true, get: function () { return topup_1.verifyTopUp; } });
Object.defineProperty(exports, "razorpayWebhook", { enumerable: true, get: function () { return topup_1.razorpayWebhook; } });
var tasks_1 = require("./tasks");
Object.defineProperty(exports, "postTask", { enumerable: true, get: function () { return tasks_1.postTask; } });
Object.defineProperty(exports, "acceptTask", { enumerable: true, get: function () { return tasks_1.acceptTask; } });
var lifecycle_1 = require("./lifecycle");
Object.defineProperty(exports, "approveTask", { enumerable: true, get: function () { return lifecycle_1.approveTask; } });
Object.defineProperty(exports, "autoReleaseTasks", { enumerable: true, get: function () { return lifecycle_1.autoReleaseTasks; } });
Object.defineProperty(exports, "cancelTask", { enumerable: true, get: function () { return lifecycle_1.cancelTask; } });
Object.defineProperty(exports, "getCounterpartContact", { enumerable: true, get: function () { return lifecycle_1.getCounterpartContact; } });
Object.defineProperty(exports, "onTaskStatusChanged", { enumerable: true, get: function () { return lifecycle_1.onTaskStatusChanged; } });
Object.defineProperty(exports, "processWithdrawal", { enumerable: true, get: function () { return lifecycle_1.processWithdrawal; } });
Object.defineProperty(exports, "rateWorker", { enumerable: true, get: function () { return lifecycle_1.rateWorker; } });
Object.defineProperty(exports, "requestWithdrawal", { enumerable: true, get: function () { return lifecycle_1.requestWithdrawal; } });
Object.defineProperty(exports, "startTask", { enumerable: true, get: function () { return lifecycle_1.startTask; } });
Object.defineProperty(exports, "submitTaskProof", { enumerable: true, get: function () { return lifecycle_1.submitTaskProof; } });
Object.defineProperty(exports, "verifyTaskProofQr", { enumerable: true, get: function () { return lifecycle_1.verifyTaskProofQr; } });
//# sourceMappingURL=index.js.map