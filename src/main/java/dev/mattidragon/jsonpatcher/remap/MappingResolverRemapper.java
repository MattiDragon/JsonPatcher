package dev.mattidragon.jsonpatcher.remap;

import dev.mattidragon.jsonpatcher.lang.runtime.lib.reflection.remap.Remapper;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.MappingResolver;

public class MappingResolverRemapper implements Remapper {
    private final MappingResolver resolver = FabricLoader.getInstance().getMappingResolver();

    @Override
    public String remapClassToNamed(String name) {
        return dotToSlash(resolver.unmapClassName(
                "official",
                slashToDot(name)
        ));
    }

    @Override
    public String remapClassToRuntime(String name) {
        return dotToSlash(resolver.mapClassName(
                "official",
                slashToDot(name)
        ));
    }

    @Override
    public String remapFieldToRuntime(String owner, String name, String descriptor) {
        return dotToSlash(resolver.mapFieldName(
                "official",
                slashToDot(owner),
                name,
                descriptor
        ));
    }

    @Override
    public String remapMethodToRuntime(String owner, String name, String descriptor) {
        return dotToSlash(resolver.mapMethodName(
                "official",
                slashToDot(owner),
                name,
                descriptor
        ));
    }

    private String slashToDot(String name) {
        return name.replace('/', '.');
    }

    private String dotToSlash(String name) {
        return name.replace('.', '/');
    }
}
