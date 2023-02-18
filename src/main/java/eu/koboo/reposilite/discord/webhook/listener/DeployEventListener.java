package eu.koboo.reposilite.discord.webhook.listener;

import club.minnced.discord.webhook.WebhookClient;
import club.minnced.discord.webhook.send.WebhookEmbed;
import club.minnced.discord.webhook.send.WebhookEmbedBuilder;
import com.reposilite.maven.api.DeployEvent;
import com.reposilite.plugin.api.EventListener;
import com.reposilite.storage.api.Location;
import eu.koboo.reposilite.discord.webhook.DiscordWebhookPlugin;
import eu.koboo.reposilite.discord.webhook.settings.RepositoryWebHookSettings;
import org.jetbrains.annotations.NotNull;

public class DeployEventListener implements EventListener<DeployEvent> {

    private final DiscordWebhookPlugin plugin;

    public DeployEventListener(DiscordWebhookPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void onCall(@NotNull DeployEvent deployEvent) {
        // Removing user ip address
        String username = deployEvent.getBy().split("@")[0];
        String repositoryName = deployEvent.getRepository().getName();

        RepositoryWebHookSettings settings = plugin.getRepositorySettings(repositoryName);
        if (settings == null) {
            plugin.getLogger().debug("Couldn't find RepositoryWebHookSettings for repository " + repositoryName + "!");
            return;
        }

        WebhookClient webhookClient = plugin.getWebHookClient(repositoryName);
        if (webhookClient == null || webhookClient.isShutdown()) {
            plugin.getLogger().debug("Couldn't find WebHookClient for repository " + repositoryName + "!");
            return;
        }

        String artifactFilterRegex = settings.getArtifactFilter();
        String simpleName = deployEvent.getGav().getSimpleName();
        if (artifactFilterRegex != null && !simpleName.matches(artifactFilterRegex)) {
            plugin.getLogger().debug("Regex isn't matching to artifact " + simpleName + "!");
            return;
        }

        Location parentGov = deployEvent.getGav().getParent();
        String parentString = parentGov.toString();

        // Building the full url to redirect to the repository
        String repositoryDomain = plugin.getSettings().getRepositoryDomain();
        if (!repositoryDomain.endsWith("/")) {
            repositoryDomain = repositoryDomain + "/";
        }
        String artifactUrl = repositoryDomain + "#/" + repositoryName + "/" + parentString;

        String[] split = parentString.split("\\/");
        String version = split[split.length - 1];
        String artifactId = split[split.length - 2];
        String replacedParent = parentString
                .replaceAll(version, "")
                .replaceAll(artifactId, "");
        String groupId = replacedParent
                .substring(0, replacedParent.length() - 2)
                .replace("/", ".");

        String botIconUrl = plugin.getSettings().getRootBotIconUrl();
        if(settings.getBotIconUrl() != null && !settings.getBotIconUrl().trim().isEmpty()) {
            botIconUrl = settings.getBotIconUrl();
        }

        WebhookEmbedBuilder embedBuilder = new WebhookEmbedBuilder();
        embedBuilder.setAuthor(new WebhookEmbed.EmbedAuthor(username, botIconUrl, artifactUrl));
        embedBuilder.setTitle(new WebhookEmbed.EmbedTitle("[" + simpleName + "] new deployment", artifactUrl));
        embedBuilder.setDescription("A new artifact was deployed!");

        embedBuilder.addField(new WebhookEmbed.EmbedField(
                true,
                "**Gradle (Groovy)**",
                replace("```groovy\n" +
                        "implementation \"$groupId:$artifactId:$version\"\n" +
                        "```\n", groupId, artifactId, version)
        ));
        embedBuilder.addField(new WebhookEmbed.EmbedField(
                false,
                "**Maven**",
                replace("```xml\n" +
                        "<dependency>\n" +
                        "  <groupId>$groupId</groupId>\n" +
                        "  <artifactId>$artifactId</artifactId>\n" +
                        "  <version>$version</version>\n" +
                        "</dependency>```\n", groupId, artifactId, version)
        ));
        embedBuilder.setColor(0x49BE25);
        plugin.getRootWebHookClient().send(embedBuilder.build())
                .thenAccept(readonlyMessage -> {
                    plugin.getLogger().info("WEBHOOK | Send deploy message of artifacts " + deployEvent.getGav() + " to discord-server!");
                });
    }

    private String replace(String text, String groupId, String artifactId, String version) {
        return text.replace("$groupId", groupId)
                .replace("$artifactId", artifactId)
                .replace("$version", version);
    }
}
