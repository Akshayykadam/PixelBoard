package com.akshaykadam.pixelboard.extension.settings;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Owns settings host state transitions without depending on Android lifecycle or UI classes.
 */
public final class GboardPatchesSettingsOrchestrator<F, I> {
    public enum EffectKind {
        REQUEST_RENDER,
        APPLY_RENDER,
        APPLY_RECOVERY,
        REFRESH_DEPENDENCIES,
        SCROLL_TO_TOP,
        SCHEDULE_REFRESH,
        CANCEL_REFRESH,
        CANCEL_PENDING_RENDER,
        EXECUTE_INTENT,
        EXIT,
        SHOW_FATAL
    }

    public static final class Event<F, I> {
        private enum Kind {
            RESUME,
            PAUSE,
            REQUEST_RENDER,
            OPEN,
            REPLACE_PATH,
            BACK,
            RENDER_READY,
            RENDER_FAILED,
            RECOVERY_READY,
            RECOVERY_FAILED,
            FATAL,
            FEATURE_INTENT,
            MODAL_SHOWN,
            MODAL_DISMISSED
        }

        private final Kind kind;
        private final F feature;
        private final List<F> path;
        private final int generation;
        private final long refreshIntervalMs;
        private final I payload;

        private Event(Kind kind, F feature, List<F> path, int generation,
                long refreshIntervalMs) {
            this(kind, feature, path, generation, refreshIntervalMs, null);
        }

        private Event(Kind kind, F feature, List<F> path, int generation,
                long refreshIntervalMs, I payload) {
            this.kind = kind;
            this.feature = feature;
            this.path = path;
            this.generation = generation;
            this.refreshIntervalMs = refreshIntervalMs;
            this.payload = payload;
        }

        public static <F, I> Event<F, I> resume() {
            return simple(Kind.RESUME);
        }

        public static <F, I> Event<F, I> pause() {
            return simple(Kind.PAUSE);
        }

        public static <F, I> Event<F, I> requestRender() {
            return simple(Kind.REQUEST_RENDER);
        }

        public static <F, I> Event<F, I> open(F feature) {
            return new Event<F, I>(Kind.OPEN, feature, Collections.emptyList(), 0, 0L);
        }

        public static <F, I> Event<F, I> replacePath(List<? extends F> path) {
            List<F> safePath = path == null
                    ? Collections.emptyList()
                    : new ArrayList<F>(path);
            return new Event<F, I>(Kind.REPLACE_PATH, null, safePath, 0, 0L);
        }

        public static <F, I> Event<F, I> back() {
            return simple(Kind.BACK);
        }

        public static <F, I> Event<F, I> renderReady(int generation, long refreshIntervalMs) {
            return new Event<F, I>(Kind.RENDER_READY, null, Collections.emptyList(), generation,
                    refreshIntervalMs);
        }

        public static <F, I> Event<F, I> renderFailed(int generation) {
            return new Event<F, I>(Kind.RENDER_FAILED, null, Collections.emptyList(), generation, 0L);
        }

        public static <F, I> Event<F, I> recoveryReady(int generation, long refreshIntervalMs) {
            return new Event<F, I>(Kind.RECOVERY_READY, null, Collections.emptyList(), generation,
                    refreshIntervalMs);
        }

        public static <F, I> Event<F, I> recoveryFailed(int generation) {
            return new Event<F, I>(Kind.RECOVERY_FAILED, null, Collections.emptyList(), generation,
                    0L);
        }

        public static <F, I> Event<F, I> fatal() {
            return simple(Kind.FATAL);
        }

        public static <F, I> Event<F, I> featureIntent(I intent) {
            return new Event<F, I>(Kind.FEATURE_INTENT, null, Collections.emptyList(), 0, 0L,
                    intent);
        }

        public static <F, I> Event<F, I> modalShown() {
            return simple(Kind.MODAL_SHOWN);
        }

