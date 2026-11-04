import * as functions from "firebase-functions";
import * as admin from "firebase-admin";
import { FieldValue } from "firebase-admin/firestore";

const db = admin.firestore();

/**
 * Cloud Function Trigger: syncPublicProfile
 * Listens to writes on users/{uid}.
 * Synchronizes public fields (name, photoUrl, rating, completedCount)
 * to publicProfiles/{uid} while strictly excluding phone numbers,
 * emergency contact numbers, and private preferences.
 * Matches SPEC.md Section A6 and A7.
 */
export const syncPublicProfile = functions.firestore
  .document("users/{uid}")
  .onWrite(async (change, context) => {
    const uid = context.params.uid;

    // Handle document deletion: remove corresponding public profile
    if (!change.after.exists) {
      functions.logger.info(`User ${uid} deleted. Removing public profile.`);
      try {
        await db.collection("publicProfiles").doc(uid).delete();
      } catch (err) {
        functions.logger.error(`Error deleting public profile for ${uid}:`, err);
      }
      return;
    }

    const userData = change.after.data();
    if (!userData) {
      return;
    }

    // Only extract safe public fields per SPEC.md Section A6
    const publicProfileData: { [key: string]: any } = {
      name: typeof userData.name === "string" ? userData.name : "",
      photoUrl: typeof userData.photoUrl === "string" ? userData.photoUrl : "",
      rating: typeof userData.rating === "number" ? userData.rating : 0.0,
      completedCount: typeof userData.completedCount === "number" ? userData.completedCount : 0,
      updatedAt: FieldValue.serverTimestamp(),
    };

    functions.logger.info(`Syncing public profile for user ${uid}`);

    try {
      await db.collection("publicProfiles").doc(uid).set(publicProfileData, { merge: true });
    } catch (err) {
      functions.logger.error(`Failed to update public profile for ${uid}:`, err);
      throw err;
    }
  });

