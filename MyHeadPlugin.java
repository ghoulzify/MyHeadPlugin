package com.ghoulz.myhead;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.configuration.file.YamlConfiguration;

import java.util.HashMap;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;

public class MyHeadPlugin extends JavaPlugin {

    private File dataFile;
    private YamlConfiguration dataConfig;

    @Override
    public void onEnable() {
        // Prepare data.yml to store last-claim dates.
        if (!getDataFolder().exists()) {
            getDataFolder().mkdirs();
        }
        dataFile = new File(getDataFolder(), "data.yml");
        if (!dataFile.exists()) {
            try {
                dataFile.createNewFile();
            } catch (IOException e) {
                getLogger().severe("Failed to create data.yml: " + e.getMessage());
            }
        }
        dataConfig = YamlConfiguration.loadConfiguration(dataFile);
        getLogger().info("MyHeadPlugin enabled.");
    }

    @Override
    public void onDisable() {
        saveData();
        getLogger().info("MyHeadPlugin disabled.");
    }

    private void saveData() {
        if (dataConfig != null && dataFile != null) {
            try {
                dataConfig.save(dataFile);
            } catch (IOException e) {
                getLogger().severe("Failed to save data.yml: " + e.getMessage());
            }
        }
    }

    private boolean hasClaimedToday(UUID uuid) {
        String path = "claims." + uuid.toString();
        String last = dataConfig.getString(path, null);
        if (last == null) return false;

        LocalDate lastDate;
        try {
            lastDate = LocalDate.parse(last);
        } catch (Exception e) {
            return false;
        }

        LocalDate today = LocalDate.now(ZoneId.systemDefault());
        return lastDate.isEqual(today);
    }

    private void recordClaim(UUID uuid) {
        String path = "claims." + uuid.toString();
        LocalDate today = LocalDate.now(ZoneId.systemDefault());
        dataConfig.set(path, today.toString()); // yyyy-MM-dd
        saveData();
    }

    private void giveOwnHead(Player player) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD, 1);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        if (meta != null) {
            meta.setOwningPlayer(player);
            meta.setDisplayName(ChatColor.RESET + player.getName() + "'s Head");
            head.setItemMeta(meta);
        }
        // Try add to inventory, else drop
        HashMap<Integer, ItemStack> leftovers = player.getInventory().addItem(head);
        if (!leftovers.isEmpty()) {
            player.getWorld().dropItemNaturally(player.getLocation(), head);
            player.sendMessage(ChatColor.GREEN + "You received your head! (Dropped on the ground; inventory full)");
        } else {
            player.sendMessage(ChatColor.GREEN + "You received your head!");
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!command.getName().equalsIgnoreCase("myhead")) return false;

        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this command.");
            return true;
        }

        Player player = (Player) sender;
        UUID uuid = player.getUniqueId();

        // Enforce once per real-world day (midnight server time reset)
        if (hasClaimedToday(uuid)) {
            player.sendMessage(ChatColor.YELLOW + "You've already claimed your head today. Try again after midnight.");
            return true;
        }

        giveOwnHead(player);
        recordClaim(uuid);
        return true;
    }
}