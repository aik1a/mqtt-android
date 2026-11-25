package cl.aik1a.comunidadconectada.notifications;
import android.content.Context;
import java.util.*;
import com.google.android.gms.tasks.*;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;
import com.google.firebase.messaging.FirebaseMessaging;
import cl.aik1a.comunidadconectada.BuildConfig;
public final class DeviceTokens {
    private static boolean retiring;
    private static String installation(Context c) {
        android.content.SharedPreferences p=c.getSharedPreferences("device",Context.MODE_PRIVATE);
        String id=p.getString("id",null);
        if(id==null) {id=UUID.randomUUID().toString();p.edit().putString("id",id).apply();} return id;
    }
    public static Task<Void> save(Context c,String uid,String token) {
        if(retiring || !uid.equals(FirebaseAuth.getInstance().getUid())) return Tasks.forResult(null);
        Map<String,Object> data=new HashMap<>();data.put("token",token);data.put("updatedAt",ServerValue.TIMESTAMP);
        return FirebaseDatabase.getInstance().getReference("deviceTokens").child(uid).child(installation(c)).setValue(data);
    }
    public static Task<Void> sync(Context c) {
        retiring=false;
        String uid=FirebaseAuth.getInstance().getUid();
        if(uid==null || BuildConfig.USE_EMULATORS) return Tasks.forResult(null);
        return FirebaseMessaging.getInstance().getToken().onSuccessTask(token -> save(c,uid,token));
    }
    public static Task<Void> remove(Context c) {
        retiring=true;
        String uid=FirebaseAuth.getInstance().getUid();
        if(uid==null || BuildConfig.USE_EMULATORS) return Tasks.forResult(null);
        return FirebaseDatabase.getInstance().getReference("deviceTokens").child(uid).child(installation(c)).removeValue();
    }
}
