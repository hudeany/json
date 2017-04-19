package de.soderer.utilities;

import java.sql.Timestamp;
import java.text.DateFormatSymbols;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

public class DateUtilities {
	public static final SimpleDateFormat DD_MM_YYYY_HH_MM_SS = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
	public static final SimpleDateFormat DD_MM_YYYY_HH_MM_SS_Z = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss z");
	public static final SimpleDateFormat DD_MM_YYYY_HH_MM = new SimpleDateFormat("dd.MM.yyyy HH:mm");
	public static final SimpleDateFormat DD_MM_YYYY = new SimpleDateFormat("dd.MM.yyyy");
	public static final SimpleDateFormat DDMMYYYY = new SimpleDateFormat("ddMMyyyy");
	public static final SimpleDateFormat YYYYMMDD = new SimpleDateFormat("yyyyMMdd");
	public static final SimpleDateFormat HHMMSS = new SimpleDateFormat("HHmmss");
	public static final SimpleDateFormat YYYY_MM_DD = new SimpleDateFormat("yyyy-MM-dd");
	public static final SimpleDateFormat YYYY_MM_DD_HH_MM = new SimpleDateFormat("yyyy-MM-dd HH:mm");
	public static final SimpleDateFormat EE_DD_MM_YYYY = new SimpleDateFormat("EE dd.MM.yyyy", Locale.GERMAN); // EE => Weekday
	public static final SimpleDateFormat DD_MM_YYYY_HH_MM_SS_ForFileName = new SimpleDateFormat("dd_MM_yyyy_HH_mm_ss");
	public static final SimpleDateFormat YYYYMMDDHHMMSS = new SimpleDateFormat("yyyyMMddHHmmss");
	public static final SimpleDateFormat YYYYMMDDHHMMSSSSS = new SimpleDateFormat("yyyyMMddHHmmssSSS");
	public static final SimpleDateFormat YYYY_MM_DD_HHMMSS = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	public static final SimpleDateFormat YYYYMMDD_HHMMSS = new SimpleDateFormat("yyyyMMdd-HHmmss");
	public static final SimpleDateFormat HHMM = new SimpleDateFormat("HHmm");
	public static final SimpleDateFormat DD_MMM_YYYY_ENG = new SimpleDateFormat("dd-MMM-yy", Locale.ENGLISH);
	public static final SimpleDateFormat DD_MMM_YYYY_GER = new SimpleDateFormat("dd-MMM-yy", Locale.GERMAN);
	public static final SimpleDateFormat MMM_D_HHMM = new SimpleDateFormat("MMM d HH:mm", Locale.ENGLISH);
	public static final SimpleDateFormat MMM_D_YYYY = new SimpleDateFormat("MMM d yyyy", Locale.ENGLISH);

	/** Date format for SOAP Webservices (ISO 8601) */
	public static final SimpleDateFormat ISO_8601_DATE_FORMAT_NO_TIMEZONE = new SimpleDateFormat("yyyy-MM-dd");
	/** Date format for SOAP Webservices (ISO 8601) */
	public static final SimpleDateFormat ISO_8601_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-ddX");
	/** DateTime format for SOAP Webservices (ISO 8601) */
	public static final SimpleDateFormat ISO_8601_DATETIME_FORMAT_NO_TIMEZONE = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
	/** DateTime format for SOAP Webservices (ISO 8601) */
	public static final SimpleDateFormat ISO_8601_DATETIME_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssX");
	
	/** ANSI SQL standard date format */
	public static final SimpleDateFormat ANSI_SQL_DATETIME_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

	public static String getWochenTagNamensKuerzel(GregorianCalendar datum) {
		int tagesInt = datum.get(Calendar.DAY_OF_WEEK);
		String dayString = DateFormatSymbols.getInstance().getWeekdays()[tagesInt];
		return dayString.substring(0, 2);
	}

