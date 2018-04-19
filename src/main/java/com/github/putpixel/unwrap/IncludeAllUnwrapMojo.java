package com.github.putpixel.unwrap;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.maven.model.Resource;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.google.common.collect.ImmutableList;

@Mojo(name = "unwrap", defaultPhase = LifecyclePhase.PROCESS_RESOURCES)
public class IncludeAllUnwrapMojo extends AbstractMojo {

	@Parameter(defaultValue = "${project}", readonly = true, required = true)
	private MavenProject project;

	@Parameter(property = "changes.log.master.location", required = true)
	private String changesLogMasterLocation;

	public void execute() throws MojoExecutionException {
		getLog().info("Start unwrapping includeAll for " + changesLogMasterLocation);

		List<Resource> resources = project.getBuild().getResources();
		for (Resource resource : resources) {
			processResourceFolder(resource);
		}
	}

	private void processResourceFolder(Resource resourceFolder) throws MojoExecutionException {
		String resourceDirectory = resourceFolder.getDirectory();
		File file = new File(resourceDirectory, changesLogMasterLocation);
		if (!file.exists()) {
			return;
		}

		getLog().info("Processing " + file.getAbsolutePath());

		try {
			DocumentBuilder dBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
			Document doc = dBuilder.parse(file);
			doc.getDocumentElement().normalize();

			NodeList nList = doc.getElementsByTagName("includeAll");
			for (int counter = 0; counter < nList.getLength(); counter++) {
				counter = processNode(resourceFolder, doc, nList, counter);
			}
			writeToFile(resourceFolder, doc);
		} catch (Exception e) {
			throw new MojoExecutionException("Failed to process document", e);
		}
	}

	private int processNode(Resource resourceFolder, Document doc, NodeList nList, int counter) {
		Node nNode = nList.item(counter);
		if (nNode.getNodeType() == Node.ELEMENT_NODE) {
			Element eElement = (Element) nNode;
			String path = eElement.getAttribute("path");
			checkNotNull(path);
			getLog().info("Processing <includeAll path=" + path + " />");

			Node parentNode = nNode.getParentNode();
			generateAndAddNodesFromPath(resourceFolder, nNode, parentNode, path, doc);
			parentNode.removeChild(nNode);
			counter--;
		}
		return counter;
	}

	private void generateAndAddNodesFromPath(Resource resourceFolder, Node insertBeforeThisNode, Node parentNode, String path, Document doc) {
		String resourceFolderDirectory = resourceFolder.getDirectory();
		File dir = new File(resourceFolderDirectory, path);
		generateAndAddNodesFromDir(insertBeforeThisNode, parentNode, doc, resourceFolderDirectory, dir);
	}

	private void generateAndAddNodesFromDir(Node insertBeforeThisNode, Node parentNode, Document doc, String resourceFolderDirectory, File dirToProcess) {
		checkState(dirToProcess.isDirectory(), "Given path is not a dir: " + dirToProcess.getAbsolutePath());

		getLog().info("Processing dir with files: " + dirToProcess.getAbsolutePath());

		List<File> files = getFilesInAlphabeticalOrder(dirToProcess);
		for (File file : files) {
			if (file.isFile()) {
				Element element = doc.createElement("include");
				element.setAttribute("file", computePath(resourceFolderDirectory, file));
				parentNode.insertBefore(element, insertBeforeThisNode);
			}
		}

		for (File newDir : files) {
			if (newDir.isDirectory()) {
				generateAndAddNodesFromDir(insertBeforeThisNode, parentNode, doc, resourceFolderDirectory, newDir);
			}
		}
	}

	private List<File> getFilesInAlphabeticalOrder(File dirToProcess) {
		File[] files = dirToProcess.listFiles();
		if (files == null) {
			return ImmutableList.of();
		}
		return Arrays.stream(files).sorted().collect(Collectors.toList());
	}

	private String computePath(String directory, File file) {
		String newPath = file.getAbsolutePath().replace(directory, "").replace('\\', '/');
		if (newPath.startsWith("/") || newPath.startsWith("\\")) {
			newPath = newPath.substring(1);
		}
		return newPath;
	}

	private void writeToFile(Resource resource, Document doc) throws TransformerException {
		DOMSource source = new DOMSource(doc);
		StreamResult result = new StreamResult(computeTarget(resource));

		TransformerFactory transformerFactory = TransformerFactory.newInstance();
		Transformer transformer = transformerFactory.newTransformer();
		transformer.setOutputProperty(OutputKeys.INDENT, "yes");
		transformer.transform(source, result);
	}

	private File computeTarget(Resource resource) {
		String targetPath = resource.getTargetPath();
		if (targetPath == null) {
			targetPath = project.getBuild().getOutputDirectory();
		}
		return new File(targetPath, changesLogMasterLocation);
	}
}
