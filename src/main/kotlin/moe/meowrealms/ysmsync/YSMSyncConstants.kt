package moe.meowrealms.ysmsync

object YSMSyncConstants {
    object ModelSyncConstants {
        // defaults, overridden by config.yml at startup
        @Volatile var PER_PLAYER_RATE_LIMIT_MBPS = 8.0
        @Volatile var GLOBAL_RATE_LIMIT_MBPS = 40.0
        const val MAX_CHUNK_BYTES = 30 * 1024
    }

    object ModelDefaults {
        // defaults, overridden by config.yml at startup
        @Volatile var DEFAULT_MODEL_ID = "default"
        @Volatile var DEFAULT_MODEL_TEXTURE = "default"
    }

    object PermissionConstants {
        const val YSMSYNC_COMMAND = "ysmsync.command"
        const val RELOAD_MODELS_COMMAND = "ysmsync.command.reload"
        const val SET_MODEL_COMMAND = "ysmsync.command.setmodel"
    }

    object LanguageConstants {
        const val NO_PERMISSION = "ysmsync.command.no_permission"
        const val COMMAND_HELP = "ysmsync.command.help"
        const val COMMAND_UNKNOWN_SUBCOMMAND = "ysmsync.command.unknown_subcommand"
        const val RELOAD_MODELS_STARTED = "ysmsync.reload_models.started"
        const val RELOAD_MODELS_SUCCESS = "ysmsync.reload_models.success"
        const val RELOAD_MODELS_FAILED = "ysmsync.reload_models.failed"
        const val SET_MODEL_USAGE = "ysmsync.set_model.usage"
        const val SET_MODEL_PLAYER_NOT_FOUND = "ysmsync.set_model.player_not_found"
        const val SET_MODEL_NOT_FOUND = "ysmsync.set_model.model_not_found"
        const val SET_MODEL_TEXTURE_NOT_FOUND = "ysmsync.set_model.texture_not_found"
        const val SET_MODEL_PLAYER_NO_DATA = "ysmsync.set_model.player_no_data"
        const val SET_MODEL_SUCCESS = "ysmsync.set_model.success"
    }
}
