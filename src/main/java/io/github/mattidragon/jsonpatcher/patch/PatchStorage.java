package io.github.mattidragon.jsonpatcher.patch;

import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PatchStorage {
    private final Multimap<String, Patch> namespacedPatches = LinkedHashMultimap.create();
    private final Multimap<String, Patch> pathPatches = LinkedHashMultimap.create();
    private final Multimap<Identifier, Patch> directIdPatches = LinkedHashMultimap.create();
    private final List<Patch> nonTrivialPatches = new ArrayList<>();
    private final List<Patch> alwaysActivePatches = new ArrayList<>();

    private final Map<Identifier, Patch> libraries = new HashMap<>();

    /*
    Group 1: namespaced patches
    Group 2: non-trivial patches
    Group 3: direct id patches
    Group 4: path only patches
    Group 5: always active patches

    |          |11111111222222223333111144442225|
    +----------+--------------------------------+
    |pathStart |X X X X X X X X X X X X X X X X |
    |pathEnd   |XX  XX  XX  XX  XX  XX  XX  XX  |
    |path      |XXXX    XXXX    XXXX    XXXX    |
    |namespace |XXXXXXXX        XXXXXXXX        |
    |regex     |xxxxxxxxxxxxxxxx                |
     */
    public PatchStorage(List<Patch> patches) {
        for (var patch : patches) {
            patch.target().ifPresent(target -> {
                if (target.regex().isPresent() && target.namespace().isPresent()) {
                    namespacedPatches.put(target.namespace().get(), patch);
                } else if (target.regex().isPresent()) {
                    nonTrivialPatches.add(patch);
                } else if (target.namespace().isPresent() && target.path().isPresent()) {
                    var path = target.path().get();
                    var id = Identifier.tryParse(target.namespace().get() + ":" + path);
                    if (id == null) return; // Invalid id, can't match anything

                    // Eliminate impossible cases relating to path start and end
                    if (target.pathStart().isPresent() && !path.startsWith(target.pathStart().get())) return;
                    if (target.pathEnd().isPresent() && !path.endsWith(target.pathEnd().get())) return;

                    directIdPatches.put(id, patch);
                } else if (target.namespace().isPresent()) {
                    namespacedPatches.put(target.namespace().get(), patch);
                } else if (target.path().isPresent()) {
                    var path = target.path().get();

                    // Eliminate impossible cases relating to path start and end
                    if (target.pathStart().isPresent() && !path.startsWith(target.pathStart().get())) return;
                    if (target.pathEnd().isPresent() && !path.endsWith(target.pathEnd().get())) return;

                    pathPatches.put(path, patch);
                } else if (target.pathStart().isPresent() || target.pathEnd().isPresent()) {
                    nonTrivialPatches.add(patch);
                } else {
                    alwaysActivePatches.add(patch);
                }

            });
            libraries.put(patch.id(), patch);
        }
    }

    public boolean hasPatches(Identifier id) {
        if (!alwaysActivePatches.isEmpty()) return true;
        if (namespacedPatches.containsKey(id.getNamespace())) return true;
        if (pathPatches.containsKey(id.getPath())) return true;
        if (directIdPatches.containsKey(id)) return true;
        return nonTrivialPatches.stream().anyMatch(patch -> patch.target().orElseThrow().test(id));
    }

    public List<Patch> getPatches(Identifier id) {
        var patches = new ArrayList<>(alwaysActivePatches);
        namespacedPatches.entries().stream().filter(entry -> entry.getKey().equals(id.getNamespace())).map(Map.Entry::getValue).forEach(patches::add);
        pathPatches.entries().stream().filter(entry -> entry.getKey().equals(id.getPath())).map(Map.Entry::getValue).forEach(patches::add);
        directIdPatches.entries().stream().filter(entry -> entry.getKey().equals(id)).map(Map.Entry::getValue).forEach(patches::add);
        nonTrivialPatches.stream().filter(patch -> patch.target().orElseThrow().test(id)).forEach(patches::add);
        return patches;
    }

    public int size() {
        return libraries.size();
    }

    public @Nullable Patch findLibrary(Identifier id) {
        return libraries.get(id);
    }
}
