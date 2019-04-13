/**
 * This file is the part of TemporalWhitelist plug-in.
 *
 * Copyright (c) 2019 Vasiliy (NyanGuyMF)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package nyanguymf.whitelist.core;

import static nyanguymf.whitelist.core.db.WhitelistedPlayer.allPlayers;
import static org.bukkit.Bukkit.getConsoleSender;
import static org.bukkit.Bukkit.getScheduler;

import java.io.File;
import java.io.IOException;
import java.util.Date;

import org.bukkit.plugin.java.JavaPlugin;

import nyanguymf.whitelist.core.commands.WhitelistCommand;
import nyanguymf.whitelist.core.db.DatabaseManager;
import nyanguymf.whitelist.core.db.WhitelistedPlayer;

/** @author NyanGuyMF - Vasiliy Bely */
public final class TemporalWhitelistPlugin extends JavaPlugin {
    private DatabaseManager databaseManager;
    private MessagesManager messagesManager;

    @Override public void onLoad() {
        if (!super.getDataFolder().exists()) {
            super.getDataFolder().mkdir();
        }
        if (!new File(super.getDataFolder(), "config.yml").exists()) {
            super.saveDefaultConfig();
        }

        databaseManager = new DatabaseManager(super.getDataFolder())
                .connect()
                .initDaos()
                .createTables();
        try {
            messagesManager = MessagesManager.getInstance(
                super.getDataFolder(),
                super.getConfig().getString("lang", "en")
            );
        } catch (IOException ex) {
            System.err.printf(
                "Unable to load messages file: %s\n",
                ex.getLocalizedMessage()
            );
        }
    }

    @Override public void onEnable() {
        if ((messagesManager == null) || (databaseManager == null)) {
            getConsoleSender().sendMessage(
                "\u00a73TemporalWhitelist \u00a78» \u00a7cPlugin isn't enabled."
            );
            super.getServer().getPluginManager().disablePlugin(this);
            return;
        }

        new WhitelistCommand(messagesManager).register(this);

        getScheduler().runTaskLaterAsynchronously(this, () -> {
            Date currentDate = new Date();

            for (WhitelistedPlayer player : allPlayers()) {
                if (player.isWhitelisted() && (player.getUntil() != null)) {
                    if (player.getUntil().after(currentDate)) {
                        player.setUntil(null);
                        player.setWhitelisted(false);
                        player.save();
                    }
                }
            }
        }, 20 * 1_800); // run every 30 minutes

        getConsoleSender().sendMessage(
            "\u00a73TemporalWhitelist \u00a78» \u00a7aPlugin enabled."
        );
    }

    @Override public void onDisable() {
        databaseManager.close();
    }
}
