package com.example.itemchat.net;

import com.example.itemchat.server.QuestBroadcaster;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

/**
 * 客户端 -> 服务端：玩家在 FTB 任务界面里对某条任务按了鼠标中键。
 *
 * <p>任务标识在 FTB 任务里就是一个 64 位 long（{@code QuestObjectBase#getId()}），
 * 所以这个包只传 8 个字节。服务端拿它去自己的任务文件里查出标题，再拼富文本广播。</p>
 */
public final class ShareQuestPacket {

    private final long questId;

    public ShareQuestPacket(long questId) {
        this.questId = questId;
    }

    /** FTB 任务的对象 id；0 表示没有任务（FTB 用 0 表示"空"）。 */
    public long questId() {
        return questId;
    }

    public static void encode(ShareQuestPacket packet, FriendlyByteBuf buf) {
        buf.writeLong(packet.questId);
    }

    public static ShareQuestPacket decode(FriendlyByteBuf buf) {
        return new ShareQuestPacket(buf.readLong());
    }

    public static void handle(ShareQuestPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer sender = context.getSender();
            if (sender == null) {
                return;
            }
            QuestBroadcaster.broadcast(sender, packet.questId);
        });
        context.setPacketHandled(true);
    }
}
