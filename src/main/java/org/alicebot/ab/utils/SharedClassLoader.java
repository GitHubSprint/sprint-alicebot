package org.alicebot.ab.utils;

import java.io.File;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;

public class SharedClassLoader {

    private final URLClassLoader classLoader;
    private final Method method;
    private final Class<?> loadedClass;


    public Object newInstance() throws Exception {
        return loadedClass.getDeclaredConstructor().newInstance();
    }

    public SharedClassLoader(File jarFile, String className) throws Exception {
        this.classLoader = new URLClassLoader(
                new URL[]{jarFile.toURI().toURL()},
                getClass().getClassLoader() // lub ClassLoader.getSystemClassLoader()
        );
        this.loadedClass = classLoader.loadClass(className);
        this.method = loadedClass.getMethod("processCustomResultPocessor", String.class, String.class, String.class);
    }

    public String execute(Object instance, String methodName, String parameter, String sessionId) throws Exception {
        return (String) method.invoke(instance, sessionId, parameter, methodName);
    }


    public void clear(Object instance, String sessionId) {
        try {
            Method clearMethod = instance.getClass().getMethod("clear", String.class);
            clearMethod.invoke(instance, sessionId);
        } catch (Exception e) {
            // Ignore if the method does not exist
        }
    }

    public void close() throws Exception {
        classLoader.close();
    }
}

