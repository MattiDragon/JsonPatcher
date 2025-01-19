package io.github.mattidragon.jsonpatcher.client;

import io.github.mattidragon.jsonpatcher.patch.ErrorLogger;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;

public class JsonPatcherClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ClientLifecycleEvents.CLIENT_STARTED.register(client -> ErrorLogger.CURRENT.set(error -> {
            var player = client.player;
            if (player != null) {
                player.sendMessage(error, false);
            }
        }));
    }
}
