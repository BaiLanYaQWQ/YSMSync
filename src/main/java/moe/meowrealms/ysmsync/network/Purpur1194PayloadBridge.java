package moe.meowrealms.ysmsync.network;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelPipeline;
import org.bukkit.entity.Player;
import org.bukkit.Bukkit;

import java.lang.reflect.Field;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/** Receives 1.19.4 Forge YSM payloads after Purpur has decoded the packet. */
public final class Purpur1194PayloadBridge extends ChannelInboundHandlerAdapter {
    private static final String HANDLER_NAME = "ysmsync_ysm_payload_bridge";
    private static final String CHANNEL = "yes_steve_model:2_6_0";
    private static final Map<UUID, Channel> CHANNELS = new ConcurrentHashMap<>();
    private final Player player;

    private Purpur1194PayloadBridge(Player player) { this.player = player; }

    public static void inject(Player player) {
        try {
            Channel channel = getNettyChannel(player);
            channel.eventLoop().execute(() -> {
                ChannelPipeline pipeline = channel.pipeline();
                if (pipeline.get(HANDLER_NAME) != null) pipeline.remove(HANDLER_NAME);
                if (pipeline.get("decoder") == null) {
                    Bukkit.getLogger().warning("[YSMSync] Cannot install YSM payload bridge for " + player.getName() + ": decoder is absent");
                    return;
                }
                pipeline.addAfter("decoder", HANDLER_NAME, new Purpur1194PayloadBridge(player));
                CHANNELS.put(player.getUniqueId(), channel);
                Bukkit.getLogger().info("[YSMSync] Installed native YSM payload bridge for " + player.getName());
            });
        } catch (Exception exception) {
            Bukkit.getLogger().log(Level.WARNING, "[YSMSync] Unable to install native YSM payload bridge for " + player.getName(), exception);
        }
    }

    public static void remove(Player player) {
        Channel channel = CHANNELS.remove(player.getUniqueId());
        if (channel == null) return;
        channel.eventLoop().execute(() -> {
            if (channel.pipeline().get(HANDLER_NAME) != null) channel.pipeline().remove(HANDLER_NAME);
        });
    }

    /**
     * Send a real play-state CustomPayload packet. Bukkit's plugin-message
     * wrapper does not establish Forge SimpleChannel state on this Purpur
     * 1.19.4 server, which leaves YSM's client-side channel version unset.
     */
    public static boolean sendClientboundPayload(Player player, String channelName, byte[] payload) {
        if (player == null || !player.isOnline()) return false;
        try {
            Object key = createKey(channelName);
            Object buffer = createBuffer(payload);
            Object packet = createPayloadPacket(key, buffer);
            Channel channel = getNettyChannel(player);
            channel.eventLoop().execute(() -> channel.writeAndFlush(packet));
            Bukkit.getLogger().info("[YSMSync] Sent native YSM payload (" + payload.length + " bytes) to " + player.getName());
            return true;
        } catch (Throwable throwable) {
            Bukkit.getLogger().log(Level.WARNING, "[YSMSync] Native YSM payload send failed; using Bukkit fallback", throwable);
            return false;
        }
    }

    private static Object createKey(String name) throws Exception {
        String[] parts = name.split(":", 2);
        for (String className : new String[]{"net.minecraft.resources.MinecraftKey", "net.minecraft.resources.ResourceLocation"}) {
            try {
                Class<?> type = Class.forName(className);
                for (Constructor<?> constructor : type.getDeclaredConstructors()) {
                    constructor.setAccessible(true);
                    if (constructor.getParameterCount() == 1 && constructor.getParameterTypes()[0] == String.class) return constructor.newInstance(name);
                    if (constructor.getParameterCount() == 2 && constructor.getParameterTypes()[0] == String.class && constructor.getParameterTypes()[1] == String.class) return constructor.newInstance(parts[0], parts[1]);
                }
            } catch (ClassNotFoundException ignored) { }
        }
        throw new ClassNotFoundException("No 1.19.4 ResourceLocation class found");
    }

