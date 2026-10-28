package com.example.itemchat.net;

import com.example.itemchat.ItemChat;
import com.example.itemchat.client.ClientPayloadHandler;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * 服务端 -&gt; 客户端：请这位玩家在他本机打开 FTB 任务书，并定位到指定任务。
 *
 * <p>点聊天栏里的 [检查任务] 按钮时，走的是服务端命令 {@code /itemchat quest <任务id>}：
 * 服务端先校验该任务确实存在，再把这个包发给点击者。客户端收到后调用 FTB 任务的
 * 公开入口 {@code ClientQuestFile.openBookToQuestObject(long)}。</p>
 *
 * <p>这个包注册为 playToClient，处理器不会在专用服务端上被触发，所以这里直接引用
 * {@link ClientPayloadHandler} 是安全的（NeoForge 1.21.1 已删掉 {@code DistExecutor}，
 * 不再需要它来兜底）。</p>
 */
public record OpenQuestPacket(long questId) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<OpenQuestPacket> TYPE =
            new CustomPacketPayload.Type<>(ItemChat.id("open_quest"));

    public static final StreamCodec<RegistryFriendlyByteBuf, OpenQuestPacket> STREAM_CODEC =
            StreamCodec.of(
                    (buf, packet) -> buf.writeLong(packet.questId()),
                    buf -> new OpenQuestPacket(buf.readLong()));

    @Override
    public CustomPacketPayload.Type<OpenQuestPacket> type() {
        return TYPE;
    }

    public static void handle(OpenQuestPacket packet, IPayloadContext context) {
        ClientPayloadHandler.openQuest(packet.questId());
    }
}
