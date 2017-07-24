package edu.pitt.dbmi.birads.pathology;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import edu.pitt.dbmi.nlp.noble.mentions.NobleMentions;
import edu.pitt.dbmi.nlp.noble.mentions.model.AnnotationVariable;
import edu.pitt.dbmi.nlp.noble.mentions.model.Composition;
import edu.pitt.dbmi.nlp.noble.mentions.model.DomainOntology;
import edu.pitt.dbmi.nlp.noble.mentions.model.Instance;
import edu.pitt.dbmi.nlp.noble.ontology.IClass;
import edu.pitt.dbmi.nlp.noble.ontology.IOntologyException;
import edu.pitt.dbmi.nlp.noble.terminology.Annotation;
import edu.pitt.dbmi.nlp.noble.terminology.TerminologyException;

public class PathologyDiagnosisAnnotator {
	private static final String I = "\t";
	private static final String RESULT_FILE = "RESULT.tsv";
	

	public static void main(String[] args) throws Exception {
		String ontologyName = "breastPathologicDx.owl";
		String inputDirectory= "/input/text";
		String outputDirectory= "/output";
		String ontologyFile= "/data/"+ontologyName;
			
		
		if(args.length < 2){
			if(!new File(inputDirectory).exists()){
				System.err.println("Usage: java -jar PathologyDiagnosisAnnotator.jar <input report directory> <output resuls directory> ");
				return ;
			}
		}else {
			inputDirectory= args[0];
			outputDirectory= args[1];
		}
		
		// if ontology file is not there, use default location
		if(!new File(ontologyFile).exists()){
			ontologyFile = System.getProperty("user.home")+File.separator+".noble"+File.separator+"ontologies"+File.separator+ontologyName;
		}
		
		//String expertDirectory="/home/tseytlin/Data/BiRADS/gold/expert";
		
		// run to execute a model
		System.out.print("loading domain ontology "+ontologyFile+" .. ");
		DomainOntology domainOntology = new DomainOntology(ontologyFile);
		NobleMentions noble = new NobleMentions(domainOntology);
		System.out.println("ok");
		
		writeData(new File(outputDirectory,RESULT_FILE),"ID",Arrays.asList("Category"+I+"Diagnosis"+I+"Location"+I+"Side"+I+"O'Clock"+I+"Offsets"),false);
		File [] files = new File(inputDirectory).listFiles();
		Arrays.sort(files);
		for(File f: files){
			if(f.getName().endsWith(".txt")){
				
				// process with noble mentions
				List<String> lines = new ArrayList<String>();
				System.out.print("processing "+f.getName()+" .. ");
				Composition doc = noble.process(f);
				Set<String> vars = new HashSet<String>();
				for(AnnotationVariable var: doc.getAnnotationVariables()){
					for(Instance body: var.getModifierInstances("hasBodySite")){
						body.getInstance();
						
						String diagnosis = var.getAnchor().getLabel();
						String category  = getCategory(var.getConceptClass());
						String location = body.getLabel();
						String side = toString(body.getModifierInstances("hasLaterality"));
						String clock = toString(body.getModifierInstances("hasClockfacePosition"));
						String offsets = getOffsets(var);
						
						String l = category+I+diagnosis+I+location+I+side+I+clock;
						if(!vars.contains(l)){
							lines.add(l+I+offsets);
							vars.add(l);
						}
						
					}
				}
				
				
				// output 
				writeData(new File(outputDirectory,f.getName()),"Diagnosis",lines,false);
				writeData(new File(outputDirectory,RESULT_FILE),f.getName(),lines,true);
				
				System.out.println("ok");
			}
		}
	}
	
	
	private static String getOffsets(AnnotationVariable var) {
		List<String> pos = new ArrayList<String>();
		for(Annotation a: var.getAnnotations()){
			pos.add(a.getStartPosition()+":"+a.getEndPosition());
		}
		return toString(pos);
	}
	private static String toString(Collection obj){
		if(obj == null)
			return "";
		String s = obj.toString();
		return s.substring(1, s.length()-1);
	}
	
	
	private static String getCategory(IClass cls){
		IClass malignant = cls.getOntology().getClass("Malignant_breast_lesion_mention");
		IClass highrisk = cls.getOntology().getClass("High-risk_breast_lesion_mention");
		IClass benign = cls.getOntology().getClass("Benign_breast_lesion_mention");
		
		
		if(cls.equals(malignant) || cls.hasSuperClass(malignant))
			return "Malignant";
		if(cls.equals(highrisk) || cls.hasSuperClass(highrisk))
			return "High-Risk";
		if(cls.equals(benign) || cls.hasSuperClass(benign))
			return "Benign";
	/*	
		
		for(IClass parent: cls.getDirectSuperClasses()){
			return parent.getLabel();
		}*/
		return "";
	}
	
	
	private static void writeData(File file, String prefix, List<String> lines, boolean append) throws IOException{
		if(!file.getParentFile().exists())
			file.getParentFile().mkdirs();
		
		BufferedWriter writer = new BufferedWriter(new FileWriter(file, append));
		for(String s: lines){
			writer.write(prefix+I+s+"\n");
		}
		writer.close();
	}

}
