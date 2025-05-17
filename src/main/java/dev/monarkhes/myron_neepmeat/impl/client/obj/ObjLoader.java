package dev.monarkhes.myron_neepmeat.impl.client.obj;

import com.google.gson.*;
import dev.monarkhes.myron_neepmeat.impl.Namespaces;
import dev.monarkhes.myron_neepmeat.impl.client.Myron;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.model.ModelProviderContext;
import net.fabricmc.fabric.api.client.model.ModelResourceProvider;
import net.fabricmc.fabric.api.client.model.ModelVariantProvider;
import net.minecraft.client.render.model.UnbakedModel;
import net.minecraft.client.render.model.json.ModelTransformation;
import net.minecraft.client.render.model.json.Transformation;
import net.minecraft.client.util.ModelIdentifier;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;
import net.minecraft.util.JsonHelper;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;

public class ObjLoader extends AbstractObjLoader implements ModelResourceProvider, ModelVariantProvider {
    private static final Gson GSON = (new GsonBuilder())
            .registerTypeAdapter(ModelTransformation.class, new ModelTransformDeserializer())
            .registerTypeAdapter(Transformation.class, new TransformDeserializer())
            .create();

    private final ResourceManager resourceManager;

    public ObjLoader(ResourceManager resourceManager) {
        this.resourceManager = resourceManager;
    }


    @Override
    public @Nullable UnbakedModel loadModelResource(Identifier identifier, ModelProviderContext modelProviderContext) {
        if (!Namespaces.check(identifier.getNamespace()))
            return null;

        return loadModel(this.resourceManager, identifier, ModelTransformation.NONE, true);
    }

    @Override
    public @Nullable UnbakedModel loadModelVariant(ModelIdentifier modelIdentifier, ModelProviderContext modelProviderContext) {
        if (!Namespaces.check(modelIdentifier.getNamespace()))
            return null;

        Identifier resource = Identifier.of(
                modelIdentifier.getNamespace(),
                "models/item/" + modelIdentifier.getPath () + ".json");

        if (!modelIdentifier.getVariant().equals("inventory") || this.resourceManager.getResource(resource).isEmpty()) {
            return null;
        }

        try (Reader reader = new InputStreamReader(this.resourceManager.getResource(resource).get().getInputStream())) {
            JsonObject rawModel = JsonHelper.deserialize(reader);

            JsonElement model = rawModel.get("model");
            if (!(model instanceof JsonPrimitive) || !((JsonPrimitive) model).isString()) {
                return null;
            }

            Identifier modelPath = Identifier.of(model.getAsString());
            ModelTransformation transformation = this.getTransformation(rawModel);

            boolean isSideLit = true;

            if (rawModel.has("gui_light")) {
                isSideLit = JsonHelper.getString(rawModel, "gui_light").equals("side");
            }

            return this.loadModel(this.resourceManager, modelPath, transformation, isSideLit);
        } catch (IOException e) {
            Myron.LOGGER.warn("Failed to load model {}:\n{}", resource, e.getMessage());
            return null;
        }
    }

    private ModelTransformation getTransformation(JsonObject rawModel) throws IOException {
        if (rawModel.has("display")) {
            JsonObject rawTransform = JsonHelper.getObject(rawModel, "display");
            return GSON.fromJson(rawTransform, ModelTransformation.class);
        } else if (rawModel.has("parent")) {
            Identifier parent = Identifier.of(JsonHelper.getString(rawModel, "parent"));
            parent = Identifier.of(parent.getNamespace(), "models/" + parent.getPath() + ".json");
            return this.getTransformation(parent);
        } else {
            return ModelTransformation.NONE;
        }
    }

    private ModelTransformation getTransformation(Identifier model) throws IOException {
        if (this.resourceManager.getResource(model).isPresent()) {
            Reader reader = new InputStreamReader(this.resourceManager.getResource(model).get().getInputStream());
            return getTransformation(JsonHelper.deserialize(reader));
        } else {
            return ModelTransformation.NONE;
        }
    }

    @Environment(EnvType.CLIENT)
    public static class ModelTransformDeserializer extends ModelTransformation.Deserializer {
        public ModelTransformDeserializer() {
            super();
        }
    }
    @Environment(EnvType.CLIENT)
    public static class TransformDeserializer extends Transformation.Deserializer {
        public TransformDeserializer() {
            super();
        }
    }
}
