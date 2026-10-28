package com.example.itemchat;

import com.example.itemchat.net.ItemChatNetwork;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fml.common.Mod;

/**
 * 模组主类。
 *
 * <p>只负责在构造阶段把网络通道注册好；其它事件监听都分散在各自的分包类上，
 * 通过 {@code @Mod.EventBusSubscriber} 自动挂载。</p>
 *
 * <h3>版本</h3>
 * <p>本工程是 Forge 1.20.1 线（从 1.0.0 反编译基线重建），当前 <b>1.3.0</b>。</p>
 *
 * <h3>本地化</h3>
 * <p>所有对玩家显示的文字都走 {@code Component} 翻译键（语言文件在
 * {@code assets/itemchat/lang/}），所以会跟随游戏语言变化。</p>
 * <ul>
 *   <li>装了本模组的客户端：按它自己的语言显示。</li>
 *   <li>没装本模组的客户端：显示 {@code fallback} 里的纯文本（见 {@link #tr}），
 *       不会看到裸的翻译键名 —— 因为服务端广播的消息可能被没装模组的玩家看到。</li>
 * </ul>
 */
@Mod(ItemChat.MOD_ID)
public final class ItemChat {

    /** 模组 id，必须与 META-INF/mods.toml 里的 modId 一致 */
    public static final String MOD_ID = "itemchat";

    public ItemChat() {
        // Forge 只在模组构造期间允许注册网络通道，过了这个窗口就会报错
        ItemChatNetwork.register();
    }

    /** 便捷方法：把 "main" 之类的短路径拼成模组命名空间下的 ResourceLocation。 */
    public static ResourceLocation id(String path) {
        return new ResourceLocation(MOD_ID, path);
    }

    /**
     * 构造一条可本地化的文本。
     *
     * @param key      翻译键，对应 {@code assets/itemchat/lang/*.json} 里的条目
     * @param fallback 当前客户端语言里没有该键时的回退文本（通常是中文），
     *                 这样没装本模组的客户端也能看懂
     * @param args     格式化参数，可以是普通对象，也可以是 {@link Component}（样式会被保留）
     */
    public static MutableComponent tr(String key, String fallback, Object... args) {
        return Component.translatableWithFallback(key, fallback, args);
    }
}
