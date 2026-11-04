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
exports.syncPublicProfile = void 0;
const functions = __importStar(require("firebase-functions"));
const admin = __importStar(require("firebase-admin"));
const firestore_1 = require("firebase-admin/firestore");
const db = admin.firestore();
/**
 * Cloud Function Trigger: syncPublicProfile
 * Listens to writes on users/{uid}.
 * Synchronizes public fields (name, photoUrl, rating, completedCount)
 * to publicProfiles/{uid} while strictly excluding phone numbers,
 * emergency contact numbers, and private preferences.
 * Matches SPEC.md Section A6 and A7.
 */
exports.syncPublicProfile = functions.firestore
    .document("users/{uid}")
    .onWrite(async (change, context) => {
    const uid = context.params.uid;
    // Handle document deletion: remove corresponding public profile
    if (!change.after.exists) {
        functions.logger.info(`User ${uid} deleted. Removing public profile.`);
        try {
            await db.collection("publicProfiles").doc(uid).delete();
        }
        catch (err) {
            functions.logger.error(`Error deleting public profile for ${uid}:`, err);
        }
        return;
    }
    const userData = change.after.data();
    if (!userData) {
        return;
    }
    // Only extract safe public fields per SPEC.md Section A6
    const publicProfileData = {
        name: typeof userData.name === "string" ? userData.name : "",
        photoUrl: typeof userData.photoUrl === "string" ? userData.photoUrl : "",
        rating: typeof userData.rating === "number" ? userData.rating : 0.0,
        completedCount: typeof userData.completedCount === "number" ? userData.completedCount : 0,
        updatedAt: firestore_1.FieldValue.serverTimestamp(),
    };
    functions.logger.info(`Syncing public profile for user ${uid}`);
    try {
        await db.collection("publicProfiles").doc(uid).set(publicProfileData, { merge: true });
    }
    catch (err) {
        functions.logger.error(`Failed to update public profile for ${uid}:`, err);
        throw err;
    }
});
//# sourceMappingURL=profile.js.map