        public static <F, I> Event<F, I> modalDismissed() {
            return simple(Kind.MODAL_DISMISSED);
        }

        private static <F, I> Event<F, I> simple(Kind kind) {
            return new Event<F, I>(kind, null, Collections.emptyList(), 0, 0L);
        }
    }

    public static final class Effect<F, I> {
        private final EffectKind kind;
        private final int generation;
        private final F feature;
        private final long delayMs;
        private final I payload;

        private Effect(EffectKind kind, int generation, F feature, long delayMs) {
            this(kind, generation, feature, delayMs, null);
        }

        private Effect(EffectKind kind, int generation, F feature, long delayMs, I payload) {
            this.kind = kind;
            this.generation = generation;
            this.feature = feature;
            this.delayMs = delayMs;
            this.payload = payload;
        }

        public EffectKind getKind() {
            return kind;
        }

        public int getGeneration() {
            return generation;
        }

        public F getFeature() {
            return feature;
        }

        public long getDelayMs() {
            return delayMs;
        }

        public I getPayload() {
            return payload;
        }
    }

    public static final class State<F> {
        private final F current;
        private final List<F> backStack;
        private final int renderGeneration;
        private final boolean resumed;
        private final int modalCount;
        private final long refreshIntervalMs;
        private final boolean fatal;

        private State(F current, List<F> backStack, int renderGeneration, boolean resumed,
                int modalCount, long refreshIntervalMs, boolean fatal) {
            this.current = current;
            this.backStack = Collections.unmodifiableList(new ArrayList<F>(backStack));
            this.renderGeneration = renderGeneration;
            this.resumed = resumed;
            this.modalCount = modalCount;
            this.refreshIntervalMs = refreshIntervalMs;
            this.fatal = fatal;
        }

        public F getCurrent() {
            return current;
        }

        public List<F> getBackStack() {
            return backStack;
        }

        public int getRenderGeneration() {
            return renderGeneration;
        }

        public boolean isResumed() {
            return resumed;
        }

        public int getModalCount() {
            return modalCount;
        }

        public long getRefreshIntervalMs() {
            return refreshIntervalMs;
        }

        public boolean isFatal() {
            return fatal;
        }
    }

    public static final class Transition<F, I> {
        private final State<F> state;
        private final List<Effect<F, I>> effects;

        private Transition(State<F> state, List<Effect<F, I>> effects) {
            this.state = state;
            this.effects = Collections.unmodifiableList(new ArrayList<Effect<F, I>>(effects));
        }

        public State<F> getState() {
            return state;
        }

        public List<Effect<F, I>> getEffects() {
            return effects;
        }
    }

    private final List<F> backStack = new ArrayList<F>();
    private F current;
    private int renderGeneration;
    private boolean resumed;
    private int modalCount;
    private long refreshIntervalMs;
    private boolean fatal;

