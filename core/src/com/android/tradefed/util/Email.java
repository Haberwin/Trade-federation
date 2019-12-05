/*
 * Copyright (C) 2010 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.tradefed.util;

import com.android.ddmlib.Log;
import com.android.tradefed.log.LogUtil.CLog;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

/**
 * A helper class to send an email.  Note that this class is NOT PLATFORM INDEPENDENT.  It will
 * likely fail on Windows, and possibly on Mac OS X as well.  It will fail on any machine where
 * The binary pointed at by the {@code mailer} constant doesn't exist.
 */
public class Email implements IEmail {
    private static final String LOG_TAG = "Email";
    private static final String[] mailer = {"/usr/sbin/sendmail", "-t", "-i"};
    static final String CRLF = "\r\n";

    private static String join(Collection<String> list, String sep) {
        StringBuilder builder = new StringBuilder();
        Iterator<String> iter = list.iterator();
        while (iter.hasNext()) {
            String element = iter.next();
            builder.append(element);
            if(iter.hasNext()) {
                builder.append(sep);
            }
        }
        return builder.toString();
    }

    /**
     * A helper method to use ProcessBuilder to create a new process.  This can't use
     * {@link com.android.tradefed.util.IRunUtil} because that class doesn't provide a way to pass
     * data to the stdin of the spawned process, which is the usage paradigm for most commandline
     * mailers such as mailx and sendmail.
     * <p/>
     * Exposed for mocking
     *
     * @param cmd The {@code String[]} to pass to the {@link ProcessBuilder} constructor
     * @return The {@link Process} returned from from {@link ProcessBuilder#start()}
     * @throws IOException if sending email failed in a synchronously-detectable way
     */
    Process run(String[] cmd) throws IOException {
        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.redirectErrorStream(true);
        return pb.start();
    }

    /**
     * A small helper function that adds the specified header to the header list only if the value
     * is non-null
     */
    private void addHeader(List<String> headers, String name, String value) {
        if (name == null || value == null) return;
        headers.add(String.format("%s: %s", name, value));
    }

    /**
     * A small helper function that adds the specified header to the header list only if the value
     * is non-null
     */
    private void addHeaders(List<String> headers, String name, Collection<String> values) {
        if (name == null || values == null) return;
        if (values.isEmpty()) return;

        final String strValues = join(values, ",");
        headers.add(String.format("%s: %s", name, strValues));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void send(Message msg) throws IllegalArgumentException, IOException {
        // Sanity checks
        if (msg.getTo() == null) {
            throw new IllegalArgumentException("Message is missing a destination");
        } else if (msg.getSubject() == null) {
            throw new IllegalArgumentException("Message is missing a subject");
        } else if (msg.getBody() == null) {
            throw new IllegalArgumentException("Message is missing a body");
        }

        // Sender, Recipients, CC, BCC, Subject are all set with appropriate email headers
        final ArrayList<String> headers = new ArrayList<String>();
        final String[] mailCmd;
        if (msg.getSender() != null) {
            addHeader(headers, "From", msg.getSender());

            // Envelope Sender (will receive any errors related to the email)
            int cmdLen = mailer.length + 2; //change bu liuwenhua
            mailCmd = Arrays.copyOf(mailer, cmdLen);
            mailCmd[cmdLen - 2] = "-f";
            mailCmd[cmdLen - 1] = msg.getSender();
        } else {
            mailCmd = mailer;
        }
        addHeaders(headers, "To", msg.getTo());
        addHeaders(headers, "Cc", msg.getCc());
        addHeaders(headers, "Bcc", msg.getBcc());
        addHeader(headers, "Content-type", msg.getContentType());
        addHeader(headers, "Subject", msg.getSubject());
        CLog.d("liuwenhua:Check annex: %s",
                msg.getAnnex());
        //addHeader(headers,"uuencode",msg.getAnnex()); //add by liuwenhua
        //headers.add(String.format(";uuencode %s TestReport.html",msg.getAnnex()));
        /*
        mailCmd[cmdLen - 2] = "-a";
        mailCmd[cmdLen - 1] = msg.getAnnex();
        */
        final StringBuilder fullMsg = new StringBuilder();
        fullMsg.append(join(headers, CRLF));
        fullMsg.append(CRLF);
        fullMsg.append(CRLF);
        //fullMsg.append(msg.getBody());
        CLog.i("liuwenhua: Send Email: %s",
                fullMsg);
        CLog.d("About to send email with command: %s",
                Arrays.toString(mailCmd));

        Log.d(LOG_TAG, String.format("About to send email with command: %s",
                Arrays.toString(mailCmd)));


        //String[] uuencode= {"uuencode",msg.getAnnex(),"TestReport.html"};
        //Process uuencodeProc=run(uuencode);



        Process mailerProc = run(mailCmd);

        //PipedInputStream uuencodeStdin = new PipedInputStream();
        //PipedOutputStream pipeOut=new PipedOutputStream();
        //BufferedInputStream uuencodeStdin = new BufferedInputStream(uuencodeProc.getInputStream());
        BufferedOutputStream mailerStdin = new BufferedOutputStream(mailerProc.getOutputStream());
        //BufferedReader br = new BufferedReader(uuencodeStdin);

        /* There is no such thing as a "character" in the land of the shell; there are only bytes.
         * Here, we convert the body from a Java string (consisting of characters) to a byte array
         * encoding each character with UTF-8.  Each character will be represented as between one
         * and four bytes apiece.
         */
        mailerStdin.write(fullMsg.toString().getBytes("UTF-8"));
        //mailerStdin.flush();

        File fileAttach=new File(msg.getAnnex());
        BufferedInputStream fileInput = new BufferedInputStream(new FileInputStream(fileAttach));
        StringBuilder strb = new StringBuilder();
        byte[] bytearray = new byte[1024];
        int length = 0;
        while((length =fileInput.read(bytearray)) != -1) {
            mailerStdin.write(bytearray);
            strb.append(new String(bytearray,0,length));
            if(fileInput.available()<1024){
                bytearray=null;
                bytearray=new byte[1024];
            }

        }
      //mailerStdin.write(strb.toString().getBytes());
        fileInput.close();
        mailerStdin.flush();

        /*
        try {
            uuencodeStdin.wait(3000);
        } catch (InterruptedException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
        */
        //uuencodeStdin.close();
        //CLog.d("liuwenhua:check mail: \n%s",strb.toString());




        mailerStdin.close();

        int retValue;
        try {
            retValue = mailerProc.waitFor();
            CLog.i("liuwenhua: Send Email success!");
        } catch (InterruptedException e) {
            // ignore, but set retValue to something bogus
            retValue = -12345;
        }
        if (retValue != 0) {
            Log.e(LOG_TAG, String.format("Mailer finished with non-zero return value: %d", retValue));
            BufferedInputStream mailerStdout = new BufferedInputStream(mailerProc.getInputStream());
            StringBuilder stdout = new StringBuilder();
            int theByte;
            while((theByte = mailerStdout.read()) != -1) {
                stdout.append((char)theByte);
            }
            Log.e(LOG_TAG, "Mailer output was: " + stdout.toString());
        } else {
            Log.i(LOG_TAG, "Mailer returned successfully.");
        }
    }
}