    private static Object createBuffer(byte[] bytes) throws Exception {
        ByteBuf raw = io.netty.buffer.Unpooled.wrappedBuffer(bytes);
        for (String className : new String[]{"net.minecraft.network.PacketDataSerializer", "net.minecraft.network.FriendlyByteBuf"}) {
            try {
                Class<?> type = Class.forName(className);
                for (Constructor<?> constructor : type.getDeclaredConstructors()) {
                    if (constructor.getParameterCount() == 1 && constructor.getParameterTypes()[0].isAssignableFrom(ByteBuf.class)) {
                        constructor.setAccessible(true);
                        return constructor.newInstance(raw);
                    }
                }
            } catch (ClassNotFoundException ignored) { }
        }
        throw new ClassNotFoundException("No 1.19.4 packet buffer class found");
    }

    private static Object createPayloadPacket(Object key, Object buffer) throws Exception {
        for (String className : new String[]{"net.minecraft.network.protocol.game.PacketPlayOutCustomPayload", "net.minecraft.network.protocol.game.ClientboundCustomPayloadPacket"}) {
            try {
                Class<?> type = Class.forName(className);
                for (Constructor<?> constructor : type.getDeclaredConstructors()) {
                    Class<?>[] types = constructor.getParameterTypes();
                    if (types.length == 2 && types[0].isInstance(key) && types[1].isInstance(buffer)) {
                        constructor.setAccessible(true);
                        return constructor.newInstance(key, buffer);
                    }
                }
            } catch (ClassNotFoundException ignored) { }
        }
        throw new NoSuchMethodException("No compatible native custom-payload constructor found");
    }

    @Override public void channelRead(ChannelHandlerContext context, Object message) throws Exception {
        // This handler sits immediately after Minecraft's packet decoder.  At
        // this point custom payloads are native PacketPlayInCustomPayload
        // objects, so neither ViaVersion packet ids nor wire framing matter.
        try {
            byte[] payload = extractYsmPayload(message);
            if (payload != null) {
                ClientConnectionManager.onNativeClientMessage(player, payload);
                Bukkit.getLogger().info("[YSMSync] Captured decoded YSM payload (" + payload.length + " bytes) from " + player.getName());
            }
        } catch (ReflectiveOperationException | RuntimeException ignored) { }
        // Do not consume anything: Purpur and every other plugin retain their
        // normal packet path, including vanilla clients with no YSM installed.
        super.channelRead(context, message);
    }

    private static byte[] extractYsmPayload(Object packet) throws IllegalAccessException {
        if (packet == null || !packet.getClass().getName().contains("CustomPayload")) return null;
        String channel = null;
        ByteBuf data = null;
        for (Class<?> type = packet.getClass(); type != null; type = type.getSuperclass()) {
            for (Field field : type.getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers())) continue;
                field.setAccessible(true);
                Object value = field.get(packet);
                if (value instanceof ByteBuf buffer) data = buffer;
                else if (value != null && (field.getType().getName().contains("MinecraftKey") || field.getType().getName().contains("ResourceLocation"))) channel = value.toString();
            }
        }
        if (!CHANNEL.equals(channel) || data == null) return null;
        byte[] payload = new byte[data.readableBytes()];
        data.getBytes(data.readerIndex(), payload);
        return payload;
    }

    private static Channel getNettyChannel(Player player) throws Exception {
        Method getHandle = player.getClass().getMethod("getHandle");
        return findChannel(getHandle.invoke(player));
    }

    private static Channel findChannel(Object root) throws IllegalAccessException {
        record Node(Object value, int depth) { }
        ArrayDeque<Node> queue = new ArrayDeque<>();
        Set<Object> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        queue.add(new Node(root, 0));
        while (!queue.isEmpty()) {
            Node node = queue.removeFirst(); Object value = node.value();
            if (value == null || !seen.add(value) || node.depth() > 5) continue;
            if (value instanceof Channel channel) return channel;
            Class<?> type = value.getClass();
            if (!type.getPackageName().startsWith("net.minecraft")) continue;
            for (Class<?> cursor = type; cursor != null; cursor = cursor.getSuperclass()) for (Field field : cursor.getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers()) || field.getType().isPrimitive()) continue;
                String packageName = field.getType().getPackageName();
                if (!Channel.class.isAssignableFrom(field.getType()) && !packageName.startsWith("net.minecraft")) continue;
                try {
                    field.setAccessible(true); Object child = field.get(value);
                    if (child instanceof Channel channel) return channel;
                    if (child != null) queue.addLast(new Node(child, node.depth() + 1));
                } catch (RuntimeException ignored) { }
            }
        }
        throw new IllegalStateException("Unable to locate player Netty channel");
    }
}
