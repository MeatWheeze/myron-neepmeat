package dev.monarkhes.myron_neepmeat.impl.client.model;

import dev.monarkhes.myron_neepmeat.impl.Namespaces;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.minecraft.client.MinecraftClient;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.function.Consumer;

public class MyronModelProvider implements ModelLoadingPlugin
{
    public static final ModelLoadingPlugin INSTANCE = new MyronModelProvider();

    private MyronModelProvider()
    {

    }

    @Override
    public void onInitializeModelLoader(Context context)
    {
        ResourceManager manager = MinecraftClient.getInstance().getResourceManager();
        Collection<Identifier> ids = new HashSet<>();

        Collection<Identifier> candidates = new ArrayList<>();
        candidates.addAll(manager.findResources("models/block", path -> true).keySet());
        candidates.addAll(manager.findResources("models/item", path -> true).keySet());
        candidates.addAll(manager.findResources("models/misc", path -> true).keySet());

        for (Identifier id : candidates)
        {
            if (!Namespaces.check(id.getNamespace()))
                continue;

            if (id.getPath().endsWith(".obj"))
            {
                ids.add(id);
                ids.add(Identifier.of(id.getNamespace(), id.getPath().substring(0, id.getPath().indexOf(".obj"))));
            }
            else
            {
                Identifier test = Identifier.of(id.getNamespace(), id.getPath() + ".obj");

                if (manager.getResource(test).isPresent())
                {
                    ids.add(id);
                }
            }
        }

        Consumer<Identifier> out = context::addModels;

        ids.forEach(
                id ->
                {
                    String path = id.getPath();

                    if (path.startsWith("models/"))
                    {
                        out.accept(Identifier.of(id.getNamespace(), path.substring("models/".length())));
                    }

                    out.accept(id);
                }
        );
    }
}
