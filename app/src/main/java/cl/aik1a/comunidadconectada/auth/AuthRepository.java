package cl.aik1a.comunidadconectada.auth;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;
import cl.aik1a.comunidadconectada.model.Profile;
public class AuthRepository {
    private final FirebaseAuth auth=FirebaseAuth.getInstance();
    public Task<Void> register(String name, String home, String email, String password) {
        String error=Validation.registration(name,home,email,password);
        if(error!=null) return Tasks.forException(new IllegalArgumentException(error));
        return auth.createUserWithEmailAndPassword(email,password).onSuccessTask(result ->
            FirebaseDatabase.getInstance().getReference("users").child(result.getUser().getUid())
                .setValue(new Profile(name.trim(),home)));
        // If Auth succeeds but RTDB fails, the signed-in user can finish the missing profile.
    }
    public Task<Void> completeProfile(String name, String home) {
        String error=Validation.name(name);
        if(error==null) error=Validation.home(home);
        if(error!=null) return Tasks.forException(new IllegalArgumentException(error));
        if(auth.getCurrentUser()==null) return Tasks.forException(new IllegalStateException("Inicia sesión."));
        return FirebaseDatabase.getInstance().getReference("users").child(auth.getUid()).setValue(new Profile(name.trim(),home));
    }
}
