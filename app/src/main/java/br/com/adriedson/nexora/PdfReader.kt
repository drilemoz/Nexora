package br.com.adriedson.nexora

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import java.io.File
import java.io.FileOutputStream
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

/** Extrai texto embutido no PDF, offline. Não interpreta scans sem OCR. */
object PdfReader {
    const val MAX_BYTES: Long = 12L * 1024 * 1024
    const val MAX_PAGES = 40
    const val MAX_CHARS = 24_000
    data class Result(val name:String,val pages:Int,val content:String,val partial:Boolean)

    suspend fun extract(context:Context,uri:Uri):Result {
        PDFBoxResourceLoader.init(context.applicationContext)
        var name="Documento.pdf"
        context.contentResolver.query(uri,arrayOf(OpenableColumns.DISPLAY_NAME,OpenableColumns.SIZE),null,null,null)?.use { c ->
            if(c.moveToFirst()) {
                val col=c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if(col>=0 && !c.isNull(col)) name=c.getString(col).take(130)
                val len=c.getColumnIndex(OpenableColumns.SIZE)
                if(len>=0 && !c.isNull(len) && c.getLong(len)>MAX_BYTES) error("PDF excede 12 MiB")
            }
        }
        require(name.endsWith(".pdf",true)) { "Escolha um arquivo PDF" }
        val temp=File.createTempFile("nexora-pdf-", ".pdf",context.cacheDir)
        try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                FileOutputStream(temp).use { out ->
                    val buffer=ByteArray(32*1024)
                    var total=0L
                    while(true) {
                        currentCoroutineContext().ensureActive()
                        val n=stream.read(buffer)
                        if(n<0)break
                        total+=n
                        check(total<=MAX_BYTES) { "PDF excede 12 MiB" }
                        out.write(buffer,0,n)
                    }
                }
            } ?: error("Não foi possível abrir o PDF")
            check(temp.length()>=5L) { "PDF vazio" }
            temp.inputStream().use { input ->
                val magic=ByteArray(5)
                check(input.read(magic)==5 && String(magic,Charsets.US_ASCII)=="%PDF-") { "Cabeçalho de PDF inválido" }
            }
            PDDocument.load(temp).use { doc ->
                check(!doc.isEncrypted) { "PDF protegido por senha não suportado" }
                check(doc.numberOfPages>0) { "PDF sem páginas" }
                val maxPages=minOf(doc.numberOfPages,MAX_PAGES)
                val out=StringBuilder()
                var partial=doc.numberOfPages>MAX_PAGES
                // Página a página impede textos enormes de crescerem em memória.
                val stripper=PDFTextStripper()
                for(page in 1..maxPages) {
                    currentCoroutineContext().ensureActive()
                    stripper.startPage=page;stripper.endPage=page
                    val extracted=stripper.getText(doc)
                    if(out.length+extracted.length>MAX_CHARS) {
                        out.append(extracted.take(MAX_CHARS-out.length));partial=true;break
                    }
                    out.append(extracted).append('\n')
                }
                val content=out.toString().trim()
                check(content.isNotBlank()) { "O PDF não contém texto extraível. PDF digitalizado requer OCR." }
                return Result(name,doc.numberOfPages,content,partial)
            }
        } finally { temp.delete() }
    }
}
