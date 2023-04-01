package nep.timeline.EventSystem;

import nep.timeline.EventSystem.type.EventPriority;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface EventListener
{
    EventList event() default EventList.NONE;
    EventPriority priority() default EventPriority.MEDIUM;
}
