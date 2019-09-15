package io.th0rgal.oraxen.settings;

import io.th0rgal.oraxen.OraxenPlugin;
import org.bukkit.ChatColor;

public enum Pack implements ConfigEnum {

    URL("Pack.url"),
    SHA1("Pack.SHA1"),
    SEND("Pack.send"),
    GENERATE("Pack.generate"),
    COMPRESSION("Pack.compression"),
    COMMENT("Pack.comment");

    private Object value;

    Pack(String section) {
        this.value = new ConfigsManager(OraxenPlugin.get()).getSettings().get(section);
    }

    public Object getValue() {
        return this.value;
    }

    @Override
    public String toString() {
        return ChatColor.translateAlternateColorCodes('&', this.value.toString());
    }

}