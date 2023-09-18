package io.github.mattidragon.jsonpatch.lang.runtime.function;

import com.google.common.collect.ImmutableMap;

import java.util.Map;

import static io.github.mattidragon.jsonpatch.lang.runtime.function.PatchFunction.BuiltInPatchFunction.numberUnary;

// Suppress spell checker because math functions have weird names
@SuppressWarnings("SpellCheckingInspection")
public class BuiltInFunctions {
    public static final PatchFunction.BuiltInPatchFunction ASIN = numberUnary(Math::asin);
    public static final PatchFunction.BuiltInPatchFunction SIN = numberUnary(Math::sin);
    public static final PatchFunction.BuiltInPatchFunction SINH = numberUnary(Math::sinh);
    public static final PatchFunction.BuiltInPatchFunction ACOS = numberUnary(Math::acos);
    public static final PatchFunction.BuiltInPatchFunction COS = numberUnary(Math::cos);
    public static final PatchFunction.BuiltInPatchFunction COSH = numberUnary(Math::cosh);
    public static final PatchFunction.BuiltInPatchFunction ATAN = numberUnary(Math::atan);
    public static final PatchFunction.BuiltInPatchFunction TAN = numberUnary(Math::tan);
    public static final PatchFunction.BuiltInPatchFunction TANH = numberUnary(Math::tanh);
    public static final PatchFunction.BuiltInPatchFunction EXP = numberUnary(Math::exp);
    public static final PatchFunction.BuiltInPatchFunction LOG = numberUnary(Math::log);
    public static final PatchFunction.BuiltInPatchFunction LOG10 = numberUnary(Math::log10);
    public static final PatchFunction.BuiltInPatchFunction SQRT = numberUnary(Math::sqrt);
    public static final PatchFunction.BuiltInPatchFunction CBRT = numberUnary(Math::cbrt);
    public static final PatchFunction.BuiltInPatchFunction CEIL = numberUnary(Math::ceil);
    public static final PatchFunction.BuiltInPatchFunction FLOOR = numberUnary(Math::floor);
    public static final PatchFunction.BuiltInPatchFunction ABS = numberUnary(Math::abs);
    public static final PatchFunction.BuiltInPatchFunction SIGNUM = numberUnary(Math::signum);

    public static final Map<String, PatchFunction.BuiltInPatchFunction> ALL_FUNCTIONS = ImmutableMap.<String, PatchFunction.BuiltInPatchFunction>builder()
            .put("asin", ASIN)
            .put("sin", SIN)
            .put("sinh", SINH)
            .put("acos", ACOS)
            .put("cos", COS)
            .put("cosh", COSH)
            .put("atan", ATAN)
            .put("tan", TAN)
            .put("tanh", TANH)
            .put("exp", EXP)
            .put("log", LOG)
            .put("log10", LOG10)
            .put("sqrt", SQRT)
            .put("cbrt", CBRT)
            .put("ceil", CEIL)
            .put("floor", FLOOR)
            .put("abs", ABS)
            .put("sign", SIGNUM)
            .build();
}
