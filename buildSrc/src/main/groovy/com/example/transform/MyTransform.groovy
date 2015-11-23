package com.example.transform
import com.android.build.api.transform.Context
import com.android.build.api.transform.DirectoryInput
import com.android.build.api.transform.Format
import com.android.build.api.transform.JarInput
import com.android.build.api.transform.QualifiedContent
import com.android.build.api.transform.QualifiedContent.ContentType
import com.android.build.api.transform.QualifiedContent.Scope
import com.android.build.api.transform.Transform
import com.android.build.api.transform.TransformException
import com.android.build.api.transform.TransformInput
import com.android.build.api.transform.TransformOutputProvider
import com.google.common.collect.ImmutableSet
import com.google.common.collect.Sets
import org.apache.commons.io.FileUtils
import java.util.Map
import java.util.HashMap

/**
* Prints out some information and copies inputs to outputs.
*/
public class MyTransform extends Transform {

	public static final int PASS_THROUGH_INPUT_NAMES = 1;
	public static final int PASS_THROUGH_INPUT_NAMES_W_PATH = 2;
	public static final int GUARANTEE_UNIQUE_NAMES = 3;

	private int type;
	private boolean printNames, printCopyPath;
	
	public MyTransform(int type) {
		this(type, true, true)
	}
	
	public MyTransform(int type, boolean printNames, boolean printCopyPath) {
		this.type=type;
		this.printNames = printNames;
		this.printCopyPath = printCopyPath;
	}

	@Override
	public String getName() {
		return "MyTransform_"+type;
	}

	@Override
	public Set<ContentType> getInputTypes() {
		return ImmutableSet.<ContentType>of(QualifiedContent.DefaultContentType.CLASSES);
	}
	
	@Override
	public Set<Scope> getScopes() {
		return Sets.immutableEnumSet(Scope.PROJECT, Scope.PROJECT_LOCAL_DEPS, Scope.EXTERNAL_LIBRARIES);
	}


	@Override
	public boolean isIncremental() {
		return false;
	}

	@Override
	public void transform(Context context, Collection<TransformInput> inputs,
			Collection<TransformInput> referencedInputs,
			TransformOutputProvider outputProvider, boolean isIncremental)
			throws IOException, TransformException, InterruptedException {
		Map<String, Integer> nameMap = new HashMap<String, Integer>()
		if (printNames || printCopyPath) {
			print("\n\n***********START*************");
		}
		if (printNames) {
		print("Type: "+type);
		print("Path: "+context.getPath());
		print("isIncremental="+isIncremental);
		printTransformInputs(inputs);
		}
		for (TransformInput input: inputs)
		{
			for (DirectoryInput dirInput: input.directoryInputs) {
				File dest = outputProvider.getContentLocation(dirInput.name, dirInput.contentTypes, dirInput.scopes, Format.DIRECTORY);
				if (printCopyPath) {
					print("Copying "+dirInput.name+" to "+dest.absolutePath)
				}
				FileUtils.copyDirectory(dirInput.getFile(), dest);
			}
			for (JarInput jarInput: input.jarInputs) {
			    def src = jarInput.getFile()
				String jarName = jarInput.name;
				if (jarName.endsWith(".jar")) {
					jarName = jarName.substring(0, jarName.length()-4);
				}
				if (type == PASS_THROUGH_INPUT_NAMES_W_PATH) {
					String path =  src.absolutePath
					int idx = path.indexOf("exploded-aar")
					if (idx != -1) {
						jarName = path.substring(idx+13);
					}
				} else if (type == GUARANTEE_UNIQUE_NAMES) {
					if (nameMap.containsKey(jarName)) {
						int num = nameMap.get(jarName)
						num++
						nameMap.put(jarName, num)
						jarName += "-"+num
					} else {
						nameMap.put(jarName, 0);
					}
				}
				File dest = outputProvider.getContentLocation(jarName, jarInput.contentTypes, jarInput.scopes, Format.JAR);
				if (printCopyPath) {
					print("Copying "+jarName+" to "+dest.absolutePath)
				}
				FileUtils.copyFile(jarInput.getFile(), dest);
			}
		}
		if (printNames || printCopyPath) {
			print("\n************END**************\n");
		}
	}

	private void print(String s) {
		System.out.println(s);
	}
	
	private void printTransformInputs(Collection<TransformInput> inputs) {
		for (TransformInput input: inputs) {
			printTransformInput(input);
		}
	}

	private void printTransformInput(TransformInput input) {
		print("Inputs :")//+input);
		Collection<DirectoryInput> dirInputs = input.getDirectoryInputs();
		int i=0;
		print("Dir Inputs :")
		for (DirectoryInput dirInput: dirInputs) {
			printQualifiedContent(dirInput, false, i++)
			//print("Directory input["+(i++)+"]: "+ dirInput);
		}
		i=0;
		print("Jar Inputs :")
		Collection<JarInput> jarInputs = input.getJarInputs();
		for (JarInput jarInput: jarInputs) {
			printQualifiedContent(jarInput, true, i++)
			//print("Jar input["+(i++)+"]: "+ jarInput);
		}
	}
	
	private void printQualifiedContent(QualifiedContent content, boolean isJar, int num) {
		String line = isJar?"Jar":"Directory"
		line += " " +num
		line += " "+ content.name;
		line += " : " + content.file.absolutePath
		print(line)
	}

}