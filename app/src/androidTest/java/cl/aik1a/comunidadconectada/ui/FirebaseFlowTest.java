package cl.aik1a.comunidadconectada.ui;
import org.junit.Rule;
import org.junit.Test;
import android.app.Instrumentation;
import android.widget.TextView;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.ActivityTestRule;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.*;
import static androidx.test.espresso.assertion.ViewAssertions.*;
import static androidx.test.espresso.matcher.ViewMatchers.*;
import static org.junit.Assert.fail;
import cl.aik1a.comunidadconectada.R;
public class FirebaseFlowTest {
    @Rule public ActivityTestRule<MainActivity> activity=new ActivityTestRule<>(MainActivity.class);
    private void waitText(int id,String expected) throws Exception {
        Instrumentation instrumentation=InstrumentationRegistry.getInstrumentation();
        for(int i=0;i<150;i++) {
            boolean[] found={false};
            instrumentation.runOnMainSync(()->{
                TextView view=activity.getActivity().findViewById(id);
                found[0]=view!=null && view.getText().toString().contains(expected);
            });
            if(found[0]) return;
            Thread.sleep(200);
        }
        fail("Expected UI text: "+expected);
    }
    @Test public void registerChatProductProfileLogoutAndLogin() throws Exception {
        String email="ui"+System.nanoTime()+"@example.test", password="DemoPass123";
        onView(withId(R.id.register)).perform(click());
        onView(withId(R.id.name)).perform(typeText("Vecino UI"));
        onView(withId(R.id.home)).perform(typeText("311A"));
        onView(withId(R.id.email)).perform(typeText(email));
        onView(withId(R.id.password)).perform(typeText(password),closeSoftKeyboard());
        onView(withId(R.id.submit)).perform(scrollTo(),click());
        waitText(R.id.identity,"Vecino UI");
        onView(withId(R.id.chat)).perform(click());
        onView(withId(R.id.body)).perform(typeText("Hola desde Android"),closeSoftKeyboard());
        onView(withId(R.id.send)).perform(click());waitText(R.id.status,"Enviado y confirmado");
        onView(withId(R.id.back)).perform(click());onView(withId(R.id.products)).perform(click());
        onView(withId(R.id.title)).perform(typeText("Pan"));
        onView(withId(R.id.price)).perform(typeText("1500"),closeSoftKeyboard());
        onView(withId(R.id.send)).perform(click());waitText(R.id.status,"Enviado y confirmado");
        onView(withId(R.id.back)).perform(click());onView(withId(R.id.profile)).perform(click());
        onView(withId(R.id.name)).perform(replaceText("Vecino Editado"),closeSoftKeyboard());
        onView(withId(R.id.submit)).perform(click());waitText(R.id.status,"Nombre actualizado");
        onView(withId(R.id.back)).perform(click());onView(withId(R.id.logout)).perform(scrollTo(),click());
        waitText(R.id.login,"Iniciar sesión");
        onView(withId(R.id.email)).perform(typeText(email));onView(withId(R.id.password)).perform(typeText(password),closeSoftKeyboard());
        onView(withId(R.id.login)).perform(click());waitText(R.id.identity,"Vecino Editado");
        onView(withId(R.id.logout)).perform(scrollTo(),click());waitText(R.id.login,"Iniciar sesión");
    }
}
