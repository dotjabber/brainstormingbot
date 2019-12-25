package pl.wilddogsoftware.slack;

import lombok.extern.slf4j.Slf4j;
import pl.wilddogsoftware.slack.entities.Idea;
import pl.wilddogsoftware.slack.entities.Sheet;
import pl.wilddogsoftware.slack.entities.User;
import pl.wilddogsoftware.slack.utils.MessageResponse;
import pl.wilddogsoftware.slack.utils.RoundList;

import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static pl.wilddogsoftware.slack.utils.BotConfig.*;
import static pl.wilddogsoftware.slack.utils.SlackUtils.*;

@Slf4j
public class Bot {
    public static void main(String[] args) throws InterruptedException, IOException {
        long timeout = getInt("bot.problem.timeout") * 60 * 1000;
        RoundList<User> users = initUsers();

        // initial round
        final List<MessageResponse> responses = users.parallelStream().map(user -> {
            try {
                return setMessage(user.getChannel(), MessageFormat.format(getString("bot.first"),
                        getString("bot.problem.description"),
                        getString("bot.problem.timeout")));
            } catch (ExecutionException | InterruptedException e) {
                log.error(e.getMessage(), e);
            }
            return null;
        }).collect(Collectors.toList());

        // wait for responses
        Thread.sleep(timeout);

        // get initial responses, create idea sheets
        final List<Sheet> sheets = responses.parallelStream().map(response -> {
            Sheet oneSheet = null;
            try {
                oneSheet = getMessages(response.getChannel(), response.getTimestamp()).stream().map(message ->
                        new Idea(message.getText())).collect(Collectors.toCollection(Sheet::new));

            } catch (ExecutionException | InterruptedException e) {
                log.error(e.getMessage(), e);
            }

            return oneSheet;

        }).collect(Collectors.toList());

        // refinement rounds (user count - 1)
        IntStream.rangeClosed(1, getInt("bot.problem.refinements")).forEach(round -> {
            log.info("Executing refinement round: " + round);

            try {
                // round robin users
                users.turn();

                log.info("Sending ideas...");
                users.forEach(user -> {

                    // prepare message for user
                    // every user is given all the ideas and refinements
                    try {
                        setMessage(user.getChannel(), MessageFormat.format(getString("bot.next"),
                                round,
                                getString("bot.problem.description"),
                                getString("bot.problem.timeout")));
                    } catch (ExecutionException | InterruptedException e) {
                        log.error(e.getMessage(), e);
                    }

                    // post every idea in a sheet
                    int j = users.indexOf(user);
                    sheets.get(j).forEach(idea -> {

                        // prepare idea message
                        StringBuilder sb = new StringBuilder(MessageFormat.format(getString("bot.idea"), idea.getContent()));

                        // are there any refinements?
                        if (idea.getRefinements().size() > 0) sb.append("\n\r");

                        // add them all
                        idea.getRefinements().forEach(refinement -> sb.append(
                                MessageFormat.format(getString("bot.refinement"), refinement)
                        ).append("\n\r"));

                        // send to user
                        try {
                            MessageResponse response = setMessage(users.get(j).getChannel(), sb.toString());
                            idea.setChannel(response.getChannel());
                            idea.setTimestamp(response.getTimestamp());

                        } catch (ExecutionException | InterruptedException e) {
                            log.error(e.getMessage(), e);
                        }
                    });
                });

                // wait for responses
                Thread.sleep(timeout);

                // gather the responses
                log.info("Getting refinements...");
                sheets.forEach(sheet -> sheet.forEach(idea -> {
                    try {
                        getMessages(idea.getChannel(), idea.getTimestamp())
                                .forEach(response -> idea.getRefinements().add(response.getText()));
                    } catch (ExecutionException | InterruptedException e) {
                        log.error(e.getMessage(), e);
                    }
                }));

            } catch (InterruptedException e) {
                log.error(e.getMessage(), e);
            }
        });

        // send goodbye/thank you message :)
        users.parallelStream().forEach(user -> {
            try {
                setMessage(user.getChannel(), getString("bot.last"));
            } catch (ExecutionException | InterruptedException e) {
                log.error(e.getMessage(), e);
            }
        });

        // save the results
        export(getString("bot.problem.description"), sheets, new File(getString("bot.problem.file")));
    }
}
