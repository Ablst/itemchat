package com.example.itemchat.net;

import com.example.itemchat.server.ItemBroadcaster;
import java.util.function.Supplier;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

/**
 * 客户端 -> 服务端：玩家在容器界面 / JEI 里对某个物品按了鼠标中键。
 *
 * <p>只带一个 {@link ItemStack}。服务端收到后会重新构造富文本（而不是直接转发玩家发的消息），
 * 这样广播出去的组件才带得了悬停预览和可点击按钮。</p>
 *
 * <p>为什么要做 NBT 长度限制：某些模组的物品 NBT 极其庞大（成千上万字符），
 * 直接塞进网络包会把包撑爆，所以超过阈值就把 NBT 丢掉，只分享"物品本身 + 数量"。</p>
 */
public final class ShareItemPacket {

    /** NBT 序列化后超过这个字符数就丢弃 NBT */
    private static final int MAX_TAG_CHARS = 12000;

    private final ItemStack stack;

    public ShareItemPacket(ItemStack stack) {
        this.stack = sanitizeForNetwork(stack);
    }

    public ItemStack stack() {
        return stack;
    }

    public static void encode(ShareItemPacket packet, FriendlyByteBuf buf) {
        buf.writeItem(packet.stack);
    }

    public static ShareItemPacket decode(FriendlyByteBuf buf) {
        // 注意：这里会再走一遍构造器里的清洗逻辑
        return new ShareItemPacket(buf.readItem());
    }

    public static void handle(ShareItemPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer sender = context.getSender();
            if (sender == null) {
                // 拿不到发送者说明这个包不是从玩家来的，直接忽略
                return;
            }
            ItemBroadcaster.broadcast(sender, packet.stack);
        });
        context.setPacketHandled(true);
    }

    /** 复制一份再清洗，避免污染调用方手里的那个 ItemStack。 */
    private static ItemStack sanitizeForNetwork(ItemStack stack) {
        ItemStack copy = stack.copy();
        CompoundTag tag = copy.getTag();
        if (tag != null && tag.toString().length() > MAX_TAG_CHARS) {
            copy.setTag(null);
        }
        return copy;
    }
}
