package com.example.itemchat.net;

import com.example.itemchat.ItemChat;
import com.example.itemchat.server.ItemBroadcaster;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * 客户端 -> 服务端：请求把“某个玩家展示了某个物品”广播出去。
 *
 * <p>为什么需要走网络包？因为聊天消息里的可点击按钮（{@code ClickEvent}）属于富文本，
 * 而玩家自己发出去的普通聊天只有纯文本。只有服务端构造并下发的组件，才能让所有玩家
 * 看到同样的悬停预览和按钮。</p>
 *
 * <p>NeoForge 1.21.1 的自定义包统一实现 {@link CustomPacketPayload}：
 * 一个 {@link Type} 做通道标识，一个 {@link StreamCodec} 负责编解码。</p>
 */
public record ShareItemPacket(ItemStack stack) implements CustomPacketPayload {

    /**
     * 自定义数据包单包上限约 32KB。像装满物品的潜影盒这种组件数据会轻易超限，
     * 所以超过这个长度就直接只保留物品本体和数量。
     */
    private static final int MAX_NBT_CHARS = 12_000;

    public static final CustomPacketPayload.Type<ShareItemPacket> TYPE =
            new CustomPacketPayload.Type<>(ItemChat.id("share_item"));

    /**
     * 用 {@link ItemStack#OPTIONAL_STREAM_CODEC}：它需要 {@link RegistryFriendlyByteBuf}
     * 才能按注册表解析物品和组件，正好和 {@code playToServer} 要求的缓冲类型一致。
     */
    public static final StreamCodec<RegistryFriendlyByteBuf, ShareItemPacket> STREAM_CODEC =
            StreamCodec.composite(
                    ItemStack.OPTIONAL_STREAM_CODEC, ShareItemPacket::stack,
                    ShareItemPacket::new);

    /** 紧凑构造器：无论包是本地构造还是解码出来的，都先做一次体积裁剪。 */
    public ShareItemPacket {
        stack = sanitize(stack);
    }

    @Override
    public CustomPacketPayload.Type<ShareItemPacket> type() {
        return TYPE;
    }

    /**
     * 服务端处理。
     *
     * <p>这个包注册为 playToServer，所以处理器只会在服务端被调用，
     * {@code context.player()} 就是发包的那个玩家，客户端伪造不了发送者。</p>
     */
    public static void handle(ShareItemPacket packet, IPayloadContext context) {
        if (context.player() instanceof ServerPlayer sender) {
            ItemBroadcaster.broadcast(sender, packet.stack());
        }
    }

    private static ItemStack sanitize(ItemStack stack) {
        ItemStack copy = stack.copy();
        if (tooLarge(copy)) {
            // 1.20.5 起 ItemStack 的数据从 CompoundTag 换成了组件（DataComponentPatch），
            // 这里用官方 codec 编成 NBT 再量长度，等价于旧版对 getTag().toString() 的判断。
            return new ItemStack(copy.getItem(), copy.getCount());
        }
        return copy;
    }

    private static boolean tooLarge(ItemStack stack) {
        return ItemStack.CODEC.encodeStart(NbtOps.INSTANCE, stack)
                .result()
                .map(tag -> tag.toString().length() > MAX_NBT_CHARS)
                .orElse(false);
    }
}
