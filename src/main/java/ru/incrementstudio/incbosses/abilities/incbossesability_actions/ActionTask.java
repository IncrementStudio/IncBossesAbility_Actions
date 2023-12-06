package ru.incrementstudio.incbosses.abilities.incbossesability_actions;

import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import ru.incrementstudio.incbosses.api.bosses.Boss;

import java.util.function.BooleanSupplier;

public class ActionTask {
    public static BukkitTask create(Boss boss, Action action, long delay, long period, int repeats) {
        return create(boss, action, () -> false, delay, period, repeats);
    }
    public static BukkitTask create(Boss boss, Action action, BooleanSupplier stopCondition, long delay, long period, int repeats) {
        int phaseId = boss.getCurrentPhase().getId();
        return new BukkitRunnable() {
            int i = 0;
            @Override
            public void run() {
                if (
                        stopCondition.getAsBoolean()
                        || i == repeats
                        || (repeats == -1 && boss.isKilled())
                        || (repeats == -2 && (boss.isKilled() || boss.getCurrentPhase().getId() != phaseId))
                ) {
                    cancel();
                    return;
                }
                action.exec();
                i++;
            }
        }.runTaskTimer(Main.getInstance(), delay, period);
    }
}
