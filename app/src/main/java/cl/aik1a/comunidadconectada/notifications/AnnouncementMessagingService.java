package cl.aik1a.comunidadconectada.notifications;
import android.Manifest;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.os.Build;
import androidx.core.app.NotificationCompat;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;
import com.google.firebase.messaging.*;
import cl.aik1a.comunidadconectada.R;
import cl.aik1a.comunidadconectada.model.Profile;
import cl.aik1a.comunidadconectada.ui.MainActivity;
public class AnnouncementMessagingService extends FirebaseMessagingService {
    @Override public void onNewToken(String token) {
        String uid=FirebaseAuth.getInstance().getUid();
        if(uid!=null) DeviceTokens.save(this,uid,token);
    }
    @Override public void onMessageReceived(RemoteMessage message) {
        String id=message.getData().get("announcementId"), cid=message.getData().get("communityId");
        String uid=FirebaseAuth.getInstance().getUid();
        if(uid==null || id==null || cid==null || !id.matches("[-A-Za-z0-9_]{1,128}") || !cid.matches("[A-Za-z0-9_-]{1,64}")) return;
        FirebaseDatabase.getInstance().getReference("users").child(uid).get().addOnSuccessListener(s -> {
            Profile p=s.getValue(Profile.class);
            if(p==null || !cid.equals(p.communityId) || !uid.equals(FirebaseAuth.getInstance().getUid())) return;
            FirebaseDatabase.getInstance().getReference("communities").child(cid).child("announcements").child(id).get().addOnSuccessListener(a -> {
                if(!a.exists() || !uid.equals(FirebaseAuth.getInstance().getUid())) return;
                notifyAnnouncement(cid,id,a.child("title").getValue(String.class));
            });
        });
    }
    private void notifyAnnouncement(String cid,String id,String title) {
        if(Build.VERSION.SDK_INT>=33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED) return;
        NotificationManager manager=getSystemService(NotificationManager.class);
        if(Build.VERSION.SDK_INT>=26) manager.createNotificationChannel(new NotificationChannel("announcements","Anuncios oficiales",NotificationManager.IMPORTANCE_DEFAULT));
        Intent intent=new Intent(this,MainActivity.class).putExtra("announcementId",id).putExtra("communityId",cid)
            .setData(android.net.Uri.parse("comunidad://announcement/"+cid+"/"+id))
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent tap=PendingIntent.getActivity(this,0,intent,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        manager.notify(cid+"/"+id,0,new NotificationCompat.Builder(this,"announcements").setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title==null?"Anuncio oficial":title).setContentText("Toca para leer el anuncio.")
            .setContentIntent(tap).setAutoCancel(true).build());
    }
}
