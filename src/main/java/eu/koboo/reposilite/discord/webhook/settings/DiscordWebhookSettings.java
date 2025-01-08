package eu.koboo.reposilite.discord.webhook.settings;

import com.reposilite.configuration.shared.api.Doc;
import com.reposilite.configuration.shared.api.SharedSettings;
import io.javalin.openapi.JsonSchema;

import java.util.Collections;
import java.util.List;

@JsonSchema()
@Doc(title = "Discord Webhook", description = "Discord webhook settings")
public class DiscordWebhookSettings implements SharedSettings {

    public static final String DEFAULT_WEBHOOK = "PLACE_YOUR_WEBHOOK_HERE";

    private String rootWebHookUrl = DEFAULT_WEBHOOK;
    private String rootBotIconUrl = "https://reposilite.com/images/favicon.png";
    private String repositoryDomain = "";
    private List<RepositoryWebHookSettings> announcedRepositoriesList = Collections.emptyList();

    @Doc(title = "Root WebHook URL",
            description = "This is the url of the default webhook, which should be used, \n" +
                          "to send messages of new deployments. If the repository itself doesn't contain\n" +
                          "a webhook-url this one is used.\n")
    public String getRootWebHookUrl() {
        return rootWebHookUrl;
    }

    @Doc(title = "Root Bot Icon URL",
            description = "This is the url of the default icon used for the embedded messages.\n" +
                          "If the repository itself doesn't contain\n" +
                          "a bot-icon-url this one is used.")
    public String getRootBotIconUrl() {
        return rootBotIconUrl;
    }

    @Doc(title = "Repository-Domain",
            description = "This is the domain, which is used to create a link for the posted artifacts " +
                    "on your reposilite instance. This has to be a domain and not the full repository." +
                    "(Example: https://repo.panda-lang.org/)")
    public String getRepositoryDomain() {
        return repositoryDomain;
    }

    @Doc(title = "Repository settings",
            description = "This is the list of repositories that should be posted. \n" +
                          "Only if a repository is in this list will a deployment be sent. \n"
    )
    public List<RepositoryWebHookSettings> getAnnouncedRepositoriesList() {
        return announcedRepositoriesList;
    }
}
