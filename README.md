# JsonPatcher
JsonPatcher is a mod that allows you to modify json files in datapacks and resourcepacks without overriding them.
This is primarily useful for mod developers and pack makers, but can also be used by players to modify their own packs.

## Usage
<details>
<summary>Gradle dependency (for mod developers)</summary>

You can add jsonpatcher to your mods buildscript like this:
```groovy
repositories {
    maven {
        url 'https://jitpack.io'
    }
}

dependencies {
    modImplementation(include("com.github.mattidragon:jsonpatcher:${jsonpatcher_version}"))
}
```

</details>

To begin using jsonpatcher, just create a file with the `.jsonpatch` extension 
in the `data/<namespace>/jsonpatcher` directory of your datapack 
or `assets/<namespace>/jsonpatcher` directory of your resourcepack.

You'll want to begin your patch with a `@version` meta tag like this. 
This will let jsonpatcher know that your patch will work with this version. 
In the future you may be able to select a version in more ways.
```
@version "1"
```

Next you'll want to select the files you want to patch. 
This is done with the `@target` meta tag. All meta tags follow a json format.
You can either select a single file with a string, use an object for broader selections, 
or use an array for multiple selections. If any selector in the array matches, the patch will apply.

Now you can begin modifying the target file. The patch files use a c-like syntax. 
You can access the target files contents with `$`. It can be used like a variable,
or you can put the property you want to access directly after it.

For more info on the language, see [the wiki](https://github.com/MattiDragon/JsonPatch/wiki).
