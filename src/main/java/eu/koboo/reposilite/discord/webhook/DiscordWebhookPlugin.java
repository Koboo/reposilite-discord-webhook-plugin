package eu.koboo.reposilite.discord.webhook;

import club.minnced.discord.webhook.WebhookClient;
import club.minnced.discord.webhook.WebhookClientBuilder;
import club.minnced.discord.webhook.send.WebhookEmbed;
import club.minnced.discord.webhook.send.WebhookEmbedBuilder;
import com.reposilite.maven.api.DeployEvent;
import com.reposilite.plugin.api.Facade;
import com.reposilite.plugin.api.Plugin;
import com.reposilite.plugin.api.ReposiliteInitializeEvent;
import com.reposilite.plugin.api.ReposilitePlugin;
import com.reposilite.storage.api.Location;
import org.jetbrains.annotations.Nullable;

@Plugin(name = "discord-webhook-plugin")
public class DiscordWebhookPlugin extends ReposilitePlugin {

    private static final String WEBHOOK = "https://discord.com/api/webhooks/1070301314963230730/wWeNkBNc-0Jv9QfbMkUZzPJPN7dwbueY8da5iO50oPbH_hXmDL-F57rPEkwAZBm7Nnjk";
    private static final String REPOSILITE_ICON = "https://reposilite.com/images/favicon.png";
    private static final String REPOSITORY_URL = "https://reposilite.koboo.eu";

    WebhookClient webhookClient;

    public DiscordWebhookPlugin() {
        WebhookClientBuilder webhookClientBuilder = new WebhookClientBuilder(WEBHOOK);
        webhookClientBuilder.setThreadFactory((job) -> {
            Thread thread = new Thread(job);
            thread.setName("WebHook-Thread-DiscordWebhookPlugin");
            thread.setDaemon(true);
            return thread;
        });
        webhookClientBuilder.setWait(true);

        this.webhookClient = webhookClientBuilder.build();
    }

    @Override
    public @Nullable Facade initialize() {
        extensions().registerEvent(ReposiliteInitializeEvent.class, event -> {
            getLogger().info("");
            getLogger().info(DiscordWebhookPlugin.class + " was successful loaded!");
        });
        extensions().registerEvent(DeployEvent.class, deployEvent -> {
            if (!deployEvent.getGav().getExtension().endsWith("jar")) {
                return;
            }
            // Removing user ip address
            String username = deployEvent.getBy().split("@")[0];
            String repositoryName = deployEvent.getRepository().getName();

            Location parentGov = deployEvent.getGav().getParent();
            String parentString = parentGov.toString();

            // Building the full url to redirect to the repository
            String artifactUrl = REPOSITORY_URL + "/#/" + repositoryName + "/" + parentString;

            String[] split = parentString.split("\\/");
            String version = split[split.length - 1];
            String artifactId = split[split.length - 2];
            String replacedParent = parentString
                    .replaceAll(version, "")
                    .replaceAll(artifactId, "");
            String groupId = replacedParent
                    .substring(0, replacedParent.length() - 2)
                    .replace("/", ".");

            WebhookEmbedBuilder embedBuilder = new WebhookEmbedBuilder();
            embedBuilder.setAuthor(new WebhookEmbed.EmbedAuthor(username, REPOSILITE_ICON, artifactUrl));
            embedBuilder.setTitle(new WebhookEmbed.EmbedTitle("[" + deployEvent.getGav().getSimpleName() + "] new deployment", artifactUrl));
            embedBuilder.setDescription("A new artifact was deployed!");

            embedBuilder.addField(new WebhookEmbed.EmbedField(
                    true,
                    "**Gradle (Groovy)**",
                    replace("""
                            ```groovy
                            implementation "$groupId:$artifactId:$version"
                            ```
                            """, groupId, artifactId, version)
            ));
            embedBuilder.addField(new WebhookEmbed.EmbedField(
                    false,
                    "**Maven**",
                    replace("""
                            ```xml
                            <dependency>
                              <groupId>$groupId</groupId>
                              <artifactId>$artifactId</artifactId>
                              <version>$version</version>
                            </dependency>```
                            """, groupId, artifactId, version)
            ));
            embedBuilder.setColor(0x49BE25);
            webhookClient.send(embedBuilder.build())
                    .thenAccept(readonlyMessage -> {
                        getLogger().info("WEBHOOK | Send deploy message of artifacts " + deployEvent.getGav() + " to discord-server!");
                    });
        });
        return null;
    }

    private String replace(String text, String groupId, String artifactId, String version) {
        return text.replace("$groupId", groupId)
                .replace("$artifactId", artifactId)
                .replace("$version", version);
    }
}