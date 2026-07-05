package luzzr.xi.domain.model

import com.google.gson.annotations.SerializedName

data class TranslationResult(
    @SerializedName("translation") val translation: String?,
    @SerializedName("detected_language") val detectedLanguage: String? = null,
    @SerializedName("alternatives") val alternatives: List<String>? = null
)

data class EssayCorrectionJson(
    @SerializedName("grammar_errors") val grammarErrors: List<GrammarError>? = null,
    @SerializedName("vocabulary") val vocabulary: List<VocabularySuggestion>? = null,
    @SerializedName("structure") val structure: StructureAnalysis? = null,
    @SerializedName("style") val style: StyleAnalysis? = null,
    @SerializedName("score") val score: ScoreBreakdown? = null,
    @SerializedName("corrected_essay") val correctedEssay: String? = null,
    @SerializedName("writing_tips") val writingTips: List<String>? = null,
    @SerializedName("error") val error: String? = null
)

data class GrammarError(
    @SerializedName("line") val line: Int? = null,
    @SerializedName("original") val original: String,
    @SerializedName("corrected") val corrected: String,
    @SerializedName("type") val type: String? = null,
    @SerializedName("explanation") val explanation: String
)

data class VocabularySuggestion(
    @SerializedName("original") val original: String,
    @SerializedName("suggested") val suggested: String,
    @SerializedName("reason") val reason: String,
    @SerializedName("register") val register: String? = null
)

data class StructureAnalysis(
    @SerializedName("organization") val organization: String? = null,
    @SerializedName("transitions") val transitions: String? = null,
    @SerializedName("logical_flow") val logicalFlow: String? = null
)

data class StyleAnalysis(
    @SerializedName("sentence_variety") val sentenceVariety: String? = null,
    @SerializedName("tone") val tone: String? = null,
    @SerializedName("conciseness") val conciseness: String? = null,
    @SerializedName("academic_register") val academicRegister: String? = null
)

data class ScoreBreakdown(
    @SerializedName("grammar") val grammar: Int = 0,
    @SerializedName("vocabulary") val vocabulary: Int = 0,
    @SerializedName("structure") val structure: Int = 0,
    @SerializedName("style") val style: Int = 0,
    @SerializedName("total") val total: Int = 0,
    @SerializedName("grade") val grade: String? = null
)
