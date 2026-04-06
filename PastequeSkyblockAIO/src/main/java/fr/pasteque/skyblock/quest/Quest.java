package fr.pasteque.skyblock.quest;

public class Quest {

    private final String id;
    private final String name;
    private final String description;
    private final QuestType type;
    private final String target;
    private final int amount;
    private final double moneyReward;
    private final int xpReward;
    private final String nextQuestId;
    private final String npcName;
    private final boolean firstJoin;

    public Quest(String id, String name, String description, QuestType type, String target,
                 int amount, double moneyReward, int xpReward, String nextQuestId,
                 String npcName, boolean firstJoin) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.type = type;
        this.target = target;
        this.amount = amount;
        this.moneyReward = moneyReward;
        this.xpReward = xpReward;
        this.nextQuestId = nextQuestId;
        this.npcName = npcName;
        this.firstJoin = firstJoin;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public QuestType getType() {
        return type;
    }

    public String getTarget() {
        return target;
    }

    public int getAmount() {
        return amount;
    }

    public double getMoneyReward() {
        return moneyReward;
    }

    public int getXpReward() {
        return xpReward;
    }

    public String getNextQuestId() {
        return nextQuestId;
    }

    public String getNpcName() {
        return npcName;
    }

    public boolean isFirstJoin() {
        return firstJoin;
    }
}
