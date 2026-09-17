import android.content.Context;

import java.util.HashMap;
import java.util.Map;

public final class oql {
    private static final Map<Integer, Boolean> VALUES = new HashMap<Integer, Boolean>();

    private oql() {
    }

    public static oql O(Context context) {
        return new oql();
    }

    public static void reset() {
        VALUES.clear();
    }

    public static void setBoolean(int keyResId, boolean value) {
        VALUES.put(Integer.valueOf(keyResId), Boolean.valueOf(value));
    }

    public boolean at(int keyResId) {
        return Boolean.TRUE.equals(VALUES.get(Integer.valueOf(keyResId)));
    }
}
