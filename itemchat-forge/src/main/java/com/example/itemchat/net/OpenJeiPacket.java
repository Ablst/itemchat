package com.example.itemchat.net;

import com.example.itemchat.client.ClientPayloadHandler;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

/**
 * 服务端 -> 客户端：请这位玩家在他本机打开 JEI 里该物品的配方界面。
 *
 * <p>只带一个物品 id（{@link ResourceLocation}），客户端自己拿它去原版注册表里查物品，
 * 再调用 JEI 的 API 打开配方。之所以不在服务端直接做，是因为 JEI 完全是客户端模组。</p>
 *
 * <p>这里用 {@link DistExecutor#unsafeRunWhenOn} 把处理逻辑隔离到客户端：
 * 专用服务端（dedicated server）上 {@code ClientPayloadHandler} 这个类根本不会被加载，
 * 避免类加载期就炸。{@code unsafe} 指的是不做二次封装，因此必须保证这个 lambda
 * 只在物理客户端执行，Forge 的 dist 判定已经替我们保证了这点。</p>
 */
public final class OpenJeiPacket {

    private final ResourceLocation itemId;

    public OpenJeiPacket(ResourceLocation itemId) {
        this.itemId = itemId;
    }

    public ResourceLocation itemId() {
        return itemId;
    }

    public static void encode(OpenJeiPacket packet, FriendlyByteBuf buf) {
        buf.writeResourceLocation(packet.itemId);
    }

    public static OpenJeiPacket decode(FriendlyByteBuf buf) {
        return new OpenJeiPacket(buf.readResourceLocation());
    }

    public static void handle(OpenJeiPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() ->
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                        () -> () -> ClientPayloadHandler.openJei(packet.itemId)));
        context.setPacketHandled(true);
    }
}
