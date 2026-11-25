import { createHash } from 'node:crypto';
export function recipients(users, devices, communityId) {
  const seen = new Set();
  return Object.entries(users ?? {}).flatMap(([uid, user]) => {
    if (user.communityId !== communityId || !['neighbor', 'admin'].includes(user.role)) return [];
    return Object.values(devices?.[uid] ?? {}).flatMap(device => {
      if (typeof device.token !== 'string' || !device.token || seen.has(device.token)) return [];
      seen.add(device.token);
      return [{ uid, token: device.token, key: createHash('sha256').update(device.token).digest('hex') }];
    });
  });
}
export function validAnnouncement(value, author, communityId) {
  return !!value && author?.role === 'admin' && author.communityId === communityId
    && typeof value.authorUid === 'string' && typeof value.title === 'string'
    && value.title.trim().length > 0 && value.title.length <= 120
    && typeof value.body === 'string' && value.body.trim().length > 0 && value.body.length <= 4000
    && Number.isFinite(value.createdAt);
}
export function envelope(communityId, announcementId, token) {
  return { token, data: { communityId, announcementId },
    android: { priority: 'high', ttl: 3600000, collapseKey: announcementId } };
}
// Once a send is started, a process crash leaves its outcome uncertain. Never
// automatically resend uncertain attempts: FCM has no exactly-once idempotency API.
export async function deliverOne({ state, key, persist, send }) {
  if (['sent', 'sending', 'invalid'].includes(state[key]?.status)) return 'skipped';
  state[key] = { status: 'sending', at: Date.now() };
  await persist();
  try {
    await send();
    state[key] = { status: 'sent', at: Date.now() };
    await persist();
    return 'sent';
  } catch (error) {
    const invalid = ['messaging/registration-token-not-registered', 'messaging/invalid-registration-token'].includes(error.code);
    // A transport error may happen after FCM accepted the request. Keep uncertain.
    state[key] = { status: invalid ? 'invalid' : 'sending', at: Date.now() };
    await persist();
    return invalid ? 'invalid' : 'uncertain';
  }
}
