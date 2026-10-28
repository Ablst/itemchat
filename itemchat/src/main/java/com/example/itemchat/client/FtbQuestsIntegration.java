package com.example.itemchat.client;

import dev.ftb.mods.ftblibrary.ui.IScreenWrapper;
import dev.ftb.mods.ftblibrary.ui.Widget;
import dev.ftb.mods.ftbquests.client.ClientQuestFile;
import dev.ftb.mods.ftbquests.client.gui.quests.QuestPositionableButton;
import dev.ftb.mods.ftbquests.client.gui.quests.QuestScreen;
import dev.ftb.mods.ftbquests.quest.Movable;
import dev.ftb.mods.ftbquests.quest.Quest;
import dev.ftb.mods.ftbquests.quest.QuestLink;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

/**
 * FTB 任务的软依赖桥接层（客户端侧）。
 *
 * <p><b>这个类只有 FTB 任务已加载时才会被加载</b> —— 所有调用点都先经过
 * {@code ModList.get().isLoaded("ftbquests")} 守卫，所以里面可以放心 import FTB 的类型。</p>
 *
 * <h3>用到的 FTB API（2101.x / MC 1.21.1 线，均已对实机 jar 用 javap 核对）</h3>
 * <ul>
 *   <li>{@code ClientQuestFile.openBookToQuestObject(long)} —— 打开任务书并定位到任务，
 *       是 FTB 自带的深链入口</li>
 *   <li>{@code QuestScreen#questPanel}（public final 字段）+ {@code Panel#getWidgets()}
 *       —— 遍历任务按钮</li>
 *   <li>{@code QuestPositionableButton#moveAndDeleteFocus()} —— 公开接口，返回该按钮对应的
 *       {@code Movable}，绕开了包私有的 {@code QuestButton.quest} 字段</li>
 *   <li>{@code Widget#getTitle()} —— 按钮自己的标题（下面会解释为什么标题要取这个）</li>
 *   <li>{@code Movable#getMovableID()} —— 拿到对象的 long id</li>
 *   <li>{@code Widget#isMouseOver()} —— FTB 的任务按钮是六边形/自定义形状，
 *       坐标命中判断由它自己的 {@code checkMouseOver} 负责，不要自己算坐标</li>
 * </ul>
 */
public final class FtbQuestsIntegration {

    private FtbQuestsIntegration() {
    }

    /**
     * 鼠标下那个任务按钮的信息。
     *
     * @param questId 对象 id。可能是 {@code Quest} 自己的 id，也可能是 {@code QuestLink}
     *                这个"链接对象"的 id —— 后者由服务端负责追到真正的任务（见下）
     * @param title   该按钮渲染好的标题，也就是玩家在任务书里看到的那一行
     */
    public record QuestUnderMouse(long questId, Component title) {
    }

    /**
     * 从 Minecraft 的 Screen 里取出 FTB 任务书本体。
     *
     * <p>FTB Library 的界面真正的 Screen 是 {@code ScreenWrapper}，任务界面本体
     * （{@code QuestScreen}）挂在它的 {@code getGui()} 上，所以必须转两层。</p>
     */
    @Nullable
    private static QuestScreen asQuestScreen(Screen screen) {
        if (screen instanceof IScreenWrapper wrapper && wrapper.getGui() instanceof QuestScreen questScreen) {
            return questScreen;
        }
        return null;
    }

    /**
     * 取鼠标下那个任务按钮；鼠标没停在"任务"上时返回 {@code null}。
     *
     * <h3>为什么 id 和标题要分别取</h3>
     *
     * <p>任务书里"看起来是任务"的按钮其实有<b>三种</b>对象：</p>
     * <ol>
     *   <li>{@code QuestButton} → 真正的 {@link Quest}</li>
     *   <li>{@code QuestLinkButton} → 一条 {@link QuestLink}。这是 FTB 为了让同一条任务
     *       能同时出现在多个章节里而自动生成的"链接"对象，它有自己的 id，
     *       和被链接的那条任务不是同一个 id，外观上和普通任务一模一样</li>
     *   <li>{@code ChapterImageButton} → {@code ChapterImage}，章节里的装饰图（不是任务）</li>
     * </ol>
     *
     * <p><b>id 取按钮对应的对象 id（前两种都算），交给服务端去追</b>：
     * {@code QuestLink} 要顺着它的 {@code linkId} 找到真正的任务，这件事只能在服务端做 ——
     * FTB 在网络同步 {@code QuestLink} 时把 {@code linkId} 写死成 {@code 0L}
     * （见 {@code BaseQuestFile#readNetData}），所以<b>客户端手里的链接对象是追不到目标的</b>。
     * 服务端的链接对象是从 SNBT 读出来的，{@code linkId} 是完整的。</p>
     *
     * <p><b>标题取 {@code Widget#getTitle()} 而不是自己去查任务</b>：
     * {@code QuestLinkButton} 构造时传进去的就是被链接的那条 {@code Quest}，
     * 所以按钮标题天然就是"任务书里那一行"（本机语言 + 颜色代码都解析好了）。
     * 自己去查反而会因为客户端链接追不到目标而拿不到标题。</p>
     *
     * <p>装饰图返回 {@code null}：那本来就不是任务，直接当没命中，
     * 免得报一句莫名其妙的"找不到这条任务"。</p>
     */
    @Nullable
    public static QuestUnderMouse getQuestUnderMouse(Screen screen) {
        QuestScreen questScreen = asQuestScreen(screen);
        if (questScreen == null) {
            return null;
        }

        Widget hovered = questScreen.questPanel.getWidgets().stream()
                .filter(QuestPositionableButton.class::isInstance)
                .filter(Widget::isMouseOver)
                .findFirst()
                .orElse(null);
        if (hovered == null) {
            return null;
        }

        Movable movable = ((QuestPositionableButton) hovered).moveAndDeleteFocus();
        if (!(movable instanceof Quest) && !(movable instanceof QuestLink)) {
            return null;
        }
        if (movable.getMovableID() == 0L) {
            return null;
        }

        return new QuestUnderMouse(movable.getMovableID(), hovered.getTitle().copy());
    }

    /** 打开任务书并定位到指定任务。 */
    public static void openQuest(long questId) {
        ClientQuestFile.openBookToQuestObject(questId);
    }
}
