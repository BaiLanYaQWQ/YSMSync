package moe.meowrealms.ysmsync.network;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.MinecraftKey;
import io.netty.buffer.ByteBuf;
import moe.meowrealms.ysmsync.YSMSyncMain;
import org.bukkit.Bukkit;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;

/**
 * ProtocolLib-based YSM receiver for Purpur 1.19.4.
 *
 * It deliberately observes only the YSM custom payload, cancels that one
 * packet after copying its bytes, and leaves every other packet untouched.
 * This avoids Bukkit's unreliable Forge PluginMessage forwarding path.
 */
public final class Purpur1194ProtocolLibBridge {
    private static final String CHANNEL = "yes_steve_model:2_6_0";
    private static final AtomicBoolean INITIALIZED = new AtomicBoolean();

    private Purpur1194ProtocolLibBridge() { }

    public static void init() {
        if (!INITIALIZED.compareAndSet(false, true)) return;
        if (Bukkit.getPluginManager().getPlugin("ProtocolLib") == null) {
            Bukkit.getLogger().warning("[YSMSync] ProtocolLib is unavailable; YSM Forge payloads may not synchronize on Purpur 1.19.4.");
            return;
        }
        ProtocolLibrary.getProtocolManager().addPacketListener(new PacketAdapter(
                YSMSyncMain.Companion.getInstance(), PacketType.Play.Client.CUSTOM_PAYLOAD) {
            @Override public void onPacketReceiving(PacketEvent event) {
                try {
                    PacketContainer packet = event.getPacket();
                    MinecraftKey key = packet.getMinecraftKeys().readSafely(0);
                    if (key == null || !CHANNEL.equals(key.getFullKey())) return;
                    byte[] payload = payload(packet.getHandle());
                    if (payload == null) {
                        Bukkit.getLogger().warning("[YSMSync] Received YSM payload from " + event.getPlayer().getName() + " but ProtocolLib could not read its bytes.");
                        return;
                    }
                    event.setCancelled(true);
                    Bukkit.getLogger().info("[YSMSync] Received native YSM payload (" + payload.length + " bytes) from " + event.getPlayer().getName());
                    ClientConnectionManager.onNativeClientMessage(event.getPlayer(), payload);
                } catch (Throwable throwable) {
                    Bukkit.getLogger().log(Level.WARNING, "[YSMSync] Failed to process a ProtocolLib YSM payload", throwable);
                }
            }
        });
        Bukkit.getLogger().info("[YSMSync] ProtocolLib YSM payload receiver enabled.");
    }

    private static byte[] payload(Object packet) throws IllegalAccessException {
        for (Class<?> type = packet.getClass(); type != null; type = type.getSuperclass()) {
            for (Field field : type.getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers())) continue;
                if (!ByteBuf.class.isAssignableFrom(field.getType())) continue;
                field.setAccessible(true);
                ByteBuf buffer = (ByteBuf) field.get(packet);
                if (buffer == null) continue;
                byte[] bytes = new byte[buffer.readableBytes()];
                buffer.getBytes(buffer.readerIndex(), bytes);
                return bytes;
            }
        }
        return null;
    }
}
