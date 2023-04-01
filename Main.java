import nep.timeline.EventSystem.EventList;
import nep.timeline.EventSystem.EventListener;
import nep.timeline.EventSystem.EventManager;
import nep.timeline.EventSystem.events.ShutdownEvent;

public class Main {
    public static void main(String[] args) {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> EventManager.call(new ShutdownEvent())));
        System.out.println("Hello world!");
        EventManager.addListener(new Main());
    }

    @EventListener(event = EventList.SHUT_DOWN)
    public void shutdown()
    {
        System.out.println("Shutdown.");
    }
}