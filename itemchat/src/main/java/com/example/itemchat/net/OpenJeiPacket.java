package com.example.itemchat.net;

import com.example.itemchat.ItemChat;
import com.example.itemchat.client.ClientPayloadHandler;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * 服务端 -> 客户端：请接收方打开 JEI 中该物品的配方界面。
 *
 * <p>为什么要把“打开 JEI”这件事绕一圈交给服务端？因为聊天消息里的按钮用的是
 * {@code ClickEvent.Action.RUN_COMMAND}，原版 {@code Screen#handleComponentClicked}
 * 处理它时走的是 {@code ClientPacketListener#sendUnsignedCommand}，客户端注册的命令
 * 拦截不到，命令会被原样发给服务端并报“未知或不完整的命令”。</p>
 *
 * <p>因此改成：按钮执行服务端命令 {@code /itemchat jei <物品ID>}，服务端校验后用本包
 * 把结果回传给点击者，由客户端本地打开 JEI。</p>
 */
public record OpenJeiPacket(ResourceLocation itemId) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<OpenJeiPacket> TYPE =
            new CustomPacketPayload.Type<>(ItemChat.id("open_jei"));

    public static final StreamCodec<RegistryFriendlyByteBuf, OpenJeiPacket> STREAM_CODEC =
            StreamCodec.composite(
                    ResourceLocation.STREAM_CODEC, OpenJeiPacket::itemId,
                    OpenJeiPacket::new);

    @Override
    public CustomPacketPayload.Type<OpenJeiPacket> type() {
        return TYPE;
    }

    /**
     * 客户端处理。
     *
     * <p>这个包注册为 playToClient，处理器只会在客户端被调用，所以这里直接引用
     * {@link ClientPayloadHandler} 是安全的：专用服务端根本不会执行到这一行，
     * 也就不会因为解析客户端专有类而报 NoClassDefFoundError。
     * （NeoForge 已经删掉了 {@code DistExecutor}，不再需要它来兜底。）</p>
     */
    public static void handle(OpenJeiPacket packet, IPayloadContext context) {
        ClientPayloadHandler.openJei(packet.itemId());
    }
}
