package dev.monarkhes.myron_neepmeat.impl.client.obj;

import com.google.gson.*;
import dev.monarkhes.myron_neepmeat.impl.Namespaces;
import dev.monarkhes.myron_neepmeat.impl.client.Myron;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.model.UnbakedModel;
import net.minecraft.client.render.model.json.ModelTransformation;
import net.minecraft.client.render.model.json.Transformation;
import net.minecraft.client.util.ModelIdentifier;
import net.minecraft.util.Identifier;
import net.minecraft.util.JsonHelper;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Objects;

public class ObjLoader extends AbstractObjLoader implements ModelLoadingPlugin
{
    public static final ObjLoader INSTANCE = new ObjLoader();

    private static final Gson GSON = (new GsonBuilder())
            .registerTypeAdapter(ModelTransformation.class, new ModelTransformDeserializer())
            .registerTypeAdapter(Transformation.class, new TransformDeserializer())
            .create();

    private ObjLoader()
    {
    }

    public @Nullable UnbakedModel loadModelResource(Identifier identifier, ResourceGetter resourceManager)
    {
        if (!Namespaces.check(identifier.getNamespace()))
            return null;

        return loadModel(resourceManager, identifier, ModelTransformation.NONE, true);
    }

    public @Nullable UnbakedModel loadModelVariant(ModelIdentifier modelIdentifier, ResourceGetter resourceManager)
    {
        if (!Namespaces.check(modelIdentifier.id().getNamespace()))
            return null;

        Identifier resource = Identifier.of(
                modelIdentifier.id().getNamespace(),
                "models/item/" + modelIdentifier.id().getPath() + ".json");

        if (!modelIdentifier.getVariant().equals("inventory") || resourceManager.getResource(resource).isEmpty())
        {
            return null;
        }

        try (Reader reader = new InputStreamReader(resourceManager.getResource(resource).get().getInputStream()))
        {
            JsonObject rawModel = JsonHelper.deserialize(reader);

            JsonElement model = rawModel.get("model");
            if (!(model instanceof JsonPrimitive) || !((JsonPrimitive) model).isString())
            {
                return null;
            }

            Identifier modelPath = Identifier.of(model.getAsString());
            ModelTransformation transformation = this.getTransformation(rawModel, resourceManager);

            boolean isSideLit = true;

            if (rawModel.has("gui_light"))
            {
                isSideLit = JsonHelper.getString(rawModel, "gui_light").equals("side");
            }

            return this.loadModel(resourceManager, modelPath, transformation, isSideLit);
        }
        catch (IOException e)
        {
            Myron.LOGGER.warn("Failed to load model {}:\n{}", resource, e.getMessage());
            return null;
        }
    }

    private ModelTransformation getTransformation(JsonObject rawModel, ResourceGetter resourceManager) throws IOException
    {
        if (rawModel.has("display"))
        {
            JsonObject rawTransform = JsonHelper.getObject(rawModel, "display");
            return GSON.fromJson(rawTransform, ModelTransformation.class);
        }
        else if (rawModel.has("parent"))
        {
            Identifier parent = Identifier.of(JsonHelper.getString(rawModel, "parent"));
            parent = Identifier.of(parent.getNamespace(), "models/" + parent.getPath() + ".json");
            return this.getTransformation(parent, resourceManager);
        }
        else
        {
            return ModelTransformation.NONE;
        }
    }

    private ModelTransformation getTransformation(Identifier model, ResourceGetter resourceManager) throws IOException
    {
        if (resourceManager.getResource(model).isPresent())
        {
            Reader reader = new InputStreamReader(resourceManager.getResource(model).get().getInputStream());
            return getTransformation(JsonHelper.deserialize(reader), resourceManager);
        }
        else
        {
            return ModelTransformation.NONE;
        }
    }

    @Override
    public void onInitializeModelLoader(Context loaderCtx)
    {
        loaderCtx.resolveModel().register(context ->
        {
            return loadModelResource(context.id(), MinecraftClient.getInstance().getResourceManager()::getResource);
        });

        loaderCtx.modifyModelBeforeBake().register((unbakedModel, context) ->
        {
            return Objects.requireNonNullElse(
                    loadModelVariant(context.topLevelId(), MinecraftClient.getInstance().getResourceManager()::getResource),
                    unbakedModel);
        });
    }

    @Environment(EnvType.CLIENT)
    public static class ModelTransformDeserializer extends ModelTransformation.Deserializer
    {
        public ModelTransformDeserializer()
        {
            super();
        }
    }

    @Environment(EnvType.CLIENT)
    public static class TransformDeserializer extends Transformation.Deserializer
    {
        public TransformDeserializer()
        {
            super();
        }
    }
}
