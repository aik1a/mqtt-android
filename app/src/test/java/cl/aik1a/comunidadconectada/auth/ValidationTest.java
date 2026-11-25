package cl.aik1a.comunidadconectada.auth;
import org.junit.Test;
import static org.junit.Assert.*;
public class ValidationTest {
    @Test public void acceptsAlphanumericHomeAndSharedHome() {
        assertNull(Validation.registration("Vecino Uno","311A","uno@example.test","segura12"));
        assertNull(Validation.registration("Vecino Dos","311A","dos@example.test","segura12"));
    }
    @Test public void rejectsMissingFields() {
        assertNotNull(Validation.registration("","311A","a@example.test","secret1"));
        assertNotNull(Validation.registration("A","","a@example.test","secret1"));
        assertNotNull(Validation.registration("A","311A","","secret1"));
        assertNotNull(Validation.registration("A","311A","a@example.test",""));
    }
    @Test public void rejectsBadEmailHomeAndShortPassword() {
        assertNotNull(Validation.email("not-email"));assertNotNull(Validation.email("a b@example.test"));
        assertNull(Validation.email("a@example.test"));
        assertNotNull(Validation.home("311/A"));assertNotNull(Validation.home("1234567890123"));
        assertNotNull(Validation.password("12345"));
    }
    @Test public void validatesProductPrices() {
        assertEquals(Long.valueOf(0),Validation.price("0"));assertEquals(Long.valueOf(1500),Validation.price("1500"));
        for(String value:new String[]{"-1","1.5","", "999999999999999999999999", "1000000001", "1e3"}) assertNull(Validation.price(value));
    }
    @Test public void rejectsBlankAndOversizedContent() {
        assertNotNull(Validation.text(" ",100));assertNotNull(Validation.text("12345",4));assertNull(Validation.text("Hola",4));
        assertNotNull(Validation.name(" "));
    }
}
