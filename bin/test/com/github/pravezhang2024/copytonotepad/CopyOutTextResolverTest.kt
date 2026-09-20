package com.github.pravezhang2024.copytonotepad

import com.github.pravezhang2024.copytonotepad.copyout.CopyOutResult
import com.github.pravezhang2024.copytonotepad.copyout.CopyOutTextResolver
import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.openapi.fileTypes.FileTypes
import com.intellij.testFramework.LightVirtualFile
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import org.junit.Assume

class CopyOutTextResolverTest : BasePlatformTestCase() {

    fun testPlainTextFileIsReadable() {
        val file = myFixture.configureByText("sample.txt", "hello copyout").virtualFile
        assertEquals(CopyOutResult.Text("hello copyout"), CopyOutTextResolver.readVirtualFile(file))
    }

    fun testBinaryFileTypeIsUnreadable() {
        val fileType = FileTypeManager.getInstance().getFileTypeByExtension("png")
        Assume.assumeTrue(fileType.isBinary)
        val file = LightVirtualFile("sample.png", fileType, "")
        assertEquals(CopyOutResult.Unreadable, CopyOutTextResolver.readVirtualFile(file))
    }

    fun testNulBytesAreUnreadable() {
        val file = LightVirtualFile("sample.txt", FileTypes.PLAIN_TEXT, "abc\u0000def")
        assertEquals(CopyOutResult.Unreadable, CopyOutTextResolver.readVirtualFile(file))
    }
}