    public synchronized Transition<F, I> accept(Event<F, I> event) {
        if (event == null) {
            return transition(Collections.emptyList());
        }
        List<Effect<F, I>> effects = new ArrayList<Effect<F, I>>();
        if (fatal) {
            return transition(effects);
        }
        switch (event.kind) {
            case RESUME:
                resumed = true;
                add(effects, EffectKind.REFRESH_DEPENDENCIES);
                requestRender(effects);
                break;
            case PAUSE:
                resumed = false;
                renderGeneration++;
                add(effects, EffectKind.CANCEL_PENDING_RENDER);
                cancelRefresh(effects);
                break;
            case REQUEST_RENDER:
                requestRender(effects);
                break;
            case OPEN:
                if (event.feature != null) {
                    if (current == null) {
                        backStack.clear();
                    } else {
                        backStack.add(current);
                    }
                    current = event.feature;
                    navigationEffects(effects);
                }
                break;
            case REPLACE_PATH:
                replacePath(event.path, effects);
                break;
            case BACK:
                if (current == null) {
                    add(effects, EffectKind.EXIT);
                } else {
                    current = backStack.isEmpty()
                            ? null
                            : backStack.remove(backStack.size() - 1);
                    navigationEffects(effects);
                }
                break;
            case RENDER_READY:
                if (isCurrent(event.generation)) {
                    refreshIntervalMs = Math.max(0L, event.refreshIntervalMs);
                    add(effects, EffectKind.APPLY_RENDER);
                    cancelRefresh(effects);
                    scheduleRefreshIfAllowed(effects);
                }
                break;
            case RENDER_FAILED:
                if (isCurrent(event.generation)) {
                    add(effects, EffectKind.APPLY_RECOVERY);
                }
                break;
            case RECOVERY_READY:
                if (isCurrent(event.generation)) {
                    refreshIntervalMs = Math.max(0L, event.refreshIntervalMs);
                    add(effects, EffectKind.APPLY_RECOVERY);
                    cancelRefresh(effects);
                    scheduleRefreshIfAllowed(effects);
                }
                break;
            case RECOVERY_FAILED:
                if (isCurrent(event.generation)) {
                    enterFatal(effects);
                }
                break;
            case FATAL:
                enterFatal(effects);
                break;
            case FEATURE_INTENT:
                if (event.payload != null) {
                    effects.add(new Effect<F, I>(EffectKind.EXECUTE_INTENT, renderGeneration,
                            current, 0L, event.payload));
                }
                break;
            case MODAL_SHOWN:
                modalCount++;
                cancelRefresh(effects);
                break;
            case MODAL_DISMISSED:
                if (modalCount > 0) {
                    modalCount--;
                }
                scheduleRefreshIfAllowed(effects);
                break;
            default:
                break;
        }
        return transition(effects);
    }

    public synchronized State<F> snapshot() {
        return new State<F>(current, backStack, renderGeneration, resumed, modalCount,
                refreshIntervalMs, fatal);
    }

    private void replacePath(List<F> path, List<Effect<F, I>> effects) {
        List<F> sanitized = new ArrayList<F>();
        for (F feature : path) {
            if (feature != null) {
                sanitized.add(feature);
            }
        }
        if (sanitized.isEmpty()) {
            return;
        }
        backStack.clear();
        backStack.addAll(sanitized.subList(0, sanitized.size() - 1));
        current = sanitized.get(sanitized.size() - 1);
        navigationEffects(effects);
    }

    private void navigationEffects(List<Effect<F, I>> effects) {
        add(effects, EffectKind.REFRESH_DEPENDENCIES);
        add(effects, EffectKind.SCROLL_TO_TOP);
        requestRender(effects);
    }

    private void requestRender(List<Effect<F, I>> effects) {
        renderGeneration++;
        effects.add(new Effect<F, I>(EffectKind.REQUEST_RENDER, renderGeneration, current, 0L));
    }

    private void scheduleRefreshIfAllowed(List<Effect<F, I>> effects) {
        if (resumed && modalCount == 0 && refreshIntervalMs > 0L) {
            effects.add(new Effect<F, I>(EffectKind.SCHEDULE_REFRESH, renderGeneration, current,
                    refreshIntervalMs));
        }
    }

    private void cancelRefresh(List<Effect<F, I>> effects) {
        effects.add(new Effect<F, I>(EffectKind.CANCEL_REFRESH, renderGeneration, current,
                refreshIntervalMs));
    }

    private boolean isCurrent(int generation) {
        return generation == renderGeneration;
    }

    private void enterFatal(List<Effect<F, I>> effects) {
        fatal = true;
        renderGeneration++;
        cancelRefresh(effects);
        add(effects, EffectKind.SHOW_FATAL);
    }

    private void add(List<Effect<F, I>> effects, EffectKind kind) {
        effects.add(new Effect<F, I>(kind, renderGeneration, current, 0L));
    }

    private Transition<F, I> transition(List<Effect<F, I>> effects) {
        return new Transition<F, I>(snapshot(), effects);
    }
}
