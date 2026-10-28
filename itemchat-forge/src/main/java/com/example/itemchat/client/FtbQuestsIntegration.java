package com.example.itemchat.client;

import dev.ftb.mods.ftblibrary.ui.IScreenWrapper;
import dev.ftb.mods.ftblibrary.ui.Widget;
import dev.ftb.mods.ftbquests.client.ClientQuestFile;
import dev.ftb.mods.ftbquests.client.gui.quests.QuestPositionableButton;
import dev.ftb.mods.ftbquests.client.gui.quests.QuestScreen;
import dev.ftb.mods.ftbquests.quest.Movable;
import net.minecraft.client.gui.screens.Screen;
import org.jetbrains.annotations.Nullable;

/**
 * FTB 任务的软依赖桥接层（客户端侧）。
 *
 * <p><b>这个类只有 FTB 任务已加载时才会被加载</b> —— 所有调用点都先经过
 * {@code ModList.get().isLoaded("ftbquests")} 守卫，所以里面可以放心 import FTB 的类型。</p>
 *
 * <h3>用到的 FTB API（2001.x / MC 1.20.1 线，均已在 2001.4.22 反编译核对）</h3>
 * <ul>
 *   <li>{@code ClientQuestFile.openBookToQuestObject(long)} —— 打开任务书并定位到任务，
 *       是 FTB 自带的深链入口</li>
 *   <li>{@code QuestScreen#questPanel}（public）+ {@code QuestPanel#getWidgets()}（public）
 *       —— 遍历任务按钮</li>
 *   <li>{@code QuestPositionableButton#moveAndDeleteFocus()} —— 公开接口，返回该按钮对应的
 *       {@code Movable}（就是那条 Quest），绕开了包私有的 {@code QuestButton.quest} 字段</li>
 *   <li>{@code Movable#getMovableID()} —— 拿到任务的 long id</li>
 * </ul>
 */
public final class FtbQuestsIntegration {

    private FtbQuestsIntegration() {
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

    /** 当前界面是不是 FTB 任务书。 */
    public static boolean isQuestScreen(Screen screen) {
        return asQuestScreen(screen) != null;
    }

    /**
     * 取鼠标下那个任务按钮对应的任务 id；鼠标没停在任务上时返回 0。
     *
     * <p>注意用的是 {@code Widget#isMouseOver()} 而不是坐标计算 —— FTB 的任务按钮是
     * 六边形/自定义形状，坐标命中判断由它自己的 {@code checkMouseOver} 负责。</p>
     */
    public static long getQuestIdUnderMouse(Screen screen) {
        QuestScreen questScreen = asQuestScreen(screen);
        if (questScreen == null) {
            return 0L;
        }

        return questScreen.questPanel.getWidgets().stream()
                .filter(QuestPositionableButton.class::isInstance)
                .filter(Widget::isMouseOver)
                .map(widget -> ((QuestPositionableButton) widget).moveAndDeleteFocus())
                .mapToLong(Movable::getMovableID)
                .findFirst()
                .orElse(0L);
    }

    /** 打开任务书并定位到指定任务。 */
    public static void openQuest(long questId) {
        ClientQuestFile.openBookToQuestObject(questId);
    }
}
