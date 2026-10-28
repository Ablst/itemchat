package com.example.itemchat;

import com.example.itemchat.net.ItemChatNetwork;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

/**
 * 模组主类。
 *
 * <p>功能：在背包 / 任意容器界面 / JEI 物品列表里，把鼠标移到物品上按下鼠标中键，
 * 就会向全服广播一条可点击的聊天消息，消息里附带一个 [检查] 按钮，点击后打开 JEI 中
 * 该物品的配方界面；在 FTB 任务界面里中键某条任务则会广播该任务，并附带 [检查任务]
 * 按钮用于回跳任务书。</p>
 *
 * <h3>本地化</h3>
 * <p>所有对玩家显示的文字都走 {@link #tr} 构造的翻译键（语言文件在
 * {@code assets/itemchat/lang/}），所以会跟随每个客户端各自的游戏语言变化。</p>
 * <ul>
 *   <li>装了本模组的客户端：按它自己的语言显示。</li>
 *   <li>没装本模组的客户端：显示 {@code fallback} 里的纯文本，
 *       不会看到裸的翻译键名 —— 因为服务端广播的消息可能被没装模组的玩家看到。</li>
 * </ul>
 */
@Mod(ItemChat.MOD_ID)
public final class ItemChat {

    public static final String MOD_ID = "itemchat";

    /**
     * NeoForge 允许在 {@link Mod} 构造器里注入 {@link IEventBus}。
     * {@code RegisterPayloadHandlersEvent} 属于模组生命周期事件（MOD 总线），
     * 所以网络通道的注册挂在 modBus 上。
     */
    public ItemChat(IEventBus modBus) {
        modBus.addListener(ItemChatNetwork::register);
    }

    /** 便捷方法：用本模组的命名空间构造一个 {@link ResourceLocation}。 */
    public static ResourceLocation id(String path) {
        // 1.21 起 ResourceLocation 的构造器不再公开，统一走 fromNamespaceAndPath。
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }

    /**
     * 构造一条可本地化的文本。
     *
     * @param key      翻译键，对应 {@code assets/itemchat/lang/*.json} 里的条目
     * @param fallback 当前客户端语言里没有该键时的回退文本（中文），
     *                 这样没装本模组的客户端也能看懂
     * @param args     格式化参数，可以是普通对象，也可以是 {@link Component}（样式会被保留）
     */
    public static MutableComponent tr(String key, String fallback, Object... args) {
        return Component.translatableWithFallback(key, fallback, args);
    }
}
