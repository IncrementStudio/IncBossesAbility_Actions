package ru.incrementstudio.incbosses.abilities.incbossesability_actions;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.openjdk.nashorn.api.scripting.NashornScriptEngineFactory;
import ru.incrementstudio.incapi.utils.ColorUtil;
import ru.incrementstudio.incapi.utils.EffectUtil;
import ru.incrementstudio.incapi.utils.MathUtil;
import ru.incrementstudio.incapi.utils.PhysicUtil;
import ru.incrementstudio.incbosses.api.AbilityBase;
import ru.incrementstudio.incbosses.api.AbilityPlugin;
import ru.incrementstudio.incbosses.api.StartReason;
import ru.incrementstudio.incbosses.api.StopReason;
import ru.incrementstudio.incbosses.api.bosses.Boss;
import ru.incrementstudio.incbosses.api.bosses.phases.Phase;

import javax.script.Bindings;
import javax.script.ScriptEngine;
import javax.script.ScriptException;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class Ability extends AbilityBase {
    private final Boss boss;
    private final List<String> startActions = new ArrayList<>();
    private final double startPlayersActionsRadius;
    private final List<String> startPlayersActions = new ArrayList<>();
    private final List<String> startFightersActions = new ArrayList<>();
    private final List<String> stopActions = new ArrayList<>();
    private final double stopPlayersActionsRadius;
    private final List<String> stopPlayersActions = new ArrayList<>();
    private final List<String> stopFightersActions = new ArrayList<>();
    private final List<String> spawnActions = new ArrayList<>();
    private final double spawnPlayersActionsRadius;
    private final List<String> spawnPlayersActions = new ArrayList<>();
    private final List<String> spawnFightersActions = new ArrayList<>();
    private final List<String> deathActions = new ArrayList<>();
    private final double deathPlayersActionsRadius;
    private final List<String> deathPlayersActions = new ArrayList<>();
    private final List<String> deathFightersActions = new ArrayList<>();
    private final List<String> notSpawnActions = new ArrayList<>();
    private final double notSpawnPlayersActionsRadius;
    private final List<String> notSpawnPlayersActions = new ArrayList<>();
    private final List<String> notSpawnFightersActions = new ArrayList<>();
    private final List<String> notDeathActions = new ArrayList<>();
    private final double notDeathPlayersActionsRadius;
    private final List<String> notDeathPlayersActions = new ArrayList<>();
    private final List<String> notDeathFightersActions = new ArrayList<>();

    public Ability(Boss boss, Phase phase, FileConfiguration bossConfig, ConfigurationSection abilityConfig) {
        super(boss, phase, bossConfig, abilityConfig);
        this.boss = boss;
        fillActions(startActions, "start-actions");
        startPlayersActionsRadius = abilityConfig.contains("start-players-actions-radius") ?
                abilityConfig.getDouble("start-players-actions-radius") : 10;
        fillActions(startPlayersActions, "start-players-actions");
        fillActions(startFightersActions, "start-fighters-actions");
        fillActions(stopActions, "stop-actions");
        stopPlayersActionsRadius = abilityConfig.contains("stop-players-actions-radius") ?
                abilityConfig.getDouble("stop-players-actions-radius") : 10;
        fillActions(stopPlayersActions, "stop-players-actions");
        fillActions(stopFightersActions, "stop-fighters-actions");
        fillActions(spawnActions, "spawn-actions");
        spawnPlayersActionsRadius = abilityConfig.contains("spawn-players-actions-radius") ?
                abilityConfig.getDouble("spawn-players-actions-radius") : 10;
        fillActions(spawnPlayersActions, "spawn-players-actions");
        fillActions(spawnFightersActions, "spawn-fighters-actions");
        fillActions(deathActions, "death-actions");
        deathPlayersActionsRadius = abilityConfig.contains("death-players-actions-radius") ?
                abilityConfig.getDouble("death-players-actions-radius") : 10;
        fillActions(deathPlayersActions, "death-players-actions");
        fillActions(deathFightersActions, "death-fighters-actions");
        fillActions(notSpawnActions, "not-spawn-actions");
        notSpawnPlayersActionsRadius = abilityConfig.contains("not-spawn-players-actions-radius") ?
                abilityConfig.getDouble("not-spawn-players-actions-radius") : 10;
        fillActions(notSpawnPlayersActions, "not-spawn-players-actions");
        fillActions(notSpawnFightersActions, "not-spawn-fighters-actions");
        fillActions(notDeathActions, "not-death-actions");
        notDeathPlayersActionsRadius = abilityConfig.contains("not-death-players-actions-radius") ?
                abilityConfig.getDouble("not-death-players-actions-radius") : 10;
        fillActions(notDeathPlayersActions, "not-death-players-actions");
        fillActions(notDeathFightersActions, "not-death-fighters-actions");
    }

    private void fillActions(List<String> actionsMap, String path) {
        if (abilityConfig.contains(path)) {
            List<String> actions = abilityConfig.getStringList(path);
            actionsMap.addAll(actions);
        }
    }

    private String[] getAction(String string) {
        if (!string.isEmpty()) {
            if (string.contains(" ")) {
                String action = string.substring(0, string.indexOf(" "));
                if (action.length() > 2) {
                    if (action.startsWith("[") && action.endsWith("]")) {
                        action = action.substring(1, action.length() - 1);
                        String value = string.substring(string.indexOf(" ") + 1);
                        if (!value.isEmpty())
                            return new String[]{action, value};
                    }
                }
            } else {
                if (string.length() > 2) {
                    if (string.startsWith("[") && string.endsWith("]")) {
                        return new String[]{string.substring(1, string.length() - 1), null};
                    }
                }
            }
        }
        return null;
    }

    private void executeBosses(List<String> actions) {
        for (String string : actions) {
            String[] stringsElems = getAction(string);
            if (stringsElems == null) continue;
            String action = stringsElems[0];
            String value = stringsElems[1];

            long delay = 0;
            long period = 0;
            int repeats = 1;

            Matcher actionArgumentsMatcher = Pattern.compile("^(\\w+)\\((.*)\\)$").matcher(action);
            if (actionArgumentsMatcher.matches()) {
                action = actionArgumentsMatcher.group(1);
                String args = actionArgumentsMatcher.group(2);
                String[] arguments = args.split(";");
                for (String arg : arguments) {
                    Matcher argumentMatcher = Pattern.compile("^(\\w+)=(.*)$").matcher(arg);
                    if (argumentMatcher.matches()) {
                        String argName = argumentMatcher.group(1);
                        String argValue = argumentMatcher.group(2);
                        if (argName.equalsIgnoreCase("delay") || argName.equalsIgnoreCase("del") || argName.equalsIgnoreCase("d")) {
                            Matcher delayMatcher = Pattern.compile("^(\\d+)(\\w+)$").matcher(argValue);
                            if (delayMatcher.matches()) {
                                long delayValue = Long.parseLong(delayMatcher.group(1));
                                String delayMetric = delayMatcher.group(2);
                                if (delayMetric.equalsIgnoreCase("t") || delayMetric.equalsIgnoreCase("tick"))
                                    delay = delayValue;
                                else if (delayMetric.equalsIgnoreCase("s") || delayMetric.equalsIgnoreCase("sec"))
                                    delay = delayValue * 20;
                                else if (delayMetric.equalsIgnoreCase("m") || delayMetric.equalsIgnoreCase("min"))
                                    delay = delayValue * 20 * 60;
                                else
                                    Main.logger().error("Неверная единица измерения в '" + actionArgumentsMatcher.group(0) + "': '" + delayMetric + "'!");
                            } else {
                                AbilityPlugin.logger().error("Значение параметра '" + argName + "' имеет неверный формат: '" + argValue + "'!");
                            }
                        } else if (argName.equalsIgnoreCase("period") || argName.equalsIgnoreCase("per") || argName.equalsIgnoreCase("p")) {
                            Matcher periodMatcher = Pattern.compile("^(\\d+)(\\w+)$").matcher(argValue);
                            if (periodMatcher.matches()) {
                                long periodValue = Long.parseLong(periodMatcher.group(1));
                                String delayMetric = periodMatcher.group(2);
                                if (delayMetric.equalsIgnoreCase("t") || delayMetric.equalsIgnoreCase("tick"))
                                    period = periodValue;
                                else if (delayMetric.equalsIgnoreCase("s") || delayMetric.equalsIgnoreCase("sec"))
                                    period = periodValue * 20;
                                else if (delayMetric.equalsIgnoreCase("m") || delayMetric.equalsIgnoreCase("min"))
                                    period = periodValue * 20 * 60;
                                else
                                    Main.logger().error("Неверная единица измерения в '" + actionArgumentsMatcher.group(0) + "': '" + delayMetric + "'!");
                            } else {
                                AbilityPlugin.logger().error("Значение параметра '" + argName + "' имеет неверный формат: '" + argValue + "'!");
                            }
                        } else if (argName.equalsIgnoreCase("repeats") || argName.equalsIgnoreCase("rep") || argName.equalsIgnoreCase("r")) {
                            Matcher repeatMathcer = Pattern.compile("^(\\d+)$").matcher(argValue);
                            if (repeatMathcer.matches()) {
                                repeats = Integer.parseInt(repeatMathcer.group(1));
                            } else if (argValue.equalsIgnoreCase("while_alive")) {
                                repeats = -1;
                            } else if (argValue.equalsIgnoreCase("while_phase")) {
                                repeats = -2;
                            } else {
                                AbilityPlugin.logger().error("Значение параметра '" + argName + "' имеет неверный формат: '" + argValue + "'!");
                            }
                        } else {
                            AbilityPlugin.logger().error("Параметр '" + argName + "' не найден!");
                        }
                    } else {
                        AbilityPlugin.logger().error("Аргумент '" + arg + "' имеет неверный формат!");
                    }
                }
            } else if (!Pattern.compile("^(\\w+)$").matcher(action).matches()) {
                Main.logger().error("Неверное действие: '" + action + "'");
                continue;
            }

            switch (action) {
                case "console": {
                    if (value != null) {
                        ActionTask.create(boss,
                                () -> Main.getInstance().getServer().dispatchCommand(Main.getInstance().getServer().getConsoleSender(), value),
                                delay, period, repeats
                        );
                    } else {
                        Main.logger().error("[console] Ожидается 1 аргумент: <command>");
                    }
                    break;
                }
                case "command": {
                    if (value != null) {
                        ActionTask.create(boss,
                                () -> Main.getInstance().getServer().dispatchCommand(boss.getEntity(), value),
                                () -> boss.getEntity() == null,
                                delay, period, repeats
                        );
                    } else {
                        Main.logger().error("[command] Ожидается 1 аргумент: <command>");
                    }
                    break;
                }
                case "sound": {
                    if (value != null) {
                        String[] elements = value.split(":");
                        if (elements.length == 3) {
                            try {
                                float volume = Float.parseFloat(elements[1]);
                                try {
                                    float pitch = Float.parseFloat(elements[2]);
                                    final Location[] location = new Location[1];
                                    location[0] = boss.getEntity().getLocation();
                                    ActionTask.create(boss,
                                            () -> {
                                                if (boss.getEntity() != null)
                                                    location[0] = boss.getEntity().getLocation();
                                                try {
                                                    Sound sound = Sound.valueOf(elements[0]);
                                                    location[0].getWorld().playSound(
                                                            location[0],
                                                            sound,
                                                            volume,
                                                            pitch
                                                    );
                                                } catch (IllegalArgumentException e) {
                                                    location[0].getWorld().playSound(
                                                            location[0],
                                                            elements[0],
                                                            volume,
                                                            pitch
                                                    );
                                                }
                                            },
                                            () -> boss.getEntity() == null,
                                            delay, period, repeats
                                    );
                                } catch (NumberFormatException e) {
                                    Main.logger().error("[sound] Третий аргумент должен быть числом");
                                }
                            } catch (NumberFormatException e) {
                                Main.logger().error("[sound] Второй аргумент должен быть числом");
                            }
                        } else {
                            Main.logger().error("[sound] Ожидается 3 аргумента: <sound>:<volume>:<pitch>");
                        }
                    } else {
                        Main.logger().error("[sound] Ожидается 3 аргумента: <sound>:<volume>:<pitch>");
                    }
                    break;
                }
                case "attribute": {
                    if (value != null) {
                        String[] elements = value.split(":");
                        if (elements.length == 2) {
                            try {
                                Attribute attribute = Attribute.valueOf(elements[0]);
                                try {
                                    double attributeValue = Double.parseDouble(elements[1]);
                                    ActionTask.create(boss,
                                            () -> boss.getEntity().getAttribute(attribute).setBaseValue(attributeValue),
                                            () -> boss.getEntity() == null,
                                            delay, period, repeats
                                    );
                                } catch (NumberFormatException e) {
                                    Main.logger().error("[attribute] Второй аргумент должен быть числом");
                                }
                            } catch (IllegalArgumentException e) {
                                Main.logger().error("[attribute] Первый аргумент должен быть названием аттрибута");
                            }
                        } else {
                            Main.logger().error("[attribute] Ожидается 2 аргумента: <attribute>:<value>");
                        }
                    } else {
                        Main.logger().error("[attribute] Ожидается 2 аргумента: <attribute>:<value>");
                    }
                    break;
                }
                case "particles": {
                    if (value != null) {
                        String[] elements = value.split(":");
                        if (elements.length == 2) {
                            try {
                                Particle particle = Particle.valueOf(elements[0]);
                                try {
                                    int count = Integer.parseInt(elements[1]);
                                    final Location[] location = new Location[1];
                                    location[0] = boss.getEntity().getLocation();
                                    ActionTask.create(boss,
                                            () -> {
                                                if (boss.getEntity() != null)
                                                    location[0] = boss.getEntity().getLocation();
                                                location[0].getWorld().spawnParticle(
                                                        particle,
                                                        location[0],
                                                        count
                                                );
                                            },
                                            () -> boss.getEntity() == null,
                                            delay, period, repeats
                                    );
                                } catch (NumberFormatException e) {
                                    Main.logger().error("[particles] Второй аргумент должен быть целым числом");
                                }
                            } catch (IllegalArgumentException e) {
                                Main.logger().error("[particles] Первый аргумент должен быть названием частицы");
                            }
                        } else {
                            Main.logger().error("[particles] Ожидается 2 аргумента: <particle>:<count>");
                        }
                    } else {
                        Main.logger().error("[particles] Ожидается 2 аргумента: <particle>:<count>");
                    }
                    break;
                }
                case "push": {
                    if (value != null) {
                        String[] elements = value.split(":");
                        if (elements.length == 2) {
                            try {
                                double radius = Double.parseDouble(elements[0]);
                                if (radius >= 0) {
                                    try {
                                        double power = Double.parseDouble(elements[1]);
                                        ActionTask.create(boss,
                                                () -> {
                                                    List<Player> players = boss.getEntity().getNearbyEntities(radius, radius, radius).stream()
                                                            .filter(x -> x instanceof Player)
                                                            .filter(x -> boss.getEntity().getLocation().distance(x.getLocation()) <= radius)
                                                            .map(x -> (Player) x)
                                                            .collect(Collectors.toList());
                                                    for (Player player : players) {
                                                        double distance = boss.getEntity().getLocation().distance(player.getLocation());
                                                        double pushPower = MathUtil.inverseLerp(radius, 0, distance) * power;
                                                        try {
                                                            PhysicUtil.pushEntity(player, boss.getEntity().getLocation(), pushPower);
                                                        } catch (IllegalArgumentException ignore) {
                                                        }
                                                    }
                                                },
                                                () -> boss.getEntity() == null,
                                                delay, period, repeats
                                        );
                                    } catch (NumberFormatException e) {
                                        Main.logger().error("[push] Второй аргумент должен быть числом");
                                    }
                                } else {
                                    Main.logger().error("[push] Первый аргумент должен быть неотрицательным числом");
                                }
                            } catch (NumberFormatException e) {
                                Main.logger().error("[push] Первый аргумент должен быть неотрицательным числом");
                            }
                        } else {
                            Main.logger().error("[push] Ожидается 2 аргумента: <radius>:<power>");
                        }
                    } else {
                        Main.logger().error("[push] Ожидается 2 аргумента: <radius>:<power>");
                    }
                    break;
                }
                case "effect": {
                    if (value != null) {
                        String[] elements = value.split(":");
                        if (elements.length == 3) {
                            PotionEffectType effectType = PotionEffectType.getByName(elements[0]);
                            if (effectType != null) {
                                try {
                                    int level = Integer.parseInt(elements[1]);
                                    try {
                                        int duration = Integer.parseInt(elements[2]);
                                        ActionTask.create(boss,
                                                () -> EffectUtil.addEffects(boss.getEntity(), List.of(new PotionEffect(effectType, duration, level))),
                                                () -> boss.getEntity() == null, delay, period, repeats
                                        );
                                    } catch (NumberFormatException e) {
                                        Main.logger().error("[effect] Третий аргумент должен быть целым числом");
                                    }
                                } catch (NumberFormatException e) {
                                    Main.logger().error("[effect] Второй аргумент должен быть целым числом");
                                }
                            } else {
                                Main.logger().error("[effect] Первый аргумент должен быть названием эффекта");
                            }
                        } else {
                            Main.logger().error("[effect] Ожидается 3 аргумента: <effect>:<level>:<duration>");
                        }
                    } else {
                        Main.logger().error("[effect] Ожидается 3 аргумента: <effect>:<level>:<duration>");
                    }
                    break;
                }
                default: {
                    File actionFile = new File("plugins//IncBosses//abilities//" + Main.getInstance().getName() + "//bosses//" + action + ".js");
                    if (actionFile.exists()) {
                        NashornScriptEngineFactory factory = new NashornScriptEngineFactory();
                        ScriptEngine engine = factory.getScriptEngine();
                        String finalAction = action;
                        ActionTask.create(boss,
                                () -> {
                                    try {
                                        Bindings bindings = engine.createBindings();
                                        bindings.put("arg", value);
                                        bindings.put("boss", boss);
                                        engine.eval(Files.newBufferedReader(actionFile.toPath(), StandardCharsets.UTF_8), bindings);
                                    } catch (ScriptException e) {
                                        Main.logger().error("[" + finalAction + "] При выполнении скрипта '" + finalAction + ".js' произошла ошибка!");
                                        Main.logger().error(e.getMessage());
                                    } catch (IOException e) {
                                        Main.logger().error("[" + finalAction + "] Файл '" + finalAction + ".js' не найден!");
                                    }
                                }, delay, period, repeats
                        );
                    } else {
                        Main.logger().error("[" + action + "] Файл '" + action + ".js' не найден!");
                    }
                }
            }
        }
    }
    private void executePlayers(List<String> actions, List<Player> players) {
        for (String string : actions) {
            String[] stringsElems = getAction(string);
            if (stringsElems == null) continue;
            String action = stringsElems[0];
            String value = stringsElems[1];

            long delay = 0;
            long period = 0;
            int repeats = 1;

            Matcher actionArgumentsMatcher = Pattern.compile("^(\\w+)\\((.*)\\)$").matcher(action);
            if (actionArgumentsMatcher.matches()) {
                action = actionArgumentsMatcher.group(1);
                String args = actionArgumentsMatcher.group(2);
                String[] arguments = args.split(";");
                for (String arg : arguments) {
                    Matcher argumentMatcher = Pattern.compile("^(\\w+)=(.*)$").matcher(arg);
                    if (argumentMatcher.matches()) {
                        String argName = argumentMatcher.group(1);
                        String argValue = argumentMatcher.group(2);
                        if (argName.equalsIgnoreCase("delay") || argName.equalsIgnoreCase("del") || argName.equalsIgnoreCase("d")) {
                            Matcher delayMatcher = Pattern.compile("^(\\d+)(\\w+)$").matcher(argValue);
                            if (delayMatcher.matches()) {
                                long delayValue = Long.parseLong(delayMatcher.group(1));
                                String delayMetric = delayMatcher.group(2);
                                if (delayMetric.equalsIgnoreCase("t") || delayMetric.equalsIgnoreCase("tick"))
                                    delay = delayValue;
                                else if (delayMetric.equalsIgnoreCase("s") || delayMetric.equalsIgnoreCase("sec"))
                                    delay = delayValue * 20;
                                else if (delayMetric.equalsIgnoreCase("m") || delayMetric.equalsIgnoreCase("min"))
                                    delay = delayValue * 20 * 60;
                                else
                                    Main.logger().error("Неверная единица измерения параметра '" + argName + "': '" + delayMetric + "'!");
                            } else {
                                AbilityPlugin.logger().error("Значение параметра '" + argName + "' имеет неверный формат: '" + argValue + "'!");
                            }
                        } else if (argName.equalsIgnoreCase("period") || argName.equalsIgnoreCase("per") || argName.equalsIgnoreCase("p")) {
                            Matcher periodMatcher = Pattern.compile("^(\\d+)(\\w+)$").matcher(argValue);
                            if (periodMatcher.matches()) {
                                long periodValue = Long.parseLong(periodMatcher.group(1));
                                String delayMetric = periodMatcher.group(2);
                                if (delayMetric.equalsIgnoreCase("t") || delayMetric.equalsIgnoreCase("tick"))
                                    period = periodValue;
                                else if (delayMetric.equalsIgnoreCase("s") || delayMetric.equalsIgnoreCase("sec"))
                                    period = periodValue * 20;
                                else if (delayMetric.equalsIgnoreCase("m") || delayMetric.equalsIgnoreCase("min"))
                                    period = periodValue * 20 * 60;
                                else
                                    Main.logger().error("Неверная единица измерения параметра '" + argName + "': '" + delayMetric + "'!");
                            } else {
                                AbilityPlugin.logger().error("Значение параметра '" + argName + "' имеет неверный формат: '" + argValue + "'!");
                            }
                        } else if (argName.equalsIgnoreCase("repeats") || argName.equalsIgnoreCase("rep") || argName.equalsIgnoreCase("r")) {
                            Matcher repeatMathcer = Pattern.compile("^(\\d+)$").matcher(argValue);
                            if (repeatMathcer.matches()) {
                                repeats = Integer.parseInt(repeatMathcer.group(1));
                            } else if (argValue.equalsIgnoreCase("while_alive")) {
                                repeats = -1;
                            } else if (argValue.equalsIgnoreCase("while_phase")) {
                                repeats = -2;
                            } else {
                                AbilityPlugin.logger().error("Значение параметра '" + argName + "' имеет неверный формат: '" + argValue + "'!");
                            }
                        } else {
                            AbilityPlugin.logger().error("Параметр '" + argName + "' не найден!");
                        }
                    } else {
                        AbilityPlugin.logger().error("Аргумент '" + arg + "' имеет неверный формат!");
                    }
                }
            } else if (!Pattern.compile("^(\\w+)$").matcher(action).matches()) {
                Main.logger().error("Неверное действие: '" + action + "'");
                continue;
            }

            for (Player player : players) {
                switch (action) {
                    case "message": {
                        if (value != null) {
                            ActionTask.create(boss,
                                    () -> player.sendMessage(ColorUtil.toColor(value
                                            .replace("%player%", player.getName())
                                            .replace("%boss-name%", boss.getData().getBossName())
                                            .replace("%phase-name%", phase.getData().getPhaseName()))),
                                    () -> !player.isOnline(),
                                    delay, period, repeats
                            );
                        } else {
                            Main.logger().error("[message] Ожидается 1 аргумент: <message>");
                        }
                        break;
                    }
                    case "console": {
                        if (value != null) {
                            ActionTask.create(boss,
                                    () -> Main.getInstance().getServer().dispatchCommand(Main.getInstance().getServer().getConsoleSender(), value
                                            .replace("%player%", player.getName())),
                                    () -> !player.isOnline(),
                                    delay, period, repeats
                            );
                        } else {
                            Main.logger().error("[console] Ожидается 1 аргумент: <command>");
                        }
                        break;
                    }
                    case "command": {
                        if (value != null) {
                            ActionTask.create(boss,
                                    () -> Main.getInstance().getServer().dispatchCommand(player, value
                                            .replace("%player%", player.getName())),
                                    () -> !player.isOnline(),
                                    delay, period, repeats
                            );
                        } else {
                            Main.logger().error("[command] Ожидается 1 аргумент: <command>");
                        }
                        break;
                    }
                    case "sound": {
                        if (value != null) {
                            String[] elements = value.split(":");
                            if (elements.length == 3) {
                                try {
                                    float volume = Float.parseFloat(elements[1]);
                                    try {
                                        float pitch = Float.parseFloat(elements[2]);
                                        ActionTask.create(boss,
                                                () -> {
                                                    try {
                                                        Sound sound = Sound.valueOf(elements[0]);
                                                        player.playSound(
                                                                player.getLocation(),
                                                                sound,
                                                                volume,
                                                                pitch
                                                        );
                                                    } catch (IllegalArgumentException e) {
                                                        player.playSound(
                                                                player.getLocation(),
                                                                elements[0],
                                                                volume,
                                                                pitch
                                                        );
                                                    }
                                                },
                                                () -> !player.isOnline(), delay, period, repeats
                                        );
                                    } catch (NumberFormatException e) {
                                        Main.logger().error("[sound] Третий аргумент должен быть числом");
                                    }
                                } catch (NumberFormatException e) {
                                    Main.logger().error("[sound] Второй аргумент должен быть числом");
                                }
                            } else {
                                Main.logger().error("[sound] Ожидается 3 аргумента: <sound>:<volume>:<pitch>");
                            }
                        } else {
                            Main.logger().error("[sound] Ожидается 3 аргумента: <sound>:<volume>:<pitch>");
                        }
                        break;
                    }
                    case "particles": {
                        if (value != null) {
                            String[] elements = value.split(":");
                            if (elements.length == 2) {
                                try {
                                    Particle particle = Particle.valueOf(elements[0]);
                                    try {
                                        int count = Integer.parseInt(elements[1]);
                                        ActionTask.create(boss,
                                                () -> player.getWorld().spawnParticle(
                                                        particle,
                                                        player.getLocation(),
                                                        count
                                                ),
                                                () -> !player.isOnline(), delay, period, repeats
                                        );
                                    } catch (NumberFormatException e) {
                                        Main.logger().error("[particles] Второй аргумент должен быть целым числом");
                                    }
                                } catch (IllegalArgumentException e) {
                                    Main.logger().error("[particles] Первый аргумент должен быть названием частицы");
                                }
                            } else {
                                Main.logger().error("[particles] Ожидается 2 аргумента: <particle>:<count>");
                            }
                        } else {
                            Main.logger().error("[particles] Ожидается 2 аргумента: <particle>:<count>");
                        }
                        break;
                    }
                    case "effect": {
                        if (value != null) {
                            String[] elements = value.split(":");
                            if (elements.length == 3) {
                                PotionEffectType effectType = PotionEffectType.getByName(elements[0]);
                                if (effectType != null) {
                                    try {
                                        int level = Integer.parseInt(elements[1]);
                                        try {
                                            int duration = Integer.parseInt(elements[2]);
                                            ActionTask.create(boss,
                                                    () -> EffectUtil.addEffects(player, List.of(new PotionEffect(effectType, duration, level))),
                                                    () -> !player.isOnline(), delay, period, repeats
                                            );
                                        } catch (NumberFormatException e) {
                                            Main.logger().error("[effect] Третий аргумент должен быть целым числом");
                                        }
                                    } catch (NumberFormatException e) {
                                        Main.logger().error("[effect] Второй аргумент должен быть целым числом");
                                    }
                                } else {
                                    Main.logger().error("[effect] Первый аргумент должен быть названием эффекта");
                                }
                            } else {
                                Main.logger().error("[effect] Ожидается 3 аргумента: <effect>:<level>:<duration>");
                            }
                        } else {
                            Main.logger().error("[effect] Ожидается 3 аргумента: <effect>:<level>:<duration>");
                        }
                        break;
                    }
                    default: {
                        File actionFile = new File("plugins//IncBosses//abilities//" + Main.getInstance().getName() + "//players//" + action + ".js");
                        if (actionFile.exists()) {
                            NashornScriptEngineFactory factory = new NashornScriptEngineFactory();
                            ScriptEngine engine = factory.getScriptEngine();
                            String finalAction = action;
                            ActionTask.create(boss,
                                    () -> {
                                        try {
                                            Bindings bindings = engine.createBindings();
                                            bindings.put("arg", value);
                                            bindings.put("boss", boss);
                                            bindings.put("player", player);
                                            engine.eval(Files.newBufferedReader(actionFile.toPath(), StandardCharsets.UTF_8), bindings);
                                        } catch (ScriptException e) {
                                            Main.logger().error("[" + finalAction + "] При выполнении скрипта '" + finalAction + ".js' произошла ошибка!");
                                            Main.logger().error(e.getMessage());
                                        } catch (IOException e) {
                                            Main.logger().error("[" + finalAction + "] Файл '" + finalAction + ".js' не найден!");
                                        }
                                    }, delay, period, repeats
                            );
                        } else {
                            Main.logger().error("[" + action + "] Файл '" + action + ".js' не найден!");
                        }
                    }
                }
            }
        }
    }

    private void execute(List<String> bossActions, List<String> playersActions, double playersActionsRadius, List<String> fightersActions) {
        executeBosses(bossActions);
        executePlayers(playersActions, boss.getEntity().getNearbyEntities(playersActionsRadius, playersActionsRadius, playersActionsRadius).stream()
                .filter(x -> x instanceof Player)
                .filter(x -> x.getLocation().distance(boss.getEntity().getLocation()) <= playersActionsRadius)
                .map(x -> (Player) x)
                .collect(Collectors.toList()));
        executePlayers(fightersActions, boss.getDamageMap().keySet().stream()
                .map(Bukkit::getPlayer)
                .toList());
    }

    @Override
    public void start(StartReason reason) {
        execute(startActions, startPlayersActions, startPlayersActionsRadius, startFightersActions);
        if (reason == StartReason.SPAWN) {
            execute(spawnActions, spawnPlayersActions, spawnPlayersActionsRadius, spawnFightersActions);
        } else if (reason == StartReason.PHASE_CHANGING) {
            execute(notSpawnActions, notSpawnPlayersActions, notSpawnPlayersActionsRadius, notSpawnFightersActions);
        }
    }

    @Override
    public void stop(StopReason reason) {
        execute(stopActions, stopPlayersActions, stopPlayersActionsRadius, stopFightersActions);
        if (reason == StopReason.DEATH)
            execute(deathActions, deathPlayersActions, deathPlayersActionsRadius, deathFightersActions);
        else if (reason == StopReason.PHASE_CHANGING)
            execute(notDeathActions, notDeathPlayersActions, notDeathPlayersActionsRadius, notDeathFightersActions);
        ActionTask.stopAll();
    }
}
