package com.github.pravezhang2024.copytonotepad.copyout

sealed class CopyOutResult {
    data class Text(val content: String) : CopyOutResult()
    data object Unreadable : CopyOutResult()
    data object NotImplemented : CopyOutResult()
    data object Empty : CopyOutResult()
}
