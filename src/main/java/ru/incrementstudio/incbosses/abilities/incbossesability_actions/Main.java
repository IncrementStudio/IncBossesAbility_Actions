package ru.incrementstudio.incbosses.abilities.incbossesability_actions;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import ru.incrementstudio.incbosses.api.AbilityBase;
import ru.incrementstudio.incbosses.api.AbilityPlugin;
import ru.incrementstudio.incbosses.api.bosses.Boss;
import ru.incrementstudio.incbosses.api.bosses.phases.Phase;

import java.io.File;

public final class Main extends AbilityPlugin {
    @Override
    public String getAbilityName() {
        return getName();
    }

    @Override
    public void onAbilityEnable() {
        File bossesActionsDirectory = new File("plugins//IncBosses//abilities//" + Main.getInstance().getName() + "//actions//bosses");
        if (!bossesActionsDirectory.exists())
            bossesActionsDirectory.mkdirs();
        File playersActionsDirectory = new File("plugins//IncBosses//abilities//" + Main.getInstance().getName() + "//actions//players");
        if (!playersActionsDirectory.exists())
            playersActionsDirectory.mkdirs();
    }

    @Override
    public AbilityBase getAbility(Boss boss, Phase phase, FileConfiguration bossConfig, ConfigurationSection abilityConfig) {
        return new Ability(boss, phase, bossConfig, abilityConfig);
    }
}
