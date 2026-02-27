import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonElement

@Serializable(with = DifficultySerializer::class)
enum class Difficulty(val code: Int) {
    BASE(0),
    MID(1),
    HARD(2),
    GROB(3);

    companion object {
        private val byCode = entries.associateBy(Difficulty::code)

        fun fromCode(code: Int): Difficulty {
            return byCode[code] ?: throw SerializationException("Unknown difficulty code: $code")
        }
    }
}

object DifficultySerializer : KSerializer<Difficulty> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("Difficulty", PrimitiveKind.INT)

    override fun serialize(encoder: Encoder, value: Difficulty) {
        encoder.encodeInt(value.code)
    }

    override fun deserialize(decoder: Decoder): Difficulty {
        return Difficulty.fromCode(decoder.decodeInt())
    }
}

@Serializable
data class TaskFile(
    val url: String = "",
    val name: String = "",
    @SerialName("user_id")
    val userId: String = ""
)

@Serializable
data class TaskItem(
    val id: String = "",
    val number: Int = 0,
    val taskId: Int = 0,
    val comment: String = "",
    val text: String = "",
    val key: String = "",
    val hide: Boolean = false,
    val videotype: String = "",
    val video: String = "",
    val timecode: String = "",
    val solve_text: String = "",
    @SerialName("user_id")
    val userId: String = "",
    val files: List<TaskFile> = emptyList(),
    val subTask: List<JsonElement> = emptyList(),
    val table: JsonElement? = null,
    val difficulty: Difficulty = Difficulty.BASE,
    val createdAt: String = "",
    val updatedAt: String = ""
)
