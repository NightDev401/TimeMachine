package com.github.zwarunek.timemachine.util;

import com.github.zwarunek.timemachine.TimeMachine;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import javax.net.ssl.HttpsURLConnection;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;

public class UpdateChecker {

    private final JavaPlugin javaPlugin;
    private final String localPluginVersion;
    private String githubPluginVersion;

    //Constants. Customize to your liking.
    private static final int ID = 83621; //The ID of your resource. Can be found in the resource URL.
    private int checkInterval; //In ticks.
    //PermissionDefault.FALSE == OPs need the permission to be notified.
    //PermissionDefault.TRUE == all OPs are notified regardless of having the permission.
    private static final Permission UPDATE_PERM = new Permission("timemachine.update", PermissionDefault.FALSE);

    public UpdateChecker(final JavaPlugin javaPlugin) {
        this.javaPlugin = javaPlugin;
        this.localPluginVersion = javaPlugin.getDescription().getVersion();
        this.checkInterval = javaPlugin.getConfig().getInt("updateCheckInterval", 72000);
    }

    public void checkForUpdate() {
        new BukkitRunnable() {
            @Override
            public void run() {
                //The request is executed asynchronously as to not block the main thread.
                Bukkit.getScheduler().runTaskAsynchronously(javaPlugin, () -> {
                    //Request the current version of your plugin on SpigotMC.
                    check(this);
                });
            }
        }.runTaskTimer(javaPlugin, 0, checkInterval * 20);
    }
    public void check(BukkitRunnable runable) {
        try {
            final HttpsURLConnection connection = (HttpsURLConnection) new URL("https://api.github.com/repos/zwarunek/timemachine/releases/latest").openConnection();
            connection.setRequestMethod("GET");

            StringBuilder responseString = new StringBuilder();
            BufferedReader in = new BufferedReader(new InputStreamReader(
                    connection.getInputStream()));
            String inputLine;

            while ((inputLine = in.readLine()) != null) {
                responseString.append(inputLine);
            }
            in.close();
            JSONObject response;
            JSONParser parser = new JSONParser();
            Object temp = parser.parse(String.valueOf(responseString));
            response = (JSONObject) temp;
            githubPluginVersion = response.get("tag_name").toString().substring(1);
            System.out.println();
        } catch (final IOException | ParseException e) {
            Bukkit.getServer().getConsoleSender().sendMessage(TimeMachine.NAME + "Error checking for new version");
            e.printStackTrace();
            runable.cancel();
            return;
        }

        //Check if the requested version is the same as the one in your plugin.yml.
        if (localPluginVersion.equals(githubPluginVersion)) return;

        Bukkit.getServer().getConsoleSender().sendMessage(TimeMachine.NAME + "This is an outdated version. Version " + githubPluginVersion + " is out at: " + ChatColor.RESET + "https://www.spigotmc.org/resources/" + ID + "/history");

        //Register the PlayerJoinEvent
        Bukkit.getScheduler().runTask(javaPlugin, () -> Bukkit.getPluginManager().registerEvents(new Listener() {
            @EventHandler(priority = EventPriority.MONITOR)
            public void onPlayerJoin(final PlayerJoinEvent event) {
                final Player player = event.getPlayer();
                if (!player.hasPermission(UPDATE_PERM)) return;
                player.sendMessage(TimeMachine.NAME + "This is an outdated version. Version " + githubPluginVersion + " is out at: " + ChatColor.RESET + "https://www.spigotmc.org/resources/" + ID + "/history");
            }
        }, javaPlugin));
        runable.cancel(); //Cancel the runnable as an update has been found.
    }
}
