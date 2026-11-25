package cl.aik1a.comunidadconectada.data;
import java.util.HashMap;
import java.util.Map;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.*;
import cl.aik1a.comunidadconectada.auth.Validation;
import cl.aik1a.comunidadconectada.model.Profile;
public class CommunityRepository {
    private static final Map<String,Task<Void>> pending = new HashMap<>();
    public Task<Void> pending(String uid,String kind) { return pending.get(uid+"/"+kind); }
    public Task<Void> send(Profile p,String uid,String kind,Map<String,Object> data) {
        String key=uid+"/"+kind;
        if(pending.containsKey(key)) return pending.get(key);
        Task<Void> task=channel(p.communityId,kind).push().setValue(data);
        pending.put(key,task);
        task.addOnCompleteListener(t->pending.remove(key));
        return task;
    }
    private final DatabaseReference root=FirebaseDatabase.getInstance().getReference();
    public DatabaseReference channel(String cid,String kind) { return root.child("communities").child(cid).child(kind); }
    public Query feed(Profile p, String kind) {
        return root.child("communities").child(p.communityId).child(kind).orderByChild("createdAt").limitToLast(50);
    }
    public DatabaseReference announcement(Profile p, String id) {
        return root.child("communities").child(p.communityId).child("announcements").child(id);
    }
    public DatabaseReference newPost(Profile p, String kind) { return root.child("communities").child(p.communityId).child(kind).push(); }
    public Map<String,Object> post(Profile p,String uid,String kind,String title,String body,String price) {
        Map<String,Object> m=new HashMap<>();
        m.put("createdAt",ServerValue.TIMESTAMP);
        if("messages".equals(kind)) {
            require(Validation.text(body,1000)); m.put("uid",uid); m.put("name",p.name); m.put("home",p.home); m.put("text",body.trim());
        } else if("announcements".equals(kind)) {
            if(!p.isAdmin()) throw new IllegalArgumentException("Solo el administrador puede publicar anuncios.");
            require(Validation.text(title,120)); require(Validation.text(body,4000));
            m.put("authorUid",uid); m.put("authorName",p.name); m.put("title",title.trim()); m.put("body",body.trim());
        } else if("products".equals(kind)) {
            require(Validation.text(title,100)); Long cost=Validation.price(price);
            if(cost==null) throw new IllegalArgumentException("El precio debe ser un entero entre 0 y 1.000.000.000 CLP.");
            m.put("ownerUid",uid); m.put("ownerName",p.name); m.put("home",p.home); m.put("name",title.trim()); m.put("priceCLP",cost);
        } else throw new IllegalArgumentException("Canal desconocido.");
        return m;
    }
    private void require(String e) { if(e!=null) throw new IllegalArgumentException(e); }
    public Task<Void> rename(String uid,String name) {
        require(Validation.name(name)); return root.child("users").child(uid).child("name").setValue(name.trim());
    }
}
