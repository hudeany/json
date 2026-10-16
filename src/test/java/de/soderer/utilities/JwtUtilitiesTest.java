package de.soderer.utilities;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import java.time.ZonedDateTime;

import org.junit.jupiter.api.Test;

import de.soderer.json.utilities.DateUtilities;
import de.soderer.json.utilities.JwtUtilities;

@SuppressWarnings("static-method")
public class JwtUtilitiesTest {
	@Test
	public void test() {
		try {
			final String jwtTokenString = "eyJhbGciOiJIUzI1NiJ9.eyJSb2xlIjoiQWRtaW4iLCJJc3N1ZXIiOiJJc3N1ZXIiLCJVc2VybmFtZSI6IkphdmFJblVzZSIsImV4cCI6MjE0ODU0Nzg3MSwiaWF0IjoxNzY5ODU2NjcxfQ.8KIzCP516AnzHeq7yXgRtyZ01z5_d2cabWbDRo24OZ4";
			final ZonedDateTime expireDateTime = JwtUtilities.getJwtTokenValidity(jwtTokenString);
			assertEquals("2038-01-31T10:51:11", DateUtilities.formatDate(DateUtilities.ISO_8601_DATETIME_FORMAT_NO_TIMEZONE, expireDateTime));
		} catch (final Exception e) {
			fail(e.getMessage());
		}
	}
}
