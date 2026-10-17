package moe.meowrealms.ysmsync.data

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.annotations.SerializedName
import it.unimi.dsi.fastutil.ints.Int2ObjectMap
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap
import it.unimi.dsi.fastutil.objects.Object2FloatMap
import moe.meowrealms.ysmsync.YSMSyncConstants
import moe.meowrealms.ysmsync.model.ModelManager
import moe.meowrealms.ysmsync.network.data.DispatchServerDrivenProperty
import moe.meowrealms.ysmsync.utils.FastutilMapAdapterFactory
import org.bukkit.entity.Player

class PlayerData (
    @SerializedName("selected_model_id")
    public var selectedModelId: String = ModelManager.getDefaultModelConfig(
        YSMSyncConstants.ModelDefaults.DEFAULT_MODEL_ID,
        YSMSyncConstants.ModelDefaults.DEFAULT_MODEL_TEXTURE
    ).left,
    @SerializedName("selected_model_texture")
    public var selectedModelTexture: String = ModelManager.getDefaultModelConfig(
        YSMSyncConstants.ModelDefaults.DEFAULT_MODEL_ID,
        YSMSyncConstants.ModelDefaults.DEFAULT_MODEL_TEXTURE
    ).right,
    @SerializedName("mandatory")
    public var mandatory: Boolean = false,
    @SerializedName("disabled")
    public var disabled: Boolean = false,
    @SerializedName("molang_datastorage")
    public var molangVariables: Int2ObjectMap<Object2FloatMap<String>> = Int2ObjectOpenHashMap(),
    @SerializedName("stared_models")
    public var staredModels: MutableSet<String> = HashSet()
){
    @Transient
    lateinit var owner: Player
    @Transient
    lateinit var animationData: DispatchServerDrivenProperty

    companion object {
        val GSON: Gson = GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .registerTypeAdapterFactory(FastutilMapAdapterFactory())
            .create()
    }

    @Volatile
    @Transient
    private var dirty: Boolean = false

    fun initSubComponents(owner: Player) {
        this.owner = owner
        this.animationData = this.createModelState(this.owner)
    }

    fun markDirty() {
        this.dirty = true
    }

    fun isDirty(): Boolean{
        return this.dirty
    }

    fun validateAndCorrectModelSelection(defaultModelIdFallback: String, defaultTextureFallback: String) {
        val validateResult = ModelManager.validateSelectedModel(this.selectedModelId, this.selectedModelTexture)
        val defaultModelConfig = ModelManager.getDefaultModelConfig(defaultModelIdFallback, defaultTextureFallback)

        // model not found
        if (!validateResult.left) {
            this.selectedModelId = defaultModelConfig.left
            this.selectedModelTexture = defaultModelConfig.right

            this.markDirty()

        // else: texture not found
        } else if (!validateResult.right) {
            this.selectedModelTexture = defaultModelConfig.right

            this.markDirty()
        }
    }

    fun validateModelSelection(): Boolean {
        val validateResult = ModelManager.validateSelectedModel(this.selectedModelId, this.selectedModelTexture)

        return validateResult.left && validateResult.right
    }


    fun createModelState(player: Player): DispatchServerDrivenProperty {
        val instanced = DispatchServerDrivenProperty(player.entityId)

        instanced.clear(player.entityId)

        return instanced
    }
}