package luzzr.xi.domain.model

data class CorrectionResult(
    val grammarErrors: List<GrammarError>,
    val vocabulary: List<VocabularySuggestion>,
    val structure: StructureAnalysis?,
    val style: StyleAnalysis?,
    val score: ScoreBreakdown?,
    val correctedEssay: String,
    val writingTips: List<String>,
    val usage: Usage? = null
)
