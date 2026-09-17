package com.akshaykadam.pixelboard.extension.writingtools;

import android.content.Context;
import android.util.Log;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public final class GboardAiWritingToolsVoiceCommandRuntime {
    private static final String TAG = "GboardPatches";
    private static final String LOG_PREFIX = "[gboard-writing-tools-voice-commands-18.0.3] ";
    private static final int PROTO_CACHE_LIMIT = 8;
    private static final AtomicInteger INFO_LOG_COUNT = new AtomicInteger();
    private static final AtomicInteger ERROR_LOG_COUNT = new AtomicInteger();
    private static final IdentityHashMap<Object, Object> PROTO_REPLACEMENTS =
            new IdentityHashMap<Object, Object>();
    private static final Object GEN_AI_INIT_LOCK = new Object();
    private static final IdentityHashMap<Object, GenAiInitCall> GEN_AI_INIT_CALLS =
            new IdentityHashMap<Object, GenAiInitCall>();
    private static final IdentityHashMap<Object, Boolean> GEN_AI_SMART_REFRESHES =
            new IdentityHashMap<Object, Boolean>();
    private static final IdentityHashMap<Object, Boolean> GEN_AI_RETRY_ATTEMPTED =
            new IdentityHashMap<Object, Boolean>();
    private static final ThreadLocal<Boolean> GEN_AI_RETRY_IN_PROGRESS =
            new ThreadLocal<Boolean>();
    private static final Object SMART_EDIT_MODULE_LOCK = new Object();
    private static final IdentityHashMap<Class<?>, Object> SMART_EDIT_FALLBACK_MODULES =
            new IdentityHashMap<Class<?>, Object>();

    private GboardAiWritingToolsVoiceCommandRuntime() {
    }

    public static boolean applyGenAiInitClientType(Object clientType, boolean stockResult) {
        try {
            return applyUnknownClientTypeGate(isFeatureEnabled(), clientType, stockResult);
        } catch (Throwable throwable) {
            logError("GenAI init client-type gate failed", throwable);
            return stockResult;
        }
    }

    public static void rememberGenAiInitCall(Object handler, Object manager, Object module) {
        if (handler == null || manager == null || module == null) {
            return;
        }
        synchronized (GEN_AI_INIT_LOCK) {
            GEN_AI_INIT_CALLS.put(manager, new GenAiInitCall(handler, module));
            if (!Boolean.TRUE.equals(GEN_AI_RETRY_IN_PROGRESS.get())) {
                GEN_AI_RETRY_ATTEMPTED.remove(manager);
            }
        }
    }

    public static void beginGenAiRefresh(Object manager) {
        if (manager == null) {
            return;
        }
        synchronized (GEN_AI_INIT_LOCK) {
            GEN_AI_SMART_REFRESHES.remove(manager);
        }
    }

    public static void observeGenAiRefreshClientType(Object manager, Object clientType) {
        if (manager == null) {
            return;
        }
        String clientTypeName = clientType instanceof Enum<?>
                ? ((Enum<?>) clientType).name()
                : null;
        if (!GboardAiWritingToolsVoiceCommandPolicy
                .isSmartDictationClientType(clientTypeName)) {
            return;
        }
        synchronized (GEN_AI_INIT_LOCK) {
            GEN_AI_SMART_REFRESHES.put(manager, Boolean.TRUE);
        }
    }

    public static void finishGenAiRefresh(Object manager) {
        try {
            finishGenAiRefresh(isFeatureEnabled(), manager);
        } catch (Throwable throwable) {
            logError("GenAI manager refresh retry failed", throwable);
        }
    }

    static boolean finishGenAiRefresh(boolean featureEnabled, Object manager) {
        boolean sawSmartClientType;
        synchronized (GEN_AI_INIT_LOCK) {
            sawSmartClientType = GEN_AI_SMART_REFRESHES.remove(manager) != null;
        }
        return retryGenAiManager(featureEnabled, sawSmartClientType, manager);
    }

    public static boolean applySmartEditInitClientType(Object clientType, boolean stockResult) {
        try {
            return applyUnknownClientTypeGate(isFeatureEnabled(), clientType, stockResult);
        } catch (Throwable throwable) {
            logError("SmartEdit init client-type gate failed", throwable);
            return stockResult;
        }
    }

    public static boolean shouldEnableSmartEditModule(Object flag) {
        try {
            return shouldEnableSmartEditModule(isFeatureEnabled(), flag);
        } catch (Throwable throwable) {
            logError("SmartEdit module predicate failed", throwable);
            return false;
        }
    }

    public static void afterCommandAmbiguityConstructed(Object receiver) {
        try {
            applyCommandAmbiguityCapability(isFeatureEnabled(), receiver);
        } catch (Throwable throwable) {
            logError("command ambiguity capability failed", throwable);
        }
    }

    public static Object applyClassifierModelessFlagValue(Object stockResult) {
        try {
            return applyClassifierModelessFlagValue(isFeatureEnabled(), stockResult);
        } catch (Throwable throwable) {
            logError("classifier modeless flag failed", throwable);
            return stockResult;
        }
    }

    public static Object applyClassificationRegexVersion(Object stockResult) {
        try {
            return applyClassificationRegexVersion(isFeatureEnabled(), stockResult);
        } catch (Throwable throwable) {
            logError("classification regex version failed", throwable);
            return stockResult;
        }
    }

    public static Object applyVoiceCommandLanguages(Object flagReceiver, Object stockResult) {
        try {
            return maybeMergeVoiceCommandLanguages(
                    isFeatureEnabled(),
                    flagReceiver,
                    stockResult);
        } catch (Throwable throwable) {
            logError("voice-command language merge failed", throwable);
            return stockResult;
        }
    }

    static boolean applyUnknownClientTypeGate(
            boolean featureEnabled,
            Object clientType,
            boolean stockResult) {
        String clientTypeName = clientType instanceof Enum<?>
                ? ((Enum<?>) clientType).name()
                : null;
        boolean promoted = GboardAiWritingToolsVoiceCommandPolicy
                .shouldPromoteUnknownClientType(
                        featureEnabled,
                        stockResult,
                        clientTypeName);
        if (promoted) {
            logInfo("promoted transient clientType=" + clientTypeName);
        }
        return promoted || stockResult;
    }

    public static Object restoreSmartEditGeneratorModule(
            Object stockResult,
            Object moduleManager,
            Object requestedClass) {
        try {
            return restoreSmartEditGeneratorModule(
                    isFeatureEnabled(),
                    stockResult,
                    moduleManager,
                    requestedClass);
        } catch (Throwable throwable) {
            logError("SmartEdit generator module restore failed", throwable);
            return stockResult;
        }
    }

    static Object restoreSmartEditGeneratorModule(
            boolean featureEnabled,
            Object stockResult,
            Object moduleManager,
            Object requestedClass) {
        if (!featureEnabled
                || stockResult != null
                || moduleManager == null
                || !(requestedClass instanceof Class<?>)) {
            return stockResult;
        }
        Class<?> moduleClass = (Class<?>) requestedClass;
        if (!"hhf".equals(moduleClass.getSimpleName())) {
            return stockResult;
        }
        try {
            synchronized (SMART_EDIT_MODULE_LOCK) {
                Object cached = SMART_EDIT_FALLBACK_MODULES.get(moduleClass);
                if (cached != null) {
                    return cached;
                }
            }

            Method loadMethod = findMethod(moduleManager.getClass(), "b", Class.class);
            if (Modifier.isStatic(loadMethod.getModifiers())) {
                return stockResult;
            }
            loadMethod.setAccessible(true);
            Object loadedModule = loadMethod.invoke(moduleManager, moduleClass);
            if (loadedModule != null) {
                return moduleClass.isInstance(loadedModule) ? loadedModule : stockResult;
            }

            synchronized (SMART_EDIT_MODULE_LOCK) {
                Object cached = SMART_EDIT_FALLBACK_MODULES.get(moduleClass);
                if (cached != null) {
                    return cached;
                }

                ClassLoader classLoader = moduleClass.getClassLoader();
                if (classLoader == null) {
                    classLoader = moduleManager.getClass().getClassLoader();
                }
                String moduleClassName = moduleClass.getName();
                String implementationClassName = moduleClassName.substring(
                        0,
                        moduleClassName.length() - "hhf".length()) + "hhi";
                Class<?> implementationClass = Class.forName(
                        implementationClassName,
                        true,
                        classLoader);
                if (!moduleClass.isAssignableFrom(implementationClass)) {
                    return stockResult;
                }

                Constructor<?> constructor = implementationClass.getDeclaredConstructor();
                constructor.setAccessible(true);
                Object fallbackModule = constructor.newInstance();
                Field contextField = findField(moduleManager.getClass(), "b");
                contextField.setAccessible(true);
                Object context = contextField.get(moduleManager);
                Method initMethod = findSmartEditModuleInitMethod(
                        implementationClass,
                        context);
                initMethod.invoke(fallbackModule, context, null);
                SMART_EDIT_FALLBACK_MODULES.put(moduleClass, fallbackModule);
                logInfo("restored missing SmartEdit generator module="
                        + moduleClass.getName());
                return fallbackModule;
            }
        } catch (InvocationTargetException exception) {
            Throwable cause = exception.getCause();
            logError(
                    "SmartEdit generator module restore failed",
                    cause == null ? exception : cause);
            return stockResult;
        } catch (Throwable throwable) {
            logError("SmartEdit generator module restore failed", throwable);
            return stockResult;
        }
    }

    private static boolean retryGenAiManager(
            boolean featureEnabled,
            boolean sawSmartClientType,
            Object manager) {
        if (!featureEnabled || !sawSmartClientType || manager == null) {
            return false;
        }
        try {
            GenAiInitCall initCall;
            boolean alreadyAttempted;
            synchronized (GEN_AI_INIT_LOCK) {
                initCall = GEN_AI_INIT_CALLS.get(manager);
                alreadyAttempted = GEN_AI_RETRY_ATTEMPTED.containsKey(manager);
            }
            boolean managerMissing = isGenAiManagerMissing(manager);
            if (!GboardAiWritingToolsVoiceCommandPolicy.shouldRetryGenAiManager(
                    featureEnabled,
                    sawSmartClientType,
                    managerMissing,
                    initCall != null,
                    alreadyAttempted)) {
                return false;
            }
            synchronized (GEN_AI_INIT_LOCK) {
                if (GEN_AI_RETRY_ATTEMPTED.containsKey(manager)) {
                    return false;
                }
                GEN_AI_RETRY_ATTEMPTED.put(manager, Boolean.TRUE);
            }

            Method initMethod = findGenAiInitMethod(
                    initCall.handler,
                    manager,
                    initCall.module);
            GEN_AI_RETRY_IN_PROGRESS.set(Boolean.TRUE);
            initMethod.invoke(initCall.handler, manager, initCall.module);
            boolean restored = !isGenAiManagerMissing(manager);
            logInfo("retried GenAI manager after Smart Dictation refresh restored=" + restored);
            return restored;
        } catch (Throwable throwable) {
            logError("GenAI manager refresh retry failed", throwable);
            return false;
        } finally {
            GEN_AI_RETRY_IN_PROGRESS.remove();
        }
    }

    private static boolean isGenAiManagerMissing(Object manager)
            throws ReflectiveOperationException {
        if (manager == null) {
            return false;
        }
        Field field = findField(manager.getClass(), "genAiVoiceEditManager");
        field.setAccessible(true);
        return field.get(manager) == null;
    }

    private static Method findGenAiInitMethod(
            Object handler,
            Object manager,
            Object module) throws NoSuchMethodException {
        for (Class<?> type = handler.getClass(); type != null; type = type.getSuperclass()) {
            for (Method method : type.getDeclaredMethods()) {
                Class<?>[] parameters = method.getParameterTypes();
                if (method.getName().equals("c")
                        && parameters.length == 2
                        && parameters[0].isInstance(manager)
                        && parameters[1].isInstance(module)
                        && method.getReturnType() == void.class
                        && !Modifier.isStatic(method.getModifiers())) {
                    method.setAccessible(true);
                    return method;
                }
            }
        }
        throw new NoSuchMethodException("GenAI init method c(manager,module)");
    }

    private static Method findMethod(
            Class<?> type,
            String name,
            Class<?>... parameterTypes) throws NoSuchMethodException {
        for (Class<?> current = type; current != null; current = current.getSuperclass()) {
            try {
                return current.getDeclaredMethod(name, parameterTypes);
            } catch (NoSuchMethodException ignored) {
                // Continue through the hierarchy.
            }
        }
        throw new NoSuchMethodException(name);
    }

    private static Method findSmartEditModuleInitMethod(
            Class<?> implementationClass,
            Object context) throws NoSuchMethodException {
        for (Class<?> type = implementationClass;
                type != null;
                type = type.getSuperclass()) {
            for (Method method : type.getDeclaredMethods()) {
                Class<?>[] parameters = method.getParameterTypes();
                if (method.getName().equals("fw")
                        && parameters.length == 2
                        && context != null
                        && parameters[0].isInstance(context)
                        && "pab".equals(parameters[1].getSimpleName())
                        && method.getReturnType() == void.class
                        && !Modifier.isStatic(method.getModifiers())) {
                    method.setAccessible(true);
                    return method;
                }
            }
        }
        throw new NoSuchMethodException("hhi#fw(context,pab)");
    }

    private static Field findField(Class<?> type, String name) throws NoSuchFieldException {
        for (Class<?> current = type; current != null; current = current.getSuperclass()) {
            try {
                return current.getDeclaredField(name);
            } catch (NoSuchFieldException ignored) {
                // Continue through the hierarchy.
            }
        }
        throw new NoSuchFieldException(name);
    }

    static boolean shouldEnableSmartEditModule(boolean featureEnabled, Object flag)
            throws ReflectiveOperationException {
        return GboardAiWritingToolsVoiceCommandPolicy.shouldPromoteModulePredicate(
                featureEnabled,
                readFieldFlagName(flag),
                false);
    }

    static void applyCommandAmbiguityCapability(boolean featureEnabled, Object receiver)
            throws ReflectiveOperationException {
        if (!featureEnabled || receiver == null) {
            return;
        }
        Field field = receiver.getClass().getDeclaredField("b");
        if (field.getType() != boolean.class) {
            return;
        }
        field.setAccessible(true);
        boolean stockValue = field.getBoolean(receiver);
        if (GboardAiWritingToolsVoiceCommandPolicy
                .shouldPromoteCommandAmbiguityCapability(featureEnabled, stockValue)) {
            field.setBoolean(receiver, true);
            logInfo("promoted local targeted-edit capability");
        }
    }

    static Object applyClassifierModelessFlagValue(
            boolean featureEnabled,
            Object stockResult) {
        return GboardAiWritingToolsVoiceCommandPolicy
                .maybeOverrideClassifierModelessFlag(featureEnabled, stockResult);
    }

    static Object applyClassificationRegexVersion(
            boolean featureEnabled,
            Object stockResult) {
        return GboardAiWritingToolsVoiceCommandPolicy
                .maybeOverrideRegexVersion(featureEnabled, stockResult);
    }

    @SuppressWarnings("unchecked")
    static synchronized Object maybeMergeVoiceCommandLanguages(
            boolean featureEnabled,
            Object flagReceiver,
            Object stockResult) {
        if (!featureEnabled || flagReceiver == null || stockResult == null) {
            return stockResult;
        }
        try {
            Method flagNameMethod = flagReceiver.getClass().getDeclaredMethod("h");
            flagNameMethod.setAccessible(true);
            Object flagName = flagNameMethod.invoke(flagReceiver);
            if (!GboardAiWritingToolsVoiceCommandPolicy.VOICE_COMMAND_LANGUAGES_FLAG
                    .equals(flagName)) {
                return stockResult;
            }

            Object cached = PROTO_REPLACEMENTS.get(stockResult);
            if (cached != null) {
                return cached;
            }

            Field languageField = stockResult.getClass().getDeclaredField("b");
            languageField.setAccessible(true);
            Object originalListObject = languageField.get(stockResult);
            if (!(originalListObject instanceof List<?>)) {
                return stockResult;
            }
            List<String> originalLanguages = (List<String>) originalListObject;
            List<String> mergedLanguages = GboardAiWritingToolsVoiceCommandPolicy
                    .mergeRequiredLanguages(originalLanguages);
            if (mergedLanguages == originalLanguages) {
                return stockResult;
            }

            Constructor<?> protoConstructor = stockResult.getClass().getDeclaredConstructor();
            Constructor<?> listConstructor = originalListObject.getClass()
                    .getDeclaredConstructor();
            protoConstructor.setAccessible(true);
            listConstructor.setAccessible(true);
            Object copiedProto = protoConstructor.newInstance();
            Object copiedListObject = listConstructor.newInstance();
            if (!(copiedListObject instanceof List<?>)) {
                return stockResult;
            }
            List<String> copiedLanguages = (List<String>) copiedListObject;
            copiedLanguages.addAll(mergedLanguages);
            languageField.set(copiedProto, copiedListObject);

            Method finalizeMethod = stockResult.getClass().getSuperclass()
                    .getDeclaredMethod("ca");
            finalizeMethod.setAccessible(true);
            finalizeMethod.invoke(copiedProto);

            if (PROTO_REPLACEMENTS.size() >= PROTO_CACHE_LIMIT) {
                PROTO_REPLACEMENTS.clear();
            }
            PROTO_REPLACEMENTS.put(stockResult, copiedProto);
            logInfo("merged voice-command languages=" + copiedLanguages);
            return copiedProto;
        } catch (Throwable throwable) {
            logError("voice-command proto reflection failed", throwable);
            return stockResult;
        }
    }

    private static String readFieldFlagName(Object flag) throws ReflectiveOperationException {
        if (flag == null) {
            return null;
        }
        Field field = flag.getClass().getDeclaredField("a");
        if (field.getType() != String.class) {
            return null;
        }
        field.setAccessible(true);
        return (String) field.get(flag);
    }

    private static boolean isFeatureEnabled() {
        Context context = GboardAiWritingToolsRuntime.resolveContext();
        return context != null
                && GboardAiWritingToolsSettings.snapshot(context).featureEnabled;
    }

    private static void logInfo(String message) {
        if (INFO_LOG_COUNT.incrementAndGet() > 30) {
            return;
        }
        safeLog(message, null);
    }

    private static void logError(String message, Throwable throwable) {
        if (ERROR_LOG_COUNT.incrementAndGet() > 10) {
            return;
        }
        safeLog(message, throwable);
    }

    private static void safeLog(String message, Throwable throwable) {
        try {
            if (throwable == null) {
                Log.i(TAG, LOG_PREFIX + message);
            } else {
                Log.w(TAG, LOG_PREFIX + message, throwable);
            }
        } catch (Throwable ignored) {
            // Host-side unit tests do not provide android.util.Log.
        }
    }

    private static final class GenAiInitCall {
        final Object handler;
        final Object module;

        GenAiInitCall(Object handler, Object module) {
            this.handler = handler;
            this.module = module;
        }
    }
}
