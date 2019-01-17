package com.fit2cloud.codedeploy2;

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.RegionUtils;
import com.amazonaws.services.s3.AmazonS3Client;
import hudson.Util;
import hudson.model.Run;
import hudson.model.BuildListener;
import hudson.model.TaskListener;

import java.io.IOException;
import java.util.Map;

/**
 * Created by zhangbohan on 15/9/25.
 */
public class Utils {
    public static final String FWD_SLASH = "/";

    public static boolean isNullOrEmpty(final String name) {
        boolean isValid = false;
        if (name == null || name.matches("\\s*")) {
            isValid = true;
        }
        return isValid;
    }

    public static boolean isNumber(final String name) {
        boolean isNumber = false;
        if (name == null || name.matches("[0-9]+")) {
            isNumber = true;
        }
        return isNumber;
    }

    public static AmazonS3Client getAvaiableS3Client(String accessKey, String secretKey) {
        try {
            AmazonS3Client client = new AmazonS3Client(new BasicAWSCredentials(accessKey, secretKey));
            client.setRegion(RegionUtils.getRegion("cn-north-1"));
            client.getS3AccountOwner();
            return client;
        } catch (Exception e) {
            try {
                AmazonS3Client client = new AmazonS3Client(new BasicAWSCredentials(accessKey, secretKey));
                client.getS3AccountOwner();
                return client;
            } catch (Exception e1) {
                e1.printStackTrace();
                return null;
            }
        }
    }

    public static String replaceTokens(Run<?, ?> build,
                                       TaskListener listener, String text) throws IOException, InterruptedException {
        String newText = null;
        if (!isNullOrEmpty(text)) {
            Map<String, String> envVars = build.getEnvironment(listener);
            newText = Util.replaceMacro(text, envVars);
        }
        return newText;
    }



}
