package com.example.itemchat.client;

import com.example.itemchat.ItemChat;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.resources.ResourceLocation;

/**
 * JEI 插件入口。
 *
 * <p>JEI 启动时会扫描带 {@link JeiPlugin} 注解的类；这里只是把 JEI 运行时实例
 * 存起来，真正的调用都在 {@link JeiIntegration} 里。</p>
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