import test from 'node:test';
import assert from 'node:assert/strict';
import { recipients, validAnnouncement, envelope, deliverOne } from '../delivery.js';
test('recipients isolate community, roles and duplicate tokens', () => {
  const actual = recipients({ a:{communityId:'one',role:'neighbor'},b:{communityId:'two',role:'admin'},c:{communityId:'one',role:'admin'},d:{communityId:'one',role:'invalid'} },
    { a:{x:{token:'abc'},y:{token:'abc'}}, b:{x:{token:'foreign'}},c:{x:{token:'def'}},d:{x:{token:'bad'}} },'one');
  assert.deepEqual(actual.map(x => x.token), ['abc','def']);
});
test('announcement requires authorized administrator and complete content', () => {
  const a={authorUid:'a',title:'Aviso',body:'Prueba',createdAt:100};
  assert.ok(validAnnouncement(a,{role:'admin',communityId:'one'},'one'));
  assert.equal(validAnnouncement(a,{role:'neighbor',communityId:'one'},'one'),false);
  assert.equal(validAnnouncement(a,{role:'admin',communityId:'two'},'one'),false);
  assert.equal(validAnnouncement({...a,body:''},{role:'admin',communityId:'one'},'one'),false);
});
test('data-only notification carries detail route without exposing body', () => {
  const e=envelope('one','notice1','abc');assert.deepEqual(e.data,{communityId:'one',announcementId:'notice1'});assert.equal(e.notification,undefined);
});
test('successful delivery is persisted before/after and never repeated', async () => {
  const state={};let sends=0;const saved=[];
  const args={state,key:'notice/token',persist:async()=>saved.push(state['notice/token'].status),send:async()=>{sends++;}};
  assert.equal(await deliverOne(args),'sent');assert.equal(await deliverOne(args),'skipped');
  assert.deepEqual(saved,['sending','sent']);assert.equal(sends,1);
});
test('uncertain transport outcomes never automatically resend', async () => {
  const state={};let sends=0;
  const args={state,key:'n/t',persist:async()=>{},send:async()=>{sends++;throw new Error('timeout');}};
  assert.equal(await deliverOne(args),'uncertain');assert.equal(await deliverOne(args),'skipped');assert.equal(sends,1);
});
test('invalid token is marked permanently', async () => {
  const state={};const args={state,key:'n/t',persist:async()=>{},send:async()=>{throw Object.assign(new Error(),{code:'messaging/registration-token-not-registered'});}};
  assert.equal(await deliverOne(args),'invalid');assert.equal(await deliverOne(args),'skipped');
});
