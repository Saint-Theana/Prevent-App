package me.piebridge.prevent.framework.util;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.Map;

import me.piebridge.prevent.framework.PreventLog;

/**
 * Created by thom on 16/2/3.
 */
public class ReflectUtils {

    private static final Map<String, Method> METHOD_CACHES = new LinkedHashMap<String, Method>();

    private ReflectUtils() {

    }

    public static Field getDeclaredField(Object target, String name) {
        if (target == null) {
            return null;
        }
        Field field = null;
        Class clazz = target.getClass();
        while (clazz != null&&field==null) {
            try {
                field = clazz.getField(name);
            } catch (NoSuchFieldException e) {
                PreventLog.d("cannot find field " + name + " in " + clazz);
                clazz = clazz.getSuperclass();
            }
        }
        if (field == null) {
            PreventLog.e("cannot find field " + name + " in " + target.getClass());
        } else {
            field.setAccessible(true);
        }
        return field;
    }

    public static Object invoke(Object target, String name) {
        String key = target.getClass() + "#" + name;
        Method method = METHOD_CACHES.get(key);
        try {
            if (method == null) {
                method = getMethod(target, name);
                method.setAccessible(true);
                METHOD_CACHES.put(key, method);
            }
            return method.invoke(target, null);
        } catch (NoSuchMethodException e) {
            PreventLog.e("cannot find method " + name + " in " + target.getClass());
        } catch (InvocationTargetException e) {
            PreventLog.e("cannot invoke " + method + " in " + target.getClass());
        } catch (IllegalAccessException e) {
            PreventLog.e("cannot access " + method + " in " + target.getClass());
        }
        return null;
    }

    public static Object invoke(Object target, String name, Class<?>[] parameterTypes, Object[] args) {
        String key = target.getClass() + "#" + name;
        Method method = METHOD_CACHES.get(key);
        try {
            if (method == null) {
                method = getMethod(target, name, parameterTypes);
                method.setAccessible(true);
                METHOD_CACHES.put(key, method);
            }
            return method.invoke(target, args);
        } catch (NoSuchMethodException e) {
            PreventLog.e("cannot find method " + name + " in " + target.getClass());
        } catch (InvocationTargetException e) {
            PreventLog.e("cannot invoke " + method + " in " + target.getClass());
        } catch (IllegalAccessException e) {
            PreventLog.e("cannot access " + method + " in " + target.getClass());
        }
        return null;
    }

    private static Method getMethod(Object target, String name) throws NoSuchMethodException {
        for (Method m : target.getClass().getMethods()) {
            if (m.getName().equals(name)) {
                return m;
            }
        }
        Class superClass = target.getClass().getSuperclass();
        while (superClass != null) {
            for (Method m : target.getClass().getMethods()) {
                if (m.getName().equals(name)) {
                    return m;
                }
            }
            superClass = superClass.getSuperclass();
        }
        throw new NoSuchMethodException();
    }

    private static Method getMethod(Object target, String name, Class<?>[] parameterTypes) throws NoSuchMethodException {
        try {
            return target.getClass().getMethod(name, parameterTypes);
        } catch (NoSuchMethodException e) {
            Class superClass = target.getClass().getSuperclass();
            while (superClass != null) {
                try {
                    return superClass.getMethod(name, parameterTypes);
                } catch (NoSuchMethodException e1) {
                    superClass = superClass.getSuperclass();
                }
            }
        }
        throw new NoSuchMethodException();
    }

    public static Object get(Object target, String name) {
        Field field=null;
        try {
            field = getDeclaredField(target, name);
            field.setAccessible(true);
            return field.get(target);
        } catch (Exception e) {
            PreventLog.e("cannot access " + field + " in " + target.getClass());
        }
        return null;
    }

}
