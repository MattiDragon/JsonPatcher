# JsonPatcher
[![Badge showing the amount of downloads on modrinth](https://img.shields.io/badge/dynamic/json?color=2d2d2d&colorA=5da545&label=&suffix=%20downloads%20&query=downloads&url=https://api.modrinth.com/v2/project/qlXnlZLG&style=flat&logo=modrinth&logoColor=2d2d2d)](https://modrinth.com/mod/jsonpatcher)
[![Badge showing the amount of downloads on curseforge](https://img.shields.io/badge/dynamic/json?query=value&url=https://img.shields.io/curseforge/dt/936929.json&label=&logo=curseforge&color=2d2d2d&style=flat&labelColor=F16436&logoColor=2d2d2d&suffix=%20downloads)](https://www.curseforge.com/minecraft/mc-mods/jsonpatcher)
[![Badge linking to issues on github](https://img.shields.io/badge/dynamic/json?query=value&url=https://img.shields.io/github/issues-raw/mattidragon/jsonpatcher.json&label=&logo=github&color=2d2d2d&style=flat&labelColor=6e5494&logoColor=2d2d2d&suffix=%20issues)](https://github.com/MattiDragon/jsonpatcher/issues)
[![Badge linking to support on discord](https://img.shields.io/discord/760524772189798431?label=&logo=discord&color=2d2d2d&style=flat&labelColor=5865f2&logoColor=2d2d2d)](https://discord.gg/26T5KK2PBv)

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
