// SPDX-License-Identifier: GPL-3.0-only
import android.content.Context;
import android.content.SharedPreferences;
import android.inputmethodservice.InputMethodService;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;

import com.akshaykadam.pixelboard.extension.rambler.GboardMicBridge;

import java.util.ArrayList;
import java.util.List;

/** Handwritten native boundary simulations for the real extension. Never packaged in an APK. */
/** Native boundaries only. GboardMicBridge and its upstream selection runtime are real code. */
abstract class pbb extends InputMethodService {
    InputConnection connection;
    EditorInfo editor;
    boolean ramblerHandled = true;
    boolean consumeWithoutStarting;
    boolean unobservableRambler, hiddenRamblerRecording;
    public Object F;
    final kmp microphone = new kmp();

    pbb() {
        F = new obl(new obr(new fde(new fdq(new kls(new kme(new kmc(microphone)))))));
    }

    final List<Integer> normalEvents = new ArrayList<>();
    final List<Integer> nativeEvents = new ArrayList<>();
    final List<Object> nativeSources = new ArrayList<>();
    final List<Boolean> savedModesAtDelivery = new ArrayList<>();

    @Override
    public InputConnection getCurrentInputConnection() {
        return connection;
    }

    @Override
    public EditorInfo getCurrentInputEditorInfo() {
        return editor;
    }

    public void dG(oas event) {
        GboardMicBridge.beforeEvent(this, event);
        normalEvents.add(event.a());
        savedModesAtDelivery.add(qou.I(this).x(0x7f140954, false));
    }

    public boolean aw(oas event) {
        GboardMicBridge.beforeEvent(this, event);
        nativeEvents.add(event.a());
        nativeSources.add(event.l);
        savedModesAtDelivery.add(qou.I(this).x(0x7f140954, false));
        boolean handled =
                ramblerHandled
                        && qou.I(this).x(0x7f140954, false)
                        && event.a() == -10217
                        && event.l == mnp.a;
        if (handled && consumeWithoutStarting) return true;
        if (handled && unobservableRambler) {
            hiddenRamblerRecording = !hiddenRamblerRecording;
            return true;
        }
        if (handled && ((obl) F).wrapper.h instanceof fdj) {
            fdj loaded = (fdj) ((obl) F).wrapper.h;
            if (loaded.c.h() == fgf.PROCESSING || loaded.c.h() == fgf.RETRYING) return false;
            if (loaded.recorder().running) {
                loaded.recorder().running = false;
                loaded.c.d.set(fgf.PROCESSING);
            } else {
                loaded.recorder().running = true;
                loaded.c.d.set(fgf.LISTENING);
            }
        } else if (handled) microphone.listening = !microphone.listening;
        return handled;
    }

    protected abstract owa ai();
}

final class oas {
    private final int code;
    public Object l;

    private oas(int code) {
        this.code = code;
    }

    public int a() {
        return code;
    }

    public static oas e(int code, Object payload) {
        return new oas(code);
    }
}

final class mnp {
    public static final Object a = new Object();
}

interface owa {
    void u();

    void q(CharSequence text, int position);
}

final class qou {
    private static qou current;
    private final SharedPreferences values;
    final List<Boolean> writes = new ArrayList<>();
    Runnable onWrite;
    boolean failNextWrite, ignoreNextWrite;

    private qou(Context context) {
        values = context.getSharedPreferences("native_bridge_fixture", Context.MODE_PRIVATE);
    }

    static void reset(Context context) {
        current = new qou(context);
        current.values.edit().clear().commit();
    }

    public static qou I(Context context) {
        if (current == null) current = new qou(context);
        return current;
    }

    public void q(int key, boolean value) {
        writes.add(value);
        if (failNextWrite) {
            failNextWrite = false;
            throw new IllegalStateException("Fixture preference write failed");
        }
        if (ignoreNextWrite) {
            ignoreNextWrite = false;
            return;
        }
        values.edit().putBoolean("enable_jetson", value).commit();
        if (onWrite != null) onWrite.run();
    }

    public boolean x(int key, boolean fallback) {
        return values.getBoolean("enable_jetson", fallback);
    }

    void seed(boolean value) {
        values.edit().putBoolean("enable_jetson", value).commit();
    }
}

/** Public native-state boundary for production bridge reflection; never packaged. */
class fde {
    public ffr c = new ffr();
    public fdq g;

    public fde(fdq value) {
        g = value;
    }
}

/** Lite is registered under its fde base module type. */
class fdj extends fde {
    private ifs E;
    private Object F = new Object();

    public fdj(ifs recorder) {
        super(null);
        E = recorder;
    }

    public ifs recorder() {
        return E;
    }
}

/** Public native-state boundary for production bridge reflection; never packaged. */
class fdq {
    public kls d;

    public fdq(kls value) {
        d = value;
    }
}

/** Already-loaded native session boundary; reading h never opens the UI. */
class ffr {
    public final java.util.concurrent.atomic.AtomicReference<fgf> d =
            new java.util.concurrent.atomic.AtomicReference<>(fgf.IDLING);

    public fgf h() {
        return d.get();
    }
}

/** Native Rambler UI/session stages, pinned independently from the microphone bit. */
enum fgf {
    IDLING,
    WAITING,
    LISTENING,
    PROCESSING,
    RETRYING,
    ERROR
}

/** Independent Lite/Muse recorder native boundary, not the classic kmp recorder. */
class ifs {
    public boolean running;

    public synchronized boolean b() {
        return running;
    }
}

/** Public native-state boundary for production bridge reflection; never packaged. */
class kls {
    public kme e;

    public kls(kme value) {
        e = value;
    }
}

/** Public native-state boundary for production bridge reflection; never packaged. */
class kmc {
    public kmp i;

    public kmc(kmp value) {
        i = value;
    }
}

/** Public native-state boundary for production bridge reflection; never packaged. */
class kme {
    public kmc recorder;

    public kme(kmc value) {
        recorder = value;
    }

    public kmc b() {
        return recorder;
    }
}

/** Public native-state boundary for production bridge reflection; never packaged. */
class kmp {
    public boolean listening;

    public boolean e() {
        return listening;
    }
}

/** Native b() hides inactive wrappers even when their h instance already exists. */
class obl {
    public obr wrapper;
    private final java.util.Map<Class<?>, obr> q =
            new java.util.AbstractMap<Class<?>, obr>() {
                @Override
                public obr get(Object key) {
                    return key == mvr.class ? wrapper : null;
                }

                @Override
                public java.util.Set<java.util.Map.Entry<Class<?>, obr>> entrySet() {
                    throw new UnsupportedOperationException();
                }
            };

    public obl(obr value) {
        wrapper = value;
    }

    public obr b(Class<?> type) {
        obr found = q.get(type);
        return found != null && found.l ? found : null;
    }
}

/** Already-created native extension wrapper; l is activation, not allocation. */
class obr {
    public Object h;
    public boolean l = true;

    public obr(Object value) {
        h = value;
    }
}

/** Handwritten selector boundary, not native Gboard implementation. Never packaged. */
final class mvq {
    public static boolean a(android.content.Context context) {
        return qou.I(context).x(0x7f140954, false);
    }
}

/** Native Rambler module interface: fdf registers Rambler under mvr, implemented by fde. */
interface mvr {}
