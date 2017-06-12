interp.load.ivy("org.scalaj" %% "scalaj-http" % "2.3.0")
import ammonite.ops._, scalaj.http._
import $ivy.`org.jsoup:jsoup:1.7.2`
import org.jsoup._ // import Jsoup
import collection.JavaConversions._

val paths = ls! pwd/up/'_posts

println
println

def findImageUrlsInPath(p: Path): List[String] = {
	val doc = Jsoup.parse(p.toIO, "UTF-8", "blog.stephenn.com")
	doc.select("img").toList.map(_.attributes.get("src"))
}

def downloadImagesForPath(p: Path) = {
	println("path "+p)
	val pathFileName = p.name.replaceAll(".html", "")
	val updates = findImageUrlsInPath(p).map { url =>
		val imageFileName = url.drop(url.zipWithIndex.filter(_._1 == '/').last._2 + 1)
		val outputFileName = pathFileName + "-" + imageFileName
		val outputFileNameWithEx = if (!imageFileName.endsWith(".jpg") && !imageFileName.endsWith(".png")) {
			outputFileName + ".png"
		} else outputFileName
		println("url "+url)
		println("output " + outputFileName)
		downloadImage(url, outputFileNameWithEx)
		url -> outputFileNameWithEx
	}
	replaceLinks(updates, p, pwd/'out/p.name)
	println(updates)
}

def downloadImage(url: String, outputFileName: String): java.io.File = {
	val outFile = new java.io.File(outputFileName)
	if (!outFile.exists) {
		Http(url).execute { is =>
			java.nio.file.Files.copy(is, outFile.toPath)
		}
	}
	outFile
}

def replaceLinks(updates: Seq[(String,String)], file: Path, outpath: Path): Unit = {
	val content = read! file
	
	val updatedFile = updates.foldLeft(content) { case (l, (old,newUrl)) =>
		l.replaceAll(old.replaceAll("\\+", "\\\\+"), "/assets/"+ newUrl)
	}
	write(outpath, updatedFile)
}

paths.foreach(downloadImagesForPath)
