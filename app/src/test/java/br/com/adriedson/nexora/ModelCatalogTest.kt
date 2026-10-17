package br.com.adriedson.nexora

import org.junit.Assert.*
import org.junit.Test

class ModelCatalogTest {
    @Test fun catalogHasStableUniqueEntries() {
        assertEquals(4, ModelCatalog.models.size)
        assertEquals(4, ModelCatalog.models.map { it.id }.distinct().size)
        assertEquals(4, ModelCatalog.models.map { it.filename }.distinct().size)
        assertTrue(ModelCatalog.models.all { ModelCatalogChecks.isValidEntry(it) })
    }
    @Test fun urlsUsePinnedCommitAndGguf() {
        for (model in ModelCatalog.models) {
            assertTrue(model.downloadUrl.endsWith("/${model.filename}"))
            assertTrue(model.downloadUrl.contains(Regex("/resolve/[a-f0-9]{40}/")))
            assertTrue(model.pageUrl.contains(Regex("/blob/[a-f0-9]{40}/")))
        }
    }
    @Test fun unknownModelRejected() {
        assertNull(ModelCatalog.byId("unknown"))
    }
}
