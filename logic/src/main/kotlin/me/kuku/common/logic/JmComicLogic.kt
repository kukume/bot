package me.kuku.common.logic

import me.kuku.common.utils.command
import org.openpdf.text.Document
import org.openpdf.text.Image
import org.openpdf.text.PageSize
import org.openpdf.text.pdf.PdfWriter
import java.io.FileOutputStream
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.absolutePathString
import kotlin.io.path.exists

object JmComicLogic {

    fun id(id: String): List<Path> {
        val home = System.getProperty("user.home")
        command("""~/.local/bin/jmcomic $id --option="$home/jmcomic/config.yml"""")
        val name = id.filter { it.isDigit() }
        val path = Path("$home/jmcomic/$name")
        if (!path.exists()) error("can't find jm $id")
        return Files.walk(path)
            .filter { Files.isRegularFile(it) }
            .sorted()
            .toList()
    }

    fun pdf(id: String): String {
        val home = System.getProperty("user.home")
        val name = id.filter { it.isDigit() }
        val outPath = "${home}/jmcomic/$name.pdf"
        val paths = id(id)
        Document(PageSize.A4).use { document ->
            PdfWriter.getInstance(document, FileOutputStream(outPath))
            document.open()
            for (path in paths) {
                val image = Image.getInstance(path.absolutePathString())
                image.scaleToFit(PageSize.A4.width, PageSize.A4.height)
                image.setAbsolutePosition(
                    (PageSize.A4.width - image.scaledWidth) / 2,
                    (PageSize.A4.height - image.scaledHeight) / 2
                )

                document.newPage()
                document.add(image)
            }
        }
        return outPath
    }

}