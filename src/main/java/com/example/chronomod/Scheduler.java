package com.example.chronomod;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/** 몇 틱 뒤에 실행할 작업을 예약하는 간단한 스케줄러 (이펙트 연출용) */
public final class Scheduler {
    private record Task(long runAt, Runnable action) {}

    private static final List<Task> TASKS = new ArrayList<>();

    private Scheduler() {}

    public static void later(int delayTicks, Runnable action) {
        TASKS.add(new Task(TimeManager.now + Math.max(0, delayTicks), action));
    }

    public static void tick(long now) {
        if (TASKS.isEmpty()) return;
        List<Task> due = new ArrayList<>();
        for (Iterator<Task> it = TASKS.iterator(); it.hasNext(); ) {
            Task t = it.next();
            if (t.runAt() <= now) {
                due.add(t);
                it.remove();
            }
        }
        for (Task t : due) {
            try {
                t.action().run();
            } catch (Exception e) {
                ChronoMod.LOGGER.error("Scheduled task failed", e);
            }
        }
    }

    public static void clear() {
        TASKS.clear();
    }
}
