A1.What ROZKAAM is
ROZKAAM is a same-day micro-task app for India. A poster creates a small task with a
price. A nearby worker who needs money today accepts it, arrives, enters an OTP from the
poster, does the work, uploads a photo as proof, and gets paid from escrow when the
poster approves. One account has two modes, Worker and Poster, with a switch in Profile.
A2. Non-negotiable rules (this must be a real app)
No hardcoded data. No fake users, tasks, prices, balances, ratings, names, photos or
addresses in code. Every list reads live from Firestore and needs a loading state, an
empty state and an error state. The only literals allowed are UI labels, design tokens
and welcome carousel copy.
ROZKAAM: Build Spec and Milestone Prompts
Page 1 of 11
Categories come from Firestore ( config/categories , created by the owner in the
Firebase console). The app never ships a category list.
Keys. The Maps API key and Razorpay Key ID live in local.properties and are read
through BuildConfig and manifest placeholders. The Razorpay Key Secret exists only
on the backend as a Firebase secret. local.properties and google-services.json
must not go to a public repo.
The app never changes money. All wallet and escrow changes happen in Cloud
Functions using Firestore transactions, recorded in an append-only transactions
ledger.
Everything real: Google Maps SDK, Firebase phone Auth, Razorpay Checkout (test
keys), Firebase Storage. No mocks, no stub repositories, no lorem text.
A3. Stack
Kotlin, Jetpack Compose, Material 3, single activity, MVVM with ViewModel and
StateFlow, Navigation Compose with two nested graphs (worker, poster).
Package com.rozkaam.app , minSdk 26, latest stable compileSdk and targetSdk, Gradle
Kotlin DSL with a version catalog.
Firebase: Auth (phone), Firestore, Storage, Cloud Messaging, Cloud Functions
(TypeScript, Node 20).
Google Maps SDK forAndroid with maps-compose, Android Geocoder for address
text, geofire-android-common for geohash queries, and an intent to open Google Maps
for navigation. No Directions API and no route lines.
RazorpayAndroid Checkout SDK in test mode.
Coil for images. Google Fonts through ui-text-google-fonts: Poppins for headings,
Nunito for body text.
A4. Design tokens
Token Value Use
Primary /
text
Old Burgundy #43302E Buttons, headings, body text
Highlight Buttermilk #FFF1B5 Cards, highlights, icon tiles
Background Light tint of Buttermilk, about
#FFFAE6
Screen background
ROZKAAM: Build Spec and Milestone Prompts
Page 2 of 11
Cards use a 24dp corner radius, buttons are pill-shaped, tap targets are at least 48dp,
body text is at least 14sp. Never put Pastel Blue text on Buttermilk (low contrast). The logo
files are in app/src/main/res once I provide them.
A5. Screens and what each contains
Before login
1. Splash with the logo.
2. Welcome carousel, 3 slides (Find nearby tasks, Earn today, Get paid fast), then Get
Started.
3. Login in two steps: +91 phone number and Send OTP, then the OTP step. Firebase
sends a 6-digit code, so use 6 boxes (the mockup shows 4), with a 30 second resend
timer.
4. Role selection (I want to work / I want to post tasks) with a note that you can switch
anytime, then name and emergency contact.
Worker mode (bottom bar: Home, Earnings, Tasks, Profile)
Home: greeting with the real name, bell with unread count, I'm free now toggle with
radius, today's earnings vs. daily goal (computed from the ledger), Nearby Tasks / Map
toggle, task cards (category icon, title, price, distance, time).
Task details: map with marker and distance, Open in Google Maps, description, poster
name and rating, Accept, Contact locked until accepted.
Active job: step tracker (Accepted, Arrived and entered OTP, Working, Photo proof,
Paid), 4-digit OTP entry, SOS, Contact, Open in Google Maps.
Upload photo proof: camera or gallery, preview, upload to Storage, Submit.
Job completed: amount credited and new balance.
Earnings: wallet balance, this week (earned, tasks, rating), history from the ledger,
Withdraw.
Withdraw: UPI ID, amount, confirm.
Token Value Use
Secondary Pastel Blue #C1DBE8 Chips, info cards, secondary
buttons
Success Muted green #4F8A5B Success states only
Error Muted red #B5473A Errors and SOS only
ROZKAAM: Build Spec and Milestone Prompts
Page 3 of 11
Tasks tab: Active and Completed.
Poster mode (bottom bar: Home, Tasks, Wallet, Profile)
Home: greeting, summary cards (Active, Awaiting approval, Completed), Post a Task,
recent tasks.
Post a Task, 3 steps: (1) what: category from config, short description, (2) where and
when: map with draggable pin, address from Geocoder, use my current location, Now /
Today / Schedule, (3) price and pay: price input, summary, wallet balance, note that
money is held until approval, Post and Pay from wallet, with an insufficient balance
state that links to Add money.
Task detail: content depends on status. While accepted: worker card and a large OTP
card (share it when the worker arrives), Contact enabled, Cancel with the refund rule.
When submitted: proof photo and Approve and Release. After completion: Payment
released and Rate this worker.
Tasks tab: filters Open, Active, Awaiting approval, Completed.
Wallet: available balance, held in escrow, Add money, history.
Add money: amount chips (from config/app ), Razorpay Checkout, success state.
Shared: Profile (avatar, name, rating, tasks completed, mode switch, emergency contact,
language, sign out), Notifications list plus push notifications, SOS (sends an SMS with a
Google Maps link to the emergency contact).
A6. Data model(Firestore)
All money is stored in paise as integers. The UI shows rupees.
users/{uid} : name, phone, photoUrl, activeMode, emergencyContact, available,
radiusKm, dailyGoal, rating, ratingCount, completedCount, fcmToken, createdAt.
Readable only by the owner. The client may write only name, photoUrl, activeMode,
emergencyContact, available, radiusKm, dailyGoal, fcmToken.
publicProfiles/{uid} : name, photoUrl, rating, completedCount. Readable by signedin users, maintained by a Cloud Function trigger. This keeps phone numbers private.
wallets/{uid} : available, held. Owner can read. Only Functions can write.
transactions/{id} : uid, type (topup, hold, release, refund, withdrawal), amount,
taskId, razorpayPaymentId, createdAt. Owner can read. Functions only.
tasks/{id} : title, description, category, price, lat, lng, geohash, address, posterId,
workerId, status (open, accepted, started, submitted, completed, cancelled),
scheduledFor, proofPath, timestamps, rated. Signed-in users can read. Functions only
ROZKAAM: Build Spec and Milestone Prompts
Page 4 of 11
write.
taskSecrets/{taskId} : otp, failedAttempts. Only the poster can read. Functions only
write.
payments/{razorpayOrderId} , withdrawals/{id} , reviews/{id} ,
notifications/{uid}/items/{id} .
config/categories (list of id, label, icon) and config/app (quickAmounts, minPrice,
maxPrice, arrivalRadiusMeters, autoReleaseHours).
A7. Cloud Functions
Callable unless stated. Every function checks request.auth and validates inputs.
createTopUpOrder(amount) : creates a Razorpay order through the Orders API and
saves a payments doc.
verifyTopUp(orderId, paymentId, signature) : recomputes HMAC-SHA256 of
orderId|paymentId with the Key Secret, compares in constant time, then credits the
wallet exactly once (idempotent) and writes a ledger entry. Also add an HTTP webhook
for order.paid that verifies the webhook signature, as a backup.
postTask(data) : validates price bounds from config/app , checks
wallet.available >= price , and in one transaction moves the price to held ,
creates the task (status open, geohash), creates taskSecrets with a random 4-digit
OTP, and writes a hold ledger entry.
acceptTask(taskId) : transaction, only if status is open and the caller is not the
poster.
startTask(taskId, otp, lat, lng) : caller must be the worker and status accepted.
Checks distance to the task is within arrivalRadiusMeters , compares the OTP,
counts failed attempts, and locks after 5 wrong tries.
submitProof(taskId, storagePath) : worker only, path must match
tasks/{taskId}/ in Storage.
approveTask(taskId) : poster only, submitted to completed, moves held money to the
worker's available balance, writes the release entry, increments completedCount.
cancelTask(taskId) : poster only while open or accepted, refunds held to available.
autoReleaseTasks (scheduled hourly): approves tasks that have been submitted
longer than autoReleaseHours .
rateWorker(taskId, stars, comment) : poster only, after completion, once per task,
updates the aggregate in a transaction.
ROZKAAM: Build Spec and Milestone Prompts
Page 5 of 11
requestWithdrawal(amount, upiId) : validates, deducts the balance, writes a
withdrawal ledger entry and a withdrawals doc (see A9).
getCounterpartContact(taskId) : returns the other party's phone number only if the
task is accepted or later and the caller is the poster or worker.
Triggers: new task notifies nearby workers who are available (geohash and radius), and
status changes notify both sides through FCM and the notifications collection.
A8. Security rules (write them and testthem)
Default deny everywhere. Write each rule to match A6.
Clients cannot write wallets , transactions , tasks , taskSecrets or payments .
Storage: proof photos are writable only by the assigned worker, only JPEG or PNG, at
most 5 MB, and readable only by the poster and that worker.
Add rules unit tests with the Firebase emulator.
A9.Whatis real in payments
Add money: a real Razorpay Checkout flow, with the order created on the server and
the signature verified on the server. With test keys, no real money moves.
Escrow: a Firestore ledger with available and held balances. It simulates an escrow
account. Holding real customer money is regulated in India, so say this honestly in the
viva.
Withdraw: records the request and the ledger entry, and a test-mode function marks it
processed. Real payouts need RazorpayX or another payout provider with business
verification.
A10. Maps and location
Show tasks with the Maps SDK and markers. Nearby search uses geohash ranges, then an
exact distance filter. Handle the location permission with a rationale screen and a graceful
fallback. Navigation opens the Google Maps app through an intent.
A11. Quality bar
Loading, empty and error states everywhere. A clear offline message. No crashes when a
permission is denied. Strings in strings.xml . ProGuard rules for Razorpay. Short,
readable code with comments where the logic is not obvious