	public static int getWeekdayIndex(String weekday) {
		if (Utilities.isBlank(weekday)) {
			return -1;
		} else {
			weekday = weekday.toLowerCase().trim();
			String[] localeWeekdays = DateFormatSymbols.getInstance().getWeekdays();
			for (int i = 0; i < localeWeekdays.length; i++) {
				if (localeWeekdays[i].toLowerCase().startsWith(weekday)) {
					return i;
				}
			}

			if (weekday.startsWith("so") || weekday.startsWith("su")) {
				return Calendar.SUNDAY;
			} else if (weekday.startsWith("mo")) {
				return Calendar.MONDAY;
			} else if (weekday.startsWith("di") || weekday.startsWith("tu")) {
				return Calendar.TUESDAY;
			} else if (weekday.startsWith("mi") || weekday.startsWith("we")) {
				return Calendar.WEDNESDAY;
			} else if (weekday.startsWith("do") || weekday.startsWith("th")) {
				return Calendar.THURSDAY;
			} else if (weekday.startsWith("fr")) {
				return Calendar.FRIDAY;
			} else if (weekday.startsWith("sa")) {
				return Calendar.SATURDAY;
			} else {
				return -1;
			}
		}
	}

	/**
	 * Format a timestamp (Nanos in .Net format)
	 *
	 * @param ts
	 *            a Timestamp
	 * @return a String in format 'yyyy-mm-dd HH:MM:SS.NNNNNN'
	 */
	public static String formatTimestamp_yyyyMMdd_HHmmssNNNNNN(Timestamp ts) {
		String returnString = "";

		if (ts != null) {
			String s = YYYY_MM_DD_HHMMSS.format(ts);
			String nanosString = Integer.toString(ts.getNanos());

			if (nanosString.length() > 6) {
				nanosString = nanosString.substring(0, 6);
			} else {
				while (nanosString.length() < 6) {
					nanosString += "0";
				}
			}

			returnString = s + nanosString;
		}

		return returnString;
	}

	/**
	 * Format a timestampString from format "dd.MM.yyyy" or "dd-MM-yyyy" to "yyyy-MM-dd"
	 *
	 * @param ddMMyyyyString
	 * @return
	 */
	public static String convert_ddMMyyyy_to_yyyyMMdd(String ddMMyyyyString) {
		return ddMMyyyyString.substring(6, 10) + "-" + ddMMyyyyString.substring(3, 5) + "-" + ddMMyyyyString.substring(0, 2);
	}

	/**
	 * Format a timestampString from format "yyyy-MM-dd" or "yyyy.MM.dd" to "dd.MM.yyyy"
	 *
	 * @param ddMMyyyyString
	 * @return
	 */
	public static String convert_yyyyMMdd_to_ddMMyyyy(String yyyyMMddString) {
		return yyyyMMddString.substring(8, 10) + "." + yyyyMMddString.substring(5, 7) + "." + yyyyMMddString.substring(0, 4);
	}

	public static Date getDay(int daysToAdd) {
		GregorianCalendar returnValue = new GregorianCalendar();
		returnValue.set(Calendar.HOUR_OF_DAY, 0);
		returnValue.set(Calendar.MINUTE, 0);
		returnValue.set(Calendar.SECOND, 0);
		returnValue.set(Calendar.MILLISECOND, 0);
		if (daysToAdd != 0) {
			returnValue.add(Calendar.DAY_OF_MONTH, daysToAdd);
		}
		return returnValue.getTime();
	}

	public static String replaceDatePatternInString(String stringWithPatter, Date date) {
		String returnString = stringWithPatter;
		returnString = returnString.replace("yyyy", new SimpleDateFormat("yyyy").format(date));
		returnString = returnString.replace("YYYY", new SimpleDateFormat("yyyy").format(date));
		returnString = returnString.replace("MM", new SimpleDateFormat("MM").format(date));
		returnString = returnString.replace("dd", new SimpleDateFormat("dd").format(date));
		returnString = returnString.replace("DD", new SimpleDateFormat("dd").format(date));
		returnString = returnString.replace("HH", new SimpleDateFormat("HH").format(date));
		returnString = returnString.replace("hh", new SimpleDateFormat("HH").format(date));
		returnString = returnString.replace("mm", new SimpleDateFormat("mm").format(date));
		returnString = returnString.replace("SS", new SimpleDateFormat("ss").format(date));
		returnString = returnString.replace("ss", new SimpleDateFormat("ss").format(date));
		return returnString;
	}

