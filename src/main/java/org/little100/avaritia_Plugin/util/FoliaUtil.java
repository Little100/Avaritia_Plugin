package org.little100.avaritia_Plugin.util;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public class FoliaUtil {

    private static boolean isFolia = false;
    private static boolean checked = false;

    public static boolean isFolia() {
        if (!checked) {
            try {
                Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
                isFolia = true;
            } catch (ClassNotFoundException e) {
                isFolia = false;
            }
            checked = true;
        }
        return isFolia;
    }

    public static void runSync(JavaPlugin plugin, Runnable task) {
        if (isFolia()) {
            try {

                Object globalRegionScheduler = Bukkit.getServer().getClass().getMethod("getGlobalRegionScheduler")
                        .invoke(Bukkit.getServer());

                java.lang.reflect.Method executeMethod = globalRegionScheduler.getClass().getMethod(
                        "execute",
                        org.bukkit.plugin.Plugin.class,
                        Runnable.class);

                executeMethod.invoke(globalRegionScheduler, plugin, task);
            } catch (Exception e) {

                plugin.getLogger().warning("Folia调度器调用失败，使用传统调度器: " + e.getMessage());
                e.printStackTrace();
                Bukkit.getScheduler().runTask(plugin, task);
            }
        } else {

            if (Bukkit.isPrimaryThread()) {

                task.run();
            } else {
                Bukkit.getScheduler().runTask(plugin, task);
            }
        }
    }

    public static void runSyncLater(JavaPlugin plugin, Runnable task, long delay) {
        if (isFolia()) {
            try {

                Object globalRegionScheduler = Bukkit.getServer().getClass().getMethod("getGlobalRegionScheduler")
                        .invoke(Bukkit.getServer());

                java.lang.reflect.Method runDelayedMethod = globalRegionScheduler.getClass().getMethod(
                        "runDelayed",
                        org.bukkit.plugin.Plugin.class,
                        java.util.function.Consumer.class,
                        long.class);

                runDelayedMethod.invoke(
                        globalRegionScheduler,
                        plugin,
                        (java.util.function.Consumer<io.papermc.paper.threadedregions.scheduler.ScheduledTask>) (
                                t) -> task.run(),
                        delay);
            } catch (Exception e) {
                plugin.getLogger().warning("Folia延迟调度器调用失败: " + e.getMessage());
                e.printStackTrace();

                Bukkit.getScheduler().runTaskLater(plugin, task, delay);
            }
        } else {

            Bukkit.getScheduler().runTaskLater(plugin, task, delay);
        }
    }

    public static void runAsync(JavaPlugin plugin, Runnable task) {
        if (isFolia()) {
            try {

                Object asyncScheduler = Bukkit.getServer().getClass().getMethod("getAsyncScheduler")
                        .invoke(Bukkit.getServer());
                asyncScheduler.getClass().getMethod("runNow", JavaPlugin.class, Runnable.class)
                        .invoke(asyncScheduler, plugin, task);
            } catch (Exception e) {
                plugin.getLogger().warning("Folia异步调度器调用失败: " + e.getMessage());

                Bukkit.getScheduler().runTaskAsynchronously(plugin, task);
            }
        } else {

            Bukkit.getScheduler().runTaskAsynchronously(plugin, task);
        }
    }

    public static void runAtLocation(JavaPlugin plugin, org.bukkit.Location location, Runnable task) {
        if (isFolia()) {
            try {

                Object regionScheduler = Bukkit.getServer().getClass().getMethod("getRegionScheduler")
                        .invoke(Bukkit.getServer());
                regionScheduler.getClass()
                        .getMethod("execute", JavaPlugin.class, org.bukkit.Location.class, Runnable.class)
                        .invoke(regionScheduler, plugin, location, task);
            } catch (Exception e) {
                plugin.getLogger().warning("Folia区域调度器调用失败: " + e.getMessage());

                runSync(plugin, task);
            }
        } else {

            runSync(plugin, task);
        }
    }

    public static void runAtLocationDelayed(JavaPlugin plugin, org.bukkit.Location location, Runnable task,
            long delayTicks) {
        if (isFolia()) {
            try {

                Object regionScheduler = Bukkit.getServer().getClass().getMethod("getRegionScheduler")
                        .invoke(Bukkit.getServer());

                java.lang.reflect.Method runDelayedMethod = regionScheduler.getClass().getMethod(
                        "runDelayed",
                        org.bukkit.plugin.Plugin.class,
                        org.bukkit.Location.class,
                        java.util.function.Consumer.class,
                        long.class);

                runDelayedMethod.invoke(
                        regionScheduler,
                        plugin,
                        location,
                        (java.util.function.Consumer<io.papermc.paper.threadedregions.scheduler.ScheduledTask>) (
                                t) -> task.run(),
                        delayTicks);
            } catch (Exception e) {
                plugin.getLogger().warning("Folia区域延迟调度失败: " + e.getMessage());
                e.printStackTrace();

                runSync(plugin, task);
            }
        } else {

            Bukkit.getScheduler().runTaskLater(plugin, task, delayTicks);
        }
    }

    public static io.papermc.paper.threadedregions.scheduler.ScheduledTask runAtLocation(
            JavaPlugin plugin, org.bukkit.Location location, Runnable task, long delayTicks, long periodTicks) {
        if (isFolia()) {
            try {

                Object regionScheduler = Bukkit.getServer().getClass().getMethod("getRegionScheduler")
                        .invoke(Bukkit.getServer());

                java.lang.reflect.Method runAtFixedRateMethod = regionScheduler.getClass().getMethod(
                        "runAtFixedRate",
                        org.bukkit.plugin.Plugin.class,
                        org.bukkit.Location.class,
                        java.util.function.Consumer.class,
                        long.class,
                        long.class);

                Object scheduledTask = runAtFixedRateMethod.invoke(
                        regionScheduler,
                        plugin,
                        location,
                        (java.util.function.Consumer<io.papermc.paper.threadedregions.scheduler.ScheduledTask>) (
                                t) -> task.run(),
                        delayTicks,
                        periodTicks);

                return (io.papermc.paper.threadedregions.scheduler.ScheduledTask) scheduledTask;
            } catch (Exception e) {
                plugin.getLogger().warning("Folia区域重复任务调度失败: " + e.getMessage());
                e.printStackTrace();
                return null;
            }
        } else {

            final int[] taskId = new int[1];
            taskId[0] = Bukkit.getScheduler().runTaskTimer(plugin, task, delayTicks, periodTicks).getTaskId();

            return new io.papermc.paper.threadedregions.scheduler.ScheduledTask() {
                private boolean cancelled = false;

                @Override
                public io.papermc.paper.threadedregions.scheduler.ScheduledTask.ExecutionState getExecutionState() {
                    return cancelled ? io.papermc.paper.threadedregions.scheduler.ScheduledTask.ExecutionState.CANCELLED
                            : io.papermc.paper.threadedregions.scheduler.ScheduledTask.ExecutionState.RUNNING;
                }

                @Override
                public io.papermc.paper.threadedregions.scheduler.ScheduledTask.CancelledState cancel() {
                    if (!cancelled) {
                        Bukkit.getScheduler().cancelTask(taskId[0]);
                        cancelled = true;
                        return io.papermc.paper.threadedregions.scheduler.ScheduledTask.CancelledState.CANCELLED_BY_CALLER;
                    }
                    return io.papermc.paper.threadedregions.scheduler.ScheduledTask.CancelledState.NEXT_RUNS_CANCELLED;
                }

                @Override
                public JavaPlugin getOwningPlugin() {
                    return plugin;
                }

                @Override
                public boolean isRepeatingTask() {
                    return true;
                }
            };
        }
    }

    public static void runLater(JavaPlugin plugin, org.bukkit.entity.Entity entity, Runnable task, long delayTicks) {

        Bukkit.getScheduler().runTaskLater(plugin, task, delayTicks);
    }

    public static void runTimer(JavaPlugin plugin, org.bukkit.Location location,
            java.util.function.Consumer<io.papermc.paper.threadedregions.scheduler.ScheduledTask> task,
            long delay, long period) {
        if (isFolia()) {
            try {

                Object regionScheduler = Bukkit.getServer().getClass().getMethod("getRegionScheduler")
                        .invoke(Bukkit.getServer());

                java.lang.reflect.Method runAtFixedRateMethod = regionScheduler.getClass().getMethod(
                        "runAtFixedRate",
                        org.bukkit.plugin.Plugin.class,
                        org.bukkit.Location.class,
                        java.util.function.Consumer.class,
                        long.class,
                        long.class);

                runAtFixedRateMethod.invoke(
                        regionScheduler,
                        plugin,
                        location,
                        task,
                        delay,
                        period);
            } catch (Exception e) {
                plugin.getLogger().warning("Folia区域定时器调度失败: " + e.getMessage());
                e.printStackTrace();
            }
        } else {

            final int[] taskId = new int[1];
            org.bukkit.scheduler.BukkitTask bukkitTask = Bukkit.getScheduler().runTaskTimer(plugin, new Runnable() {
                @Override
                public void run() {

                    io.papermc.paper.threadedregions.scheduler.ScheduledTask wrappedTask = new io.papermc.paper.threadedregions.scheduler.ScheduledTask() {
                        private boolean cancelled = false;

                        @Override
                        public io.papermc.paper.threadedregions.scheduler.ScheduledTask.ExecutionState getExecutionState() {
                            return cancelled
                                    ? io.papermc.paper.threadedregions.scheduler.ScheduledTask.ExecutionState.CANCELLED
                                    : io.papermc.paper.threadedregions.scheduler.ScheduledTask.ExecutionState.RUNNING;
                        }

                        @Override
                        public io.papermc.paper.threadedregions.scheduler.ScheduledTask.CancelledState cancel() {
                            if (!cancelled) {
                                Bukkit.getScheduler().cancelTask(taskId[0]);
                                cancelled = true;
                                return io.papermc.paper.threadedregions.scheduler.ScheduledTask.CancelledState.CANCELLED_BY_CALLER;
                            }
                            return io.papermc.paper.threadedregions.scheduler.ScheduledTask.CancelledState.NEXT_RUNS_CANCELLED;
                        }

                        @Override
                        public JavaPlugin getOwningPlugin() {
                            return plugin;
                        }

                        @Override
                        public boolean isRepeatingTask() {
                            return true;
                        }
                    };

                    task.accept(wrappedTask);
                }
            }, delay, period);

            taskId[0] = bukkitTask.getTaskId();
        }
    }

    public static void runGlobalTimer(JavaPlugin plugin, Runnable task, long delay, long period) {
        if (isFolia()) {
            try {

                Object globalScheduler = Bukkit.getServer().getClass().getMethod("getGlobalRegionScheduler")
                        .invoke(Bukkit.getServer());

                java.lang.reflect.Method runAtFixedRateMethod = globalScheduler.getClass().getMethod(
                        "runAtFixedRate",
                        org.bukkit.plugin.Plugin.class,
                        java.util.function.Consumer.class,
                        long.class,
                        long.class);

                runAtFixedRateMethod.invoke(
                        globalScheduler,
                        plugin,
                        (java.util.function.Consumer<io.papermc.paper.threadedregions.scheduler.ScheduledTask>) (
                                t) -> task.run(),
                        delay,
                        period);
            } catch (Exception e) {
                plugin.getLogger().warning("Folia全局定时器调度失败: " + e.getMessage());
                e.printStackTrace();
            }
        } else {

            Bukkit.getScheduler().runTaskTimer(plugin, task, delay, period);
        }
    }

    public static void runEntityTask(JavaPlugin plugin, org.bukkit.entity.Entity entity, Runnable task) {
        if (isFolia()) {
            try {

                Object entityScheduler = entity.getClass().getMethod("getScheduler").invoke(entity);
                java.lang.reflect.Method runMethod = entityScheduler.getClass().getMethod(
                        "run",
                        org.bukkit.plugin.Plugin.class,
                        java.util.function.Consumer.class,
                        Runnable.class);

                runMethod.invoke(
                        entityScheduler,
                        plugin,
                        (java.util.function.Consumer<io.papermc.paper.threadedregions.scheduler.ScheduledTask>) (
                                t) -> task.run(),
                        null);
            } catch (Exception e) {
                plugin.getLogger().warning("Folia实体调度器调用失败: " + e.getMessage());

                Bukkit.getScheduler().runTask(plugin, task);
            }
        } else {

            Bukkit.getScheduler().runTask(plugin, task);
        }
    }

    public static void runEntityTaskLater(JavaPlugin plugin, org.bukkit.entity.Entity entity, Runnable task,
            long delay) {
        if (isFolia()) {
            try {

                Object entityScheduler = entity.getClass().getMethod("getScheduler").invoke(entity);
                java.lang.reflect.Method runDelayedMethod = entityScheduler.getClass().getMethod(
                        "runDelayed",
                        org.bukkit.plugin.Plugin.class,
                        java.util.function.Consumer.class,
                        Runnable.class,
                        long.class);

                runDelayedMethod.invoke(
                        entityScheduler,
                        plugin,
                        (java.util.function.Consumer<io.papermc.paper.threadedregions.scheduler.ScheduledTask>) (
                                t) -> task.run(),
                        null,
                        delay);
            } catch (Exception e) {
                plugin.getLogger().warning("Folia实体延迟调度器调用失败: " + e.getMessage());

                Bukkit.getScheduler().runTaskLater(plugin, task, delay);
            }
        } else {

            Bukkit.getScheduler().runTaskLater(plugin, task, delay);
        }
    }

    public static String getServerType() {
        if (isFolia()) {
            return "Folia";
        }

        try {
            Class.forName("io.papermc.paper.configuration.Configuration");
            return "Paper";
        } catch (ClassNotFoundException e) {

        }

        try {
            Class.forName("org.spigotmc.SpigotConfig");
            return "Spigot";
        } catch (ClassNotFoundException e) {
            return "CraftBukkit";
        }
    }
}
