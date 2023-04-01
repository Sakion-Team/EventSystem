package nep.timeline.EventSystem.events;

import nep.timeline.EventSystem.EventCore;
import nep.timeline.EventSystem.EventList;

public class ShutdownEvent extends EventCore
{
    public ShutdownEvent()
    {
        super(EventList.SHUT_DOWN);
    }
}
