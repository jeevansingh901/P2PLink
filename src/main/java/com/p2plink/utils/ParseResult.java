package com.p2plink.utils;

public class ParseResult {
    private  final String fileName;
    private  final byte[] fileContent;
    private final  String contentType;

    public ParseResult(String fileName, byte[] fileContent, String contentType) {
        this.fileName = fileName;
        this.fileContent = fileContent;
        this.contentType = contentType;
    }

    public static int findSequence(byte[] data, byte[] sequence,int startPos) {
        outer:
             for(int i=startPos;i<=data.length-sequence.length;i++){

                 for(int j=0;j<sequence.length;j++){
                     if(data[i+j] !=sequence[j]){
                       continue outer;
                     }
                 }
               return i;
             }
             return -1;
    }

    public String getFileName() {
        return fileName;
    }

    public byte[] getFileContent() {
        return fileContent;
    }

    public String getContentType() {
        return contentType;
    }
}
