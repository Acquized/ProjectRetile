/*
 * Copyright 2016 Acquized
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.acquized.retile.commands;

import com.github.acquized.retile.ProjectRetile;
import com.github.acquized.retile.api.RetileAPIException;
import com.github.acquized.retile.reports.Report;
import com.sk89q.minecraft.util.commands.Command;
import com.sk89q.minecraft.util.commands.CommandContext;
import com.sk89q.minecraft.util.commands.CommandException;
import com.sk89q.minecraft.util.commands.CommandPermissions;

import net.md_5.bungee.api.CommandSender;

import java.util.Arrays;
import java.util.Date;

import dev.wolveringer.bungeeutil.inventory.Inventory;
import dev.wolveringer.bungeeutil.item.ItemStack;
import dev.wolveringer.bungeeutil.item.Material;
import dev.wolveringer.bungeeutil.item.meta.SkullMeta;
import dev.wolveringer.bungeeutil.packets.PacketPlayInWindowClick;
import dev.wolveringer.bungeeutil.player.Player;

import static com.github.acquized.retile.utils.Utility.DARK_AQUA;
import static com.github.acquized.retile.utils.Utility.GRAY;
import static com.github.acquized.retile.utils.Utility.GREEN;
import static com.github.acquized.retile.utils.Utility.RED;
import static com.github.acquized.retile.utils.Utility.formatLegacy;

public class QueueCommand {

    @CommandPermissions({ "projectretile.commands.queue" })
    @Command(aliases = { "waitingqueue", "listqueue", "queue" },
             desc = "Shows a Gui with all waiting reports. Executing this command moves all to the normal queue.", max = 0)
    public static void onQueue(CommandSender sender, CommandContext args) throws CommandException {
        Report[] reports;
        try {
            reports = ProjectRetile.getInstance().getApi().getWaitingReports();
        } catch (RetileAPIException ex) {
            throw new CommandException("", ex);
        }
        Inventory inv = new Inventory(54, "Reports in Queue (" + reports.length + ")");
        Player p = (Player) sender;
        int slot = 0;
        for(final Report r : reports) {
            String reporter = ProjectRetile.getInstance().getCache().username(r.getReporter());
            final String victim = ProjectRetile.getInstance().getCache().username(r.getVictim());
            ItemStack item = new ItemStack(Material.SKULL_ITEM, 1, (short) 3) {
                @Override
                public void click(Click click) {
                    try {
                        if(click.getMode() == PacketPlayInWindowClick.Mode.NORMAL_LEFT_CLICK) {
                            click.getPlayer().connect(ProjectRetile.getInstance().getApi().resolveServer(r.getVictim()));
                        } else if(click.getMode() == PacketPlayInWindowClick.Mode.NORMAL_RIGHT_CLICK) {
                            ProjectRetile.getInstance().getApi().removeReport(r);
                            click.getPlayer().sendMessage(formatLegacy(RED + "> " + GRAY + "Report " + DARK_AQUA + r.getToken() + GRAY + " deleted."));
                            click.getPlayer().closeInventory();
                        }
                    } catch (RetileAPIException ex) {
                        ProjectRetile.getInstance().getLog().error("Could not resolve server of " + victim, ex);
                    }
                }
            };
            SkullMeta meta = (SkullMeta) item.getItemMeta();
            meta.setSkullOwner(victim);
            meta.setDisplayName(DARK_AQUA + victim);
            meta.setLore(Arrays.asList(GRAY + "ID: " + DARK_AQUA + r.getToken(),
                    GRAY + "Reported by: " + DARK_AQUA + reporter,
                    GRAY + "Reason: " + DARK_AQUA + r.getReason(),
                    GRAY + "Time: " + DARK_AQUA + ProjectRetile.getInstance().getDateFormat().format(new Date(r.getTimestamp())),
                    GRAY + " ",
                    GREEN + "Left click to connect",
                    RED + "Right click to delete"));
            inv.setItem(slot, item);
            slot++;
        }
        try {
            ProjectRetile.getInstance().getApi().clearWaitingQueue();
        } catch (RetileAPIException ex) {
            throw new CommandException("", ex);
        }
        p.openInventory(inv);
    }

}
