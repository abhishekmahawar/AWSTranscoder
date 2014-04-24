import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import com.amazonaws.auth.ClasspathPropertiesFileCredentialsProvider;
import com.amazonaws.services.elastictranscoder.AmazonElasticTranscoderClient;
import com.amazonaws.services.elastictranscoder.model.CreateJobOutput;
import com.amazonaws.services.elastictranscoder.model.CreateJobRequest;
import com.amazonaws.services.elastictranscoder.model.JobInput;
import com.amazonaws.services.elastictranscoder.model.ReadJobRequest;
import com.amazonaws.services.elastictranscoder.model.ReadJobResult;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.S3Object;


public class TranscoderTest {
	private static AmazonS3Client s3 = new AmazonS3Client(new ClasspathPropertiesFileCredentialsProvider());
	private static AmazonElasticTranscoderClient transcoder = new AmazonElasticTranscoderClient(new ClasspathPropertiesFileCredentialsProvider());
	private static String SOURCE_BUCKET = "srcBucketNameGoesHere";
	private static String TARGET_BUCKET = "targetBucketNameGoesHere";
	private static String INPUT_KEY = "inputKeyGoesHere";
	private static String OUTPUT_KEY = "outputKeyGoesHere";
	
	public static void main(String[] args) throws IOException {
		upload();
		String jobId = transcode();
		download(jobId);
		
	}

	private static void upload() {
		System.out.println("Uploading the file to be transcoded to " + SOURCE_BUCKET + " bucket.");
		s3.putObject(new PutObjectRequest(SOURCE_BUCKET, INPUT_KEY, new File("C:\\mediafile.wmv")));
		System.out.println("Upload completed.");
	}
	
	private static String transcode() {
		CreateJobRequest jobReq = new CreateJobRequest();
		jobReq.setPipelineId("pipelineIdGoesHere");//created pipeline must have name, roleARN and notifications set
		JobInput jobInput = new JobInput();
		jobInput.setContainer("auto");
		jobInput.setKey(INPUT_KEY);
		jobReq.setInput(jobInput);
		CreateJobOutput jobOutput = new CreateJobOutput();
		jobOutput.setPresetId("presetIdGoesHere");//can use already created presets
		jobOutput.setKey(OUTPUT_KEY);
		jobReq.setOutput(jobOutput);
		return transcoder.createJob(jobReq).getJob().getId();
	}


	private static void download(String jobId) throws IOException {
		ReadJobRequest jobReq = new ReadJobRequest().withId(jobId);
		ReadJobResult res = transcoder.readJob(jobReq);
		while("Submitted".equals(res.getJob().getStatus()) || "Progressing".equals(res.getJob().getStatus())) {
			res = transcoder.readJob(jobReq);
		}
		if("Complete".equals(res.getJob().getStatus())) {
			System.out.println("Downloading the transcoded file.");
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
	        byte[] buf = new byte[1024];
	        try {
	        	S3Object transcodedFile = s3.getObject(new GetObjectRequest(TARGET_BUCKET, OUTPUT_KEY));
	            for (int readNum; (readNum = transcodedFile.getObjectContent().read(buf)) != -1;) {
	                bos.write(buf, 0, readNum);
	            }
	        } catch (IOException ex) {
	            ex.printStackTrace();
	        }
	        byte[] bytes = bos.toByteArray();
	        File outputFile = new File("F:\\downloaded");
	        FileOutputStream fos = new FileOutputStream(outputFile);
	        fos.write(bytes);
	        fos.flush();
	        fos.close();
			System.out.println("Download completed.");
		} else {
			System.out.println("Download failed as job failed.");
		}
	}
}