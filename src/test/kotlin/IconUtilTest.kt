package cssvarsassistant.documentation

import kotlin.test.Test
import kotlin.test.assertTrue
import javax.swing.Icon
import com.intellij.util.ui.ColorIcon

class IconUtilTest {
    @Test fun `tiny red dot dataUri`() {
        val red = ColorIcon(4, java.awt.Color.RED, /*border=*/false)
        val uri = (red as Icon).toPngDataUri()
        assertTrue(uri.startsWith("data:image/png;base64,"), "got: $uri")
        // decode the Base64 body and ensure it’s non‐empty:
        val b64 = uri.removePrefix("data:image/png;base64,")
        val bytes = java.util.Base64.getDecoder().decode(b64)
        println("bytes.length = ${bytes.size}")
        println("uri = \n $uri")
        assertTrue(bytes.isNotEmpty())
    }
}
