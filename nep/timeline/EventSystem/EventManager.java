package nep.timeline.EventSystem;

import nep.timeline.EventSystem.type.EventPriority;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

public class EventManager
{
    public static final MethodHandles.Lookup lookup = MethodHandles.lookup();
    private static final CopyOnWriteArrayList<Object> listeners = new CopyOnWriteArrayList<>();
    private static final ExecutorService executorService = Executors.newCachedThreadPool();

    public static void reset()
    {
        listeners.clear();
    }

    public static void addListener(Object... projects)
    {
        for (Object object : projects)
            if (!listeners.contains(object))
                listeners.add(object);
    }

    public static void removeListener(Object... projects)
    {
        if (!listeners.isEmpty())
            listeners.removeAll(Arrays.asList(projects));
    }

    public static void callEvent(EventCore event, Object listener, Method method)
    {
        synchronized (EventManager.class) {
            try
            {
                MethodHandle handle = lookup.unreflect(method);
                EventListener annotation = method.getDeclaredAnnotation(EventListener.class);
                if (annotation == null)
                    return;

                EventList events = annotation.event();

                if (events != EventList.NONE && (events == event.getEvent() || events == EventList.ALL))
                {
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

                    if (methodObject != null && methodObject == event.getClass())
                        handle.invoke(listener, event);
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
    }

    public static EventCore call(EventCore event)
    {
        return call(event, true);
    }

    public static EventCore call(EventCore event, boolean multiThread)
    {
        List<CompletableFuture<Void>> futures = listeners.stream().flatMap(listener -> {
            List<Method> methods = Arrays.stream(listener.getClass().getDeclaredMethods())
                    .filter(method -> method.isAnnotationPresent(EventListener.class))
                    .sorted((method1, method2) -> {
                        EventPriority priority1 = method1.getDeclaredAnnotation(EventListener.class).priority();
                        EventPriority priority2 = method2.getDeclaredAnnotation(EventListener.class).priority();
                        return Integer.compare(priority1.getLevel(), priority2.getLevel());
                    }).collect(Collectors.toList());

            return methods.stream().map(method -> {
                if (!method.canAccess(listener))
                    method.setAccessible(true);

                if (method.getParameterCount() > 1)
                    throw new EventException("Too many method types! method name:" + method.getName());

                Runnable task = () -> callEvent(event, listener, method);
                return multiThread ? CompletableFuture.runAsync(task, executorService) : CompletableFuture.runAsync(task);
            });
        }).collect(Collectors.toList());

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        return event;
    }

    /*public static EventCore call(EventCore event, boolean multiThread)
    {
        listeners.forEach(listener -> {
            List<Method> methods = Arrays.stream(listener.getClass().getDeclaredMethods())
                    .filter(method -> method.isAnnotationPresent(EventListener.class))
                    .sorted((method1, method2) -> {
                        EventPriority priority1 = method1.getDeclaredAnnotation(EventListener.class).priority();
                        EventPriority priority2 = method2.getDeclaredAnnotation(EventListener.class).priority();
                        return Integer.compare(priority1.getLevel(), priority2.getLevel());
                    }).collect(Collectors.toList());

            methods.forEach(method -> {
                if (!method.isAccessible())
                    method.setAccessible(true);

                if (method.getParameterCount() > 1)
                    throw new EventException("Too many method types! method name:" + method.getName());

                if (multiThread)
                    executorService.execute(() -> callEvent(event, listener, method));
                else
                    callEvent(event, listener, method);
            });
        });

        return event;
    }*/
}
