package io.github.mattidragon.jsonpatch.patch;

import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class PatchStorage {
    private final Multimap<Identifier, Patch> namespacedPatches = LinkedHashMultimap.create();
    private final Multimap<String, Patch> nonNamespacedPatches = LinkedHashMultimap.create();
    private final List<Patch> nonTrivialPatches = new ArrayList<>();

    public PatchStorage(List<Patch> patches) {
        for (var patch : patches) {
            var target = patch.target();
            // All patches with negative patterns or non-trivial positive patterns are checked individually
            if (!target.negative().isEmpty()
                || !target.positive()
                        .stream()
                        .map(PatchTarget.Pattern::path)
                        .flatMap(List::stream)
                        .allMatch(PatchTarget.Segment.Named.class::isInstance)) {
                nonTrivialPatches.add(patch);
                continue;
            }
            for (PatchTarget.Pattern pattern : target.positive()) {
                var path = pattern
                        .path()
                        .stream()
                        .map(PatchTarget.Segment.Named.class::cast)
                        .map(PatchTarget.Segment.Named::value)
                        .collect(Collectors.joining("/"));
                if (pattern.namespace().isEmpty()) {
                    nonNamespacedPatches.put(path, patch);
                } else {
                    namespacedPatches.put(new Identifier(pattern.namespace().get(), path), patch);
                }
            }
        }
    }

    public boolean hasPatches(Identifier id) {
        if (namespacedPatches.containsKey(id)) return true;
        if (nonNamespacedPatches.containsKey(id.getPath())) return true;
        return nonTrivialPatches.stream().anyMatch(patch -> patch.target().test(id));
    }

    public List<Patch> getPatches(Identifier id) {
        var patches = new ArrayList<Patch>();
        if (namespacedPatches.containsKey(id)) patches.addAll(namespacedPatches.get(id));
        if (nonNamespacedPatches.containsKey(id.getPath())) patches.addAll(nonNamespacedPatches.get(id.getPath()));
        nonTrivialPatches.stream().filter(patch -> patch.target().test(id)).forEach(patches::add);
        return patches;
    }

    public int size() {
        return nonTrivialPatches.size() + namespacedPatches.size() + nonNamespacedPatches.size();
    }
}
