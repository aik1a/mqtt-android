package cl.aik1a.comunidadconectada.auth;
public final class Validation {
    private Validation() {}
    public static String name(String value) {
        return value!=null && !value.trim().isEmpty() && value.trim().length()<=80 ? null : "Escribe un nombre de 1 a 80 caracteres.";
    }
    public static String home(String value) {
        return value!=null && value.matches("[A-Za-z0-9]{1,12}") ? null : "La vivienda debe tener entre 1 y 12 letras o números, por ejemplo 311A.";
    }
    public static String email(String value) {
        return value!=null && value.length()<=254 && value.matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+") ? null : "Escribe un correo válido.";
    }
    public static String password(String value) {
        return value!=null && value.length()>=6 && value.length()<=128 ? null : "La contraseña debe tener entre 6 y 128 caracteres.";
    }
    public static String text(String value, int max) {
        return value!=null && !value.trim().isEmpty() && value.trim().length()<=max ? null : "Escribe contenido de 1 a "+max+" caracteres.";
    }
    public static Long price(String value) {
        if(value==null || !value.matches("[0-9]{1,10}")) return null;
        try { long result=Long.parseLong(value); return result<=1000000000L ? result : null; }
        catch(NumberFormatException ex) { return null; }
    }
    public static String registration(String name, String home, String email, String password) {
        String[] errors={name(name),home(home),email(email),password(password)};
        for(String e:errors) if(e!=null) return e;
        return null;
    }
}
