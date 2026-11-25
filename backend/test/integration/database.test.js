import test, { before, after, beforeEach } from 'node:test';
import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';
import { initializeTestEnvironment, assertSucceeds, assertFails } from '@firebase/rules-unit-testing';
import { ref, set, update, get, query, orderByChild, limitToLast, serverTimestamp, onValue } from 'firebase/database';
let env;
const profile = (name='Vecino Uno',communityId='comunidad01',role='neighbor') => ({name,home:'311A',communityId,role});
const db = uid => uid ? env.authenticatedContext(uid).database('https://demo-comunidad-default-rtdb.firebaseio.com') : env.unauthenticatedContext().database('https://demo-comunidad-default-rtdb.firebaseio.com');
const path = 'communities/comunidad01/';
const feed = (uid, kind) => query(ref(db(uid),path+kind),orderByChild('createdAt'),limitToLast(50));
const message = (uid='one',name='Vecino Uno') => ({uid,name,home:'311A',text:'Hola',createdAt:serverTimestamp()});
const notice = () => ({authorUid:'admin',authorName:'Administrador',title:'Aviso',body:'Prueba',createdAt:serverTimestamp()});
before(async()=>{
  env=await initializeTestEnvironment({projectId:'demo-comunidad',database:{host:'127.0.0.1',port:9000,rules:await readFile('../database.rules.json','utf8')}});
});
after(async()=>{await env?.cleanup();});
beforeEach(async()=>{
  await env.clearDatabase();await env.withSecurityRulesDisabled(async ctx=>{
    await set(ref(ctx.database('https://demo-comunidad-default-rtdb.firebaseio.com'),'users'),{one:profile(),two:profile('Vecino Dos'),admin:profile('Administrador','comunidad01','admin'),foreign:profile('Ajeno','otra')});
  });
});
test('new user may create own neighbor profile and share housing',async()=>{
  await assertSucceeds(set(ref(db('new'),'users/new'),profile('Nuevo')));
  await assertSucceeds(set(ref(db('another'),'users/another'),profile('Otro')));
});
test('self-registration as admin and foreign community is denied',async()=>{
  await assertFails(set(ref(db('new'),'users/new'),profile('Nuevo','comunidad01','admin')));
  await assertFails(set(ref(db('new'),'users/new'),profile('Nuevo','otra')));
});
test('name is editable but role, community and home are immutable',async()=>{
  await assertSucceeds(update(ref(db('one'),'users/one'),{name:'Nombre nuevo'}));
  for(const patch of [{role:'admin'},{communityId:'otra'},{home:'999'},{extra:true}]) await assertFails(update(ref(db('one'),'users/one'),patch));
  await assertFails(set(ref(db('one'),'users/one'),null));
});
test('private profiles and device tokens cannot be read by neighbors',async()=>{
  await assertFails(get(ref(db('one'),'users/two')));await assertFails(get(ref(db('one'),'users')));
  await assertSucceeds(set(ref(db('one'),'deviceTokens/one/install'),{token:'demo-token',updatedAt:serverTimestamp()}));
  await assertFails(get(ref(db('two'),'deviceTokens/one')));
  await assertFails(set(ref(db('two'),'deviceTokens/one/install'),{token:'stolen',updatedAt:serverTimestamp()}));
  await assertSucceeds(set(ref(db('one'),'deviceTokens/one/install'),null));
});
test('neighbor cannot publish announcement directly or change role',async()=>{
  await assertFails(set(ref(db('one'),path+'announcements/n1'),notice()));
  await assertFails(update(ref(db('one'),'users/one'),{role:'admin'}));
});
test('admin creates immutable announcement, both roles read and open detail',async()=>{
  await assertSucceeds(set(ref(db('admin'),path+'announcements/n1'),notice()));
  await assertSucceeds(get(feed('one','announcements')));
  const snap=await assertSucceeds(get(ref(db('two'),path+'announcements/n1')));
  assert.equal(snap.val().title,'Aviso');
  await assertFails(update(ref(db('admin'),path+'announcements/n1'),{title:'Cambiar'}));
});
test('message identities and time cannot be forged',async()=>{
  await assertSucceeds(set(ref(db('one'),path+'messages/m1'),message()));
  for(const patch of [{uid:'two'},{name:'Falso'},{home:'99'},{createdAt:1},{extra:'oops'},{text:''},{text:'   '},{text:'x'.repeat(1001)}])
    await assertFails(set(ref(db('one'),path+'messages/m2'),{...message(),...patch}));
});
test('two Firebase clients receive message live without refresh',async()=>{
  const result=new Promise((resolve,reject)=>{
    let stop;const timeout=setTimeout(()=>{stop?.();reject(new Error('live listener timeout'));},8000);
    stop=onValue(feed('two','messages'),snapshot=>{if(snapshot.hasChild('live')){clearTimeout(timeout);stop();resolve(snapshot.child('live').val());}},reject);
  });
  await set(ref(db('one'),path+'messages/live'),message());
  assert.equal((await result).home,'311A');
});
test('product publication syncs to second client with valid owner and price',async()=>{
  const product={ownerUid:'one',ownerName:'Vecino Uno',home:'311A',name:'Pan',priceCLP:1500,createdAt:serverTimestamp()};
  await assertSucceeds(set(ref(db('one'),path+'products/p1'),product));
  assert.equal((await get(feed('two','products'))).child('p1/priceCLP').val(),1500);
  for(const patch of [{priceCLP:-1},{priceCLP:1.5},{priceCLP:1000000001},{name:''},{ownerUid:'two'},{ownerName:'Falso'}])
    await assertFails(set(ref(db('one'),path+'products/p2'),{...product,...patch}));
});
test('unauthenticated and foreign community access is denied for every feed',async()=>{
  for(const kind of ['messages','announcements','products']) {
    await assertFails(get(feed(null,kind)));await assertFails(get(feed('foreign',kind)));
  }
  await assertFails(set(ref(db(null),path+'messages/x'),message()));
  await assertFails(set(ref(db('foreign'),path+'messages/x'),message('foreign','Ajeno')));
  await assertFails(get(ref(db('foreign'),path+'announcements/x')));
});
test('unbounded collection queries are denied; bounded history is 50',async()=>{
  await assertFails(get(ref(db('one'),path+'messages')));
  await assertFails(get(query(ref(db('one'),path+'messages'),orderByChild('createdAt'),limitToLast(51))));
  await env.withSecurityRulesDisabled(async ctx=>{
    const posts=Object.fromEntries(Array.from({length:55},(_,i)=>['m'+i,{...message(),createdAt:i}]));
    await set(ref(ctx.database('https://demo-comunidad-default-rtdb.firebaseio.com'),path+'messages'),posts);
  });
  assert.equal((await get(feed('one','messages'))).size,50);
});
test('invalid and incomplete profiles are rejected',async()=>{
  for(const patch of [{name:''},{name:'   '},{home:'311/A'},{home:null},{role:'owner'},{communityId:null},{other:true}])
    await assertFails(set(ref(db('new'),'users/new'),{...profile(),...patch}));
});