	public static Date calculateETA(Date start, long itemsToDo, long itemsDone) {
		if (start == null || itemsToDo <= 0 || itemsDone <= 0) {
			return null;
		} else {
			Date now = new Date();
			long millisStartToNow = now.getTime() - start.getTime();
			long millisStartToEnd = itemsToDo * millisStartToNow / itemsDone;
			return new Date(start.getTime() + millisStartToEnd);
		}
	}

	public static String getShortHumanReadableTimespan(long valueInMillis, boolean showMillis) {
		String returnValue = "";
		long rest = valueInMillis;

		long millis = rest % 1000;
		rest = rest / 1000;

		long seconds = rest % 60;
		rest = rest / 60;

		long minutes = rest % 60;
		rest = rest / 60;

		long hours = rest % 24;
		rest = rest / 24;

		long days = rest % 7;
		rest = rest / 7;

		long weeks = rest % 52;
		rest = rest / 52;

		long years = rest;

		if (millis != 0 && showMillis) {
			returnValue = millis + "ms";
		}

		if (seconds != 0) {
			if (returnValue.length() > 0) {
				returnValue = " " + returnValue;
			}
			returnValue = seconds + "s" + returnValue;
		}

		if (minutes != 0) {
			if (returnValue.length() > 0) {
				returnValue = " " + returnValue;
			}
			returnValue = minutes + "m" + returnValue;
		}

		if (hours != 0) {
			if (returnValue.length() > 0) {
				returnValue = " " + returnValue;
			}
			returnValue = hours + "h" + returnValue;
		}

		if (days != 0) {
			if (returnValue.length() > 0) {
				returnValue = " " + returnValue;
			}
			returnValue = days + "d" + returnValue;
		}

		if (weeks != 0) {
			if (returnValue.length() > 0) {
				returnValue = " " + returnValue;
			}
			returnValue = weeks + "w" + returnValue;
		}

		if (years != 0) {
			if (returnValue.length() > 0) {
				returnValue = " " + returnValue;
			}
			returnValue = years + "y" + returnValue;
		}

		if (returnValue.length() > 0) {
			return returnValue;
		} else if (!showMillis) {
			return "0s";
		} else {
			return "0ms";
		}
	}

	public static String getHumanReadableTimespan(long valueInMillis, boolean showMillis) {
		String returnValue = "";
		long rest = valueInMillis;

		long millis = rest % 1000;
		rest = rest / 1000;

		long seconds = rest % 60;
		rest = rest / 60;

		long minutes = rest % 60;
		rest = rest / 60;

		long hours = rest % 24;
		rest = rest / 24;

		long days = rest % 7;
		rest = rest / 7;

		long weeks = rest % 52;
		rest = rest / 52;

		long years = rest;

		if (millis != 0 && showMillis) {
			returnValue = millis + " millis";
		}

		if (seconds != 0) {
			if (returnValue.length() > 0) {
				returnValue = " " + returnValue;
			}
			returnValue = seconds + " seconds" + returnValue;
		}

		if (minutes != 0) {
			if (returnValue.length() > 0) {
				returnValue = " " + returnValue;
			}
			returnValue = minutes + " minutes" + returnValue;
		}

		if (hours != 0) {
			if (returnValue.length() > 0) {
				returnValue = " " + returnValue;
			}
			returnValue = hours + " hours" + returnValue;
		}

		if (days != 0) {
			if (returnValue.length() > 0) {
				returnValue = " " + returnValue;
			}
			returnValue = days + " days" + returnValue;
		}

		if (weeks != 0) {
			if (returnValue.length() > 0) {
				returnValue = " " + returnValue;
			}
			returnValue = weeks + " weeks" + returnValue;
		}

		if (years != 0) {
			if (returnValue.length() > 0) {
				returnValue = " " + returnValue;
			}
			returnValue = years + " years" + returnValue;
		}

		if (returnValue.length() > 0) {
			return returnValue;
		} else if (!showMillis) {
			return "0s";
		} else {
			return "0ms";
		}
	}

