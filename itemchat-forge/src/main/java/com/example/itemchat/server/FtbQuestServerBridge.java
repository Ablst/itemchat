package com.example.itemchat.server;

import com.example.itemchat.ItemChat;
import dev.ftb.mods.ftbquests.api.FTBQuestsAPI;
import dev.ftb.mods.ftbquests.quest.BaseQuestFile;
import dev.ftb.mods.ftbquests.quest.Quest;
import dev.ftb.mods.ftbquests.quest.QuestObject;
import dev.ftb.mods.ftbquests.quest.QuestObjectBase;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextColor;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/**
 * 真正接触 FTB 任务类的那部分服务端逻辑。
 *
 * <p><b>这个类只有在 FTB 任务已加载时才会被触碰</b>（调用点全部先经过
 * {@code ModList.get().isLoaded("ftbquests")} 守卫），所以这里可以放心 import
 * {@code dev.ftb.mods.ftbquests} 下的类型。</p>
 *
 * <h3>用到的 FTB API（2001.x / MC 1.20.1 线）</h3>
 * <ul>
 *   <li>{@code FTBQuestsAPI.api().getQuestFile(false)} —— 拿服务端那份任务文件
 *       （内部就是 {@code ServerQuestFile.INSTANCE}）</li>
 *   <li>{@code BaseQuestFile#get(long)} —— 按 id 取对象，返回 {@code QuestObject}</li>
 *   <li>{@code QuestObjectBase#getCodeString(long)} / {@code #parseCodeString(String)}
 *       —— id 与「16 位大写十六进制字符串」之间的互转，这是 FTB 自己的规范写法</li>
 * </ul>
 *
 * <p>文本走翻译键（{@link ItemChat#tr}），跟随各客户端自己的游戏语言。</p>
 */
final class FtbQuestServerBridge {

    private FtbQuestServerBridge() {
    }

    /** 把命令参数（FTB 规范的十六进制串）解析成任务 id；解析失败返回 0。 */
    static long parseId(String code) {
        return QuestObjectBase.parseCodeString(code);
    }

    /** 服务端校验：这条 id 确实指向一条存在的任务，避免广播/打开一个不存在的对象。 */
    static boolean questExists(long questId) {
        BaseQuestFile file = serverQuestFile();
        return file != null && file.get(questId) instanceof Quest;
    }

    /** 构造「某玩家展示了某任务」的富文本并广播给全服。 */
    static void broadcast(ServerPlayer sender, long questId) {
        MinecraftServer server = sender.getServer();
        if (server == null) {
            return;
        }

        BaseQuestFile file = serverQuestFile();
        if (file == null) {
            return;
        }

        Quest quest = questById(file, questId);
        if (quest == null) {
            sender.sendSystemMessage(ItemChat.tr("itemchat.msg.quest_not_found",
                            "找不到这条 FTB 任务：%s", QuestObjectBase.getCodeString(questId))
                    .withStyle(ChatFormatting.RED));
            return;
        }

        // 点击 [检查任务] 跑服务端命令，服务端再校验一次并把 OpenQuestPacket 发回给点击者
        String code = QuestObjectBase.getCodeString(questId);
        ClickEvent openQuest = new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/itemchat quest " + code);

        // 任务标题：绿色 + 悬停提示 + 点击同样能跳转
        MutableComponent questLabel = quest.getTitle().copy().withStyle(style -> style
                .withColor(TextColor.fromLegacyFormat(ChatFormatting.GREEN))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                        ItemChat.tr("itemchat.chat.quest_label.hover", "点击在 FTB 任务中查看这条任务")))
                .withClickEvent(openQuest));

        MutableComponent checkButton = ItemChat.tr("itemchat.chat.check_quest", "[检查任务]").withStyle(style -> style
                .withColor(TextColor.fromLegacyFormat(ChatFormatting.YELLOW))
                .withBold(true)
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                        ItemChat.tr("itemchat.chat.check_quest.hover", "点击打开 FTB 任务书并定位到这条任务")))
                .withClickEvent(openQuest));

        MutableComponent message = ItemChat.tr("itemchat.chat.share_quest",
                "[任务] %1$s 展示了 %2$s %3$s",
                Component.literal(sender.getName().getString()).withStyle(ChatFormatting.AQUA),
                questLabel,
                checkButton);

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            player.sendSystemMessage(message);
        }
    }

    private static Quest questById(BaseQuestFile file, long questId) {
        QuestObject object = file.get(questId);
        return object instanceof Quest quest ? quest : null;
    }

    private static BaseQuestFile serverQuestFile() {
        // 参数 false 表示取服务端那份任务文件
        return FTBQuestsAPI.api().getQuestFile(false);
    }
}
