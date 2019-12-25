package pl.wilddogsoftware.slack.entities;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class Idea {
    private String content;
    private List<String> refinements;

    private String channel;
    private String timestamp;

    public Idea(String content) {
        this.content = content;
        refinements = new ArrayList<>();
    }
}
