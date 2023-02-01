package eu.koboo.reposilite.discord.webhook;

import com.reposilite.configuration.shared.api.Doc;
import com.reposilite.configuration.shared.api.SharedSettings;
import io.javalin.openapi.JsonSchema;

@JsonSchema(requireNonNulls = false)
@Doc(title = "Discord Webhook", description = "Discord webhook settings")
public class DiscordWebhookSettings implements SharedSettings {

    String webHookUrl = "PLACE_YOUR_WEBHOOK_HERE";

    @Doc(title = "Webhook-URL",
            description = """
                    This is the url of the webhook, which should be used,<br />\s
                    to send messages of new deployments<br />\s
                            """)
    public String getWebHookUrl() {
        return webHookUrl;
    }
}
