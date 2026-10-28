package com.example.itemchat.net;

import com.example.itemchat.ItemChat;
import com.example.itemchat.server.QuestBroadcaster;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * 客户端 -&gt; 服务端：玩家在 FTB 任务界面里对某条任务按了鼠标中键。
 *
 * <p>带两个字段：任务 id，以及<b>由客户端解析好的任务名组件</b>。</p>
 *
 * <h3>为什么任务名要由客户端提供，而不是服务端自己取</h3>
 *
 * <p>FTB 的 {@code QuestObjectBase#getTitle()} 在两个端的实现是<b>不一样</b>的
 * （见 FTB 源码 {@code QuestObjectBase.java}）：</p>
 * <pre>
 *   this.cachedTitle = !getRawTitle().isEmpty()
 *       ? (getQuestFile().isServerSide()
 *           ? Component.literal(getRawTitle())                       // 服务端：纯字面量
 *           : TextUtils.parseRawText(getRawTitle(), holderLookup())) // 客户端：解析
 *       : getAltTitle();
 * </pre>
 *
 * <p>也就是说：<b>服务端拿到的是"原样字符串"</b>，既不翻译、也不解析 {@code &a} 这类
 * 颜色代码；只有客户端才走 {@code TextUtils.parseRawText} 把颜色代码变成真实样式。
 * 而且 FTB 的翻译根本不是原版的 {@code Language}，是它自己的 locale 表
 * （{@code TranslationManager}，按「对象 id + 语言」存，值来自整合包里的
 * {@code config/ftbquests/quests/lang/<语言>.snbt}）。</p>
 *
 * <p>结果就是：如果服务端用 {@code quest.getTitle()}，中文客户端看到的会是
 * {@code &aGeneral Storage Options} 这种"英文 + 裸颜色代码"。所以改成让<b>点中键的那个
 * 客户端</b>（它本来就在用 {@code ClientQuestFile}，语言是自己的游戏语言）把渲染好的
 * 组件传上来，服务端只负责校验任务存在并把它广播出去。</p>
 *
 * <p>注意这里只暴露 {@code long} 和 {@code Component}，<b>不 import 任何 FTB 类型</b>：
 * FTB 缺席时这个类照常可用，真正接触 FTB 的代码在 {@code QuestBroadcaster} 后面。</p>
 */
public record ShareQuestPacket(long questId, Component title) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<ShareQuestPacket> TYPE =
            new CustomPacketPayload.Type<>(ItemChat.id("share_quest"));

    /**
     * 显式写成 {@code StreamCodec.of} 而不是 {@code StreamCodec.composite}：
     * 后者会把缓冲类型推断成 {@code ByteBuf}，而这里直接指明
     * {@link RegistryFriendlyByteBuf}，与 {@code playToServer} 要求的类型一致，
     * 省掉一次推断上的踩坑。
     *
     * <p>{@code ComponentSerialization.STREAM_CODEC} 是按 JSON 编解码组件，
     * decode 时需要注册表来解析可能出现的物品悬停事件，正好 {@link RegistryFriendlyByteBuf}
     * 自带注册表访问。</p>
     */
    public static final StreamCodec<RegistryFriendlyByteBuf, ShareQuestPacket> STREAM_CODEC =
            StreamCodec.of(
                    (buf, packet) -> {
                        buf.writeLong(packet.questId());
                        ComponentSerialization.STREAM_CODEC.encode(buf, packet.title());
                    },
                    buf -> new ShareQuestPacket(buf.readLong(), ComponentSerialization.STREAM_CODEC.decode(buf)));

    /** 任何一端都不该出现 null 标题，这里兜一下底。 */
    public ShareQuestPacket {
        if (title == null) {
            title = Component.empty();
        }
    }

    @Override
    public CustomPacketPayload.Type<ShareQuestPacket> type() {
        return TYPE;
    }

    /**
     * 服务端处理。
     *
     * <p>这个包注册为 playToServer，所以处理器只会在服务端被调用，
     * {@code context.player()} 就是发包的那个玩家，客户端伪造不了发送者。</p>
     */
    public static void handle(ShareQuestPacket packet, IPayloadContext context) {
        if (context.player() instanceof ServerPlayer sender) {
            QuestBroadcaster.broadcast(sender, packet.questId(), packet.title());
        }
    }
}
