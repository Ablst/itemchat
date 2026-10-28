package com.example.itemchat.client;

import com.example.itemchat.ItemChat;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;

/**
 * 处理服务端下发给本机客户端的两个"打开"包：{@code OpenJeiPacket} 与 {@code OpenQuestPacket}。
 *
 * <p>本类直接引用 {@code Minecraft} 等客户端专有类，因此只能由这两个注册为
 * {@code playToClient} 的包在客户端侧调用 —— 这类处理器在专用服务端上不会被触发，
 * 所以不需要 {@code DistExecutor}（NeoForge 1.21.1 也已经把它删掉了）。</p>
 *
 * <p>文案全部走翻译键（{@link ItemChat#tr}），跟随本机客户端的游戏语言。</p>
 */
public final class ClientPayloadHandler {

    private static final String JEI_MOD_ID = "jei";
    private static final String FTB_QUESTS_MOD_ID = "ftbquests";

    private ClientPayloadHandler() {
    }

    /** 在本地 JEI 中打开该物品的配方界面；JEI 缺席或物品未知时给出友好提示。 */
    public static void openJei(ResourceLocation itemId) {
        // 1.21.1 的 Registry 没有 getValue：ITEM 是 DefaultedRegistry，它的 get() 对
        // 未知 ID 会返回默认值（空气）而不是 null，所以这里必须用 getOptional，
        // 才能和旧版 ForgeRegistries.ITEMS.getValue() 一样区分出"未知物品"。
        Item item = BuiltInRegistries.ITEM.getOptional(itemId).orElse(null);
        if (item == null) {
            notifyPlayer(ItemChat.tr("itemchat.msg.unknown_item", "未知物品：%s", itemId), ChatFormatting.RED);
            return;
        }
        // 先判断 JEI 是否存在，再触碰 JeiIntegration —— 这样 JEI 缺席时该类不会被加载。
        if (!ModList.get().isLoaded(JEI_MOD_ID)) {
            notifyPlayer(ItemChat.tr("itemchat.msg.no_jei", "未检测到 JEI，无法查看物品配方。"), ChatFormatting.YELLOW);
            return;
        }

        closeChatScreen();
        JeiIntegration.showRecipes(new ItemStack(item));
    }

    /** 打开 FTB 任务书并定位到指定任务。 */
    public static void openQuest(long questId) {
        // 同理：先过软依赖守卫，没装 FTB 时 FtbQuestsIntegration 不会被加载。
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
     * <p>点"检查"按钮时聊天界面是开着的：原版 {@code Screen#handleComponentClicked}
     * 处理 {@code RUN_COMMAND} 只负责发命令，不会关闭聊天界面。而 JEI 的
     * {@code RecipesGui#open()} 会把"打开配方界面前的那一个界面"记进 {@code parentScreen}，
     * 退出配方界面时再切回去。如果不先关掉聊天框，退出 JEI 后就会回到"按 T 呼出鼠标"的
     * 聊天输入状态，还得再按一次回车或 Esc 才能继续游戏。</p>
     *
     * <p>这里必须在调用 JEI / FTB 的打开入口 <b>之前</b> 关，因为 parentScreen 是在
     * 打开时当场读取 {@code Minecraft.screen} 的；先关掉，记下的父界面就是 null，
     * 退出时直接回到游戏画面。</p>
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
