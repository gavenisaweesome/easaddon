/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.world.item.Item
 *  net.minecraft.world.item.Item$Properties
 *  net.neoforged.neoforge.registries.DeferredItem
 *  net.neoforged.neoforge.registries.DeferredRegister
 *  net.neoforged.neoforge.registries.DeferredRegister$Items
 */
package dev.protomanly.pmweather.item;

import dev.protomanly.pmweather.item.ConnectorItem;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems((String)"pmweather");
    public static final DeferredItem<Item> CONNECTOR = ITEMS.register("connector", () -> new ConnectorItem(new Item.Properties().stacksTo(1)));
    public static final DeferredItem<Item> WEATHER_BALLOON = ITEMS.register("weather_balloon", () -> new Item(new Item.Properties().stacksTo(8)));
}

