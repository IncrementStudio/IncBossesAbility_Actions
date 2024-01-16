package ru.incrementstudio.incbosses.abilities.incbossesability_actions;

import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import ru.incrementstudio.incapi.Action;
import ru.incrementstudio.incbosses.api.bosses.Boss;

import java.util.*;
import java.util.function.BooleanSupplier;

public class ActionTask {
    private static final Map<Integer, BukkitTask> tasks = new HashMap<>();

    public static void stopAll() {
        for (BukkitTask bukkitTask : tasks.values())
            bukkitTask.cancel();
        tasks.clear();
    }

    public static BukkitTask create(Boss boss, Action action, long delay, long period, int repeats) {
        return create(boss, action, () -> false, delay, period, repeats);
    }

    public static BukkitTask create(Boss boss, Action action, BooleanSupplier stopCondition, long delay, long period, int repeats) {
        if (repeats == 1 && delay == 0) {
            action.execute();
            return null;
        }
        int phaseId = boss.getCurrentPhase().getId();
        int id;
        do {
            id = new Random().nextInt();
        } while (tasks.containsKey(id));
        int finalId = id;
        BukkitTask task = new BukkitRunnable() {
            int i = 0;

            @Override
            public void run() {
                if (
                        stopCondition.getAsBoolean()
                                || i == repeats
                                || (repeats == -1 && boss.isKilled())
                                || (repeats == -2 && (boss.isKilled() || boss.getCurrentPhase().getId() != phaseId))
                ) {
                    tasks.remove(finalId);
                    cancel();
                    return;
                }
                action.execute();
                i++;
            }
        }.runTaskTimer(Main.getInstance(), delay, period);
        tasks.put(id, task);
        return task;
    }
}