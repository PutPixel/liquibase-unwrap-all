package com.github.putpixel.unwrap;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.stream.Collectors;

import org.apache.maven.cli.MavenCli;
import org.junit.BeforeClass;
import org.junit.Test;

public class IncludeAllUnwrapMojoTestManual {

	private static final String TEST_PROJECT_DIR = "src/test/resources/unit/";
	private static final String RESULTING_XML = "target/classes/db/changelog/db.changelog-master.xml";
	private static final String SHOULD_BE_XML = "src/main/resources/db/changelog/db.changelog-master-should-be.xml";

	@BeforeClass
	public static void beforeClass() {
		System.setProperty("maven.multiModuleProjectDirectory", TEST_PROJECT_DIR);
		exec("clean", "install");
	}

	private static void exec(String... args) {
		MavenCli cli = new MavenCli();
		int result = cli.doMain(args, TEST_PROJECT_DIR, System.out, System.err);
		assertThat(result, is(0));
	}

	@Test
	public void checkWithUnwrap() throws Exception {
		checkProject("project-to-test-with-unwrap");
	}

	private void checkProject(String project) throws IOException {
		String prefix = TEST_PROJECT_DIR + project + "/";
		File resultingXml = new File(prefix + RESULTING_XML);
		assertThat(resultingXml.exists(), is(true));

		File shouldBe = new File(prefix + SHOULD_BE_XML);
		assertThat(shouldBe.exists(), is(true));

		String resultingLines = Files.readAllLines(resultingXml.toPath(), Charset.defaultCharset()).stream().collect(Collectors.joining("\n"));
		String shouldBeLines = Files.readAllLines(shouldBe.toPath(), Charset.defaultCharset()).stream().collect(Collectors.joining("\n"));

		assertThat(resultingLines.replaceAll("\\s", ""), is(shouldBeLines.replaceAll("\\s", "")));
	}

	@Test
	public void checkWithNoUnwrap() throws Exception {
		checkProject("project-to-test-no-unwrap");
	}

	@Test
	public void checkNested() throws Exception {
		checkProject("project-to-test-with-unwrap-with-nested-folders");
	}

}
