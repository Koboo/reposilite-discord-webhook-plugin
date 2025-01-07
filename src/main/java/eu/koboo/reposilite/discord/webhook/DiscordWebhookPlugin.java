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
import java.util.regex.Pattern;

@Plugin(name = "discord-webhook-plugin", dependencies = {"configuration", "local-configuration", "shared-configuration"}, settings = DiscordWebhookSettings.class)
public class DiscordWebhookPlugin extends ReposilitePlugin {

    private static final Pattern WEB_SCHEME_PATTERN = Pattern.compile("^(https|http)(://)(.+)$");

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
        settingsRef.subscribe(this::handleConfigurationUpdate);
        handleConfigurationUpdate(settingsRef.get());

        extensions().registerEvent(DeployEvent.class, new DeployEventListener(this));
        extensions().registerEvent(ReposiliteDisposeEvent.class, new DisposeEventListener(this));
        return null;
    }

    private void handleConfigurationUpdate(DiscordWebhookSettings rootSettings) {
        getLogger().debug("Recreating WebHookClients from configuration..");
        closePreviousWebHooksClients();

        String rootWebHookUrl = rootSettings.getRootWebHookUrl();
        if (rootWebHookUrl.equalsIgnoreCase(DiscordWebhookSettings.DEFAULT_WEBHOOK)) {
            getLogger().info("You need to configure the settings of the " +
                             "Discord WebHook Plugin in the frontend!");
            return;
        }
        if(!WEB_SCHEME_PATTERN.matcher(rootWebHookUrl).matches()) {
            getLogger().info(createInvalidURLMessage("root webhook", null));
            return;
        }

        String rootBotIconUrl = rootSettings.getRootBotIconUrl();
        if(rootBotIconUrl != null && !WEB_SCHEME_PATTERN.matcher(rootBotIconUrl).matches()) {
            getLogger().info(createInvalidURLMessage("root boticon", null));
            return;
        }

        String repositoryDomain = rootSettings.getRepositoryDomain();
        if(repositoryDomain != null && !WEB_SCHEME_PATTERN.matcher(repositoryDomain).matches()) {
            getLogger().info(createInvalidURLMessage("root repository domain", null));
            return;
        }

        try {
            rootWebHookClient = createWebhookClient("Root", rootWebHookUrl);
            getLogger().debug("Created root WebHookClient!");
            List<RepositoryWebHookSettings> repositoriesList = rootSettings.getAnnouncedRepositoriesList();
            if (repositoriesList != null && !repositoriesList.isEmpty()) {
                for (RepositoryWebHookSettings settings : repositoriesList) {
                    if (settings.getReference() == null
                        || settings.getReference().trim().equalsIgnoreCase("")) {
                        getLogger().info("Couldn't create a new WebHookClient for a repository, " +
                                         "because the repository name is empty. " +
                                         "Please check the configuration of the Discord WebHook Plugin.");
                        continue;
                    }
                    String webHookUrl = settings.getWebHookUrl();
                    if (webHookUrl == null) {
                        continue;
                    }
                    if(!WEB_SCHEME_PATTERN.matcher(rootWebHookUrl).matches()) {
                        getLogger().info(createInvalidURLMessage("webhook", settings.getReference()));
                        continue;
                    }
                    String botIconUrl = settings.getBotIconUrl();
                    if(botIconUrl != null && !WEB_SCHEME_PATTERN.matcher(botIconUrl).matches()) {
                        getLogger().info(createInvalidURLMessage("boticon", settings.getReference()));
                        continue;
                    }
                    WebhookClient repoWebHookClient = createWebhookClient(settings.getReference(), webHookUrl);
                    webhookClientMap.put(settings.getReference(), repoWebHookClient);
                    getLogger().debug("Created WebHookClient for repository \""  +
                                      settings.getReference() + "\"!");
                }
            }
        } catch (Exception e) {
            getLogger().info("Couldn't create WebHookClients. " +
                             "Please check the configuration of the Discord WebHook Plugin.");
            getLogger().exception(e);
        }
    }

    private String createInvalidURLMessage(String type, String repository) {
        String repositoryPart = "";
        if(repository != null && !repository.trim().isEmpty()) {
            repositoryPart = " of the repository \"" + repository + "\"";
        }
        return "The " + type + " url" + repositoryPart + " is incorrect! " +
                "You need to provide a url, which starts with \"https://\" or \"http://\"! " +
                "Please check the configuration of the Discord WebHook Plugin.";
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
        for (String repositoryName : webhookClientMap.keySet()) {
            WebhookClient webhookClient = webhookClientMap.remove(repositoryName);
            if (webhookClient == null) {
                continue;
            }
            webhookClient.close();
        }
        webhookClientMap.clear();
        if (rootWebHookClient != null) {
            rootWebHookClient.close();
            rootWebHookClient = null;
        }
    }

    public WebhookClient getWebHookClient(String repositoryName) {
        WebhookClient retClient = webhookClientMap.get(repositoryName);
        if (retClient == null) {
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

    public RepositoryWebHookSettings getRepositorySettings(String repositoryName) {
        if(repositoryName == null) {
            return null;
        }
        for (RepositoryWebHookSettings repositoryWebHookSettings : settingsRef.get().getAnnouncedRepositoriesList()) {
            if(repositoryWebHookSettings.getReference() == null) {
                continue;
            }
            if (!repositoryName.equalsIgnoreCase(repositoryWebHookSettings.getReference())) {
                continue;
            }
            return repositoryWebHookSettings;
        }
        return null;
    }
}