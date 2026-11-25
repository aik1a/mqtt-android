import test from 'node:test';
import assert from 'node:assert/strict';
const base='http://127.0.0.1:9099/identitytoolkit.googleapis.com/v1/';
async function auth(method,data) {
  const response=await fetch(base+method+'?key=demo-key',{method:'POST',headers:{'content-type':'application/json'},body:JSON.stringify(data)});
  return {ok:response.ok,data:await response.json()};
}
test('Auth emulator: register, login, invalid password, reset request and profile by UID',async()=>{
  const email='demo-'+Date.now()+'@example.test',password='DemoPass123';
  const registered=await auth('accounts:signUp',{email,password,returnSecureToken:true});assert.ok(registered.ok);
  const login=await auth('accounts:signInWithPassword',{email,password,returnSecureToken:true});assert.ok(login.ok);assert.equal(login.data.localId,registered.data.localId);
  assert.equal((await auth('accounts:signInWithPassword',{email,password:'incorrect',returnSecureToken:true})).ok,false);
  assert.ok((await auth('accounts:sendOobCode',{email,requestType:'PASSWORD_RESET'})).ok);
  const uid=registered.data.localId;
  const response=await fetch('http://127.0.0.1:9000/users/'+uid+'.json?ns=demo-comunidad-default-rtdb&auth='+login.data.idToken,{method:'PUT',headers:{'content-type':'application/json'},body:JSON.stringify({name:'Demo',home:'311A',communityId:'comunidad01',role:'neighbor'})});
  assert.equal(response.ok,true);assert.equal((await response.json()).home,'311A');
});
