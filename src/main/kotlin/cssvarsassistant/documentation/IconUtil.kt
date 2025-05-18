package cssvarsassistant.documentation

import com.intellij.openapi.util.IconLoader
import com.intellij.util.ui.ImageUtil.createImage
import com.intellij.util.ui.UIUtil
import javax.imageio.ImageIO
import javax.swing.Icon
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.util.Base64

/**
 * Renders this Icon to a PNG and returns a `data:` URI string.
 */
fun Icon.toPngDataUri(): String {
    // 1) create a buffered image
    val image = createImage(iconWidth, iconHeight, BufferedImage.TYPE_INT_ARGB)
    val g = image.createGraphics()
    try {
        // 2) paint the icon
        this.paintIcon(null, g, 0, 0)
    } finally {
        g.dispose()
    }

    // 3) write to a BAOS
    val baos = ByteArrayOutputStream()
    ImageIO.write(image, "PNG", baos)

    // 4) Base64 encode
    val b64 = Base64.getEncoder().encodeToString(baos.toByteArray())
    return "data:image/png;base64,$b64"
}
