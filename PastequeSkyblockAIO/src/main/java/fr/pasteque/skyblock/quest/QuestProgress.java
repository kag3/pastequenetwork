package fr.pasteque.skyblock.quest;

public class QuestProgress {

    private final String questId;
    private int current;
    private boolean completed;
    private boolean claimed;

    public QuestProgress(String questId, int current, boolean completed, boolean claimed) {
        this.questId = questId;
        this.current = current;
        this.completed = completed;
        this.claimed = claimed;
    }

    public QuestProgress(String questId) {
        this(questId, 0, false, false);
    }

    public String getQuestId() {
        return questId;
    }

    public int getCurrent() {
        return current;
    }

    public void setCurrent(int current) {
        this.current = current;
    }

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }

    public boolean isClaimed() {
        return claimed;
    }

    public void setClaimed(boolean claimed) {
        this.claimed = claimed;
    }
}
