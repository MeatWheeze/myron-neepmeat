package dev.monarkhes.myron_neepmeat.impl.client.obj;

import de.javagl.obj.Obj;
import de.javagl.obj.ObjReader;
import dev.monarkhes.myron_neepmeat.impl.client.Myron;
import dev.monarkhes.myron_neepmeat.impl.client.model.MyronMaterial;
import dev.monarkhes.myron_neepmeat.impl.client.model.MyronUnbakedModel;
import net.minecraft.client.render.model.UnbakedModel;
import net.minecraft.client.render.model.json.ModelTransformation;
import net.minecraft.client.texture.MissingSprite;
import net.minecraft.client.util.SpriteIdentifier;
import net.minecraft.resource.Resource;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;

public class AbstractObjLoader {
    public static final SpriteIdentifier DEFAULT_SPRITE = new SpriteIdentifier(PlayerScreenHandler.BLOCK_ATLAS_TEXTURE, null);

    public interface ResourceGetter
    {
        Optional<Resource> getResource(Identifier id);
    }

    protected @Nullable UnbakedModel loadModel(
            ResourceGetter resourceManager, Identifier identifier, ModelTransformation transformation, boolean isSideLit) {
        boolean isBlock = identifier.getPath().startsWith("block");

        if (!identifier.getPath().endsWith(".obj")) {
            identifier = Identifier.of(identifier.getNamespace(), identifier.getPath() + ".obj");
        }

        if (!identifier.getPath().startsWith("models/")) {
            identifier = Identifier.of(identifier.getNamespace(), "models/" + identifier.getPath());
        }

        if (resourceManager.getResource(identifier).isPresent()) {
            try {

                InputStream inputStream = resourceManager.getResource(identifier).get().getInputStream();
                Obj obj = ObjReader.read(inputStream);
                Map<String, MyronMaterial> materials = Myron.getMaterials(resourceManager, identifier, obj);

                Collection<SpriteIdentifier> textureDependencies = new HashSet<>();

                for (MyronMaterial material : materials.values()) {
                    Identifier materialTexture = material.getTexture();
                    if (materialTexture != null)
                    {
                        textureDependencies.add(new SpriteIdentifier(PlayerScreenHandler.BLOCK_ATLAS_TEXTURE, materialTexture));
                    }
                    else
                    {
                        textureDependencies.add(new SpriteIdentifier(PlayerScreenHandler.BLOCK_ATLAS_TEXTURE,
                                MissingSprite.getMissingSpriteId()));
                        Myron.LOGGER.warn("No texture specified for material '{}' in model '{}'; using missing texture.", material.name, identifier);
                    }
                }

                MyronMaterial material = materials.get("sprite");
                return new MyronUnbakedModel(
                        obj, materials,
                        textureDependencies, !materials.isEmpty()
                        ? new SpriteIdentifier(PlayerScreenHandler.BLOCK_ATLAS_TEXTURE, (material == null
                        ? materials.values().iterator().next()
                        : material).getTexture())
                        : DEFAULT_SPRITE, transformation, isSideLit, isBlock);
            } catch (IOException e) {
                Myron.LOGGER.warn("Failed to load model {}:\n{}", identifier, e.getMessage());
            }
        }

        return null;
    }
}
