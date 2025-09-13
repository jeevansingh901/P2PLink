package com.p2plink.parser;

import com.p2plink.utils.ParseResult;

import static com.p2plink.utils.ParseResult.findSequence;

public class Multiparser {

    private final byte[] data;
    private final String boundary;
    public Multiparser(byte[] data, String boundary) {
      this.data = data;
      this.boundary = boundary;
    }

    public byte[] getData() {
      return data;
    }
    public String getBoundary() {
      return boundary;
    }

    public ParseResult parse(){
        try {
            String dataAsString = new String(this.data, "UTF-8");
            String fileNameMarker="filename=\"";
            int fileNameStart = dataAsString.indexOf(fileNameMarker);
            if (fileNameStart == -1){
                return null;
            }
            int fileNameEnd=dataAsString.indexOf("\"",fileNameStart+fileNameMarker.length());
            String fileName = dataAsString.substring(fileNameStart + fileNameMarker.length(), fileNameEnd);
            fileName = fileName.replaceAll("[\\\\/:*?\"<>|]", "_");

            String  contentTypeMarker="Content-Type: ";
            String contentType="application/octet-stream";
            int contentTypeStart = dataAsString.indexOf(contentTypeMarker,fileNameEnd);
            if (contentTypeStart != -1){
                contentTypeStart=contentTypeStart + contentTypeMarker.length();
                int contentTypeEnd=dataAsString.indexOf("\r\n",contentTypeStart);
                contentType=dataAsString.substring(contentTypeStart,contentTypeEnd);

            }
            String headerEndMarker="\r\n\r\n";
            int headerEnd = dataAsString.indexOf(headerEndMarker);
            if (headerEnd == -1){
                return null;
            }
            int contentStart=headerEnd + headerEndMarker.length();
            byte[] boundaryBytes = ("\r\n--"+boundary+"--").getBytes();
            int contentEnd=findSequence(data,boundaryBytes,contentStart);
            if (contentEnd == -1){
                boundaryBytes = ("\r\n--"+boundary).getBytes();
                contentEnd=findSequence(data,boundaryBytes,contentStart);
            }
            if (contentEnd == -1 || contentEnd<contentStart){
                return null;
            }
            byte[] fileContent=new byte[contentEnd-contentStart];
            System.arraycopy(data,contentStart,fileContent,0,fileContent.length);
            return new ParseResult(fileName,fileContent,contentType);

        }
        catch (Exception e){
            System.err.println("Multiparser Error: "+e.getMessage());
            return null;
        }
    }


}
