public class Message {
    private final int senderId;
    private final int receiverId;
    private final MessageType type;
    private final int leaderId;

    public Message(int senderId, int receiverId, MessageType type, int leaderId) {
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.type = type;
        this.leaderId = leaderId;
    }

    public int getSenderId() { return senderId; }
    public int getReceiverId() { return receiverId; }
    public MessageType getType() { return type; }
    public int getLeaderId() { return leaderId; }

    @Override
    public String toString() {
        return String.format("[%d->%d] %s (leader=%d)", senderId, receiverId, type, leaderId);
    }
}
