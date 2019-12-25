package pl.wilddogsoftware.slack.utils;

import com.google.common.escape.UnicodeEscaper;
import com.hubspot.slack.client.SlackClient;
import com.hubspot.slack.client.SlackClientFactory;
import com.hubspot.slack.client.SlackClientRuntimeConfig;
import com.hubspot.slack.client.methods.params.chat.ChatPostMessageParams;
import com.hubspot.slack.client.methods.params.conversations.ConversationsRepliesParams;
import com.hubspot.slack.client.models.response.chat.ChatPostMessageResponse;
import com.hubspot.slack.client.models.users.UserProfile;
import lombok.extern.slf4j.Slf4j;
import pl.wilddogsoftware.slack.entities.Sheet;
import pl.wilddogsoftware.slack.entities.User;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.Normalizer;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

@Slf4j
public class SlackUtils {
    private static SlackClient client;

    private static SlackClient getClient() throws ExecutionException, InterruptedException {
        log.debug("Getting the client...");

        if(client == null) {
            client = SlackClientFactory.defaultFactory().build(
                    SlackClientRuntimeConfig
                            .builder()
                            .setTokenSupplier(() -> BotConfig.getString("bot.token"))
                            .build()
            );

            if(!client.testAuth().get().isOk()) {
                client = null;
                throw new ExecutionException(new RuntimeException("Could not establish connection with Slack, token may be invalid."));
            }
        }

        return client;
    }

    public static MessageResponse setMessage(String channel, String message) throws ExecutionException, InterruptedException {
        log.debug(String.format("Setting the message '%s' to channel '%s'.", message, channel));

        ChatPostMessageResponse response = getClient().postMessage(
                ChatPostMessageParams.builder().setChannelId(channel).setAsUser(true).setText(Normalizer.normalize(message, Normalizer.Form.NFC)).build()).get().unwrapOrElseThrow();

        return new MessageResponse(response.getChannel(), response.getTs(), message);
    }

    public static List<MessageResponse> getMessages(String channel, String timestamp) throws ExecutionException, InterruptedException {
        log.debug(String.format("Getting responses for the message timestamp '%s' to channel '%s'", timestamp, channel));

        return getClient().getConversationReplies(ConversationsRepliesParams.builder().setChannel(channel).setTs(timestamp).build())
                .get().unwrapOrElseThrow().getMessages().stream()
                .skip(1)
                .map(message -> new MessageResponse(channel, message.getTimestamp(), message.getText())).collect(Collectors.toList());
    }

    public static RoundList<User> initUsers() {
        log.debug("Gathering user info...");

        return Arrays.stream(BotConfig.getStrings("bot.problem.users")).map(userName -> {
            User retrievedUser = null;
            try {
                retrievedUser = getClient().listUsers().iterator().next().get().unwrapOrElseThrow().stream()
                        .filter(user -> {
                            UserProfile profile = user.getProfile().orElse(null);
                            return profile != null && userName.equals(profile.getUsername().orElse(""));
                        })
                        .map(user -> {
                            UserProfile profile = user.getProfile().orElse(null);
                            return new User(user.getId(), profile.getUsername().orElse(""), user.getId());
                        })
                        .findFirst().orElse(null);

            } catch (InterruptedException | ExecutionException e) {
                log.error(e.getMessage(), e);
            }

            return retrievedUser;

        }).collect(Collectors.toCollection(RoundList::new));
    }

    public static void export(String description, List<Sheet> sheets, File file) throws IOException {
        PrintWriter pw = new PrintWriter(file);
        pw.println(description);
        pw.println("============");

        sheets.forEach(sheet -> {
            sheet.forEach(idea -> {
                pw.println();
                pw.println(idea.getContent());
                pw.println("-------------");

                idea.getRefinements().forEach(refinement -> {
                    pw.println("* " + refinement);
                });
            });
        });

        pw.close();
    }
}
