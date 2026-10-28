package com.example.itemchat.client;

import com.example.itemchat.ItemChat;
import com.example.itemchat.net.ItemChatNetwork;
import com.example.itemchat.net.ShareItemPacket;
import com.example.itemchat.net.ShareQuestPacket;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.InputEvent;
import org.lwjgl.glfw.GLFW;

/**
 * 客户端的中键捕获。
 *
 * <h3>为什么统一用 {@link InputEvent.MouseButton.Pre}，而不是 {@code ScreenEvent}</h3>
 *
 * <p>{@code ScreenEvent.MouseButtonPressed.Pre} 是在 {@code Screen#mouseClicked} 那条链上发的，
 * 很多模组都会监听它，而且一旦有人先 {@code setCanceled(true)}，事件系统就不会再调用
 * 后面那些没声明 {@code receiveCanceled} 的监听器。</p>
 *
 * <p>实测就撞上过这种情况：同一份 jar，在 JEI 15.20 的整合包里中键物品正常，
 * 在 JEI 15.49 的整合包里就没反应（背包槽位、FTB 任务都正常）。</p>
 *
 * <p>所以改成用更底层的 {@link InputEvent.MouseButton.Pre} —— 它在
 * {@code ClientHooks#onMouseButtonPre} 里、<b>把点击分发给屏幕之前</b>触发
 * （NeoForge 源码：{@code return NeoForge.EVENT_BUS.post(new InputEvent.MouseButton.Pre(...)).isCanceled();}），
 * 取消后屏幕（包括 JEI 的浮层）根本收不到这次点击，从根上绕开了"谁先取消"的竞争。</p>
 *
 * <p>优先级仍然是 {@code HIGHEST}，保证我们排在其它模组前面。
 * {@code InputEvent} 属于游戏事件，所以 {@code bus} 不用写 —— NeoForge 21.1 起
 * {@code @EventBusSubscriber} 的 {@code bus} 属性已废弃（待删除），FML 会按处理方法的
 * 事件类型自动判断该挂哪个总线。</p>
 */
@EventBusSubscriber(modid = ItemChat.MOD_ID, value = Dist.CLIENT)
public final class ClientEvents {

    private static final String JEI_MOD_ID = "jei";
    private static final String FTB_QUESTS_MOD_ID = "ftbquests";

    private ClientEvents() {
    }

    /**
     * 中键的统一入口。按「JEI 悬停物品 &gt; FTB 任务 &gt; 容器槽位」的顺序处理，
     * 命中任意一个就吃掉这次中键。
     *
     * <p>这个监听会对所有鼠标按下都触发，所以除了「按下 + 中键」之外一律直接放行，
     * 不会影响任何其它操作。</p>
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onMouseButtonPre(InputEvent.MouseButton.Pre event) {
        if (event.getAction() != GLFW.GLFW_PRESS) {
            return;
        }
        if (event.getButton() != GLFW.GLFW_MOUSE_BUTTON_MIDDLE) {
            return;
        }

        Screen screen = Minecraft.getInstance().screen;
        if (screen == null) {
            return;
        }

        // 1) JEI：物品管理器列表 / 配方界面 / 书签栏里鼠标悬停的物品。
        //    这一条要在容器槽位之前判，因为 JEI 的列表浮层会盖在容器界面之上。
        //    先过 isLoaded 守卫，这样没装 JEI 时 JeiIntegration 这个类根本不会被加载。
        if (ModList.get().isLoaded(JEI_MOD_ID)) {
            ItemStack jeiStack = JeiIntegration.getItemStackUnderMouse();
            if (!jeiStack.isEmpty()) {
                shareItem(jeiStack);
                event.setCanceled(true);
                return;
            }
        }

        // 2) FTB 任务界面里鼠标下的任务。同样先过软依赖守卫。
        //    注意"任务"有两种对象：真任务和 FTB 自动生成的隐形任务链接，
        //    两者都由 getQuestUnderMouse 统一识别，链接的目标交给服务端去追。
        if (ModList.get().isLoaded(FTB_QUESTS_MOD_ID)) {
            FtbQuestsIntegration.QuestUnderMouse hit = FtbQuestsIntegration.getQuestUnderMouse(screen);
            if (hit != null) {
                shareQuest(hit);
                event.setCanceled(true);
                return;
            }
        }

        // 3) 普通容器界面的槽位
        if (screen instanceof AbstractContainerScreen<?> containerScreen) {
            Slot slot = containerScreen.getSlotUnderMouse();
            if (slot == null || !slot.hasItem()) {
                return;
            }
            ItemStack stack = slot.getItem();
            if (stack.isEmpty()) {
                return;
            }
            shareItem(stack);
            event.setCanceled(true);
        }
    }

    /** 把物品发给服务端广播；服务端没装本模组时只在自己屏幕上提示一句。 */
    private static void shareItem(ItemStack stack) {
        if (!isChannelPresent(ShareItemPacket.TYPE)) {
            notifySelf(ItemChat.tr("itemchat.msg.no_itemchat_item",
                    "当前服务器没有安装 ItemChat，无法向其他玩家展示物品。"));
            return;
        }
        ItemChatNetwork.sendToServer(new ShareItemPacket(stack));
    }

    /**
     * 把任务发给服务端广播。
     *
     * <p>包里带的是"对象 id + 该按钮渲染好的标题"，两者都是客户端这边的原样信息：</p>
     * <ul>
     *   <li>id 可能是任务链接的 id —— 服务端会顺着链接追到真正的任务；</li>
     *   <li>标题必须是客户端渲染的：服务端的 FTB {@code getTitle()} 只返回原样字面量，
     *       既不翻译也不解析 {@code &a} 这类颜色代码（详见 {@code ShareQuestPacket} 的说明）。</li>
     * </ul>
     */
    private static void shareQuest(FtbQuestsIntegration.QuestUnderMouse hit) {
        if (!isChannelPresent(ShareQuestPacket.TYPE)) {
            notifySelf(ItemChat.tr("itemchat.msg.no_itemchat_quest",
                    "当前服务器没有安装 ItemChat，无法向其他玩家展示任务。"));
            return;
        }
        ItemChatNetwork.sendToServer(new ShareQuestPacket(hit.questId(), hit.title()));
    }

    /**
     * 服务端是否能收某个功能的包（即通道是否可用）。
     *
     * <p>按包类型分别判断，而不是只看一个包：这样旧版 ItemChat 客户端连新版服务端
     * （或反过来）时，缺哪个功能就只降级哪个功能。用 {@code hasChannel} 探测，
     * 对端没装时不会报错。</p>
     */
    private static boolean isChannelPresent(CustomPacketPayload.Type<?> type) {
        ClientPacketListener connection = Minecraft.getInstance().getConnection();
        return connection != null && connection.hasChannel(type);
    }

    /** 只给自己看的提示（走 action bar 上方那条聊天提示，不进聊天记录）。 */
    private static void notifySelf(Component text) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null) {
            player.displayClientMessage(text.copy().withStyle(ChatFormatting.YELLOW), true);
        }
    }
}
