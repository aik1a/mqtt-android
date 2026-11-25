package cl.aik1a.comunidadconectada;
import android.app.Application;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;
public class CommunityApp extends Application {
    public static boolean configured;
    @Override public void onCreate() {
        super.onCreate();
        if (BuildConfig.USE_EMULATORS) {
            // Never reuse production options in the local flavor, even if a
            // developer has a private google-services.json in app/.
            for (FirebaseApp existing : FirebaseApp.getApps(this)) existing.delete();
            FirebaseApp.initializeApp(this,
                new FirebaseOptions.Builder().setApplicationId("1:123456789:android:demo")
                .setApiKey("demo-key").setProjectId("demo-comunidad")
                .setDatabaseUrl("https://demo-comunidad-default-rtdb.firebaseio.com").build());
            FirebaseAuth.getInstance().useEmulator("10.0.2.2", 9099);
            FirebaseDatabase.getInstance().useEmulator("10.0.2.2", 9000);
            configured = true;
        } else {
            configured = FirebaseApp.initializeApp(this) != null;
        }
    }
}
