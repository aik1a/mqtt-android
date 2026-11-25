// Creates synthetic demo accounts in local emulators ONLY. No production seed.
import { initializeApp } from 'firebase-admin/app';
import { getAuth } from 'firebase-admin/auth';
import { getDatabase } from 'firebase-admin/database';
import { randomBytes } from 'node:crypto';
import { mkdir,writeFile } from 'node:fs/promises';
if(process.env.FIREBASE_AUTH_EMULATOR_HOST!=='127.0.0.1:9099' || process.env.FIREBASE_DATABASE_EMULATOR_HOST!=='127.0.0.1:9000') throw new Error('Local emulator endpoints required.');
initializeApp({projectId:'demo-comunidad',databaseURL:'https://demo-comunidad-default-rtdb.firebaseio.com'});
const accounts=[];
for(const [uid,name,role] of [['demo-neighbor','Vecino Demo','neighbor'],['demo-admin','Administrador Demo','admin']]) {
 const password=randomBytes(15).toString('base64url'); const email=uid+'@example.test';
 try {await getAuth().createUser({uid,email,password});} catch(e) {if(e.code!=='auth/uid-already-exists' && e.code!=='auth/email-already-exists') throw e; await getAuth().updateUser(uid,{password});}
 await getDatabase().ref('users/'+uid).set({name,home:'311A',communityId:'comunidad01',role});
 accounts.push({email,password,role});
}
await mkdir('.emulator-data',{recursive:true});
await writeFile('.emulator-data/credentials.json',JSON.stringify(accounts,null,2),{mode:0o600});
console.log('Two synthetic accounts created. Credentials saved privately in .emulator-data/credentials.json.');
getDatabase().goOffline();
