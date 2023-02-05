package eu.koboo.reposilite.discord.webhook.listener;

import com.reposilite.plugin.api.EventListener;
import com.reposilite.plugin.api.ReposiliteDisposeEvent;
import eu.koboo.reposilite.discord.webhook.DiscordWebhookPlugin;
import org.jetbrains.annotations.NotNull;

public class DisposeEventListener implements EventListener<ReposiliteDisposeEvent> {

    private final DiscordWebhookPlugin webhookPlugin;

    public DisposeEventListener(DiscordWebhookPlugin webhookPlugin) {
        this.webhookPlugin = webhookPlugin;
    }

    @Override
    public void onCall(@NotNull ReposiliteDisposeEvent reposiliteDisposeEvent) {
        webhookPlugin.closePreviousWebHooksClients();
    }
}
