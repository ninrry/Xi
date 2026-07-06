package luzzr.xi.domain.model

/**
 * Essay-specific error types for type-safe error handling.
 */
sealed class EssayError : Exception() {
    data object TextEmpty : EssayError() {
        override val message: String = "TEXT_EMPTY"
    }
    
    data object ImageNull : EssayError() {
        override val message: String = "IMAGE_NULL"
    }
    
    data object PdfNull : EssayError() {
        override val message: String = "PDF_NULL"
    }
    
    data object PdfUnreadable : EssayError() {
        override val message: String = "PDF_UNREADABLE"
    }
    
    data class Unknown(override val cause: Throwable?) : EssayError() {
        override val message: String = "UNKNOWN"
    }
}
