package cl.aik1a.comunidadconectada.ui;
import org.junit.Rule;
import org.junit.Test;
import androidx.test.rule.ActivityTestRule;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.*;
import static androidx.test.espresso.assertion.ViewAssertions.*;
import static androidx.test.espresso.matcher.ViewMatchers.*;
import cl.aik1a.comunidadconectada.R;
public class AuthUiTest {
    @Rule public ActivityTestRule<MainActivity> activity=new ActivityTestRule<>(MainActivity.class);
    @Test public void emptyLoginShowsValidation() {
        onView(withId(R.id.login)).perform(click());
        onView(withId(R.id.status)).check(matches(withText("Escribe un correo válido.")));
    }
    @Test public void registrationRequiresName() {
        onView(withId(R.id.register)).perform(click());onView(withId(R.id.submit)).perform(scrollTo(),click());
        onView(withId(R.id.status)).check(matches(withText("Escribe un nombre de 1 a 80 caracteres.")));
    }
}
