package khkh.bblearn.b2.testbed.domain;

public class Message {
    private final long id;
    private final String message;

    public Message(long id, String message) {
        this.id = id;
        this.message = message;
    }

    public long getId() {
        return id;
    }

    public String getMessage() {
        return message;
    }

}
