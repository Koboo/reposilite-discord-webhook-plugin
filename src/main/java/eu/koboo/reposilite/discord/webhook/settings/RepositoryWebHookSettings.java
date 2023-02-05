package eu.koboo.reposilite.discord.webhook.settings;

import com.reposilite.configuration.shared.api.Doc;
import com.reposilite.configuration.shared.api.Min;

@Doc(title = "Maven Repository", description = "Settings for a given repository.")
public class RepositoryWebHookSettings {

    @Min(min = 1)
    private String reference = "";

    private String webHookUrl = null;
    private String botIconUrl = null;
    private boolean enablePosting = false;
    private String artifactFilter = null;

    @Doc(title = "Repository",
            description = "This is the name of the repository, which should be announced.")
    public String getReference() {
        return reference;
    }

    @Doc(title = "WebHook URL",
            description = "This is the url of the webhook, which should be used, \n" +
                          "to send messages of new deployments. If you leave this empty, the root webhook url is used.\n")
    public String getWebHookUrl() {
        return webHookUrl;
    }

    @Doc(title = "Bot Icon URL",
            description = "This is the url of the icon used for the embedded messages.\n" +
                          " If you leave this empty, the root bot icon url is used.\n")
    public String getBotIconUrl() {
        return botIconUrl;
    }

    @Doc(title = "Enable deployment posting",
            description = "If checked, the selected deployments of this repository are announced through the webhook.\n")
    public boolean isEnablePosting() {
        return enablePosting;
    }

    @Doc(title = "Artifact Filter (RegEx)",
            description = "Insert a regex filter to filter the posted artifacts. \n" +
                          "If empty, every deployment of every file is posted.\n" +
                          "You don't need to escape regex chars twice. A single escape \"\\\" is enough." +
                          "(e.g. this one filters all files ending with the version-number and \".jar\": \".*([0-9]\\.jar$)\")")
    public String getArtifactFilter() {
        return artifactFilter;
    }
}
