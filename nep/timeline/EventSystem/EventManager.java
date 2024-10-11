package nep.timeline.EventSystem;

import nep.timeline.EventSystem.type.EventPriority;
import nep.timeline.projects.Sakura.Core;
import nep.timeline.projects.Sakura.module.ModuleCore;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

public class EventManager
{
    private static final MethodHandles.Lookup lookup = MethodHandles.lookup();
    private static final Map<String, MethodHandle> methodHandles = new HashMap<>();
    private static final CopyOnWriteArrayList<MethodHandler> listeners = new CopyOnWriteArrayList<>();
    private static final ExecutorService executorService = Executors.newCachedThreadPool();

    public static void reset()
    {
        listeners.clear();
    }

    public static void addListener(Object... projects)
    {
        for (Object object : projects)
            for (Method method : object.getClass().getDeclaredMethods()) {
                if (!method.isAccessible())
                    method.setAccessible(true);

                MethodHandler methodHandler = new MethodHandler(method, object);
                if (!listeners.contains(methodHandler))
                    listeners.add(methodHandler);
            }
    }

    public static void removeListener(Object... projects)
    {
        if (!listeners.isEmpty())
            Arrays.asList(projects).forEach(o -> listeners.removeIf(i -> i.getListener().equals(o)));
    }

    public static void callEvent(EventCore event, Object listener, Method method)
    {
        try
        {
            EventListener annotation = method.getDeclaredAnnotation(EventListener.class);
            if (annotation == null)
                return;

            EventList events = annotation.event();

            if (events != EventList.NONE && (events == event.getEvent() || events == EventList.ALL))
            {
                MethodHandle handle;
                String key = listener.getClass().getTypeName() + "#" + method.getName() + "#" + getParametersString(method.getParameterTypes());
                if (methodHandles.containsKey(key))
                    handle = methodHandles.get(key);
                else {
                    handle = EventManager.lookup.unreflect(method);
                    methodHandles.put(key, handle);
                }

                if (method.getParameterCount() == 1)
                {
                    Class<?> methodObject = method.getParameterTypes()[0];
                    if (methodObject != event.getClass())
                        throw new EventException("[EventSystem] The event does not match! method name:" + method.getName());
                    handle.invoke(listener, event);
                }
                else
                {
                    handle.invoke(listener);
                }
            }
            else if (events == EventList.NONE && method.getParameterCount() == 1)
            {
                Class<?> methodObject = method.getParameterTypes()[0];

                if (methodObject != null && methodObject == event.getClass()) {
                    MethodHandle handle;
                    String key = listener.getClass().getTypeName() + "#" + method.getName() + "#" + getParametersString(method.getParameterTypes());
                    if (methodHandles.containsKey(key))
                        handle = methodHandles.get(key);
                    else {
                        handle = EventManager.lookup.unreflect(method);
                        methodHandles.put(key, handle);
                    }
                    handle.invoke(listener, event);
                }
            }
            else if (events == event.getEvent())
            {
                throw new EventException("Incorrect usage! method name:" + method.getName());
            }
        }
        catch (InvocationTargetException e)
        {
            System.err.println(listener.getClass().getTypeName() + " | " + method.getName());
            e.printStackTrace();
        }
        catch (Throwable throwable)
        {
            throwable.printStackTrace();
        }
    }

    public static Object getListenerByMethod(Method method) {
        for (MethodHandler methodHandler : listeners)
            if (methodHandler.getMethod().equals(method))
                return methodHandler.getListener();

        return null;
    }

    public static EventCore call(EventCore event)
    {
        return call(event, false);
    }

    public static EventCore call(EventCore event, boolean multiThread)
    {
        CompletableFuture.allOf(listeners.stream().flatMap(listener -> {
            List<Method> methods = Arrays.stream(listener.getClass().getDeclaredMethods())
                    .filter(method -> method.isAnnotationPresent(EventListener.class))
                    .sorted((method1, method2) -> {
                        EventPriority priority1 = method1.getDeclaredAnnotation(EventListener.class).priority();
                        EventPriority priority2 = method2.getDeclaredAnnotation(EventListener.class).priority();
                        return Integer.compare(priority1.getLevel(), priority2.getLevel());
                    }).collect(Collectors.toList());

            return methods.stream().map(method -> {
                if (!method.isAccessible())
                    method.setAccessible(true);

                if (method.getParameterCount() > 1)
                    throw new EventException("Too many method types! method name:" + method.getName());

                Runnable task = () -> callEvent(event, listener, method);
                return multiThread ? CompletableFuture.runAsync(task, executorService) : CompletableFuture.runAsync(task);
            });
        }).toArray(CompletableFuture[]::new)).join();

        return event;
    }

    private static String getParametersString(Class<?>... clazzes) {
        StringBuilder sb = new StringBuilder("(");
        boolean first = true;
        for (Class<?> clazz : clazzes) {
            if (first)
                first = false;
            else
                sb.append(",");

            if (clazz != null)
                sb.append(clazz.getCanonicalName());
            else
                sb.append("null");
        }
        sb.append(")");
        return sb.toString();
    }
}
