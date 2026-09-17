import android.content.Context;

public final class njr implements njz {
    public static boolean failConstruction;
    public final Context context;
    public final oxe config;

    public njr(Context context, oxe config) {
        if (failConstruction) {
            throw new IllegalStateException("AiCore unavailable");
        }
        this.context = context;
        this.config = config;
    }

    @Override
    public wcz request(String payload) {
        return wcz.success(payload);
    }

    @Override
    public String i() {
        return "AICORE";
    }

    @Override
    public void o() {
    }

    @Override
    public void close() {
    }
}
