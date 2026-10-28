package com.example.itemchat.client;

import com.example.itemchat.ItemChat;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.resources.ResourceLocation;

/**
 * JEI 插件入口。JEI 会通过 {@code @JeiPlugin} 注解自动发现它。
 *
 * <p>这里只做一件事：把 JEI 的运行时对象转交给 {@link JeiIntegration} 保管，
 * 之后"打开配方"和"读取鼠标悬停物品"都靠这个运行时对象。</p>
 */
@JeiPlugin
public final class ItemChatJeiPlugin implements IModPlugin {

    @Override
    public ResourceLocation getPluginUid() {
        return ItemChat.id("jei_plugin");
    }

    @Override
    public void onRuntimeAvailable(IJeiRuntime jeiRuntime) {
        JeiIntegration.setRuntime(jeiRuntime);
    }

    @Override
    public void onRuntimeUnavailable() {
        JeiIntegration.clearRuntime();
    }
}
