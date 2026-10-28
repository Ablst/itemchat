package com.example.itemchat.net;

import com.example.itemchat.client.ClientPayloadHandler;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

/**
 * 服务端 -> 客户端：请这位玩家在他本机打开 FTB 任务书，并定位到指定任务。
 *
 * <p>点聊天栏里的 [检查任务] 按钮时，走的是服务端命令 {@code /itemchat quest <任务id>}：
 * 服务端先校验该任务确实存在，再把这个包发给点击者。客户端收到后调用 FTB 任务的
 * 公开入口 {@code ClientQuestFile.openBookToQuestObject(long)}。</p>
 *
 * <p>客户端处理逻辑同样用 {@link DistExecutor} 隔离；FTB 任务本身是软依赖，
 * 具体分发见 {@link ClientPayloadHandler#openQuest}。</p>
 */
public final class OpenQuestPacket {

    private final long questId;

    public OpenQuestPacket(long questId) {
        this.questId = questId;
    }

    public long questId() {
        return questId;
    }

    public static void encode(OpenQuestPacket packet, FriendlyByteBuf buf) {
        buf.writeLong(packet.questId);
    }

    public static OpenQuestPacket decode(FriendlyByteBuf buf) {
        return new OpenQuestPacket(buf.readLong());
    }

    public static void handle(OpenQuestPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() ->
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                        () -> () -> ClientPayloadHandler.openQuest(packet.questId)));
        context.setPacketHandled(true);
    }
}
