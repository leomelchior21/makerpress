package com.makerpress.app

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import org.junit.Rule
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.annotation.SQLiteMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = MakerPressApp::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@SQLiteMode(SQLiteMode.Mode.NATIVE)
class ComposeLaunchTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    @org.junit.Before fun resetLanguage() {
        kotlinx.coroutines.runBlocking {
            (compose.activity.application as MakerPressApp).settings.update {
                it.copy(language = com.makerpress.app.domain.AppLanguage.PT_BR, lastUpdateCheck = System.currentTimeMillis())
            }
        }
        compose.waitForIdle()
    }
    @After fun closeActivityAndClearJobs() {
        val app = compose.activity.application as MakerPressApp
        compose.activityRule.scenario.close()
        // Robolectric can reuse the Application between activity scenarios. Its lazy
        // Room singleton must stay open for that Application's entire lifetime.
        kotlinx.coroutines.runBlocking(kotlinx.coroutines.Dispatchers.IO) { app.database.clearAllTables() }
    }
    @Test fun launchesAllToolsAndNavigatesTabs() {
        compose.onNodeWithText("Create. Print. Play.").assertExists()
        for (name in listOf("Text Studio", "Image Studio", "Sudoku", "Word Search", "QR Studio", "Math Challenges", "Golf", "Galaxy", "Kakooma", "Connect the Dots")) {
            compose.onNode(hasText(name) and hasClickAction()).performScrollTo().performClick()
            compose.onNodeWithText("Visualizar impressão").assertExists()
            compose.onNodeWithContentDescription("Voltar").performClick()
        }
        compose.onNodeWithText("Fila", useUnmergedTree = true).performClick()
        compose.onNodeWithText("Fila & criações").assertExists()
        compose.onNodeWithText("Impressora", useUnmergedTree = true).performClick()
        compose.onNodeWithText("Sua impressora").assertExists()
        compose.onNodeWithText("Ajustes", useUnmergedTree = true).performClick()
        compose.onNodeWithText("Aplicativo").assertExists()
    }
    @Test fun textPreviewAndSaveReachHistory() {
        val key = compose.activity.viewModelStore.keys().single { it.endsWith("AppViewModel") }
        val vm = androidx.lifecycle.ViewModelProvider(compose.activity)[key, AppViewModel::class.java]
        compose.onNodeWithText("Text Studio").performClick()
        compose.onNode(hasSetTextAction() and hasText("Seu texto")).performTextInput("Olá MakerPress! Ação e criação.")
        compose.onNodeWithText("Visualizar impressão").performScrollTo().performClick()
        compose.waitUntil(15000) { compose.onAllNodesWithContentDescription("Saída monocromática exata").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Salvar", substring = false).performScrollTo().assertIsDisplayed().assertIsEnabled()
        compose.onNodeWithText("Salvar", substring = false).performClick()
        compose.onNodeWithContentDescription("Voltar").performClick()
        compose.onNodeWithContentDescription("Voltar").performClick()
        compose.onNodeWithText("Fila", useUnmergedTree = true).performClick()
        compose.waitUntil(15000) { compose.waitForIdle(); vm.jobs.value.any { it.state == "SAVED" } }
        compose.onNode(hasScrollAction()).performScrollToNode(hasText("1 cópia(s) · Salvo"))
        compose.onNodeWithText("1 cópia(s) · Salvo").assertExists()
    }
    @Test fun languageToggleAndStudyAnswerKeyWork() {
        compose.onNodeWithText("PT-BR / EN").performClick()
        compose.waitUntil(15000) { compose.onAllNodesWithText("Create", useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Math Challenges").performScrollTo().performClick()
        compose.onNodeWithText("Geometry").performScrollTo().performClick()
        compose.onNodeWithText("Preview print").performScrollTo().performClick()
        compose.waitUntil(15000) { compose.onAllNodesWithText("Show answer key").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Show answer key").performScrollTo().performClick()
        compose.waitUntil(15000) { compose.onAllNodesWithText("Show questions").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("EN / PT-BR").performClick()
        compose.waitUntil(15000) { compose.onAllNodesWithText("PT-BR / EN").fetchSemanticsNodes().isNotEmpty() }
    }
    @Test fun gameCopiesAreUniqueAndHistoryReprintsAreFrozen() {
        val key = compose.activity.viewModelStore.keys().single { it.endsWith("AppViewModel") }
        val vm = androidx.lifecycle.ViewModelProvider(compose.activity)[key, AppViewModel::class.java]
        compose.onNodeWithText("Golf").performScrollTo().performClick()
        compose.runOnIdle { org.junit.Assert.assertEquals("Golf", vm.editor?.tool) }
        compose.onNodeWithText("Visualizar impressão").performScrollTo().performClick()
        compose.waitUntil(15000) { compose.onAllNodesWithContentDescription("Saída monocromática exata").fetchSemanticsNodes().isNotEmpty() }
        org.junit.Assert.assertTrue("${vm.preview} / ${vm.message}", vm.preview?.blocks?.any { it is com.makerpress.app.domain.PrintBlock.Game } == true)
        val first = vm.preview!!
        val before = vm.jobs.value.size
        compose.runOnIdle { vm.copies = 3; vm.save(true) }
        compose.waitUntil(15000) { compose.waitForIdle(); vm.jobs.value.size == before + 3 && vm.preview != first }
        val queued = vm.jobs.value.take(3)
        val levels = queued.map { vm.app.jobs.document(it).blocks.filterIsInstance<com.makerpress.app.domain.PrintBlock.Game>().single().level }
        org.junit.Assert.assertEquals(3, levels.map { it.cells }.distinct().size)
        org.junit.Assert.assertTrue(queued.all { it.copies == 1 })
        org.junit.Assert.assertEquals(first, vm.app.jobs.document(queued.last()))
        compose.runOnIdle { vm.requeue(queued.last()) }
        compose.waitUntil(15000) { compose.waitForIdle(); vm.jobs.value.size == before + 4 }
        org.junit.Assert.assertEquals(first, vm.app.jobs.document(vm.jobs.value.first()))
    }
}
