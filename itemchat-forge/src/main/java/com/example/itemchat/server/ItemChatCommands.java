package com.example.itemchat.server;

import com.example.itemchat.ItemChat;
import com.example.itemchat.net.ItemChatNetwork;
import com.example.itemchat.net.OpenJeiPacket;
import com.example.itemchat.net.OpenQuestPacket;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;

/**
 * 服务端命令 {@code /itemchat ...}。
 *
 * <p>聊天栏里的按钮不直接调客户端命令，而是跑一条服务端命令绕一圈：</p>
 * <pre>
 *   [检查]      -> /itemchat jei &lt;物品id&gt;     -> OpenJeiPacket   -> 客户端打开 JEI
 *   [检查任务]  -> /itemchat quest &lt;任务id&gt;   -> OpenQuestPacket -> 客户端打开 FTB 任务书
 * </pre>
 *
 * <p>这样做有两个好处：一是不会在服务器上触发「未知命令」的报错；二是服务端可以
 * 先校验参数合法（物品存在 / 任务存在），只有点按钮的那个玩家会收到回包。</p>
 *
 * <p>{@code RegisterCommandsEvent} 是游戏事件（NeoForge 里对应 GAME 总线，
 * 旧版 Forge 里叫 FORGE 总线），所以 {@code bus} 必须写 {@code Bus.FORGE}。</p>
 */
@Mod.EventBusSubscriber(modid = ItemChat.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ItemChatCommands {

    /** FTB 任务的可选依赖 id，用于在命令层做降级提示 */
    private static final String FTB_QUESTS_MOD_ID = "ftbquests";

    private ItemChatCommands() {
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

        dispatcher.register(Commands.literal("itemchat")
                .then(Commands.literal("jei")
                        .then(Commands.argument("item", ResourceLocationArgument.id())
                                .executes(context -> openInJei(context.getSource(),
                                        ResourceLocationArgument.getId(context, "item")))))
                .then(Commands.literal("quest")
                        .then(Commands.argument("id", StringArgumentType.word())
                                .executes(context -> openInFtbQuest(context.getSource(),
                                        StringArgumentType.getString(context, "id"))))));
    }

    /** 物品 id 是原版 ResourceLocation，直接透传给客户端去查注册表。 */
    private static int openInJei(CommandSourceStack source, ResourceLocation itemId) {
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            // 控制台没有客户端，什么也做不了
            return 0;
        }
        ItemChatNetwork.sendToPlayer(player, new OpenJeiPacket(itemId));
        return 1;
    }

    /** 任务 id 用 FTB 规范的十六进制串，服务端先解析 + 校验存在性。 */
    private static int openInFtbQuest(CommandSourceStack source, String code) {
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            return 0;
        }

        if (!ModList.get().isLoaded(FTB_QUESTS_MOD_ID)) {
            source.sendFailure(ItemChat.tr("itemchat.msg.no_ftbquests",
                    "当前服务器没有安装 FTB 任务。").withStyle(ChatFormatting.RED));
            return 0;
        }

        long questId = FtbQuestServerBridge.parseId(code);
        if (questId == 0L || !FtbQuestServerBridge.questExists(questId)) {
            source.sendFailure(ItemChat.tr("itemchat.msg.quest_not_found",
                    "找不到这条 FTB 任务：%s", code).withStyle(ChatFormatting.RED));
            return 0;
        }

        ItemChatNetwork.sendToPlayer(player, new OpenQuestPacket(questId));
        return 1;
    }
}
