package dev.mattidragon.jsonpatcher.remap;

import dev.mattidragon.jsonpatcher.lang.runtime.lib.reflection.remap.Remapper;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.MappingResolver;
import net.fabricmc.mappingio.tree.MappingTree;
import net.minecraft.util.annotation.FieldsAreNonnullByDefault;
import net.minecraft.util.annotation.MethodsReturnNonnullByDefault;

import javax.annotation.ParametersAreNonnullByDefault;

@FieldsAreNonnullByDefault
@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class ReflectionRemapper implements Remapper {
    private final MappingTree tree = MappingsLoader.MAPPING_TREE;
    private final MappingResolver resolver = FabricLoader.getInstance().getMappingResolver();

    private static final int RUNTIME_NAMESPACE = -1;
    private static final int NAMED_NAMESPACE = 0;

    @Override
    public String remapClassToNamed(String name) {
        return tree.mapClassName(
                dotToSlash(resolver.unmapClassName(
                        "intermediary",
                        slashToDot(name)
                )),
                NAMED_NAMESPACE
        );
    }

    @Override
    public String remapClassToRuntime(String name) {
        return dotToSlash(resolver.mapClassName(
                "intermediary",
                slashToDot(tree.mapClassName(
                        name, NAMED_NAMESPACE, RUNTIME_NAMESPACE
                ))
        ));
    }

    @Override
    public String remapFieldToRuntime(String owner, String name, String descriptor) {
        var clazz = tree.getClass(owner, NAMED_NAMESPACE);
        if (clazz == null) return name;

        var className = clazz.getName(RUNTIME_NAMESPACE);
        if (className == null) return name;

        var field = clazz.getField(name, descriptor, NAMED_NAMESPACE);
        if (field == null) return name;

        var fieldName = field.getName(RUNTIME_NAMESPACE);
        if (fieldName == null) return name;

        var fieldDesc = field.getDesc(RUNTIME_NAMESPACE);
        if (fieldDesc == null) return name;

        return dotToSlash(resolver.mapFieldName(
                "intermediary",
                slashToDot(className),
                fieldName,
                fieldDesc
        ));
    }

    @Override
    public String remapMethodToRuntime(String owner, String name, String descriptor) {
        var clazz = tree.getClass(owner, NAMED_NAMESPACE);
        if (clazz == null) return name;

        var className = clazz.getName(RUNTIME_NAMESPACE);
        if (className == null) return name;

        var method = clazz.getMethod(name, descriptor, NAMED_NAMESPACE);
        if (method == null) return name;

        var methodName = method.getName(RUNTIME_NAMESPACE);
        if (methodName == null) return name;

        var methodDesc = method.getDesc(RUNTIME_NAMESPACE);
        if (methodDesc == null) return name;

        return dotToSlash(resolver.mapMethodName(
                "intermediary",
                slashToDot(className),
                methodName,
                methodDesc
        ));
    }

    private String slashToDot(String name) {
        return name.replace('/', '.');
    }

    private String dotToSlash(String name) {
        return name.replace('.', '/');
    }
}
