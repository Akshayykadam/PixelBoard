import android.content.Context;

public final class njt implements njz {
    public final Context context;

    public njt(Context context) {
        this.context = context;
    }

    @Override
    public wcz request(String payload) {
        return wcz.success(payload);
    }

    @Override
    public String i() {
        return "ASTREA";
    }

    @Override
    public void o() {
    }

    @Override
    public void close() {
    }
}
