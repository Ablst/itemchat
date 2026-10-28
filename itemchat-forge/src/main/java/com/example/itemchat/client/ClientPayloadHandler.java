package com.example.itemchat.client;

import com.example.itemchat.ItemChat;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * 处理服务端下发给本机客户端的两个"打开"包：{@code OpenJeiPacket} 与 {@code OpenQuestPacket}。
 *
 * <p>只有在物理客户端才会被加载（调用方是 {@code DistExecutor.unsafeRunWhenOn(Dist.CLIENT, ...)}），
 * 所以这里可以放心引用客户端类。</p>
 */
public final class ClientPayloadHandler {

    private static final String JEI_MOD_ID = "jei";
    private static final String FTB_QUESTS_MOD_ID = "ftbquests";

    private ClientPayloadHandler() {
    }

    /** 打开某物品的 JEI 配方界面。 */
    public static void openJei(ResourceLocation itemId) {
        Item item = ForgeRegistries.ITEMS.getValue(itemId);
        if (item == null) {
            notifyPlayer(ItemChat.tr("itemchat.msg.unknown_item", "未知物品：%s", itemId), ChatFormatting.RED);
            return;
        }
        if (!ModList.get().isLoaded(JEI_MOD_ID)) {
            notifyPlayer(ItemChat.tr("itemchat.msg.no_jei", "未检测到 JEI，无法查看物品配方。"), ChatFormatting.YELLOW);
            return;
        }

        closeChatScreen();
        JeiIntegration.showRecipes(new ItemStack(item));
    }

    /** 打开 FTB 任务书并定位到指定任务。 */
    public static void openQuest(long questId) {
        if (!ModList.get().isLoaded(FTB_QUESTS_MOD_ID)) {
            notifyPlayer(ItemChat.tr("itemchat.msg.no_ftbquests_client", "未检测到 FTB 任务，无法查看该任务。"),
                    ChatFormatting.YELLOW);
            return;
        }

        closeChatScreen();
        FtbQuestsIntegration.openQuest(questId);
    }

    /**
     * 打开 JEI / 任务书之前先把聊天界面关掉。
     *
     * <p>否则退出 JEI 之后聊天输入状态会残留，玩家得再按一次 Esc 或 Enter
     * 才能回到正常游戏视角。</p>
     */
    private static void closeChatScreen() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen instanceof ChatScreen) {
            minecraft.setScreen(null);
        }
    }

    private static void notifyPlayer(Component text, ChatFormatting color) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null) {
            player.displayClientMessage(text.copy().withStyle(color), false);
        }
    }
}
