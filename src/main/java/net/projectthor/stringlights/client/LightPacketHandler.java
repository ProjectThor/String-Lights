package net.projectthor.stringlights.client;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.projectthor.stringlights.light.IncompleteLightLink;
import net.projectthor.stringlights.networking.packet.KnotChangePayload;
import net.projectthor.stringlights.networking.packet.LightAttachPayload;
import net.projectthor.stringlights.networking.packet.MultiLightAttachPayload;

import static net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.registerGlobalReceiver;

@Environment(EnvType.CLIENT)
public class LightPacketHandler {

    public LightPacketHandler() {
        register();
    }

    private void register() {
        registerGlobalReceiver(LightAttachPayload.PAYLOAD_ID, LightAttachPayload::apply);
        registerGlobalReceiver(MultiLightAttachPayload.PAYLOAD_ID, MultiLightAttachPayload::apply);
        registerGlobalReceiver(KnotChangePayload.PAYLOAD_ID, KnotChangePayload::apply);
    }

    public void tick() {
        LightAttachPayload.incompleteLinks.removeIf(IncompleteLightLink::tryCompleteOrRemove);
    }
}
