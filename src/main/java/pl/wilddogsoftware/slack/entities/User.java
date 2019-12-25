package pl.wilddogsoftware.slack.entities;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@AllArgsConstructor
@Getter
@Setter
public class User {
    private String name;
    private String id;
    private String channel;
}
