package plz.lizi.supersteve.power;

import java.util.Arrays;

@SuppressWarnings("removal")
public class Stacker extends SecurityManager {
    private static final Stacker INSTANCE = new Stacker();

    public static Class<?>[] getStackTrace() {
        try {
            Class<?>[] context = INSTANCE.getClassContext();
            return Arrays.copyOfRange(context, 2, context.length);
        } catch (Throwable e) {
            return new Class[0];
        }
    }

    @Override
    protected Class<?>[] getClassContext() {
        return super.getClassContext();
    }
    
}
