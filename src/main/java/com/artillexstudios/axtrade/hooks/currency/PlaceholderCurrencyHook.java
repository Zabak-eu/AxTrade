package com.artillexstudios.axtrade.hooks.currency;

import com.artillexstudios.axapi.libs.boostedyaml.block.implementation.Section;
import com.artillexstudios.axtrade.trade.TradePlayer;
import com.artillexstudios.axtrade.trade.Trades;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.text.DecimalFormat;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class PlaceholderCurrencyHook implements CurrencyHook {
    private final String name;
    private final Section section;
    private DecimalFormat df;

    public PlaceholderCurrencyHook(String name, Section section) {
        this.name = name;
        this.section = section;
    }

    @Override
    public void setup() {
        df = new DecimalFormat("#");
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Map<String, Object> getSettings() {
        return section.getStringRouteMappedValues(true);
    }

    @Override
    public boolean worksOffline() {
        return section.getBoolean("works-offline", false);
    }

    @Override
    public boolean usesDouble() {
        return section.getBoolean("uses-double", false);
    }

    @Override
    public boolean isPersistent() {
        return false;
    }

    @Override
    public double getBalance(@NotNull UUID player) {
        final OfflinePlayer pl = Bukkit.getOfflinePlayer(player);
        final String placeholder = section.getString("settings.raw-placeholder");
        return Double.parseDouble(PlaceholderAPI.setPlaceholders(pl.getPlayer() == null ? pl : pl.getPlayer(), placeholder));
    }

    @NotNull
    @ParametersAreNonnullByDefault
    private CompletableFuture<Boolean> processBalance(UUID playerUUID, double amount, String cmdPath) {
        final OfflinePlayer pl = Bukkit.getOfflinePlayer(playerUUID);
        if (pl.getName() == null) {
            return CompletableFuture.completedFuture(false);
        }

        // Maybe use OfflinePlayer in trades instead of Player? Idk, maybe there is reason for that tho...
        @Nullable
        final Player partner = Optional.ofNullable(pl.getPlayer())
                .map(Trades::getTrade)
                .map(t -> t.getPlayer1().getPlayer().getUniqueId().equals(playerUUID) ? t.getPlayer2() : t.getPlayer1())
                .map(TradePlayer::getPlayer).orElse(null);

        String placeholder = section.getString("settings." + cmdPath)
                .replace("%amount%", parseNumber(amount))
                .replace("%player%", pl.getName());
        if (partner != null)
            placeholder = placeholder.replace("%partner%", partner.getName());

        return CompletableFuture.completedFuture(Bukkit.dispatchCommand(Bukkit.getConsoleSender(), placeholder));
    }

    @Override
    public CompletableFuture<Boolean> giveBalance(@NotNull UUID player, double amount) {
        return processBalance(player, amount, "give-command");
    }

    @Override
    public CompletableFuture<Boolean> takeBalance(@NotNull UUID player, double amount) {
        return processBalance(player, amount, "take-command");
    }

    private String parseNumber(double amount) {
        return df.format(usesDouble() ? amount : Math.round(amount));
    }
}