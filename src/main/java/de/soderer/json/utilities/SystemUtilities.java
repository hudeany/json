package de.soderer.json.utilities;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.lang.management.ManagementFactory;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.crypto.Cipher;

public class SystemUtilities {
	private static final boolean isWindows = getOsName().toLowerCase().contains("windows");
	private static final boolean isLinux = getOsName().toLowerCase().contains("unix") || getOsName().toLowerCase().contains("linux");

	public static String getOsName() {
		return System.getProperty("os.name");
	}

	public static boolean isWindowsSystem() {
		return isWindows;
	}

	public static boolean isLinuxSystem() {
		return isLinux;
	}

	public static int getProcessId() {
		final String data = ManagementFactory.getRuntimeMXBean().getName();
		if (data.contains("@")) {
			return Integer.parseInt(data.substring(0, data.indexOf("@")));
		} else if (isLinuxSystem()) {
			return Integer.parseInt(data);
		} else {
			return -1;
		}
	}

	public static String getJavaBinPath() {
		final String javaHomePath = System.getProperty("java.home");
		if (javaHomePath == null) {
			return null;
		} else {
			File javaBinFile;
			if (SystemUtilities.isLinuxSystem()) {
				javaBinFile = new File(javaHomePath + File.separator + "bin" + File.separator + "java");
			} else if (System.console() == null) {
				javaBinFile = new File(javaHomePath + File.separator + "bin" + File.separator + "java.exe");
			} else {
				javaBinFile = new File(javaHomePath + File.separator + "bin" + File.separator + "javaw.exe");
			}

			if (javaBinFile.exists() && !javaBinFile.isDirectory()) {
				return javaBinFile.getAbsolutePath();
			} else {
				return null;
			}
		}
	}

	public static long getUptimeInMillis() {
		return ManagementFactory.getRuntimeMXBean().getUptime();
	}

	public static Date getStarttime() {
		return new Date(ManagementFactory.getRuntimeMXBean().getStartTime());
	}

	public static List<String> getJavaStartupArguments() {
		return ManagementFactory.getRuntimeMXBean().getInputArguments();
	}

	public static List<String> getProcessListRaw() {
		try {
			final List<String> data = new ArrayList<>();
			Process p;
			if (getOsName().toLowerCase().contains("windows")) {
				p = Runtime.getRuntime().exec(new String[] {System.getenv("windir") + "\\system32\\" + "tasklist.exe"});
			} else {
				p = Runtime.getRuntime().exec(new String[] {"ps", "-e"});
			}
			try (BufferedReader input = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
				String line;
				while ((line = input.readLine()) != null) {
					data.add(line);
				}
			}
			return data;
		} catch (final Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	public static boolean isUnlimitedKeyStrengthAllowed() {
		try {
			return Cipher.getMaxAllowedKeyLength("AES") == Integer.MAX_VALUE;
		} catch (@SuppressWarnings("unused") final NoSuchAlgorithmException e) {
			return false;
		}
	}
}
