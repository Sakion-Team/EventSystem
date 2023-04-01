package nep.timeline.EventSystem.threads;

import nep.timeline.EventSystem.*;

import java.lang.invoke.MethodHandle;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class MethodInvoke extends Thread {
    private final Method method;
    private final Object listener;
    private final EventCore event;

    public MethodInvoke(String name, Method method, Object listener, EventCore event)
    {
        super(name);
        this.method = method;
        this.listener = listener;
        this.event = event;
    }

    @Override
    public void run()
    {
        EventManager.callEvent(event, listener, method);
    }
}
