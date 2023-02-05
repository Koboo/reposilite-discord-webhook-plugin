package eu.koboo.reposilite.discord.webhook;

import club.minnced.discord.webhook.WebhookClient;
import club.minnced.discord.webhook.WebhookClientBuilder;
import com.reposilite.configuration.shared.SharedConfigurationFacade;
import com.reposilite.maven.api.DeployEvent;
import com.reposilite.plugin.api.Facade;
import com.reposilite.plugin.api.Plugin;
import com.reposilite.plugin.api.ReposiliteDisposeEvent;
import com.reposilite.plugin.api.ReposilitePlugin;
import eu.koboo.reposilite.discord.webhook.listener.DeployEventListener;
import eu.koboo.reposilite.discord.webhook.listener.DisposeEventListener;
import eu.koboo.reposilite.discord.webhook.settings.DiscordWebhookSettings;
import eu.koboo.reposilite.discord.webhook.settings.RepositoryWebHookSettings;
import kotlin.jvm.JvmClassMappingKt;
import kotlin.reflect.KClass;
import org.jetbrains.annotations.Nullable;
import panda.std.reactive.MutableReference;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Plugin(name = "discord-webhook-plugin", dependencies = {"configuration", "local-configuration", "shared-configuration"}, settings = DiscordWebhookSettings.class)
public class DiscordWebhookPlugin extends ReposilitePlugin {

    private static final String WEBHOOK = "https://discord.com/api/webhooks/1070301314963230730/wWeNkBNc-0Jv9QfbMkUZzPJPN7dwbueY8da5iO50oPbH_hXmDL-F57rPEkwAZBm7Nnjk";
    private static final String REPOSILITE_ICON = "https://reposilite.com/images/favicon.png";
    private static final String REPOSITORY_URL = "https://reposilite.koboo.eu";

    MutableReference<DiscordWebhookSettings> settingsRef;

    WebhookClient rootWebHookClient;
    Map<String, WebhookClient> webhookClientMap;

    @Override
    public @Nullable Facade initialize() {
        webhookClientMap = new HashMap<>();

        SharedConfigurationFacade sharedConfigurationFacade = extensions().facade(SharedConfigurationFacade.class);
        sharedConfigurationFacade.updateSharedSettings("discord-webhook", new DiscordWebhookSettings());

        KClass<DiscordWebhookSettings> kotlinClass = JvmClassMappingKt.getKotlinClass(DiscordWebhookSettings.class);
        settingsRef = sharedConfigurationFacade.getDomainSettings(kotlinClass);
        settingsRef.subscribe(settings -> handleConfigurationUpdate());

        extensions().registerEvent(DeployEvent.class, new DeployEventListener(this));
        extensions().registerEvent(ReposiliteDisposeEvent.class, new DisposeEventListener(this));
        return null;
    }

    private void handleConfigurationUpdate() {
        closePreviousWebHooksClients();

        if (settingsRef.get().getRootWebHookUrl().equalsIgnoreCase(DiscordWebhookSettings.DEFAULT_WEBHOOK)) {
            getLogger().info("You need to configure the settings discord-webhook-plugin in the frontend!");
            return;
        }

        try {
            rootWebHookClient = createWebhookClient("Root", settingsRef.get().getRootWebHookUrl());
            List<RepositoryWebHookSettings> repositoriesList = settingsRef.get().getAnnouncedRepositoriesList();
            if(repositoriesList != null && !repositoriesList.isEmpty()) {
                for (RepositoryWebHookSettings settings : repositoriesList) {
                    if(settings.getReference() == null || settings.getReference().trim().equalsIgnoreCase("")) {
                        getLogger().info("Couldn't create a new WebHookClient for a repository, because the repository-name was empty. Please check the configuration of the Discord WebHook.");
                        continue;
                    }
                    if(settings.getWebHookUrl() == null) {
                        continue;
                    }
                    WebhookClient repoWebHookClient = createWebhookClient(settings.getReference(), settings.getWebHookUrl());
                    webhookClientMap.put(settings.getReference(), repoWebHookClient);
                }
            }
        } catch (Exception e) {
            getLogger().info("Couldn't create WebHookClients. Please check the configuration of the Discord WebHook.");
            getLogger().exception(e);
        }
    }

    private WebhookClient createWebhookClient(String prefix, String webHookUrl) {
        WebhookClientBuilder webhookClientBuilder = new WebhookClientBuilder(webHookUrl);
        webhookClientBuilder.setThreadFactory((job) -> {
            Thread thread = new Thread(job);
            thread.setName("DiscordWebHook-Thread-" + prefix);
            thread.setDaemon(true);
            return thread;
        });
        webhookClientBuilder.setWait(true);
        return webhookClientBuilder.build();
    }

    public void closePreviousWebHooksClients() {
        for (WebhookClient webhookClient : webhookClientMap.values()) {
            if(webhookClient.isShutdown()) {
                continue;
            }
            webhookClient.close();
        }
        webhookClientMap.clear();
        if(rootWebHookClient != null) {
            rootWebHookClient.close();
            rootWebHookClient = null;
        }
    }

    public WebhookClient getWebHookClient(String repositoryName) {
        WebhookClient retClient = webhookClientMap.get(repositoryName);
        if(retClient == null) {
            retClient = rootWebHookClient;
        }
        return retClient;
    }

    public WebhookClient getRootWebHookClient() {
        return rootWebHookClient;
    }

    public DiscordWebhookSettings getSettings() {
        return settingsRef.get();
    }
}