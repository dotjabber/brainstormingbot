package pl.wilddogsoftware.slack.utils;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@AllArgsConstructor
@Getter
@Setter
public class MessageResponse {
    private String channel;
    private String timestamp;
    private String text;
}
