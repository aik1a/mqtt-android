package cl.aik1a.comunidadconectada.ui;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.*;
import android.view.*;
import android.widget.*;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.*;
import com.google.firebase.database.*;
import java.text.DateFormat;
import java.util.*;
import cl.aik1a.comunidadconectada.R;
import cl.aik1a.comunidadconectada.BuildConfig;
import cl.aik1a.comunidadconectada.CommunityApp;
import cl.aik1a.comunidadconectada.auth.*;
import cl.aik1a.comunidadconectada.data.*;
import cl.aik1a.comunidadconectada.model.Profile;
import cl.aik1a.comunidadconectada.notifications.DeviceTokens;

public class MainActivity extends Activity {
    private FirebaseAuth auth;
    private AuthRepository accounts;
    private CommunityRepository store;
    private Profile profile;
    private FrameLayout content;
    private TextView status;
    private Query watched;
    private ValueEventListener listener;
    private DatabaseReference connection;
    private ValueEventListener connectionListener;
    private boolean active, busy;
    private int screen;
    private String page="home", feed="messages", pendingId, pendingCommunity;
    private final ArrayList<DataSnapshot> items=new ArrayList<>();

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);setContentView(R.layout.activity_main);
        content=findViewById(R.id.content); status=findViewById(R.id.status);
        View root=findViewById(R.id.root);
        ViewCompat.setOnApplyWindowInsetsListener(root,(v,insets)-> {
            androidx.core.graphics.Insets bars=insets.getInsets(WindowInsetsCompat.Type.systemBars());
            int p=(int)(16*getResources().getDisplayMetrics().density);
            v.setPadding(p+bars.left,p+bars.top,p+bars.right,p+bars.bottom);return insets;
        });
        captureIntent(getIntent());
        if(state!=null) {pendingId=state.getString("announcementId",pendingId);pendingCommunity=state.getString("communityId",pendingCommunity);}
        if(!CommunityApp.configured) {status.setText("Falta configurar Firebase. Agrega google-services.json y recompila productionDebug, o usa el flavor emulator.");return;}
        auth=FirebaseAuth.getInstance();accounts=new AuthRepository();store=new CommunityRepository();
    }
    @Override protected void onStart() {
        super.onStart();active=true;
        if(auth!=null) {
            connection=FirebaseDatabase.getInstance().getReference(".info/connected");
            connectionListener=new ValueEventListener() {
                public void onDataChange(DataSnapshot s) {if(active && !Boolean.TRUE.equals(s.getValue(Boolean.class))) note("Sin conexión. Los envíos quedan pendientes; reconecta o reintenta sin duplicarlos.");}
                public void onCancelled(DatabaseError e) {note("No se pudo comprobar la conexión.");}
            };connection.addValueEventListener(connectionListener);
            if(auth.getCurrentUser()==null) login(); else loadProfile();
        }
    }
    @Override protected void onStop() {
        active=false;screen++;detach();
        if(connection!=null && connectionListener!=null) connection.removeEventListener(connectionListener);
        super.onStop();
    }
    @Override public void onSaveInstanceState(Bundle out) {
        super.onSaveInstanceState(out);out.putString("announcementId",pendingId);out.putString("communityId",pendingCommunity);
    }
    @Override protected void onNewIntent(Intent intent) {super.onNewIntent(intent);setIntent(intent);captureIntent(intent);if(profile!=null) route();}
    private void captureIntent(Intent intent) {
        if(intent!=null && intent.hasExtra("announcementId")) {
            String id=intent.getStringExtra("announcementId"),cid=intent.getStringExtra("communityId");
            if(id!=null && cid!=null && id.matches("[-A-Za-z0-9_]{1,128}") && cid.matches("[A-Za-z0-9_-]{1,64}")) {pendingId=id;pendingCommunity=cid;}
        }
    }
    private void detach() {if(watched!=null && listener!=null) watched.removeEventListener(listener);watched=null;listener=null;}
    private void show(int layout,String location) {detach();screen++;page=location;busy=false;content.removeAllViews();getLayoutInflater().inflate(layout,content,true);note("");}
    private String value(int id) {return ((EditText)findViewById(id)).getText().toString().trim();}
    private String password() {return ((EditText)findViewById(R.id.password)).getText().toString();}
    private void note(String message) {if(active) status.setText(message);}
    private boolean valid(String error) {if(error==null) return true;note(error);return false;}
    private void click(int id,Runnable action) {findViewById(id).setOnClickListener(v->{if(!busy) action.run();});}
    private void running(String text) {busy=true;note(text);}
    private boolean current(int generation,String uid) {return active && screen==generation && Objects.equals(uid,auth.getUid());}
    private String error(Exception ex) {
        if(ex instanceof IllegalArgumentException) return ex.getMessage();
        if(ex instanceof FirebaseAuthInvalidCredentialsException || ex instanceof FirebaseAuthInvalidUserException) return "Correo o contraseña incorrectos, o cuenta no disponible.";
        if(ex instanceof FirebaseAuthUserCollisionException) return "Ya existe una cuenta con ese correo. Inicia sesión o recupera la contraseña.";
        if(ex instanceof FirebaseAuthWeakPasswordException) return "La contraseña no cumple los requisitos.";
        return "No se pudo completar la operación. Revisa tu conexión y permisos e inténtalo de nuevo.";
    }
    private void login() {
        profile=null;show(R.layout.screen_auth,"login");
        click(R.id.login,()-> {
            String email=value(R.id.email),password=password();
            if(!valid(Validation.email(email)) || !valid(Validation.password(password))) return;
            running("Iniciando sesión…");
            auth.signInWithEmailAndPassword(email,password).addOnCompleteListener(this,t->{busy=false;if(t.isSuccessful()) loadProfile();else note(error(t.getException()));});
        });
        click(R.id.register,()->registration(false));
        click(R.id.recover,()-> {
            String email=value(R.id.email);if(!valid(Validation.email(email))) return;
            running("Solicitando recuperación…");
            auth.sendPasswordResetEmail(email).addOnCompleteListener(this,t->{busy=false;note(t.isSuccessful()?"Solicitud enviada. Revisa tu correo y la carpeta de spam.":error(t.getException()));});
        });
    }
    private void registration(boolean missingProfile) {
        show(R.layout.screen_register,"register");
        if(missingProfile) {
            findViewById(R.id.email).setVisibility(View.GONE);findViewById(R.id.password).setVisibility(View.GONE);
            ((Button)findViewById(R.id.submit)).setText("Completar perfil");note("Tu cuenta existe. Completa el perfil para acceder a la comunidad.");
        }
        click(R.id.back,()-> {if(missingProfile) {auth.signOut();login();} else login();});
        click(R.id.submit,()-> {
            String name=value(R.id.name),home=value(R.id.home);
            if(!valid(Validation.name(name)) || !valid(Validation.home(home))) return;
            if(!missingProfile && !valid(Validation.registration(name,home,value(R.id.email),password()))) return;
            running("Guardando cuenta y perfil…");
            Task<Void> task=missingProfile?accounts.completeProfile(name,home):accounts.register(name,home,value(R.id.email),password());
            task.addOnCompleteListener(this,t->{busy=false;if(t.isSuccessful()) loadProfile();else {
                String message=error(t.getException());
                if(auth.getCurrentUser()!=null) registration(true);
                note(message);
            }});
        });
    }
    private void loadProfile() {
        detach();int generation=++screen;String uid=auth.getUid();if(uid==null) {login();return;}
        running("Cargando perfil…");
        FirebaseDatabase.getInstance().getReference("users").child(uid).get().addOnCompleteListener(this,t->{
            if(!current(generation,uid)) return;busy=false;
            if(!t.isSuccessful()) {login();note(error(t.getException()));return;}
            profile=t.getResult().getValue(Profile.class);
            if(profile==null) {registration(true);return;}
            if(!profile.valid()) {login();note("Perfil incompleto. Solicita revisión al administrador.");return;}
            DeviceTokens.sync(this).addOnFailureListener(e->note("No se pudo registrar este dispositivo para notificaciones. Reintenta al volver al inicio."));
            if(!BuildConfig.USE_EMULATORS && Build.VERSION.SDK_INT>=33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)
                requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},10);
            route();
        });
    }
    private void route() {
        if(pendingId!=null) {
            if(!profile.communityId.equals(pendingCommunity)) {pendingId=null;pendingCommunity=null;home();note("El anuncio no pertenece a tu comunidad.");return;}
            String id=pendingId;pendingId=null;pendingCommunity=null;detail(id);
        } else home();
    }
    private void home() {
        show(R.layout.screen_home,"home");((TextView)findViewById(R.id.identity)).setText(profile.identity()+" · "+(profile.isAdmin()?"Administrador":"Vecino"));
        if(BuildConfig.USE_EMULATORS) note("Firebase local · FCM real deshabilitado");
        click(R.id.chat,()->feed("messages"));click(R.id.announcements,()->feed("announcements"));click(R.id.products,()->feed("products"));click(R.id.profile,this::editProfile);
        click(R.id.logout,()-> {
            running("Retirando dispositivo…");detach();
            DeviceTokens.remove(this).addOnCompleteListener(this,t->{
                busy=false;
                if(!t.isSuccessful()) {note("No se pudo retirar el dispositivo. Reintenta el cierre cuando tengas conexión.");return;}
                getSystemService(android.app.NotificationManager.class).cancelAll();auth.signOut();profile=null;pendingId=null;pendingCommunity=null;login();
            });
        });
    }
    private void editProfile() {
        show(R.layout.screen_profile,"profile");((EditText)findViewById(R.id.name)).setText(profile.name);
        ((TextView)findViewById(R.id.identity)).setText("Vivienda #"+profile.home+" · "+auth.getCurrentUser().getEmail());
        click(R.id.back,this::home);
        click(R.id.submit,()-> {
            String name=value(R.id.name);if(!valid(Validation.name(name))) return;running("Guardando nombre…");
            store.rename(auth.getUid(),name).addOnCompleteListener(this,t->{busy=false;if(t.isSuccessful()){profile.name=name;note("Nombre actualizado.");}else note(error(t.getException()));});
        });
    }
    private void feed(String kind) {
        feed=kind;show(R.layout.screen_feed,"feed");items.clear();
        boolean chat=kind.equals("messages"),products=kind.equals("products"),writable=!kind.equals("announcements")||profile.isAdmin();
        ((TextView)findViewById(R.id.feed_title)).setText(chat?"Chat general · últimos 50":products?"Productos · últimos 50":"Anuncios · últimos 50");
        findViewById(R.id.title).setVisibility(!chat && writable?View.VISIBLE:View.GONE);
        findViewById(R.id.body).setVisibility(!products && writable?View.VISIBLE:View.GONE);
        findViewById(R.id.price).setVisibility(products?View.VISIBLE:View.GONE);
        findViewById(R.id.send).setVisibility(writable?View.VISIBLE:View.GONE);
        click(R.id.back,this::home);click(R.id.retry,()->feed(kind));
        ArrayAdapter<String> adapter=new ArrayAdapter<>(this,android.R.layout.simple_list_item_1,new ArrayList<>());
        ListView list=findViewById(R.id.list);list.setAdapter(adapter);
        if(kind.equals("announcements")) list.setOnItemClickListener((p,v,position,id)->detail(items.get(position).getKey()));
        int generation=screen;String uid=auth.getUid();
        watched=store.channel(profile.communityId,kind).orderByChild("createdAt").limitToLast(50);
        listener=new ValueEventListener() {
            public void onDataChange(DataSnapshot snapshot) {
                if(!current(generation,uid)) return;items.clear();adapter.clear();
                for(DataSnapshot item:snapshot.getChildren()) {items.add(item);adapter.add(render(kind,item));}
                note(snapshot.exists()?"Actualizado. Los registros sin fecha están pendientes de confirmación.":"Aún no hay publicaciones.");
            }
            public void onCancelled(DatabaseError e) {if(current(generation,uid)) note("No se pudo cargar. Revisa conexión y permisos; pulsa Reintentar conexión.");}
        };watched.addValueEventListener(listener);
        click(R.id.send,()-> {
            Map<String,Object> data;
            try {data=store.post(profile,auth.getUid(),kind,value(R.id.title),value(R.id.body),value(R.id.price));}
            catch(IllegalArgumentException e) {note(e.getMessage());return;}
            running("Envío pendiente de confirmación del servidor…");
            observeSend(store.send(profile,uid,kind,data),generation,uid);
        });
        Task<Void> pending=store.pending(uid,kind);
        if(pending!=null) {running("Envío anterior pendiente de confirmación…");observeSend(pending,generation,uid);}
    }
    private void observeSend(Task<Void> task,int generation,String uid) {
        task.addOnCompleteListener(this,t->{
            if(!current(generation,uid)) return;busy=false;
            if(t.isSuccessful()) {
                ((EditText)findViewById(R.id.title)).setText("");((EditText)findViewById(R.id.body)).setText("");((EditText)findViewById(R.id.price)).setText("");note("Enviado y confirmado por el servidor.");
            } else note(error(t.getException()));
        });
    }
    private void detail(String id) {
        show(R.layout.screen_detail,"detail");click(R.id.back,()->feed("announcements"));click(R.id.retry,()->detail(id));
        int generation=screen;String uid=auth.getUid();running("Cargando anuncio…");
        store.channel(profile.communityId,"announcements").child(id).get().addOnCompleteListener(this,t->{
            if(!current(generation,uid)) return;busy=false;
            if(!t.isSuccessful()) {note(error(t.getException()));return;}
            if(!t.getResult().exists()) {note("Anuncio no disponible.");return;}
            ((TextView)findViewById(R.id.detail)).setText(render("announcements",t.getResult()));note("");
        });
    }
    private String render(String kind,DataSnapshot s) {
        Long date=s.child("createdAt").getValue(Long.class);
        String when=date==null?"Pendiente de servidor":DateFormat.getDateTimeInstance(DateFormat.SHORT,DateFormat.SHORT).format(new Date(date));
        if(kind.equals("messages")) return str(s,"name")+" · #"+str(s,"home")+"\n"+str(s,"text")+"\n"+when;
        if(kind.equals("products")) return str(s,"name")+" · $"+str(s,"priceCLP")+" CLP\n"+str(s,"ownerName")+" · #"+str(s,"home")+"\n"+when;
        return str(s,"title")+"\n"+str(s,"body")+"\n"+str(s,"authorName")+" · "+when;
    }
    private String str(DataSnapshot s,String key) {Object v=s.child(key).getValue();return v==null?"":v.toString();}
    @Override public void onBackPressed() {
        if(profile!=null && !page.equals("home")) home();
        else if(page.equals("register") && auth.getCurrentUser()==null) login();
        else super.onBackPressed();
    }
}
