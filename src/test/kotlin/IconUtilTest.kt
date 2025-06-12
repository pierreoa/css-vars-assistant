package cssvarsassistant.documentation

import com.intellij.util.ui.ColorIcon
import javax.swing.Icon
import kotlin.test.Test
import kotlin.test.assertTrue

class IconUtilTest {
    @Test fun `tiny red dot dataUri`() {
        val red = ColorIcon(4, java.awt.Color.RED, /*border=*/false)
        val uri = (red as Icon).toPngDataUri()
        assertTrue(uri.startsWith("data:image/png;base64,"), "got: $uri")
        // decode the Base64 body and ensure it’s non‐empty:
        val b64 = uri.removePrefix("data:image/png;base64,")
        val bytes = java.util.Base64.getDecoder().decode(b64)
        assertTrue(bytes.isNotEmpty())
    }
}
