package de.soderer.utilities.json;

import org.junit.Assert;
import org.junit.Test;

import de.soderer.utilities.json.compare.DirectoryHashJsonCompare;

@SuppressWarnings("static-method")
public class DirectoryHashJsonCompareTest {
	@Test
	public void testFileOk() {
		final JsonObject jsonObjectLeft = new JsonObject();
		jsonObjectLeft.add("name", "TestFile");
		jsonObjectLeft.add("isDirectory", false);
		jsonObjectLeft.add("hash", "499135070EE5D0D84F82C6DCB01A6797");
		jsonObjectLeft.add("size", 11781944046l);

		final JsonObject jsonObjectRight = new JsonObject();
		jsonObjectRight.add("name", "TestFile");
		jsonObjectRight.add("isDirectory", false);
		jsonObjectRight.add("hash", "499135070EE5D0D84F82C6DCB01A6797");
		jsonObjectRight.add("size", 11781944046l);

		final JsonObject compareResultJson = new DirectoryHashJsonCompare(
				jsonObjectLeft,
				jsonObjectRight).compare();

		Assert.assertNull(compareResultJson);
	}

	@Test
	public void testDirectoryOk() {
		final JsonObject jsonObjectLeft = new JsonObject();
		jsonObjectLeft.add("name", "TestDir");
		jsonObjectLeft.add("isDirectory", true);
		jsonObjectLeft.add("hash", "499135070EE5D0D84F82C6DCB01A6797");
		jsonObjectLeft.add("size", 11781944046l);
		jsonObjectLeft.add("directories", 2);
		jsonObjectLeft.add("files", 3);
		jsonObjectLeft.add("childs", null);

		final JsonObject jsonObjectRight = new JsonObject();
		jsonObjectRight.add("name", "TestDir");
		jsonObjectRight.add("isDirectory", true);
		jsonObjectRight.add("hash", "499135070EE5D0D84F82C6DCB01A6797");
		jsonObjectRight.add("size", 11781944046l);
		jsonObjectRight.add("directories", 2);
		jsonObjectRight.add("files", 3);
		jsonObjectRight.add("childs", null);

		final JsonObject compareResultJson = new DirectoryHashJsonCompare(
				jsonObjectLeft,
				jsonObjectRight).compare();

		Assert.assertNull(compareResultJson);
	}

	@Test
	public void testDirectoryWithDifferentHashes() {
		final JsonObject jsonObjectLeft = new JsonObject();
		jsonObjectLeft.add("name", "TestDir");
		jsonObjectLeft.add("isDirectory", true);
		jsonObjectLeft.add("hash", "499135070EE5D0D84F82C6DCB01A6797");
		jsonObjectLeft.add("size", 11781944046l);
		jsonObjectLeft.add("directories", 2);
		jsonObjectLeft.add("files", 3);
		jsonObjectLeft.add("childs", null);

		final JsonObject jsonObjectRight = new JsonObject();
		jsonObjectRight.add("name", "TestDir");
		jsonObjectRight.add("isDirectory", true);
		jsonObjectRight.add("hash", "499135070EE5D0D84F82C6DCB01A6798");
		jsonObjectRight.add("size", 11781944046l);
		jsonObjectRight.add("directories", 2);
		jsonObjectRight.add("files", 3);
		jsonObjectRight.add("childs", null);

		final JsonObject compareResultJson = new DirectoryHashJsonCompare(
				jsonObjectLeft,
				jsonObjectRight).compare();

		Assert.assertEquals("{\n"
				+ "\t\"differences\":\n"
				+ "\t\t[\n"
				+ "\t\t\t\": jsonObjectLeft hash is '499135070EE5D0D84F82C6DCB01A6797' but jsonObjectRight hash is '499135070EE5D0D84F82C6DCB01A6798'\"\n"
				+ "\t\t]\n"
				+ "}", compareResultJson.toString());
	}

	@Test
	public void testDirectoryWithDifferentFiles() {
		final JsonObject jsonObjectfile = new JsonObject();
		jsonObjectfile.add("name", "TestFile1");
		jsonObjectfile.add("isDirectory", false);
		jsonObjectfile.add("hash", "499135070EE5D0D84F82C6DCB01A6797");
		jsonObjectfile.add("size", 111);

		final JsonObject jsonObjectfile2_1 = new JsonObject();
		jsonObjectfile2_1.add("name", "TestFile2");
		jsonObjectfile2_1.add("isDirectory", false);
		jsonObjectfile2_1.add("hash", "499135070EE5D0D84F82C6DCB01A6798");
		jsonObjectfile2_1.add("size", 112);

		final JsonObject jsonObjectfile2_2 = new JsonObject();
		jsonObjectfile2_2.add("name", "TestFile2");
		jsonObjectfile2_2.add("isDirectory", false);
		jsonObjectfile2_2.add("hash", "499135070EE5D0D84F82C6DCB01A6799");
		jsonObjectfile2_2.add("size", 113);

		final JsonArray jsonArrayLeft = new JsonArray();
		jsonArrayLeft.add(jsonObjectfile);
		jsonArrayLeft.add(jsonObjectfile2_1);

		final JsonArray jsonArrayRight = new JsonArray();
		jsonArrayRight.add(jsonObjectfile2_2);
		jsonArrayRight.add(jsonObjectfile);

		final JsonObject jsonObjectLeft = new JsonObject();
		jsonObjectLeft.add("name", "TestDir");
		jsonObjectLeft.add("isDirectory", true);
		jsonObjectLeft.add("hash", null);
		jsonObjectLeft.add("size", 11781944046l);
		jsonObjectLeft.add("directories", 0);
		jsonObjectLeft.add("files", 2);
		jsonObjectLeft.add("childs", jsonArrayLeft);

		final JsonObject jsonObjectRight = new JsonObject();
		jsonObjectRight.add("name", "TestDir");
		jsonObjectRight.add("isDirectory", true);
		jsonObjectRight.add("hash", null);
		jsonObjectRight.add("size", 11781944046l);
		jsonObjectRight.add("directories", 0);
		jsonObjectRight.add("files", 2);
		jsonObjectRight.add("childs", jsonArrayRight);

		final JsonObject compareResultJson = new DirectoryHashJsonCompare(
				jsonObjectLeft,
				jsonObjectRight).compare();

		Assert.assertEquals("{\n"
				+ "\t\"differences\":\n"
				+ "\t\t[\n"
				+ "\t\t\t{\n"
				+ "\t\t\t\t\"leftItemIndex\": 1,\n"
				+ "\t\t\t\t\"name\": \"TestFile2\",\n"
				+ "\t\t\t\t\"differences\":\n"
				+ "\t\t\t\t\t[\n"
				+ "\t\t\t\t\t\t\"\\/TestDir: jsonObjectLeft hash is '499135070EE5D0D84F82C6DCB01A6798' but jsonObjectRight hash is '499135070EE5D0D84F82C6DCB01A6799'\",\n"
				+ "\t\t\t\t\t\t\"\\/TestDir: jsonObjectLeft size is '112' but jsonObjectRight size is '113'\"\n"
				+ "\t\t\t\t\t]\n"
				+ "\t\t\t}\n"
				+ "\t\t]\n"
				+ "}", compareResultJson.toString());
	}
}
