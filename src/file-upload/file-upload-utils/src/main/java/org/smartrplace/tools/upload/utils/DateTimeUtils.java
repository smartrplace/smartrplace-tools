package org.smartrplace.tools.upload.utils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.Locale;

public class DateTimeUtils {
	
	/**
	 * A date-time formatter
	 */
	public final static DateTimeFormatter DEFAULT_FORMATTER_PARSE_WITH_ZONE = new DateTimeFormatterBuilder()
			.appendPattern("yyyy")
			.optionalStart()
				.appendPattern("-MM")
				.optionalStart()
					.appendPattern("-dd")
					.optionalStart()
						.appendPattern("'T'HH")
						.optionalStart()
							.appendPattern("_mm")
							.optionalStart()
								.appendPattern("_ss")
							.optionalEnd()
						.optionalEnd()
					.optionalEnd()
				.optionalEnd()
			.optionalEnd()
			.optionalStart()
				.appendZoneOrOffsetId()
			.optionalEnd()
			.toFormatter(Locale.ENGLISH);
	
	/**
	 * A date-time formatter without time zone
	 */
	public final static DateTimeFormatter DEFAULT_FORMATTER_PARSE_NO_ZONE = new DateTimeFormatterBuilder()
			.appendPattern("yyyy")
			.optionalStart()
				.appendPattern("-MM")
				.optionalStart()
					.appendPattern("-dd")
					.optionalStart()
						.appendPattern("'T'HH")
						.optionalStart()
							.appendPattern("_mm")
							.optionalStart()
								.appendPattern("_ss")
							.optionalEnd()
						.optionalEnd()
					.optionalEnd()
				.optionalEnd()
			.optionalEnd()
			.toFormatter(Locale.ENGLISH);
	
	/**
	 * A date-time formatter suitable for using the formatted string as a file name
	 */
	public static final DateTimeFormatter DEFAULT_FORMATTER_FORMAT_WITH_ZONE = new DateTimeFormatterBuilder()
			.appendPattern("yyyy-MM-dd'T'HH_mm_ss").appendZoneId()
			.toFormatter(Locale.ENGLISH);
	
	public static final DateTimeFormatter DEFAULT_FORMATTER_FORMAT_NO_ZONE = new DateTimeFormatterBuilder()
			.appendPattern("yyyy-MM-dd'T'HH_mm_ss")
			.toFormatter(Locale.ENGLISH);
	
	public static final ZoneId UTC = ZoneId.of("Z");
	
	/**
	 * Parses a given input string as an instant, using the following strategies:
	 * <ul>
	 * 		<li>if the String can be parsed as long, then it is intepreted as millis since epoch 
	 * 			(Jan 1st 1970)
	 * 		<li>if the String can be parsed as a {@link ZonedDateTime} by the {@link #DEFAULT_FORMATTER_PARSE_WITH_ZONE} 
	 * 			then it will be interpreted in this way
	 * 		<li>if the String can be parsed as a {@link LocalDateTime} by the {@link #DEFAULT_FORMATTER_PARSE_NO_ZONE},
	 * 			then it will be interpreted as a DateTime in UTC. 
	 * </ul>
	 * Otherwise null is returned
	 * @param in
	 * @return
	 * 		the parsed Instant or null, if the input String could not be parsed or is null
	 */
	public static Instant parseAsInstant(final String in) {
		if (in == null)
			return null;
		try {
			return Instant.ofEpochMilli(Long.parseLong(in));
		} catch (NumberFormatException expected) {}
		try {
			return ZonedDateTime.from(DEFAULT_FORMATTER_PARSE_WITH_ZONE.parse(in)).toInstant();
		} catch (DateTimeException expected) {}
		try {
			return ZonedDateTime.of(LocalDateTime.from(DEFAULT_FORMATTER_PARSE_NO_ZONE.parse(in)), UTC).toInstant();
		} catch (DateTimeException expected) {}
		return null;
	}

	/**
	 * Parses a given input string as an instant, using the following strategies:
	 * <ul>
	 * 		<li>if the String can be parsed as long, then it is intepreted as millis since epoch 
	 * 			(Jan 1st 1970)
	 * 		<li>if the String can be parsed as a {@link ZonedDateTime} by the formatter
	 * 			then it will be interpreted in this way
	 * 		<li>if the String can be parsed as a {@link LocalDateTime} by the formatter,
	 * 			then it will be interpreted as a DateTime in UTC. 
	 * </ul>
	 * Otherwise null is returned
	 * @param in
	 * @param formatter
	 * @return
	 * 		the parsed Instant or null, if the input String could not be parsed or is null
	 */
	public static Instant parseAsInstant(final String in, final DateTimeFormatter formatter) {
		if (in == null)
			return null;
		if (formatter == null)
			return parseAsInstant(in);
		try {
			return Instant.ofEpochMilli(Long.parseLong(in));
		} catch (NumberFormatException expected) {}
		try {
			return ZonedDateTime.from(formatter.parse(in)).toInstant();
		} catch (DateTimeException expected) {}
		try {
			return ZonedDateTime.of(LocalDateTime.from(formatter.parse(in)), UTC).toInstant();
		} catch (DateTimeException expected) {}
		return null;
	}
	
	public static Path buildFilePath(final String uploadFolder, final String folder, String filename, final String user) throws IOException {
		final int idx = filename.lastIndexOf('.');
		String ending = null;
		if (idx >= 0 && idx < filename.length()-1) {
			ending = filename.substring(idx+1);
			filename = filename.substring(0, idx);
		}
		Path dir = Paths.get(uploadFolder, user, folder);
		Path target;
		//do {
			StringBuilder newFilename = new StringBuilder();
			newFilename.append(filename).append('_');
			newFilename.append(System.currentTimeMillis());
			if (ending != null)
				newFilename.append('.').append(ending);
			target = dir.resolve(newFilename.toString());
			
		//} while (Files.exists(target));
		return target; 
	}
	
	public static Instant parseFilenameTimestamp(final Path path) {
		String fn = path.getFileName().toString();
		final int idx = fn.lastIndexOf('.');
		if (idx >= 0) {
			fn = fn.substring(0, idx);
		}
		final int idxUnder = fn.lastIndexOf('_');
		if (idxUnder < 0 || idxUnder == fn.length() - 1)
			return null;
		return DateTimeUtils.parseAsInstant(fn.substring(idxUnder + 1));
	}
	
}
