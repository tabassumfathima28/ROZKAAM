"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.encodeGeohash = encodeGeohash;
/**
 * Standard geohash base32 encoding for geospatial point queries.
 * Matches geofire-android-common / standard geohash algorithm.
 */
const BASE32 = "0123456789bcdefghjkmnpqrstuvwxyz";
function encodeGeohash(latitude, longitude, precision = 9) {
    let latMin = -90;
    let latMax = 90;
    let lngMin = -180;
    let lngMax = 180;
    let hash = "";
    let isEven = true;
    let bit = 0;
    let ch = 0;
    while (hash.length < precision) {
        if (isEven) {
            const mid = (lngMin + lngMax) / 2;
            if (longitude >= mid) {
                ch |= (1 << (4 - bit));
                lngMin = mid;
            }
            else {
                lngMax = mid;
            }
        }
        else {
            const mid = (latMin + latMax) / 2;
            if (latitude >= mid) {
                ch |= (1 << (4 - bit));
                latMin = mid;
            }
            else {
                latMax = mid;
            }
        }
        isEven = !isEven;
        if (bit < 4) {
            bit++;
        }
        else {
            hash += BASE32[ch];
            bit = 0;
            ch = 0;
        }
    }
    return hash;
}
//# sourceMappingURL=geohash.js.map