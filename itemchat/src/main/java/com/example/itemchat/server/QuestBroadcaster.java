package com.example.itemchat.server;

import com.example.itemchat.ItemChat;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.fml.ModList;

/**
 * FTB 任务广播的「软依赖守卫层」。
 *
 * <p>这个类本身只做两件事：判断 FTB 任务装没装、没装时怎么降级。
 * 真正接触 FTB 类型的代码全都在 {@link FtbQuestServerBridge} 里，
 * 而那个类只有在 {@code ModList.get().isLoaded("ftbquests")} 成立时才会被加载 ——
 * 这样没装 FTB 任务的服务器上，永远不会因为缺类而炸。</p>
 */
public final class QuestBroadcaster {

    /** FTB 任务在服务端也要装：任务标题得由服务端从任务文件里查出来才能广播。 */
    private static final String FTB_QUESTS_MOD_ID = "ftbquests";

    private QuestBroadcaster() {
    }

    public static void broadcast(ServerPlayer sender, long questId, Component title) {
        if (questId == 0L) {
            return;
        }

        if (!ModList.get().isLoaded(FTB_QUESTS_MOD_ID)) {
            // 服务端没装 FTB 任务：拿不到任务标题，降级成一句纯文本提示，
            // 顺便说明这个功能需要服务端也装。
            sender.sendSystemMessage(ItemChat.tr("itemchat.msg.no_ftbquests",
                            "当前服务器没有安装 FTB 任务，无法展示任务。")
                    .withStyle(ChatFormatting.RED));
            return;
        }

        FtbQuestServerBridge.broadcast(sender, questId, title);
    }
}
