import { initializeApp, applicationDefault } from 'firebase-admin/app';
import { getDatabase } from 'firebase-admin/database';
import { getMessaging } from 'firebase-admin/messaging';
import { readFile, writeFile, rename, mkdir, rmdir } from 'node:fs/promises';
import { resolve } from 'node:path';
import { recipients, validAnnouncement, envelope, deliverOne } from './delivery.js';

const databaseURL = process.env.FIREBASE_DATABASE_URL;
if (!databaseURL || !/^https:\/\/[^/]+\.(firebaseio\.com|firebasedatabase\.app)\/?$/.test(databaseURL))
  throw new Error('Set FIREBASE_DATABASE_URL to your own Realtime Database HTTPS URL.');
if (process.env.FIREBASE_DATABASE_EMULATOR_HOST) throw new Error('FCM sender is production only; do not mix emulator identities and real FCM.');
initializeApp({ credential: applicationDefault(), databaseURL });
const db = getDatabase();
const path = resolve(process.env.SENDER_STATE_PATH ?? 'notification-state.json');
// Exclusive local lock: one sender per state directory. After a crash, inspect
// uncertain entries before manually removing the stale lock directory.
await mkdir(path + '.lock');
let state;
try { state = JSON.parse(await readFile(path, 'utf8')); }
catch (error) { if (error.code !== 'ENOENT') throw error; state = { since: Date.now(), deliveries: {}, done: {} }; }
async function persist() {
  await writeFile(path + '.tmp', JSON.stringify(state), { mode: 0o600 });
  await rename(path + '.tmp', path);
}
await persist();
let queue = Promise.resolve();
let stopping = false;
async function processAnnouncement(cid, id, value) {
  const job = cid + '/' + id;
  if (state.done[job]) return;
  const author = (await db.ref('users/' + value.authorUid).get()).val();
  if (!validAnnouncement(value, author, cid)) return;
  const users = (await db.ref('users').get()).val();
  const devices = (await db.ref('deviceTokens').get()).val();
  for (const recipient of recipients(users, devices, cid)) {
    const key = job + '/' + recipient.key;
    // Membership and device ownership are checked again immediately before send.
    const member = (await db.ref('users/' + recipient.uid).get()).val();
    const currentDevices = (await db.ref('deviceTokens/' + recipient.uid).get()).val();
    if (member?.communityId !== cid || !Object.values(currentDevices ?? {}).some(d => d.token === recipient.token)) continue;
    const outcome = await deliverOne({ state: state.deliveries, key, persist,
      send: () => getMessaging().send(envelope(cid, id, recipient.token)) });
    if (outcome === 'uncertain') console.error('FCM outcome uncertain; inspect local state before any manual retry.');
  }
  state.done[job] = true;
  await persist();
}
const subscriptions = new Map();
function subscribe(cid) {
  if (subscriptions.has(cid)) return;
  const query = db.ref('communities/' + cid + '/announcements').orderByChild('createdAt').startAt(state.since);
  const callback = snapshot => {
    if (stopping) return;
    queue = queue.then(() => processAnnouncement(cid, snapshot.key, snapshot.val()))
      .catch(() => console.error('Sender could not process announcement; retry on restart with the same state file.'));
  };
  query.on('child_added', callback, () => console.error('Announcement subscription failed.'));
  subscriptions.set(cid, { query, callback });
}
// The project scope is one community. Other communities are test isolation only.
subscribe('comunidad01');
console.log('Announcement sender listening. Keep this process running during the demo.');
async function stop() {
  if (stopping) return;
  stopping = true;
  for (const { query, callback } of subscriptions.values()) query.off('child_added', callback);
  await queue;
  await db.goOffline();
  await rmdir(path + '.lock');
  process.exit(0);
}
process.on('SIGINT', stop);
process.on('SIGTERM', stop);