	/**
	 * Get the duration between two timestamps as a string
	 *
	 * @param startTime
	 * @param endTime
	 * @return
	 */
	public static String getDuration(Calendar startTime, Calendar endTime) {
		int durationInMilliSeconds = (int) (endTime.getTimeInMillis() - startTime.getTimeInMillis());
		int milliSecondsPart = durationInMilliSeconds % 1000;
		int secondsPart = durationInMilliSeconds / 1000 % 60;
		int minutesPart = durationInMilliSeconds / 1000 / 60 % 60;
		int hoursPart = durationInMilliSeconds / 1000 / 60 / 60 % 24;
		int days = durationInMilliSeconds / 1000 / 60 / 60 % 24;

		String returnString = milliSecondsPart + "ms";
		if (secondsPart > 0) {
			returnString = secondsPart + "s " + returnString;
		}
		if (minutesPart > 0) {
			returnString = minutesPart + "m " + returnString;
		}
		if (hoursPart > 0) {
			returnString = hoursPart + "h " + returnString;
		}
		if (days > 0) {
			returnString = days + "d " + returnString;
		}
		return returnString;
	}

	public static boolean checkTimeMatchesPattern(String pattern, Date time) {
		Pattern timePattern = Pattern.compile(pattern.replace("*", "."));
		String timeString = DateUtilities.HHMM.format(time);
		return timePattern.matcher(timeString).matches();
	}

	/**
	 * Remove the time part of a GregorianCalendar
	 *
	 * @param value
	 * @return
	 */
	public static GregorianCalendar getDayWithoutTime(GregorianCalendar value) {
		return new GregorianCalendar(value.get(Calendar.YEAR), value.get(Calendar.MONTH), value.get(Calendar.DAY_OF_MONTH));
	}

	/**
	 * Check if a day is included in a list of days
	 *
	 * @param listOfDays
	 * @param day
	 * @return
	 */
	public static boolean dayListIncludes(List<GregorianCalendar> listOfDays, GregorianCalendar day) {
		for (GregorianCalendar listDay : listOfDays) {
			if (listDay.get(Calendar.DAY_OF_YEAR) == day.get(Calendar.DAY_OF_YEAR)) {
				return true;
			}
		}
		return false;
	}
    
    public static Date parseUnknownDateFormat(String value) throws Exception {
		try {
			return DD_MM_YYYY_HH_MM_SS.parse(value);
		} catch (ParseException e1) {
			try {
				return DD_MM_YYYY_HH_MM.parse(value);
			} catch (ParseException e2) {
				try {
					return DD_MM_YYYY.parse(value);
				} catch (ParseException e3) {
					try {
						return YYYY_MM_DD_HH_MM.parse(value);
					} catch (ParseException e4) {
						try {
							return YYYYMMDDHHMMSS.parse(value);
						} catch (ParseException e5) {
							try {
								return DDMMYYYY.parse(value);
							} catch (ParseException e6) {
								throw new Exception("Unknown date format");
							}
						}
					}
				}
			}
		}
    }

    /**
     * Parse DateTime strings for SOAP Webservices (ISO 8601)
     * 
     * @param dateValue
     * @return
     * @throws Exception
     */
    public static Date parseIso8601DateTimeString(String dateValue) throws Exception {
    	if (Utilities.isBlank(dateValue)) {
    		return null;
    	}
    	
    	dateValue = dateValue.toUpperCase();
    	
    	if (dateValue.endsWith("Z")) {
    		// Standardize UTC time
    		dateValue = dateValue.replace("Z", "+00:00");
    	}
    	
    	boolean hasTimezone = false;
    	if (dateValue.length() > 6 && dateValue.charAt(dateValue.length() - 3) == ':' && (dateValue.charAt(dateValue.length() - 6) == '+' || dateValue.charAt(dateValue.length() - 6) == '-')) {
    		hasTimezone = true;
    	}
    	
    	if (dateValue.contains("T")) {
    		// Date with time
    		if (hasTimezone) {
    			return ISO_8601_DATETIME_FORMAT.parse(dateValue);
    		} else {
    			return ISO_8601_DATETIME_FORMAT_NO_TIMEZONE.parse(dateValue);
    		}
    	} else {
    		// Date only
    		if (hasTimezone) {
    			return ISO_8601_DATE_FORMAT.parse(dateValue);
    		} else {
    			return ISO_8601_DATE_FORMAT_NO_TIMEZONE.parse(dateValue);
    		}
    	}
    }
}
