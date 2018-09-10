package org.smartrplace.tools.upload.utils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitOption;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.EnumSet;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.slf4j.LoggerFactory;

public class ZipUtils {

	/**
	 * Zip a folder recursively
	 * @param baseFolder
	 * @param out
	 * @return
	 * @throws IOException
	 */
	public static ZipOutputStream zipFolder(final Path baseFolder, final OutputStream out) throws IOException {
		final ZipOutputStream zio = new ZipOutputStream(out);
		zipFile(baseFolder, null, zio, Files.isDirectory(baseFolder), null);
		return zio;
	}
	
	/**
	 * Zip a folder recursively, or a single file, and add content to the provided zip stream
	 * @param path
	 * @param zio
	 * @param recursive
	 * @param filter
	 * 		optional filter; may be null
	 * @throws IOException
	 */
	public static void zipFile(final Path path, final Path baseForName, final ZipOutputStream zio, 
			final boolean recursive, final Predicate<Path> filter) throws IOException {
		if (Files.isRegularFile(path) && (filter == null || filter.test(path))) {
			String name = baseForName == null ? path.toString() : baseForName.relativize(path).toString() ;
			name = name.replace('\\', '/');
			final ZipEntry entry = new ZipEntry(name); 
			zio.putNextEntry(entry);
			final byte[] buffer = new byte[4096];
			int length;
			try (final InputStream in = Files.newInputStream(path)) {
				while ((length = in.read(buffer)) >= 0) {
					zio.write(buffer, 0 , length);
				}
			}
		}
		if (!recursive || !Files.isDirectory(path))
			return;
		Files.walkFileTree(path, new java.nio.file.SimpleFileVisitor<Path>() {
			
			@Override
			public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
				if (filter != null && Files.isDirectory(file)) {
					if (!filter.test(file))
						return FileVisitResult.SKIP_SUBTREE;
					else
						return FileVisitResult.CONTINUE;
				}
				if (!Files.isRegularFile(file) || (filter != null && !filter.test(file)))
					return FileVisitResult.CONTINUE;
				final Path relativeBase = baseForName != null ? baseForName : path;
				final Path relativePath = relativeBase.relativize(file);
				final ZipEntry entry = new ZipEntry(relativePath.toString().replace('\\', '/')); // TODO check
				zio.putNextEntry(entry);
				final byte[] buffer = new byte[4096];
				int length;
				try (final InputStream in = Files.newInputStream(file)) {
					while ((length = in.read(buffer)) >= 0) {
						zio.write(buffer, 0 , length);
					}
				}
				return FileVisitResult.CONTINUE;
			}
			
			@Override
			public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
				LoggerFactory.getLogger(ZipUtils.class).warn("File visit failed: {}"
						+ ". {}", file, exc.toString());
				return FileVisitResult.CONTINUE;
			}
			
		});
	}
	
	/**
	 * Parse a zip file and apply the provided listener to all files found within the zip
	 * @param zipFile
	 * @param fileListener
	 * @throws IOException
	 */
	public static void parseZipFile(final Path zipFile, final Consumer<Path> fileListener) throws IOException {
		try (FileSystem fs = FileSystems.newFileSystem(zipFile, null)) {
			final Path root = fs.getPath("/");
			// the more convenient Files.walk method sometimes fails with funny IOExceptions... 
			// see https://stackoverflow.com/questions/14654737/nosuchfileexception-while-walking-files-tree-inside-a-zip-using-java-nio
			Files.walkFileTree(root, EnumSet.noneOf(FileVisitOption.class), Integer.MAX_VALUE, new java.nio.file.SimpleFileVisitor<Path>() {
				
				@Override
				public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
					if (Files.isRegularFile(file)) {
						fileListener.accept(file);
					}
					return FileVisitResult.CONTINUE;
				}
				
				@Override
				public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
					LoggerFactory.getLogger(ZipUtils.class).warn("File visit failed: {}. {}", file,exc.toString());
					return FileVisitResult.CONTINUE;
				}
				
			});
		}
	}
	
}
