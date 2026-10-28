package com.example.itemchat.server;

import com.example.itemchat.ItemChat;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * 把「某玩家展示了某物品」构造成富文本并广播给服务器里所有人。
 *
 * <p>为什么必须由服务端构造：玩家自己发出去的聊天只有纯文本，服务端不会转发
 * {@code ClickEvent} / {@code HoverEvent}。只有服务端下发的组件，别的玩家才会看到
 * 悬停预览和可点击的 [检查] 按钮。</p>
 *
 * <p>文本全部走翻译键（{@link ItemChat#tr}），所以会跟随每个客户端自己的游戏语言；
 * 没装本模组的客户端则会显示 fallback 里的中文原文，不会看到裸的键名。</p>
 */
public final class ItemBroadcaster {

    private ItemBroadcaster() {
    }

    public static void broadcast(ServerPlayer sender, ItemStack stack) {
        if (stack.isEmpty()) {
            return;
        }

        MinecraftServer server = sender.getServer();
        if (server == null) {
            return;
        }

        ResourceLocation itemId = ForgeRegistries.ITEMS.getKey(stack.getItem());
        if (itemId == null) {
            return;
        }

        // [检查] 按钮的动作：跑一条服务端命令，由服务端校验后再下发 OpenJeiPacket 给点击者。
        // 之所以不直接在客户端调命令，是为了避免服务器上「未知命令」的报错。
        ClickEvent openInJei = new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/itemchat jei " + itemId);

        // 物品名：绿色 + 悬停显示完整 tooltip + 点击也能触发 [检查]
        MutableComponent itemLabel = stack.getHoverName().copy().withStyle(style -> style
                .withColor(TextColor.fromLegacyFormat(ChatFormatting.GREEN))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_ITEM, new HoverEvent.ItemStackInfo(stack)))
                .withClickEvent(openInJei));

        MutableComponent checkButton = ItemChat.tr("itemchat.chat.check", "[检查]").withStyle(style -> style
                .withColor(TextColor.fromLegacyFormat(ChatFormatting.YELLOW))
                // 与原版 1.0.0 一致：加粗
                .withBold(true)
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                        ItemChat.tr("itemchat.chat.check.hover", "点击检查该物品的配方")))
                .withClickEvent(openInJei));

        Component countText = stack.getCount() > 1
                ? ItemChat.tr("itemchat.chat.item_count", " ×%s", stack.getCount())
                : Component.empty();

        MutableComponent message = ItemChat.tr("itemchat.chat.share_item",
                "[物品] %1$s 展示了 %2$s%3$s %4$s",
                Component.literal(sender.getName().getString()).withStyle(ChatFormatting.AQUA),
                itemLabel,
                countText,
                checkButton);

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            player.sendSystemMessage(message);
        }
    }
}
