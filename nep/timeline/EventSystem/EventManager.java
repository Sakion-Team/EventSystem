package nep.timeline.EventSystem;

import nep.timeline.EventSystem.threads.MethodInvoke;
import nep.timeline.EventSystem.type.EventPriority;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class EventManager
{
    public static final MethodHandles.Lookup lookup = MethodHandles.lookup();
    private static final CopyOnWriteArrayList<Object> listeners = new CopyOnWriteArrayList<>();

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
            Arrays.asList(projects).forEach(listeners::remove);
    }

    public static void callEvent(EventCore event, Object listener, Method method)
    {
        try
        {
            MethodHandle handle = EventManager.lookup.unreflect(method);
            EventList events = method.getDeclaredAnnotation(EventListener.class).event();

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
        catch (InvocationTargetException | IllegalAccessException ignored)
        {

        }
        catch (Throwable throwable)
        {
            throwable.printStackTrace();
        }
    }

    public static EventCore call(EventCore event)
    {
        return call(event, false);
    }

    public static EventCore call(EventCore event, boolean multi_thread)
    {
        listeners.forEach(listener -> {
            List<Method> methods = Arrays.asList(listener.getClass().getDeclaredMethods());
            methods.sort((method1, method2) -> {
                if (!method1.isAccessible())
                    method1.setAccessible(true);

                if (!method2.isAccessible())
                    method2.setAccessible(true);

                EventPriority priority1 = null;
                EventPriority priority2 = null;

                if (method1.isAnnotationPresent(EventListener.class))
                    priority1 = method1.getDeclaredAnnotation(EventListener.class).priority();

                if (method2.isAnnotationPresent(EventListener.class))
                    priority2 = method2.getDeclaredAnnotation(EventListener.class).priority();

                if (priority1 != null && priority2 != null && priority1 != priority2)
                    return priority1.getLevel() - priority2.getLevel();
                else
                    return 0;
            });

            methods.forEach(method -> {
                if (!method.isAccessible())
                    method.setAccessible(true);

                if (method.isAnnotationPresent(EventListener.class))
                {
                    if (method.getParameterCount() > 1)
                        throw new EventException("Too many method types! method name:" + method.getName());

                    if (multi_thread)
                        new MethodInvoke("Multi Thread Invoke " + method.getName(), method, listener, event).start();
                    else
                        callEvent(event, listener, method);
                }
            });
        });

        return event;
    }
}
