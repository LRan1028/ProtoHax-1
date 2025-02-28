package dev.sora.relay.game

import com.nukkitx.network.util.DisconnectReason
import com.nukkitx.protocol.bedrock.BedrockPacket
import com.nukkitx.protocol.bedrock.packet.LoginPacket
import dev.sora.relay.RakNetRelaySession
import dev.sora.relay.RakNetRelaySessionListener
import dev.sora.relay.game.entity.EntityPlayerSP
import dev.sora.relay.game.event.*
import dev.sora.relay.game.utils.mapping.BlockMappingUtils
import dev.sora.relay.game.utils.mapping.EmptyRuntimeMapping
import dev.sora.relay.game.utils.mapping.RuntimeMapping
import dev.sora.relay.game.world.WorldClient

class GameSession : RakNetRelaySessionListener.PacketListener {

    val thePlayer = EntityPlayerSP(this)
    val theWorld = WorldClient(this)

    val eventManager = EventManager()

    lateinit var netSession: RakNetRelaySession

    var blockMapping: RuntimeMapping = EmptyRuntimeMapping()
    var legacyBlockMapping: RuntimeMapping = EmptyRuntimeMapping()

    val netSessionInitialized: Boolean
        get() = this::netSession.isInitialized

    init {
        eventManager.registerListener(thePlayer)
        eventManager.registerListener(theWorld)
    }

    override fun onPacketInbound(packet: BedrockPacket): Boolean {
        val event = EventPacketInbound(this, packet)
        eventManager.emit(event)
        if (event.isCanceled()) {
            return false
        }

        return true
    }

    override fun onPacketOutbound(packet: BedrockPacket): Boolean {
        val event = EventPacketOutbound(this, packet)
        eventManager.emit(event)
        if (event.isCanceled()) {
            return false
        }

        if (packet is LoginPacket) {
            blockMapping = BlockMappingUtils.craftMapping(packet.protocolVersion)
            legacyBlockMapping = BlockMappingUtils.craftMapping(packet.protocolVersion, "legacy")
        }

        return true
    }

    override fun onDisconnect(client: Boolean, reason: DisconnectReason) {
        eventManager.emit(EventDisconnect(this, client, reason))
    }

    fun onTick() {
        eventManager.emit(EventTick(this))
    }

    fun sendPacket(packet: BedrockPacket) {
        val event = EventPacketOutbound(this, packet)
        eventManager.emit(event)
        if (event.isCanceled()) {
            return
        }

        netSession.outboundPacket(packet)
    }

    fun sendPacketToClient(packet: BedrockPacket) {
        val event = EventPacketInbound(this, packet)
        eventManager.emit(event)
        if (event.isCanceled()) {
            return
        }
        netSession.outboundPacket(packet)
    }

    companion object {
        const val RECOMMENDED_VERSION = "1.19.50.02"
    }
}