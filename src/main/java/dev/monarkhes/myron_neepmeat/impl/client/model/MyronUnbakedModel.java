package dev.monarkhes.myron_neepmeat.impl.client.model;

import de.javagl.obj.Obj;
import dev.monarkhes.myron_neepmeat.impl.client.Myron;
import net.fabricmc.fabric.api.renderer.v1.mesh.Mesh;
import net.minecraft.client.render.model.*;
import net.minecraft.client.render.model.json.ModelTransformation;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.util.SpriteIdentifier;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.function.Function;

public class MyronUnbakedModel implements UnbakedModel {
    private final Obj obj;
    private final Map<String, MyronMaterial> materials;
    private final Collection<SpriteIdentifier> textureDependencies;
    private final SpriteIdentifier sprite;
    private final ModelTransformation transform;
    private final boolean isSideLit;
    private final boolean isBlock;

    public MyronUnbakedModel(@Nullable Obj obj, @Nullable Map<String, MyronMaterial> materials, Collection<SpriteIdentifier> textureDependencies, SpriteIdentifier sprite, ModelTransformation modelTransformation, boolean isSideLit, boolean isBlock) {
        this.obj = obj;
        this.materials = materials;
        this.textureDependencies = textureDependencies;
        this.sprite = sprite;
        this.transform = modelTransformation;
        this.isSideLit = isSideLit;
        this.isBlock = isBlock;
    }

    @Override
    public Collection<Identifier> getModelDependencies() {
        return Collections.emptyList();
    }

    @Override
    public void setParents(Function<Identifier, UnbakedModel> modelLoader) {

    }

    @Override
    public @Nullable BakedModel bake(Baker baker, Function<SpriteIdentifier, Sprite> textureGetter, ModelBakeSettings rotationContainer)
    {
        Mesh mesh;

        if (obj == null)
            // Try to load the obj (previous behavior)
//            mesh = Myron.load(modelId, textureGetter, rotationContainer, isBlock);
            mesh = c -> {};
        else
            // We already loaded the obj earlier in AbstractObjLoader. Don't use the external utility to re-load the obj
            // (it works only on absolute identifiers, not ModelIdentifiers like 'myron:torus#inventory')
            mesh = Myron.build(obj, materials, textureGetter, rotationContainer, isBlock);

        return new MyronBakedModel(mesh, this.transform, textureGetter.apply(this.sprite), this.isSideLit);
    }

//    @Nullable
//    @Override
//    public BakedModel bake(Baker baker, Function<SpriteIdentifier, Sprite> textureGetter, ModelBakeSettings rotationContainer, Identifier modelId)
//    {
//    }
}
