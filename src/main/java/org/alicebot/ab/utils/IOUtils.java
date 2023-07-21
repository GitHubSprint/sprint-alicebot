package org.alicebot.ab.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import org.alicebot.ab.MagicStrings;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class IOUtils {
	private static final Logger log = LoggerFactory.getLogger(IOUtils.class);

	public static String readInputTextLine() {
        
            String textLine = null;
            try {
                BufferedReader lineOfText = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));
                textLine = lineOfText.readLine();
            } catch (IOException e) {
                    e.printStackTrace();
            }
            return textLine;
	}


	public static String system(String evaluatedContents, String failedString) {
		Runtime rt = Runtime.getRuntime();
        log.info("System {}", evaluatedContents);
        try {
            Process p = rt.exec(evaluatedContents);
            InputStream istrm = p.getInputStream();
            InputStreamReader istrmrdr = new InputStreamReader(istrm, StandardCharsets.UTF_8);
            BufferedReader buffrdr = new BufferedReader(istrmrdr);
            StringBuilder result = new StringBuilder();
            String data = "";
            while ((data = buffrdr.readLine()) != null) {
                result.append(data).append("\n");
            }
            log.info("Result = {}", result.toString());
            return result.toString();
        } catch (Exception ex) {
            ex.printStackTrace();
            return failedString;
        }
	}
}

