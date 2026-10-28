package com.example.itemchat.net;

import com.example.itemchat.ItemChat;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

/**
 * 本模组的网络通道。
 *
 * <p>整条链路共四个包，都是"客户端动作 -> 服务端校验 -> 服务端下发"的往返模型：</p>
 * <ul>
 *   <li>{@link ShareItemPacket}：客户端中键物品 -> 服务端广播富文本</li>
 *   <li>{@link OpenJeiPacket}：服务端请某位玩家在他本机打开 JEI 配方</li>
 *   <li>{@link ShareQuestPacket}：客户端中键 FTB 任务 -> 服务端广播富文本</li>
 *   <li>{@link OpenQuestPacket}：服务端请某位玩家在他本机打开 FTB 任务书</li>
 * </ul>
 *
 * <p>"检查"这类动作之所以要绕服务端一圈，是因为玩家自己发出的聊天只带纯文本，
 * 服务端不会转发 {@code ClickEvent}；只有服务端构造并下发的组件才能让所有人看到按钮。</p>
 */
public final class ItemChatNetwork {

    /**
     * 协议版本号。两端不一致时 Forge 会拒绝握手。
     * 只要任何一个包的字段布局发生改变，就必须把这个号加一。
     */
    private static final String PROTOCOL_VERSION = "1";

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            ItemChat.id("main"),
            () -> PROTOCOL_VERSION,
            // 用 acceptMissingOr：对端没装本模组时，不因为"通道不存在"而拒绝连接。
            // 这样没装模组的原版服务器也能照常进入，只是功能降级。
            NetworkRegistry.acceptMissingOr(PROTOCOL_VERSION),
            NetworkRegistry.acceptMissingOr(PROTOCOL_VERSION));

    private ItemChatNetwork() {
    }

    /** 在模组构造阶段调用，把四个包按固定 id 顺序注册进通道。 */
    public static void register() {
        int id = 0;

        CHANNEL.registerMessage(id++, ShareItemPacket.class,
                ShareItemPacket::encode, ShareItemPacket::decode, ShareItemPacket::handle);

        CHANNEL.registerMessage(id++, OpenJeiPacket.class,
                OpenJeiPacket::encode, OpenJeiPacket::decode, OpenJeiPacket::handle);

        CHANNEL.registerMessage(id++, ShareQuestPacket.class,
                ShareQuestPacket::encode, ShareQuestPacket::decode, ShareQuestPacket::handle);

        CHANNEL.registerMessage(id++, OpenQuestPacket.class,
                OpenQuestPacket::encode, OpenQuestPacket::decode, OpenQuestPacket::handle);
    }

    /** 客户端 -> 服务端：分享物品 */
    public static void sendToServer(ShareItemPacket packet) {
        CHANNEL.sendToServer(packet);
    }

    /** 客户端 -> 服务端：分享 FTB 任务 */
    public static void sendToServer(ShareQuestPacket packet) {
        CHANNEL.sendToServer(packet);
    }

    /** 服务端 -> 指定玩家：打开 JEI 配方 */
    public static void sendToPlayer(ServerPlayer player, OpenJeiPacket packet) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }

    /** 服务端 -> 指定玩家：打开 FTB 任务书 */
    public static void sendToPlayer(ServerPlayer player, OpenQuestPacket packet) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }
}
