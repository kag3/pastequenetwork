package fr.pasteque.skyblock.arena;

import fr.pasteque.skyblock.arena.model.Selection;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class SelectionService {

    private final Map<UUID, Selection> selections = new HashMap<UUID, Selection>();

    public Selection get(UUID uuid) {
        Selection selection = selections.get(uuid);
        if (selection == null) {
            selection = new Selection();
            selections.put(uuid, selection);
        }
        return selection;
    }
}
