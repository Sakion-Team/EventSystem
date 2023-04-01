package nep.timeline.EventSystem;

import nep.timeline.EventSystem.type.EventType;

public class EventCore
{
    public static final String name = "EventSystem";
    public static final String developer = "Timeline";
    public static final String version = "3.2";
    private final EventList event;
    private EventType type = EventType.NONE;
    private boolean isCancelled = false;

    public EventCore(EventList event)
    {
        this.event = event;
    }

    public EventList getEvent()
    {
        return this.event;
    }

    public void setCancelled(boolean cancel)
    {
        this.isCancelled = cancel;
    }

    public boolean isCancelled()
    {
        return this.isCancelled;
    }

    public void setType(EventType type)
    {
        this.type = type;
    }

    public EventType getType()
    {
        return this.type;
    }
}
