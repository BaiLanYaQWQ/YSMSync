package moe.meowrealms.ysmsync.tracker

import io.papermc.paper.event.player.PlayerTrackEntityEvent
import io.papermc.paper.event.player.PlayerUntrackEntityEvent
import moe.meowrealms.ysmsync.YSMSyncConstants
import moe.meowrealms.ysmsync.YSMSyncMain
import moe.meowrealms.ysmsync.model.ModelManager
import moe.meowrealms.ysmsync.network.ClientConnectionManager.getConfirmedYsmConnectionOrNull
import moe.meowrealms.ysmsync.network.data.DispatchServerDrivenProperty
import moe.meowrealms.ysmsync.network.packet.s2c.S2CEntityModelSelectionDataPacket
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerQuitEvent
import java.util.*
import java.util.concurrent.ConcurrentHashMap

object EntityTracker : Listener{
    private val trackerVisibleMap: MutableMap<UUID, MutableSet<UUID>> = ConcurrentHashMap<UUID, MutableSet<UUID>>()

    fun init() {
        Bukkit.getPluginManager().registerEvents(this, YSMSyncMain.instance)
    }

    @EventHandler
    fun onPlayerTrackEntity(trackEvent: PlayerTrackEntityEvent) {
        val tracker = trackEvent.getPlayer()
        val tracked = trackEvent.entity

        if (tracked is Player) {
            val visibleList = this.trackerVisibleMap.computeIfAbsent(tracker.uniqueId) { HashSet() }

            if (visibleList.add(tracked.uniqueId)) {
                this.handlePairingAdd(tracker, tracked)
            }
        }
    }

    fun getVisible(of: Player): Set<Player> {
        val visibleList = this.trackerVisibleMap[of.uniqueId]

        if (visibleList != null) {
            val result = mutableSetOf<Player>()

            for (uuid in visibleList) {
                val target = Bukkit.getPlayer(uuid)

                if (target != null) {
                    result.add(target)
                }
            }

            return result
        }

        return emptySet()
    }

    fun handlePairingAdd(owner: Player, watched: Player) {
        val ownerConnection = owner.getConfirmedYsmConnectionOrNull() ?: return
        val watchedConnection = watched.getConfirmedYsmConnectionOrNull()

        if (watchedConnection != null) {
            watchedConnection.syncModelFullDataTo(owner)
            return
        }

        // YSM creates an initialized default-model capability for every player.
        // Sending nothing therefore leaves its custom player renderer active.
        // That renderer treats ItemsAdder's non-ArmorItem helmet carrier as a
        // head item and renders the complete IA model/texture on the head.
        // forceDisabled is checked by YSM before replacing the vanilla player
        // renderer, so explicitly setting it keeps both skin and IA armor on
        // Minecraft's normal rendering path. If the watched player later
        // confirms a YSM handshake, resynchronizeWatchers sends the real model
        // and clears this flag again.
        val fallback = ModelManager.getDefaultModelConfig(
            YSMSyncConstants.ModelDefaults.DEFAULT_MODEL_ID,
            YSMSyncConstants.ModelDefaults.DEFAULT_MODEL_TEXTURE
        )
        ownerConnection.send(S2CEntityModelSelectionDataPacket(
            watched.entityId,
            fallback.left,
            fallback.right,
            true,
            DispatchServerDrivenProperty(watched.entityId)
        ))
    }

    fun resynchronizeViewer(viewer: Player) {
        for (watched in this.getVisible(viewer)) {
            this.handlePairingAdd(viewer, watched)
        }
    }

    fun resynchronizeWatchers(watched: Player) {
        for ((watcherUuid, visible) in this.trackerVisibleMap) {
            if (!visible.contains(watched.uniqueId)) continue
            val watcher = Bukkit.getPlayer(watcherUuid) ?: continue
            this.handlePairingAdd(watcher, watched)
        }
    }

    @EventHandler
    fun onPlayerQuit(event: PlayerQuitEvent) {
        this.trackerVisibleMap.remove(event.player.uniqueId)
        // Paper normally emits untrack events first, but this is not guaranteed
        // during fast disconnect/reconnect or world transitions. A stale UUID
        // would make Set.add return false after rejoin and skip state delivery.
        for (visible in this.trackerVisibleMap.values) {
            visible.remove(event.player.uniqueId)
        }
    }

    @EventHandler
    fun onPlayerUntrackEntity(untrackEvent: PlayerUntrackEntityEvent) {
        val watcher = untrackEvent.getPlayer()
        val untracked = untrackEvent.entity

        if (untracked is Player) {
            val visibleList = this.trackerVisibleMap[watcher.uniqueId] ?: return

            visibleList.remove(untracked.uniqueId)
        }
    }

}
