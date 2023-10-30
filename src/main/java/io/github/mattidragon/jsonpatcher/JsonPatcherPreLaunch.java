package io.github.mattidragon.jsonpatcher;

import io.github.mattidragon.jsonpatcher.lang.runtime.stdlib.Libraries;
import net.fabricmc.loader.api.entrypoint.PreLaunchEntrypoint;

public class JsonPatcherPreLaunch implements PreLaunchEntrypoint {
    @Override
    public void onPreLaunch() {
        /*
         Force Libraries to load early to avoid issues with it being loaded during the first patch.
         This allows us to have a way shorter timeout as we don't have to wait for Libraries to load.
         As for why it takes so long, I don't really know.
         It might be related to loading many classes or reflecting lots of code for building methods.
        */
        // noinspection ResultOfMethodCallIgnored
        Libraries.BUILTIN.size();
    }
}
