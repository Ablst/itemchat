package com.example.itemchat.net;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * 本模组的网络通道。
 *
 * <p>整条链路共四个包，都是"客户端动作 -&gt; 服务端校验 -&gt; 服务端下发"的往返模型：</p>
 * <ul>
 *   <li>{@link ShareItemPacket}：客户端中键物品 -&gt; 服务端广播富文本</li>
 *   <li>{@link OpenJeiPacket}：服务端请某位玩家在他本机打开 JEI 配方</li>
 *   <li>{@link ShareQuestPacket}：客户端中键 FTB 任务 -&gt; 服务端广播富文本</li>
 *   <li>{@link OpenQuestPacket}：服务端请某位玩家在他本机打开 FTB 任务书</li>
 * </ul>
 *
 * <p>"检查"这类动作之所以要绕服务端一圈，是因为玩家自己发出的聊天只带纯文本，
 * 服务端不会转发 {@code ClickEvent}；只有服务端构造并下发的组件才能让所有人看到按钮。</p>
 *
 * <p>NeoForge 1.21.1 已经彻底去掉了旧的 {@code SimpleChannel} / {@code NetworkRegistry.newSimpleChannel}
 * 体系，改用原版 {@code CustomPacketPayload} + {@code PayloadRegistrar} 注册。</p>
 */
public final class ItemChatNetwork {

    /**
     * 协议版本。客户端与服务端必须一致，否则 NeoForge 会拒绝登录。
     *
     * <p>1.2.1 把 {@link ShareQuestPacket} 的字段布局改了（从只传任务 id 变成"id + 客户端
     * 解析好的任务名组件"），所以这里必须从 {@code "1"} 提到 {@code "2"}：改了字段布局却不动
     * 版本号的话，新旧两端会各自按自己的格式读写同一个包，直接就是乱码。</p>
     *
     * <p>版本不一致时的表现是<b>优雅降级</b>而不是拒登：通道版本对不上等于两端不认识这个通道，
     * {@code ClientEvents} 里的 {@code hasChannel} 判断会拿到 false，于是只提示一句
     * "当前服务器没有安装 ItemChat"，连接本身照常。</p>
     */
    private static final String PROTOCOL_VERSION = "2";

    private ItemChatNetwork() {
    }

    /** 由 {@code ItemChat} 主类挂在 MOD 事件总线上调用。 */
    public static void register(RegisterPayloadHandlersEvent event) {
        // optional() 对应旧版的 NetworkRegistry.acceptMissingOr：对端没装本模组时
        // 连接照常建立，只是功能降级（见 ClientEvents 里的通道判断），
        // 而不是把玩家挡在服务器外面。
        PayloadRegistrar registrar = event.registrar(PROTOCOL_VERSION).optional();

        registrar.playToServer(
                ShareItemPacket.TYPE,
                ShareItemPacket.STREAM_CODEC,
                ShareItemPacket::handle);
        registrar.playToClient(
                OpenJeiPacket.TYPE,
                OpenJeiPacket.STREAM_CODEC,
                OpenJeiPacket::handle);
        registrar.playToServer(
                ShareQuestPacket.TYPE,
                ShareQuestPacket.STREAM_CODEC,
                ShareQuestPacket::handle);
        registrar.playToClient(
                OpenQuestPacket.TYPE,
                OpenQuestPacket.STREAM_CODEC,
                OpenQuestPacket::handle);
    }

    public static void sendToServer(CustomPacketPayload packet) {
        PacketDistributor.sendToServer(packet);
    }

    /** 把"打开 JEI / 打开任务书"的结果回传给指定的点击者。 */
    public static void sendToPlayer(ServerPlayer player, CustomPacketPayload packet) {
        PacketDistributor.sendToPlayer(player, packet);
    }
}
