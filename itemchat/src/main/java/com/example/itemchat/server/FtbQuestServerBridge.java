package com.example.itemchat.server;

import com.example.itemchat.ItemChat;
import dev.ftb.mods.ftbquests.api.FTBQuestsAPI;
import dev.ftb.mods.ftbquests.quest.BaseQuestFile;
import dev.ftb.mods.ftbquests.quest.Quest;
import dev.ftb.mods.ftbquests.quest.QuestLink;
import dev.ftb.mods.ftbquests.quest.QuestObject;
import dev.ftb.mods.ftbquests.quest.QuestObjectBase;
import java.util.Optional;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
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
 * <h3>用到的 FTB API（2101.x / MC 1.21.1 线，均已对实机 jar 用 javap 核对）</h3>
 * <ul>
 *   <li>{@code FTBQuestsAPI.api().getQuestFile(false)} —— 拿服务端那份任务文件
 *       （内部就是 {@code ServerQuestFile.INSTANCE}）</li>
 *   <li>{@code BaseQuestFile#get(long)} —— 按 id 取对象，返回 {@code QuestObject}</li>
 *   <li>{@code QuestObjectBase#getCodeString(long)} / {@code #parseCodeString(String)}
 *       —— id 与「16 位大写十六进制字符串」之间的互转，这是 FTB 自己的规范写法</li>
 *   <li>{@code QuestLink#getQuest()} —— 任务链接指向的那条真实任务</li>
 * </ul>
 *
 * <p>文本走翻译键（{@link ItemChat#tr}），跟随各客户端自己的游戏语言。</p>
 */
final class FtbQuestServerBridge {

    /** 客户端传来的标题最长接受多少字符；超过就当异常输入，退回服务端自己那份。 */
    private static final int MAX_TITLE_CHARS = 200;

    private FtbQuestServerBridge() {
    }

    /** 把命令参数（FTB 规范的十六进制串）解析成任务 id；解析失败返回 0。 */
    static long parseId(String code) {
        return QuestObjectBase.parseCodeString(code);
    }

    /**
     * 服务端校验：这个 id 能不能对应到一条真实存在的任务。
     *
     * <p>注意这里不是简单的 {@code instanceof Quest}：任务书里看到的「任务」有两种对象 ——
     * 真正的 {@link Quest}，以及 FTB 为了把同一条任务显示在多个章节里而**自动生成的
     * 隐形 {@link QuestLink}**（它有自己的 id，在任务的 SNBT 数据里当然找不到，
     * 因为它压根不是从文件里读出来的）。点后者也应该算点中那条任务，
     * 所以要顺着 {@code QuestLink#getQuest()} 追到真正的那条 Quest。</p>
     */
    static boolean questExists(long questId) {
        BaseQuestFile file = serverQuestFile();
        return file != null && resolveQuest(file, questId) != null;
    }

    /** 构造「某玩家展示了某任务」的富文本并广播给全服。 */
    static void broadcast(ServerPlayer sender, long questId, Component clientTitle) {
        MinecraftServer server = sender.getServer();
        if (server == null) {
            return;
        }

        BaseQuestFile file = serverQuestFile();
        if (file == null) {
            return;
        }

        Quest quest = resolveQuest(file, questId);
        if (quest == null) {
            sender.sendSystemMessage(ItemChat.tr("itemchat.msg.quest_not_found",
                            "找不到这条 FTB 任务：%s", QuestObjectBase.getCodeString(questId))
                    .withStyle(ChatFormatting.RED));
            return;
        }

        // 点击 [检查任务] 跑服务端命令，服务端再校验一次并把 OpenQuestPacket 发回给点击者。
        // 这里用**解析后**的任务 id（而不是客户端传来的原始 id）：客户端点的可能是任务链接，
        // 链接 id 只在"某章节里的那个位置"有意义，直接拿去打开任务书会指到另一个位置。
        String code = QuestObjectBase.getCodeString(quest.getId());
        ClickEvent openQuest = new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/itemchat quest " + code);

        // 任务标题：绿色 + 悬停提示 + 点击同样能跳转
        MutableComponent questLabel = displayTitle(quest, clientTitle).copy().withStyle(style -> style
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

    /**
     * 把 id 解析成一条真实存在的任务。
     *
     * <p>{@link Quest} 直接返回自己；{@link QuestLink} 返回它指向的那条任务；
     * 其它对象（章节、章节装饰图等）返回 {@code null} —— 那些本来就不该被当成"任务"分享。</p>
     */
    private static Quest resolveQuest(BaseQuestFile file, long id) {
        QuestObject object = file.get(id);
        if (object instanceof Quest quest) {
            return quest;
        }
        if (object instanceof QuestLink link) {
            return link.getQuest().orElse(null);
        }
        return null;
    }

    /**
     * 决定用哪份标题，并把它"消毒"后再用。
     *
     * <p><b>为什么要消毒</b>：这份标题是客户端通过数据包传上来的，属于不可信输入。
     * 如果原样广播出去，一个改过的客户端就能往聊天栏里塞带 {@code run_command} 点击事件的
     * 假任务名，骗别人去点。所以这里只保留颜色 / 粗体这类纯视觉样式，
     * 把 clickEvent、hoverEvent、insertion 全部剥掉。</p>
     *
     * <p><b>为什么要退回</b>：标题为空、或者长度明显不正常（超过 {@value #MAX_TITLE_CHARS} 字），
     * 都认为客户端没给有效值，退回服务端自己那份。注意服务端这份是 FTB 的原始字面量
     * （可能残留 {@code &a} 这类颜色代码），只在异常/旧客户端时才出现。</p>
     */
    private static Component displayTitle(Quest quest, Component clientTitle) {
        String text = clientTitle.getString();
        if (text.isEmpty() || text.length() > MAX_TITLE_CHARS) {
            return quest.getTitle();
        }
        return stripInteractions(clientTitle);
    }

    /** 保留颜色等视觉样式，剥掉点击 / 悬停 / 插入这类可交互内容。 */
    private static Component stripInteractions(Component source) {
        MutableComponent out = Component.empty();
        source.visit((style, text) -> {
            Style safe = style.withClickEvent(null).withHoverEvent(null).withInsertion(null);
            out.append(Component.literal(text).withStyle(safe));
            return Optional.empty();
        }, Style.EMPTY);
        return out;
    }

    private static BaseQuestFile serverQuestFile() {
        // 参数 false 表示取服务端那份任务文件
        return FTBQuestsAPI.api().getQuestFile(false);
    }
}
