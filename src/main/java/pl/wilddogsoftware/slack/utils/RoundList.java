package pl.wilddogsoftware.slack.utils;

import java.util.ArrayList;

public class RoundList<T> extends ArrayList<T> {
    public void turn() {
        if (size() > 1) {
            add(remove(0));
        }
    }
}
